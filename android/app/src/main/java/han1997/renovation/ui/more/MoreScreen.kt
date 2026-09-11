package han1997.renovation.ui.more

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import han1997.renovation.ui.components.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import han1997.renovation.data.db.ContactEntity
import han1997.renovation.data.db.HouseProfileEntity
import han1997.renovation.data.db.NoteEntity
import han1997.renovation.data.db.QuickNoteEntity
import han1997.renovation.data.db.StageEntity
import han1997.renovation.ui.components.ConfirmDeleteDialog
import han1997.renovation.ui.components.DatePickerField
import han1997.renovation.ui.components.LabeledText
import han1997.renovation.ui.components.MoneyText
import han1997.renovation.ui.components.SectionCard
import han1997.renovation.ui.components.SectionTitle
import han1997.renovation.ui.nav.LocalExportActions
import han1997.renovation.util.DateUtil
import han1997.renovation.util.MoneyUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(onOpenPlanner: () -> Unit = {}) {
    val vm: MoreViewModel = viewModel()
    val profile by vm.profile.collectAsState(initial = null)
    val contacts by vm.contacts.collectAsState(initial = emptyList())
    val notes by vm.notes.collectAsState(initial = emptyList())
    val quickNotes by vm.quickNotes.collectAsState(initial = emptyList())
    val stages by vm.stages.collectAsState(initial = emptyList())
    val exportActions = LocalExportActions.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    OperationFeedback(vm, snackbar)

    var showHouseEdit by rememberSaveable { mutableStateOf(false) }
    var showStylePick by rememberSaveable { mutableStateOf(false) }
    var showContactId by rememberSaveable { mutableStateOf<String?>(null) }
    val showContact = when (showContactId) { null -> null; "" -> ContactEntity("", "", null, null, null, ""); else -> contacts.firstOrNull { it.id == showContactId } }
    var showNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    val showNote = when (showNoteId) { null -> null; "" -> NoteEntity("", "", "", DateUtil.today(), DateUtil.today()); else -> notes.firstOrNull { it.id == showNoteId } }
    var showQuickNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    val showQuickNote = when (showQuickNoteId) { null -> null; "" -> QuickNoteEntity("", "", QuickNoteEntity.TYPE_WISH, null, null, false, DateUtil.today(), DateUtil.today()); else -> quickNotes.firstOrNull { it.id == showQuickNoteId } }
    var showReset by rememberSaveable { mutableStateOf(false) }

    // 待确认删除的对象(统一走 ConfirmDeleteDialog)
    var deleteContactTarget by remember { mutableStateOf<ContactEntity?>(null) }
    var deleteNoteTarget by remember { mutableStateOf<NoteEntity?>(null) }
    var deleteQuickNoteId by remember { mutableStateOf<String?>(null) }

    val tierName = { id: String? -> vm.tiers.firstOrNull { it.id == id }?.name ?: id ?: "—" }
    val modeName = { id: String? -> vm.modes.firstOrNull { it.id == id }?.name ?: id ?: "—" }
    val gradeName = { id: String? -> vm.grades.firstOrNull { it.id == id }?.name ?: id ?: "—" }
    val styleName = { id: String? -> vm.styles.firstOrNull { it.id == id }?.name ?: "未设置" }

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Column {
                        Text("房屋信息", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        LabeledText("面积", "${profile?.areaM2?.let { "%.0f".format(it) } ?: "—"} ㎡")
                        LabeledText("城市能级", tierName(profile?.tierId))
                        LabeledText("装修方式", modeName(profile?.modeId))
                        LabeledText("档次", gradeName(profile?.gradeId))
                        LabeledText("开工日期", profile?.startDate?.let { DateUtil.formatCN(it) } ?: "未设置")
                        LabeledText("总预算", profile?.totalBudgetCents?.let { "¥${MoneyUtil.formatFull(it)}" } ?: "—")
                        LabeledText("风格", styleName(profile?.styleId))
                        Row(modifier = Modifier.padding(top = 8.dp)) {
                            TextButton(onClick = { showHouseEdit = true }) { Text("编辑") }
                            TextButton(onClick = { showStylePick = true }) { Text("选择风格") }
                        }
                    }
                }
            }

            item { SectionTitle("规划工具") }
            item {
                SectionCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onOpenPlanner() }.padding(vertical = 4.dp),
                    ) {
                        Text("📋", style = MaterialTheme.typography.titleLarge)
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text("需求规划", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "逐空间规划功能需求,标注重要度并生成装修需求清单",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("进入 ›", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item { SectionTitle("装修记录") }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("联系人", modifier = Modifier.weight(1f))
                    IconButton(onClick = { showContactId = "" }) {
                        Icon(Icons.Filled.Add, contentDescription = "新增联系人")
                    }
                }
            }
            if (contacts.isEmpty()) {
                item {
                    Text(
                        "把工长、设计师、监工的电话存在这里，方便随时联系",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            items(contacts, key = { it.id }) { c ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.bodyLarge)
                            c.role?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            c.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        IconButton(onClick = { showContactId = c.id }) { Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { deleteContactTarget = c }) { Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("笔记", modifier = Modifier.weight(1f))
                    IconButton(onClick = { showNoteId = "" }) {
                        Icon(Icons.Filled.Add, contentDescription = "新增笔记")
                    }
                }
            }
            if (notes.isEmpty()) {
                item {
                    Text(
                        "记下合同编号、保修期、验房要点，随时翻看",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            items(notes, key = { it.id }) { n ->
                SectionCard(onClick = { showNoteId = n.id }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(n.title.ifBlank { "(无标题)" }, style = MaterialTheme.typography.bodyLarge)
                            Text(n.body.take(40), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // 笔记此前无法删除（deleteNote 死代码），补删除入口 + 确认
                        IconButton(onClick = { deleteNoteTarget = n }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // ── 随手记：快速捕捉购物愿望 / 阶段备忘，按类型整理分组展示 ──
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("随手记", modifier = Modifier.weight(1f))
                    IconButton(onClick = { showQuickNoteId = "" }) {
                        Icon(Icons.Filled.Add, contentDescription = "新增随手记")
                    }
                }
            }
            if (quickNotes.isEmpty()) {
                item {
                    SectionCard {
                        Text(
                            "暂无随手记，用 + 记下想买的东西和施工注意点",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                val wishGroups = quickNotes.filter { it.type == QuickNoteEntity.TYPE_WISH }.groupBy { it.category ?: QuickNoteEntity.CATEGORY_DAILY }
                val memoGroups = quickNotes.filter { it.type == QuickNoteEntity.TYPE_MEMO }.groupBy { it.stageId ?: "" }
                // ── 购物愿望组：按品类（家具 / 家电 / 生活用品）分组，同组合并一卡 ──
                if (wishGroups.isNotEmpty()) {
                    item {
                        Text(
                            "购物愿望",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    wishGroups.forEach { (cat, list) ->
                        item(key = "wish-group-$cat") {
                            Text(
                                quickNoteCategoryLabel(cat),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                            )
                            SectionCard {
                                Column {
                                    list.forEachIndexed { index, q ->
                                        if (index > 0) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                        }
                                        QuickNoteRow(
                                            note = q,
                                            onToggleDone = { vm.setQuickNoteDone(q.id, !q.isDone) },
                                            onEdit = { showQuickNoteId = q.id },
                                            onDelete = { deleteQuickNoteId = q.id },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // ── 阶段备忘组：按 14 阶段分组，组标题显示阶段名，同组合并一卡 ──
                if (memoGroups.isNotEmpty()) {
                    item {
                        Text(
                            "阶段备忘",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    memoGroups.forEach { (stageId, list) ->
                        val stageName = stages.firstOrNull { it.id == stageId }?.name ?: "未关联阶段"
                        item(key = "memo-group-$stageId") {
                            Text(
                                stageName,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                            )
                            SectionCard {
                                Column {
                                    list.forEachIndexed { index, q ->
                                        if (index > 0) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                        }
                                        QuickNoteRow(
                                            note = q,
                                            onToggleDone = { vm.setQuickNoteDone(q.id, !q.isDone) },
                                            onEdit = { showQuickNoteId = q.id },
                                            onDelete = { deleteQuickNoteId = q.id },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Column {
                        Text("数据备份与重置", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.Button(onClick = {
                                scope.launch {
                                    try {
                                        val json = vm.exportJsonString()
                                        exportActions.exportJson("renovation-backup.json", json)
                                    } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                    catch (_: Exception) { snackbar.showSnackbar("准备备份失败，请重试") }
                                }
                            }) { Icon(Icons.Filled.Upload, null); Text(" 导出") }
                            androidx.compose.material3.Button(onClick = {
                                exportActions.importJson { text -> vm.prepareBackupImport(text) }
                            }) { Icon(Icons.Filled.Download, null); Text(" 导入") }
                        }
                        androidx.compose.material3.TextButton(onClick = { showReset = true }) {
                            Text("清空全部数据", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    vm.preparedImport?.let { prepared ->
        val busy by vm.isBusy.collectAsState()
        AlertDialog(onDismissRequest = vm::dismissImport, title = { Text("确认恢复备份") },
            text = { Text(prepared.summary) },
            confirmButton = { TextButton(enabled = !busy, onClick = { vm.confirmImport() }) { Text("确认恢复") } },
            dismissButton = { TextButton(onClick = vm::dismissImport) { Text("取消") } })
    }
    if (showHouseEdit) {
        val currentProfile = profile
        if (currentProfile != null) {
            HouseEditDialog(profile = currentProfile, vm = vm, onDismiss = { showHouseEdit = false })
        }
    }
    if (showStylePick) {
        StylePickDialog(styles = vm.styles, current = profile?.styleId, onPick = { vm.updateStyle(it) { showStylePick = false } }, onDismiss = { showStylePick = false })
    }
    showContact?.let { c -> androidx.compose.runtime.key("showContact:${c.id}") {
        ContactDialog(initial = c, onDismiss = { showContactId = null }) { name, role, phone, note ->
            vm.upsertContact(name, role, phone, note, if (c.id.isBlank()) null else c.id) { showContactId = null }

        }
    } }
    showNote?.let { n -> androidx.compose.runtime.key("showNote:${n.id}") {
        NoteDialog(initial = n, onDismiss = { showNoteId = null }) { title, body ->
            vm.upsertNote(title, body, if (n.id.isBlank()) null else n.id) { showNoteId = null }

        }
    } }
    showQuickNote?.let { q -> androidx.compose.runtime.key("showQuickNote:${q.id}") {
        QuickNoteDialog(
            initial = q,
            stages = stages,
            onDismiss = { showQuickNoteId = null },
        ) { content, type, category, stageId ->
            vm.upsertQuickNote(content, type, category, stageId, if (q.id.isBlank()) null else q.id) { showQuickNoteId = null }

        }
    } }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("清空全部数据") },
            text = { Text("将清空房屋、预算、报价、需求规划和全部记录，并恢复默认任务。外部备份文件不会删除。建议先导出备份，确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetAll()
                    showReset = false

                }) { Text("清除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("取消") } },
        )
    }

    // ── 统一的删除确认对话框 ──
    deleteContactTarget?.let { c ->
        ConfirmDeleteDialog(
            title = "删除联系人",
            text = "确定删除「${c.name}」？删除后不可恢复。",
            onConfirm = { vm.deleteContact(c.id); deleteContactTarget = null },
            onDismiss = { deleteContactTarget = null },
        )
    }
    deleteNoteTarget?.let { n ->
        ConfirmDeleteDialog(
            title = "删除笔记",
            text = "确定删除「${n.title.ifBlank { "(无标题)" }}」？删除后不可恢复。",
            onConfirm = { vm.deleteNote(n.id); deleteNoteTarget = null },
            onDismiss = { deleteNoteTarget = null },
        )
    }
    deleteQuickNoteId?.let { id ->
        ConfirmDeleteDialog(
            title = "删除随手记",
            onConfirm = { vm.deleteQuickNote(id); deleteQuickNoteId = null },
            onDismiss = { deleteQuickNoteId = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selectedId }?.second ?: "未选择",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, label2) ->
                DropdownMenuItem(text = { Text(label2) }, onClick = { onSelected(id); expanded = false })
            }
        }
    }
}

@Composable
private fun HouseEditDialog(profile: HouseProfileEntity, vm: MoreViewModel, onDismiss: () -> Unit) {
    var area by rememberSaveable { mutableStateOf(profile.areaM2) }
    var tier by rememberSaveable { mutableStateOf(profile.tierId) }
    var mode by rememberSaveable { mutableStateOf(profile.modeId) }
    var grade by rememberSaveable { mutableStateOf(profile.gradeId) }
    var date by rememberSaveable { mutableStateOf(profile.startDate) }
    var budget by rememberSaveable { mutableStateOf(profile.totalBudgetCents) }
    val form = remember { FormState() }
    val busy by vm.isBusy.collectAsState()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("房屋与预算设置") },
        text = { CompositionLocalProvider(LocalFormState provides form) {
            Column(Modifier.verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("建筑面积（㎡）", area) { area = it ?: area }
                ChoiceField("城市能级", tier, vm.tiers.map { it.id to it.name }) { tier = it }
                ChoiceField("装修方式", mode, vm.modes.map { it.id to it.name }) { mode = it }
                ChoiceField("档次", grade, vm.grades.map { it.id to it.name }) { grade = it }
                DatePickerField(date, { date = it }, "开工日期", onClear = { date = null })
                MoneyField("总预算上限（元）", budget) { budget = it }
                Text("修改开工日期不会重排已有任务日期。", style = MaterialTheme.typography.bodySmall)
            }
        } },
        confirmButton = { TextButton(enabled = form.valid && !busy, onClick = {
            vm.saveHouse(area, tier, mode, grade, date, budget, onDismiss)
        }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun StylePickDialog(
    styles: List<han1997.renovation.data.knowledge.StyleJson>,
    current: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        title = { Text("选择风格") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                styles.forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(s.id) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.emoji, style = MaterialTheme.typography.titleMedium)
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(s.name, style = MaterialTheme.typography.bodyLarge)
                            Text(s.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (current == s.id) {
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun ContactDialog(
    initial: ContactEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, role: String?, phone: String?, note: String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var role by rememberSaveable { mutableStateOf(initial.role ?: "") }
    var phone by rememberSaveable { mutableStateOf(initial.phone ?: "") }
    var note by rememberSaveable { mutableStateOf(initial.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), role.ifBlank { null }, phone.ifBlank { null }, note.ifBlank { null }) },
                enabled = name.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (initial.id.isBlank()) "新增联系人" else "编辑联系人") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    isError = name.isBlank(),
                    supportingText = { if (name.isBlank()) Text("请填写姓名") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("角色（工长/设计师…）") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("电话") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}

@Composable
private fun NoteDialog(
    initial: NoteEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, body: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var body by rememberSaveable { mutableStateOf(initial.body) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim(), body) },
                enabled = title.isNotBlank() || body.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (initial.id.isBlank()) "新增笔记" else "编辑笔记") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("内容") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        },
    )
}

@Composable
private fun quickNoteCategoryLabel(category: String): String = when (category) {
    QuickNoteEntity.CATEGORY_FURNITURE -> "家具"
    QuickNoteEntity.CATEGORY_APPLIANCE -> "家电"
    QuickNoteEntity.CATEGORY_DAILY -> "生活用品"
    else -> "其他"
}

/**
 * 随手记单行（同组多行合并进一张卡片，行间用 HorizontalDivider 分隔）：
 * - 勾选完成由 Checkbox 承担；
 * - 点击行内容 → 编辑弹窗，长按行 → 删除确认；
 * - 单行截断（maxLines = 1 + Ellipsis），全文进编辑弹窗查看；
 * - 已完成项视觉弱化（alpha 降低 + 删除线），排序上已由 DAO 置底。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickNoteRow(
    note: QuickNoteEntity,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = note.isDone, onCheckedChange = { onToggleDone() })
        Text(
            note.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textDecoration = if (note.isDone) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .combinedClickable(onClick = onEdit, onLongClick = onDelete)
                .alpha(if (note.isDone) 0.5f else 1f),
        )
    }
}

/**
 * 随手记新增 / 编辑弹窗：
 * - 内容一句话输入；
 * - 类型二选一（FilterChip：购物愿望 / 阶段备忘）；
 * - wish 联动品类三选一（家具 / 家电 / 生活用品）；
 * - memo 联动 14 阶段下拉。
 */
@Composable
private fun QuickNoteDialog(
    initial: QuickNoteEntity,
    stages: List<StageEntity>,
    onDismiss: () -> Unit,
    onSave: (content: String, type: String, category: String?, stageId: String?) -> Unit,
) {
    var content by rememberSaveable { mutableStateOf(initial.content) }
    var type by rememberSaveable { mutableStateOf(initial.type) }
    var category by rememberSaveable { mutableStateOf(initial.category ?: QuickNoteEntity.CATEGORY_FURNITURE) }
    var stageId by rememberSaveable { mutableStateOf(initial.stageId ?: stages.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        content.trim(),
                        type,
                        if (type == QuickNoteEntity.TYPE_WISH) category else null,
                        if (type == QuickNoteEntity.TYPE_MEMO) stageId.ifBlank { null } else null,
                    )
                },
                enabled = content.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (initial.id.isBlank()) "新增随手记" else "编辑随手记") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                // 类型二选一
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == QuickNoteEntity.TYPE_WISH,
                        onClick = { type = QuickNoteEntity.TYPE_WISH },
                        label = { Text("购物愿望") },
                    )
                    FilterChip(
                        selected = type == QuickNoteEntity.TYPE_MEMO,
                        onClick = { type = QuickNoteEntity.TYPE_MEMO },
                        label = { Text("阶段备忘") },
                    )
                }
                if (type == QuickNoteEntity.TYPE_WISH) {
                    // 品类三选一
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = category == QuickNoteEntity.CATEGORY_FURNITURE,
                            onClick = { category = QuickNoteEntity.CATEGORY_FURNITURE },
                            label = { Text("家具") },
                        )
                        FilterChip(
                            selected = category == QuickNoteEntity.CATEGORY_APPLIANCE,
                            onClick = { category = QuickNoteEntity.CATEGORY_APPLIANCE },
                            label = { Text("家电") },
                        )
                        FilterChip(
                            selected = category == QuickNoteEntity.CATEGORY_DAILY,
                            onClick = { category = QuickNoteEntity.CATEGORY_DAILY },
                            label = { Text("生活用品") },
                        )
                    }
                } else {
                    // 阶段下拉（14 阶段）
                    LabeledDropdown(
                        label = "关联阶段",
                        options = stages.map { it.id to it.name },
                        selectedId = stageId.ifBlank { null },
                        onSelected = { stageId = it },
                    )
                }
            }
        },
    )
}
