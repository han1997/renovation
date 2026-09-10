package com.renovation.guardian.ui.planner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.ui.components.*
import com.renovation.guardian.ui.planner.engine.*
import com.renovation.guardian.domain.planner.*
import com.renovation.guardian.util.LongImageRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(onBack: () -> Unit = {}, vm: PlannerViewModel = viewModel()) {
    val snackbar = remember { SnackbarHostState() }
    var footerHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val stepStates = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    var clear by remember { mutableStateOf(false) }
    OperationFeedback(vm, snackbar)
    BackHandler(vm.saving || vm.saveError != null) { scope.launch { snackbar.showSnackbar("请等待保存完成，失败时请先重试") } }
    Scaffold(topBar = { TopAppBar(title = { Text("需求规划") },
        navigationIcon = { IconButton(enabled = !vm.saving && vm.saveError == null, onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        actions = { TextButton(enabled = !vm.loading && vm.state.picked.isNotEmpty(), onClick = { clear = true }) { Text("清空已选") } }) },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = with(density) { footerHeight.toDp() })) }) { inner ->
        if (vm.loading) { LoadingState(); return@Scaffold }
        if (vm.loadError != null || vm.catalog == null) {
            Column(Modifier.padding(inner).padding(16.dp)) { Text(vm.loadError ?: "目录数据不可用"); Button(onClick = vm::reload) { Text("重试") } }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(inner).imePadding()) {
            ScrollableTabRow(selectedTabIndex = vm.state.step - 1, edgePadding = 8.dp) {
                listOf("选择需求", "添加空间", "分配需求", "生成清单").forEachIndexed { index, label ->
                    Tab(selected = vm.state.step == index + 1, onClick = { vm.setStep(index + 1) }, text = { Text("${index + 1} $label") })
                }
            }
            if (vm.saveError != null) Row(Modifier.padding(horizontal = 16.dp)) {
                Text(vm.saveError.orEmpty(), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = vm::persist) { Text("重试保存") }
            } else Text(if (vm.saving) "正在保存…" else "已自动保存", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.weight(1f)) {
                stepStates.SaveableStateProvider(vm.state.step) { when (vm.state.step) { 1 -> PickStep(vm); 2 -> RoomsStep(vm); 3 -> AssignStep(vm); else -> ResultStep(vm) } }
            }
            Row(Modifier.fillMaxWidth().onSizeChanged { footerHeight = it.height }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(enabled = vm.state.step > 1, onClick = { vm.setStep(vm.state.step - 1) }) { Text("上一步") }
                if (vm.state.step < 4) Button(onClick = { vm.setStep(vm.state.step + 1) }) { Text("下一步") }
                else TextButton(onClick = { vm.setStep(1) }) { Text("返回选择") }
            }
        }
    }
    if (clear) ConfirmDeleteDialog(title = "清空已选需求", text = "所有已选需求和分配将清空，空间列表保留。",
        onConfirm = { vm.clearPicks(); clear = false }, onDismiss = { clear = false })
}

@Composable
private fun PickStep(vm: PlannerViewModel) {
    val catalog = vm.catalog ?: return
    var query by rememberSaveable { mutableStateOf("") }
    var space by rememberSaveable { mutableStateOf("") }
    val result = remember(query, space, catalog) { PlannerRules.search(catalog, query, space) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(query, { query = it }, label = { Text("搜索需求名称或类型") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ChoiceField("空间目录", space, listOf("" to "全部空间") + catalog.spaceList.map { it.key to it.name }) { space = it }
        Text("已选 ${vm.state.picked.size} 项 · 搜索结果 ${result.size} 项", style = MaterialTheme.typography.bodySmall)
        LazyColumn(Modifier.weight(1f)) {
            if (result.isEmpty()) item { Text("没有匹配项，试试其他关键词或全部空间。") }
            items(result, key = { it.itemKey }) { pick ->
                Column {
                    ToggleRow(pick.itemName, vm.state.picked.any { it.itemKey == pick.itemKey }) { vm.togglePick(pick) }
                    Text("${pick.spaceName} / ${pick.typeName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun RoomsStep(vm: PlannerViewModel) {
    val catalog = vm.catalog ?: return
    var preset by rememberSaveable { mutableStateOf(catalog.spaceList.firstOrNull()?.key.orEmpty()) }
    var custom by rememberSaveable { mutableStateOf("") }
    var removing by remember { mutableStateOf<PlannerRoom?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = vm.state.rooms.firstOrNull { it.id == editingId }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ChoiceField("预设空间", preset, catalog.spaceList.map { it.key to it.name }) { preset = it }
            Button(onClick = { catalog.spaceList.firstOrNull { it.key == preset }?.let { vm.addPreset(it.key, it.name) } }) { Text("添加预设空间") }
            Text("同类型空间可重复添加，例如多个卧室。", style = MaterialTheme.typography.bodySmall)
        }
        items(vm.state.rooms, key = { it.id }) { room ->
            SectionCard {
                Column {
                    Text(room.name, style = MaterialTheme.typography.titleMedium)
                    Text(catalog.spaceList.firstOrNull { it.key == room.presetKey }?.name ?: "自定义空间 · 手动分配", style = MaterialTheme.typography.bodySmall)
                    Row { TextButton(onClick = { editingId = room.id }) { Text("重命名") }; TextButton(onClick = { removing = room }) { Text("删除") } }
                }
            }
        }
        item {
            OutlinedTextField(custom, { custom = it }, label = { Text("自定义空间名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = custom.isNotBlank() && vm.state.rooms.none { it.name == custom.trim() }, onClick = { vm.addRoom(custom, null); custom = "" }) { Text("添加自定义空间") }
        }
    }
    removing?.let { room -> ConfirmDeleteDialog(title = "删除空间", text = "删除「${room.name}」及其需求分配，已选需求仍保留。",
        onConfirm = { vm.removeRoom(room.id); removing = null }, onDismiss = { removing = null }) }
    editing?.let { room ->
        var name by rememberSaveable(room.id) { mutableStateOf(room.name) }
        AlertDialog(onDismissRequest = { editingId = null }, title = { Text("重命名空间") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("空间名") }) },
            confirmButton = { TextButton(enabled = name.isNotBlank() && vm.state.rooms.none { it.id != room.id && it.name == name.trim() },
                onClick = { vm.renameRoom(room.id, name); editingId = null }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { editingId = null }) { Text("取消") } })
    }
}

@Composable
private fun AssignStep(vm: PlannerViewModel) {
    var preview by remember { mutableStateOf<List<Assignment>?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Button(enabled = vm.recommendations().isNotEmpty(), onClick = { preview = vm.recommendations() }) { Text("按空间类型推荐") }
            Text("只推荐尚未分配的需求，不覆盖手工分配。可跳到清单查看未分配项。", style = MaterialTheme.typography.bodySmall)
        }
        if (vm.state.rooms.isEmpty() || vm.state.picked.isEmpty()) item { Text("请先选择需求并添加空间。") }
        items(vm.state.picked, key = { it.itemKey }) { pick ->
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(pick.itemName, style = MaterialTheme.typography.titleMedium)
                    vm.state.rooms.forEach { room ->
                        val assignment = vm.state.assignments.firstOrNull { it.itemKey == pick.itemKey && it.roomId == room.id }
                        ToggleRow(room.name, assignment != null) { vm.toggleAssignment(pick, room.id, it) }
                        if (assignment != null) ChoiceField("${room.name}的重要度", assignment.importance, Importance.entries.map { it to it.label }) {
                            vm.setImportance(pick.itemKey, room.id, it)
                        }
                    }
                }
            }
        }
    }
    preview?.let { proposal ->
        val selected = remember(proposal) { mutableStateListOf<Assignment>().apply { addAll(proposal) } }
        AlertDialog(onDismissRequest = { preview = null }, title = { Text("预览推荐分配") },
            text = { LazyColumn(Modifier.heightIn(max = 360.dp)) { items(proposal, key = { it.itemKey + ":" + it.roomId }) { item ->
                ToggleRow("${item.itemName} → ${item.roomName}", item in selected) { if (it) selected.add(item) else selected.remove(item) }
            } } },
            confirmButton = { TextButton(enabled = selected.isNotEmpty(), onClick = { vm.applyRecommendations(selected.toList()); preview = null }) { Text("应用 ${selected.size} 项") } },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("取消") } })
    }
}

@Composable
private fun ResultStep(vm: PlannerViewModel) {
    val lines = vm.lines()
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("装修需求清单", style = MaterialTheme.typography.titleLarge)
            Text("共 ${lines.size} 项，未分配 ${lines.count { it.roomName == "未分配" }} 项")
            Button(enabled = lines.isNotEmpty(), onClick = { clipboard.setText(AnnotatedString(PlannerTextBuilder.build(lines))); copied = true }) { Text(if (copied) "已复制" else "复制文本") }
            ImageExportActions("装修需求清单", "按空间分组 · 共 ${lines.size} 项", lines.map {
                LongImageRow("【${it.roomName}】${it.itemName}", it.importance?.label ?: "未分配")
            }, enabled = lines.isNotEmpty())
        }
        lines.groupBy { it.roomName }.forEach { (room, group) ->
            item { Text(room, style = MaterialTheme.typography.titleMedium) }
            items(group) { line ->
                Text(line.itemName + line.importance?.let { "（${it.label}）" }.orEmpty())
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
