package han1997.renovation.ui.stages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import han1997.renovation.ui.components.OperationFeedback
import han1997.renovation.ui.components.DatePickerField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import han1997.renovation.data.db.StageEntity
import han1997.renovation.data.db.TaskEntity
import han1997.renovation.ui.components.ConfirmDeleteDialog
import han1997.renovation.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StagesScreen(initialStageId: String? = null) {
    val vm: StagesViewModel = viewModel()
    val stages by vm.stages.collectAsState(initial = emptyList())
    val progress by vm.progress.collectAsState(initial = emptyList())
    var expandedId by rememberSaveable { mutableStateOf(initialStageId) }
    var deleteTemplateId by remember { mutableStateOf<String?>(null) }
    val host = remember { SnackbarHostState() }
    OperationFeedback(vm, host)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(initialStageId, stages) {
        val index = stages.indexOfFirst { it.id == initialStageId }
        if (index >= 0) { expandedId = initialStageId; listState.scrollToItem(index) }
    }
    var deleteCustomTask by remember { mutableStateOf<TaskEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(host) },
        topBar = { TopAppBar(title = { Text("装修流程") }) },
    ) { inner ->
        if (stages.isEmpty()) {
            // 首次启动时种子写入是异步的：表为空可能是「还没写完」也可能是「写入失败」。
            // 统一给加载占位，避免整页白屏（种子写入完成后 Flow 会自动刷新出列表）。
            Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        "正在准备数据…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp).imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(stages, key = { it.id }) { stage ->
                    val st = progress.firstOrNull { it.stageId == stage.id }
                    val pct = st?.pct ?: 0
                    val expanded = expandedId == stage.id
                    StageCard(
                        stage = stage,
                        pct = pct,
                        isDone = st?.isDone ?: false,
                        expanded = expanded,
                        onClick = { expandedId = if (expanded) null else stage.id },
                    )
                    if (expanded) {
                        val detailFlow = remember(stage.id) { vm.observeStageDetail(stage) }
                        val detail by detailFlow.collectAsState(
                            initial = StageDetailUi(stage, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                        )
                        StageDetailContent(
                            detail = detail,
                            vm = vm,
                            onRequestDeleteCustom = { deleteCustomTask = it },
                            onRequestDeleteTemplate = { templateId ->
                                deleteTemplateId = templateId
                            },
                            defaultTemplateCount = vm.defaultTemplateCount(stage.id),
                        )
                    }
                }
            }
        }
    }

    deleteTemplateId?.let { id -> ConfirmDeleteDialog(title = "删除任务",
        onConfirm = { vm.deleteTemplate(id); deleteTemplateId = null }, onDismiss = { deleteTemplateId = null }) }
    deleteCustomTask?.let { task ->
        ConfirmDeleteDialog(
            title = "删除任务",
            text = "确定删除「${task.text}」？删除后不可恢复。",
            onConfirm = { vm.deleteCustom(task.id); deleteCustomTask = null },
            onDismiss = { deleteCustomTask = null },
        )
    }
}

@Composable
private fun StageCard(
    stage: StageEntity,
    pct: Int,
    isDone: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    SectionCard(onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stage.emoji, style = MaterialTheme.typography.titleLarge)
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stage.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${stage.phase} · ${stage.duration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (isDone) "已完成" else "$pct%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            LinearProgressIndicator(
                progress = { (pct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun StageDetailContent(
    detail: StageDetailUi,
    vm: StagesViewModel,
    onRequestDeleteCustom: (TaskEntity) -> Unit,
    onRequestDeleteTemplate: (String) -> Unit,
    defaultTemplateCount: Int,
) {
    var newTask by remember { mutableStateOf("") }
    var editTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    val editTemplate = detail.templates.firstOrNull { it.id == editTemplateId }
    var editCustomId by rememberSaveable { mutableStateOf<String?>(null) }
    val editCustom = detail.customTasks.firstOrNull { it.id == editCustomId }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard {
            Column {
                DetailSectionTitle("🎯 目标")
                Spacer(Modifier.height(10.dp))
                Text(detail.stage.goal, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (detail.warnings.isNotEmpty()) {
            SectionCard {
                Column {
                    DetailSectionTitle("⚠️ 避坑要点")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        detail.warnings.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }

        if (detail.buy.isNotEmpty()) {
            SectionCard {
                Column {
                    DetailSectionTitle("🛒 需购材料")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        detail.buy.forEach { b ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(b.item, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                b.note?.let { n ->
                                    Text(
                                        "　$n",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SectionCard {
            Column {
                DetailSectionTitle("✅ 任务清单")
                Spacer(Modifier.height(4.dp))
                Column {
                    detail.templates.forEachIndexed { index, t ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editTemplateId = t.id }
                                .padding(vertical = 6.dp),
                        ) {
                            Checkbox(checked = t.done, onCheckedChange = { vm.toggleTemplate(t.id, it) })
                            Column(modifier = Modifier.weight(1f)) {
                                Text(t.text, style = MaterialTheme.typography.bodyLarge)
                                t.tip?.let {
                                    Spacer(Modifier.height(2.dp))
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { onRequestDeleteTemplate(t.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (detail.templates.isNotEmpty() && detail.customTasks.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    detail.customTasks.forEachIndexed { index, c ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editCustomId = c.id }
                                .padding(vertical = 6.dp),
                        ) {
                            Checkbox(checked = c.done, onCheckedChange = { vm.toggleCustom(c.id, it) })
                            Text(c.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRequestDeleteCustom(c) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { showRestoreConfirm = true },
                        enabled = defaultTemplateCount > 0,
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "恢复默认清单",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTask,
                        onValueChange = { newTask = it },
                        label = { Text("添加自定义任务") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newTask.isNotBlank()) {
                                    vm.addCustom(detail.stage.id, newTask) { newTask = "" }
                                }
                            },
                        ),
                    )
                    IconButton(
                        onClick = {
                            if (newTask.isNotBlank()) {
                                vm.addCustom(detail.stage.id, newTask) { newTask = "" }
                            }
                        },
                        enabled = newTask.isNotBlank(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "添加")
                    }
                }
            }
        }

        detail.checklists.forEach { cl ->
            SectionCard {
                Column {
                    DetailSectionTitle("${cl.emoji} ${cl.name}")
                    if (cl.items.isEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "（暂无条目）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Column {
                            cl.items.forEachIndexed { index, item ->
                                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                ) {
                                    Checkbox(checked = item.done, onCheckedChange = { vm.toggleChecklistItem(item.id, it) })
                                    Text(item.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editTemplate?.let { t ->
        TaskEditDialog(
            initialText = t.text,
            initialNote = t.tip,
            initialDate = t.dueDate,
            onDismiss = { editTemplateId = null },
            onSave = { text, note, date ->
                vm.saveTask(t.id, text, note, date, true) { editTemplateId = null }
            },
        )
    }

    editCustom?.let { c ->
        TaskEditDialog(
            initialText = c.text,
            initialNote = c.tip,
            initialDate = c.dueDate,
            onDismiss = { editCustomId = null },
            onSave = { text, note, date ->
                vm.saveTask(c.id, text, note, date, false) { editCustomId = null }
            },
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("恢复默认清单") },
            text = {
                Text(
                    "将还原本阶段的内置任务清单（共 $defaultTemplateCount 项），已添加的自定义任务不受影响。确定恢复吗？",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.restoreDefaultTasks(detail.stage.id)
                        showRestoreConfirm = false
                    },
                ) { Text("恢复") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun TaskEditDialog(
    initialText: String,
    initialNote: String?,
    initialDate: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?) -> Unit,
) {
    var text by rememberSaveable(initialText) { mutableStateOf(initialText) }
    var note by rememberSaveable(initialNote) { mutableStateOf(initialNote ?: "") }
    var date by rememberSaveable(initialDate) { mutableStateOf(initialDate) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑任务") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("任务内容") },
                    modifier = Modifier.fillMaxWidth(),
                )
                DatePickerField(value = date, onDateSelected = { date = it }, label = "截止日期（可选）", onClear = { date = null })
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text.trim(), note.trim().ifBlank { null }, date) },
                enabled = text.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text("取消") }
        },
    )
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
