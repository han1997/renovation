package com.renovation.guardian.ui.more

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.data.db.ContactEntity
import com.renovation.guardian.data.db.HouseProfileEntity
import com.renovation.guardian.data.db.NoteEntity
import com.renovation.guardian.data.db.QuickNoteEntity
import com.renovation.guardian.data.db.StageEntity
import com.renovation.guardian.ui.components.ConfirmDeleteDialog
import com.renovation.guardian.ui.components.DatePickerField
import com.renovation.guardian.ui.components.LabeledText
import com.renovation.guardian.ui.components.MoneyText
import com.renovation.guardian.ui.components.SectionCard
import com.renovation.guardian.ui.components.SectionTitle
import com.renovation.guardian.ui.nav.LocalExportActions
import com.renovation.guardian.util.DateUtil
import com.renovation.guardian.util.MoneyUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen() {
    val vm: MoreViewModel = viewModel()
    val profile by vm.profile.collectAsState(initial = null)
    val contacts by vm.contacts.collectAsState(initial = emptyList())
    val notes by vm.notes.collectAsState(initial = emptyList())
    val quickNotes by vm.quickNotes.collectAsState(initial = emptyList())
    val stages by vm.stages.collectAsState(initial = emptyList())
    val spaces by vm.spaces.collectAsState(initial = emptyList())
    val exportActions = LocalExportActions.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showHouseEdit by remember { mutableStateOf(false) }
    var showStylePick by remember { mutableStateOf(false) }
    var showContact by remember { mutableStateOf<ContactEntity?>(null) }
    var showNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showQuickNote by remember { mutableStateOf<QuickNoteEntity?>(null) }
    var showSpaceAdd by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    // 待确认删除的对象（统一走 ConfirmDeleteDialog）
    var deleteContactTarget by remember { mutableStateOf<ContactEntity?>(null) }
    var deleteNoteTarget by remember { mutableStateOf<NoteEntity?>(null) }
    var deleteQuickNoteId by remember { mutableStateOf<String?>(null) }
    var deleteSpaceTarget by remember { mutableStateOf<com.renovation.guardian.data.db.SpaceNeedWithStages?>(null) }

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

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("联系人", modifier = Modifier.weight(1f))
                    IconButton(onClick = { showContact = ContactEntity("", "", null, null, null, "") }) {
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
                        IconButton(onClick = { showContact = c }) { Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { deleteContactTarget = c }) { Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("笔记", modifier = Modifier.weight(1f))
                    IconButton(onClick = { showNote = NoteEntity("", "", "", DateUtil.today(), DateUtil.today()) }) {
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
                SectionCard(onClick = { showNote = n }) {
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
                    IconButton(onClick = { showQuickNote = QuickNoteEntity("", "", QuickNoteEntity.TYPE_WISH, null, null, false, DateUtil.today(), DateUtil.today()) }) {
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
                // ── 购物愿望组：按品类（家具 / 家电 / 生活用品）分组 ──
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
                        }
                        items(list, key = { it.id }) { q ->
                            QuickNoteCard(
                                note = q,
                                groupLabel = quickNoteCategoryLabel(cat),
                                onToggleDone = { vm.setQuickNoteDone(q.id, !q.isDone) },
                                onEdit = { showQuickNote = q },
                                onDelete = { deleteQuickNoteId = q.id },
                            )
                        }
                    }
                }
                // ── 阶段备忘组：按 14 阶段分组，组标题显示阶段名 ──
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
                        }
                        items(list, key = { it.id }) { q ->
                            QuickNoteCard(
                                note = q,
                                groupLabel = stageName,
                                onToggleDone = { vm.setQuickNoteDone(q.id, !q.isDone) },
                                onEdit = { showQuickNote = q },
                                onDelete = { deleteQuickNoteId = q.id },
                            )
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("空间需求", modifier = Modifier.weight(1f))
                    IconButton(onClick = { showSpaceAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "新增空间需求")
                    }
                }
            }
            if (spaces.isEmpty()) {
                item {
                    Text(
                        "写下需要改造的空间（如主卧、厨房），自动生成对应阶段的任务",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            items(spaces, key = { it.id }) { s ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.emoji, style = MaterialTheme.typography.titleLarge)
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(s.name, style = MaterialTheme.typography.bodyLarge)
                            s.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        IconButton(onClick = { deleteSpaceTarget = s }) { Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
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
                                    val json = vm.exportJsonString()
                                    exportActions.exportJson("renovation-backup.json", json)
                                    snackbar.showSnackbar("已开始导出备份")
                                }
                            }) { Icon(Icons.Filled.Upload, null); Text(" 导出") }
                            androidx.compose.material3.Button(onClick = {
                                exportActions.importJson { text ->
                                    scope.launch {
                                        val res = vm.importJson(text)
                                        snackbar.showSnackbar(if (res.success) "导入成功" else ("导入失败：${res.error ?: "文件格式不正确"}"))
                                    }
                                }
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

    if (showHouseEdit) {
        val currentProfile = profile
        if (currentProfile != null) {
            HouseEditDialog(profile = currentProfile, vm = vm, onDismiss = { showHouseEdit = false })
        }
    }
    if (showStylePick) {
        StylePickDialog(styles = vm.styles, current = profile?.styleId, onPick = { vm.updateStyle(it); showStylePick = false; scope.launch { snackbar.showSnackbar("已保存") } }, onDismiss = { showStylePick = false })
    }
    showContact?.let { c ->
        ContactDialog(initial = c, onDismiss = { showContact = null }) { name, role, phone, note ->
            vm.upsertContact(name, role, phone, note, if (c.id.isBlank()) null else c.id)
            showContact = null
            scope.launch { snackbar.showSnackbar("已保存") }
        }
    }
    showNote?.let { n ->
        NoteDialog(initial = n, onDismiss = { showNote = null }) { title, body ->
            vm.upsertNote(title, body, if (n.id.isBlank()) null else n.id)
            showNote = null
            scope.launch { snackbar.showSnackbar("已保存") }
        }
    }
    showQuickNote?.let { q ->
        QuickNoteDialog(
            initial = q,
            stages = stages,
            onDismiss = { showQuickNote = null },
        ) { content, type, category, stageId ->
            vm.upsertQuickNote(content, type, category, stageId, if (q.id.isBlank()) null else q.id)
            showQuickNote = null
            scope.launch { snackbar.showSnackbar("已保存") }
        }
    }
    if (showSpaceAdd) {
        SpaceAddDialog(presets = vm.spacePresets, onPick = { vm.addSpaceFromPreset(it); showSpaceAdd = false; scope.launch { snackbar.showSnackbar("已保存") } }, onDismiss = { showSpaceAdd = false })
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("清空全部数据") },
            text = { Text("将删除所有任务、预算、联系人、笔记与备份，且不可恢复。建议先导出备份。确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetAll()
                    showReset = false
                    scope.launch { snackbar.showSnackbar("已清空全部数据") }
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
    deleteSpaceTarget?.let { s ->
        ConfirmDeleteDialog(
            title = "删除空间需求",
            text = "确定删除「${s.name}」？其派生的未完成任务会一并移除。",
            onConfirm = { vm.deleteSpace(s.id); deleteSpaceTarget = null },
            onDismiss = { deleteSpaceTarget = null },
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
    var area by remember { mutableStateOf("%.0f".format(profile.areaM2)) }
    var tier by remember { mutableStateOf(profile.tierId) }
    var mode by remember { mutableStateOf(profile.modeId) }
    var grade by remember { mutableStateOf(profile.gradeId) }
    var startDate by remember { mutableStateOf(profile.startDate) }
    var total by remember { mutableStateOf(java.lang.String.format(java.util.Locale.ROOT, "%.2f", profile.totalBudgetCents / 100.0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                vm.updateProfile(area.toDoubleOrNull() ?: profile.areaM2, tier, mode, grade, startDate)
                vm.setTotalBudgetCents(MoneyUtil.fromYuan(total.toDoubleOrNull() ?: 0.0))
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("编辑房屋信息") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("面积（㎡）") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                LabeledDropdown("城市能级", vm.tiers.map { it.id to it.name }, tier) { tier = it }
                LabeledDropdown("装修方式", vm.modes.map { it.id to it.name }, mode) { mode = it }
                LabeledDropdown("档次", vm.grades.map { it.id to it.name }, grade) { grade = it }
                DatePickerField(value = startDate, onDateSelected = { startDate = it }, label = "开工日期")
                OutlinedTextField(value = total, onValueChange = { total = it }, label = { Text("总预算（元）") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
        },
    )
}

@Composable
private fun StylePickDialog(
    styles: List<com.renovation.guardian.data.knowledge.StyleJson>,
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
    var name by remember { mutableStateOf(initial.name) }
    var role by remember { mutableStateOf(initial.role ?: "") }
    var phone by remember { mutableStateOf(initial.phone ?: "") }
    var note by remember { mutableStateOf(initial.note ?: "") }

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
    var title by remember { mutableStateOf(initial.title) }
    var body by remember { mutableStateOf(initial.body) }

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
private fun SpaceAddDialog(
    presets: List<com.renovation.guardian.data.knowledge.SpaceNeedJson>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        title = { Text("添加空间需求") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                presets.forEach { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(p.id) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(p.emoji, style = MaterialTheme.typography.titleMedium)
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            Text(p.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
    )
}

/** 品类常量 → 中文标签（wish 分组标题用）。 */
private fun quickNoteCategoryLabel(category: String): String = when (category) {
    QuickNoteEntity.CATEGORY_FURNITURE -> "家具"
    QuickNoteEntity.CATEGORY_APPLIANCE -> "家电"
    QuickNoteEntity.CATEGORY_DAILY -> "生活用品"
    else -> "其他"
}

/**
 * 随手记单条卡片：勾选完成 + 内容 + 编辑 / 删除。
 * 已完成项视觉弱化（alpha 降低 + 删除线），排序上已由 DAO 置底。
 */
@Composable
private fun QuickNoteCard(
    note: QuickNoteEntity,
    groupLabel: String,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = note.isDone, onCheckedChange = { onToggleDone() })
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .clickable { onEdit() }
                    .alpha(if (note.isDone) 0.5f else 1f),
            ) {
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (note.isDone) TextDecoration.LineThrough else null,
                )
                Text(
                    groupLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
        }
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
    var content by remember { mutableStateOf(initial.content) }
    var type by remember { mutableStateOf(initial.type) }
    var category by remember { mutableStateOf(initial.category ?: QuickNoteEntity.CATEGORY_FURNITURE) }
    var stageId by remember { mutableStateOf(initial.stageId ?: stages.firstOrNull()?.id ?: "") }

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
