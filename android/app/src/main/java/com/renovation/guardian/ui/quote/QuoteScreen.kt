package com.renovation.guardian.ui.quote

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.data.db.QuotePlanEntity
import com.renovation.guardian.data.db.BudgetCategoryEntity
import com.renovation.guardian.ui.components.*
import com.renovation.guardian.ui.quote.engine.*
import com.renovation.guardian.domain.quote.*
import com.renovation.guardian.ui.quote.export.QuoteTextBuilder
import com.renovation.guardian.util.MoneyUtil
import com.renovation.guardian.util.LongImageRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteScreen(onBack: () -> Unit = {}, vm: QuoteViewModel = viewModel()) {
    val plans by vm.plans.collectAsState(emptyList())
    val busy by vm.isBusy.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var footerHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    LaunchedEffect(vm.editing, vm.state.wizardStep) {
        snackbar.currentSnackbarData?.dismiss()
        if (!vm.editing) footerHeight = 0
    }
    OperationFeedback(vm, snackbar)
    var discard by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<QuotePlanEntity?>(null) }
    var deleting by remember { mutableStateOf<QuotePlanEntity?>(null) }
    fun back() { if (!vm.editing) onBack() else if (vm.dirty) discard = true else vm.closeEditor() }
    BackHandler(vm.editing) { if (!busy) back() }
    Scaffold(topBar = { TopAppBar(title = { Text(if (vm.editing) vm.planName else "逐空间报价", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        actions = { if (vm.editing && vm.state.wizardStep == 4) TextButton(enabled = !busy, onClick = { vm.setStep(3) }) { Text("修改配置") } },
        navigationIcon = { IconButton(enabled = !busy, onClick = ::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = with(density) { footerHeight.toDp() })) }) { inner ->
        if (!vm.editing) {
            LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("我的报价方案", style = MaterialTheme.typography.titleLarge)
                    Text("整装、半包和局改均支持独立保存。预算比较包含自购，应用报价不会改变房屋预算上限。", style = MaterialTheme.typography.bodyMedium)
                    Button(enabled = !busy, onClick = { vm.newPlan() }) { Text("新建报价方案") }
                }
                if (plans.isEmpty()) item { Text("还没有保存的方案，从新建报价开始。") }
                items(plans, key = { it.id }) { plan ->
                    SectionCard(onClick = { if (!busy) vm.loadPlan(plan.id) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(plan.name, style = MaterialTheme.typography.titleMedium)
                            Text("${QuoteMode.entries.firstOrNull { it.name.lowercase() == plan.mode }?.label ?: plan.mode} · 更新于 ${plan.updatedAt}", style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = !busy, onClick = { vm.loadPlan(plan.id) }) { Text("打开") }
                                TextButton(enabled = !busy, onClick = { rename = plan }) { Text("重命名") }
                                TextButton(enabled = !busy, onClick = { deleting = plan }) { Text("删除") }
                            }
                        }
                    }
                }
            }
        } else QuoteEditor(vm, Modifier.padding(inner)) { footerHeight = it }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("放弃未保存的修改？") },
        text = { Text("当前修改尚未保存，离开后无法恢复。已保存的方案不受影响。") },
        confirmButton = { TextButton(onClick = { vm.closeEditor(); discard = false }) { Text("放弃修改") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("继续编辑") } })
    rename?.let { plan -> NameDialog("重命名方案", plan.name, busy, { rename = null }) { vm.renamePlan(plan, it) { rename = null } } }
    deleting?.let { plan -> ConfirmDeleteDialog(title = "删除报价方案", text = "确定删除「${plan.name}」？已应用的预算和支出不会删除。",
        onConfirm = { vm.deletePlan(plan.id); deleting = null }, onDismiss = { deleting = null }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteEditor(vm: QuoteViewModel, modifier: Modifier, onFooterMeasured: (Int) -> Unit) {
    val form = remember(vm.editorRevision, vm.state.wizardStep) { FormState() }
    val busy by vm.isBusy.collectAsState()
    val result = vm.result
    var incompleteConfirm by remember { mutableStateOf(false) }
    val step = vm.state.wizardStep
    val partial = vm.state.mode == QuoteMode.PARTIAL
    val labels = if (partial) listOf("房屋信息", "局改事项", "附加费用", "清单") else listOf("房屋信息", "划分空间", "逐空间选材", "清单")
    val nextError = when (step) {
        1 -> if (vm.state.house.totalArea <= 0 || vm.state.house.ceilingHeight <= 0) "请填写有效房屋信息" else null
        2 -> if (partial) {
            if (vm.state.partialWorks.none { it.enabled && it.qtyRows.any { row -> row.area > 0 } }) "请启用局改事项并填写面积" else null
        } else if (vm.state.rooms.isEmpty() || vm.state.rooms.any { it.name.isBlank() || it.area <= 0 }) "请添加空间并填写名称与面积" else null
        else -> vm.completionError()
    }
    Column(modifier.fillMaxSize().imePadding()) {
        if (step < 4) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            ChoiceField("报价方式", vm.state.mode, QuoteMode.entries.map { it to it.label }, enabled = !busy && form.valid) { vm.setMode(it) }
        }
        ScrollableTabRow(selectedTabIndex = step - 1, edgePadding = 8.dp) {
            labels.forEachIndexed { index, label -> Tab(selected = step == index + 1, enabled = !busy && form.valid && index + 1 <= step,
                onClick = { vm.setStep(index + 1) }, text = { Text("${index + 1} $label") }) }
        }
        }
        Box(Modifier.weight(1f)) {
            if (step == 4) QuoteResultContent(vm, onFooterMeasured)
            else key(vm.editorRevision, step) {
                CompositionLocalProvider(LocalFormState provides form) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (step) { 1 -> HouseStep(vm); 2 -> if (partial) PartialWorksStep(vm) else RoomsStep(vm); 3 -> if (partial) PartialFeesStep(vm) else MaterialsStep(vm) }
                    }
                }
            }
        }
        if (step < 4) Surface(modifier = Modifier.onSizeChanged { onFooterMeasured(it.height) }, tonalElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (result != null) {
                    Text("本方案合计 ¥${MoneyUtil.formatFull(result.estimatedTotalCents)}（含自购）", style = MaterialTheme.typography.titleMedium)
                    if (result.overBudget) Text("超出方案预算 ¥${MoneyUtil.formatFull(result.overBudgetCents)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (!form.valid || nextError != null) Text(if (!form.valid) "请修正标红字段" else nextError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(enabled = !busy && form.valid && step > 1, onClick = { vm.setStep(step - 1) }) { Text("上一步") }
                    Button(enabled = !busy && form.valid && nextError == null, onClick = { if (step == 3 && vm.unconfiguredRooms().isNotEmpty()) incompleteConfirm = true else vm.setStep(step + 1) }) { Text(if (step == 3) "生成完整清单" else "下一步") }
                }
            }
        }
    }
    if (incompleteConfirm) AlertDialog(onDismissRequest = { incompleteConfirm = false }, title = { Text("部分空间尚未配置") },
        text = { Text("${vm.unconfiguredRooms().joinToString("、") { it.name }} 未配置选材或工程，将不会计算这些空间的装修费用。") },
        confirmButton = { TextButton(onClick = { vm.setStep(4); incompleteConfirm = false }) { Text("仅生成已配置项目") } },
        dismissButton = { TextButton(onClick = { incompleteConfirm = false }) { Text("继续配置") } })

}

@Composable
private fun QuoteResultContent(vm: QuoteViewModel, onFooterMeasured: (Int) -> Unit) {
    val result = vm.result ?: return
    val categories by vm.categories.collectAsState(emptyList())
    val busy by vm.isBusy.collectAsState()
    val clipboard = LocalClipboardManager.current
    var save by rememberSaveable { mutableStateOf(false) }
    var asNew by rememberSaveable { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("报价清单", style = MaterialTheme.typography.titleLarge)
                MoneyLine("施工方报价", result.totalCents)
                MoneyLine("自购预计", result.summary.selfMainCents)
                MoneyLine("本方案合计", result.estimatedTotalCents, emphasized = true)
                Text(if (result.overBudget) "超出预算 ¥${MoneyUtil.formatFull(result.overBudgetCents)}" else "预算剩余 ¥${MoneyUtil.formatFull(result.budgetRemainCents)}",
                    color = if (result.overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                if (vm.unconfiguredRooms().isNotEmpty()) Text("未计空间费用：${vm.unconfiguredRooms().joinToString("、") { it.name }}", color = MaterialTheme.colorScheme.error)
                Text("参考报价不代替施工合同，按实际工程量与采购价核对。", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { clipboard.setText(AnnotatedString(QuoteTextBuilder.build(result, vm.state.mode.label, vm.state.house.totalArea, vm.state.house.ceilingHeight, vm.unconfiguredRooms().map { it.name }))); copied = true }) { Text(if (copied) "已复制" else "复制完整文本") }
                ImageExportActions("装修报价清单", "${vm.state.mode.label} · 合计 ¥${MoneyUtil.formatFull(result.estimatedTotalCents)}（含自购）",
                    (if (vm.unconfiguredRooms().isEmpty()) emptyList() else listOf(LongImageRow("未配置空间（未计空间费用）", vm.unconfiguredRooms().joinToString("、") { it.name }))) + listOf(LongImageRow("施工方报价", "¥${MoneyUtil.formatFull(result.totalCents)}"), LongImageRow("自购预计", "¥${MoneyUtil.formatFull(result.summary.selfMainCents)}")) + result.lines.map {
                        LongImageRow("${it.roomName?.let { name -> "【$name】" }.orEmpty()}${it.label}", "¥${MoneyUtil.formatFull(it.subtotalCents)}",
                            "${it.quantityText}${it.unit} × ¥${MoneyUtil.formatFull(it.unitPriceCents)} · ${it.sourcing.label}${it.note?.let { note -> " · $note" }.orEmpty()}")
                    })
            } }
        }
        itemsIndexed(result.lines, key = { index, line -> "${line.key}-$index" }) { index, line ->
            if (index == 0 || result.lines[index - 1].roomName != line.roomName) Text(line.roomName ?: "全屋及其他费用", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(line.label, style = MaterialTheme.typography.bodyLarge)
                Text("${line.quantityText}${line.unit} × ¥${MoneyUtil.formatFull(line.unitPriceCents)} = ¥${MoneyUtil.formatFull(line.subtotalCents)}", style = MaterialTheme.typography.bodyMedium)
                Text("${line.partLabel} · ${line.sourcing.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                line.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    Surface(modifier = Modifier.onSizeChanged { onFooterMeasured(it.height) }, tonalElevation = 2.dp) {
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !busy, onClick = { asNew = false; save = true }) { Text("保存方案") }
            OutlinedButton(enabled = !busy && result.estimatedTotalCents > 0, onClick = { applying = true }) { Text("应用预算") }
            TextButton(enabled = !busy, onClick = { asNew = true; save = true }) { Text("另存为") }
        }
    }
    }
    if (save) NameDialog(if (asNew) "另存新方案" else "保存方案", vm.planName, busy, { save = false }) { vm.savePlan(it, asNew) { save = false } }
    if (applying) BudgetApplyDialog(vm, result, categories) { applying = false }
}

@Composable
private fun BudgetApplyDialog(vm: QuoteViewModel, result: QuoteResult, categories: List<BudgetCategoryEntity>, onDismiss: () -> Unit) {
    val buckets = remember(result, categories) { QuoteBudgetMapping.buckets(result, categories) }
    val targets = remember(buckets) { mutableStateMapOf<String, String>().apply { buckets.forEach { b -> b.suggestedCategory?.let { put(b.key, it) } } } }
    val preview = runCatching { QuoteBudgetMapping.preview(result, categories, targets) }.getOrNull()
    val busy by vm.isBusy.collectAsState()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("预览分类计划变更") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("覆盖以下分类的计划金额；其他分类、房屋总预算上限和历史支出不变。")
            if (categories.isEmpty()) Text("请先在预算页添加分类。")
            buckets.forEach { bucket -> ChoiceField("${bucket.label} ¥${MoneyUtil.formatFull(bucket.cents)}", targets[bucket.key], categories.map { it.id as String? to it.name }) { id -> if (id != null) targets[bucket.key] = id } }
            if (preview == null) Text("请为每个费用大项选择目标分类。", color = MaterialTheme.colorScheme.error)
            preview?.changes?.forEach { Text("${it.name}：¥${MoneyUtil.formatFull(it.beforeCents)} → ¥${MoneyUtil.formatFull(it.afterCents)}") }
        } },
        confirmButton = { TextButton(enabled = !busy && preview != null, onClick = { preview?.let { vm.applyBudget(it, onDismiss) } }) { Text("确认应用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun NameDialog(title: String, initial: String, busy: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, label = { Text("方案名称") }, singleLine = true,
            isError = name.isBlank(), supportingText = { if (name.isBlank()) Text("请输入名称") }, modifier = Modifier.imePadding()) },
        confirmButton = { TextButton(enabled = name.isNotBlank() && !busy, onClick = { onSave(name.trim()) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
