package com.renovation.guardian.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.data.knowledge.RequirementItemJson
import com.renovation.guardian.ui.components.SectionCard
import com.renovation.guardian.ui.planner.engine.DemandPick
import com.renovation.guardian.ui.planner.engine.Importance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(onBack: () -> Unit = {}) {
    val vm: PlannerViewModel = viewModel()
    val catalog = vm.catalog
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("需求规划") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        if (catalog == null) {
            Text("目录数据未加载", modifier = Modifier.padding(inner).padding(24.dp))
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            // 步骤条
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("选需求", "空间", "分配", "清单").forEachIndexed { i, label ->
                    FilterChip(
                        selected = vm.state.step == i + 1,
                        onClick = { vm.setStep(i + 1) },
                        label = { Text("${i + 1}.$label") },
                    )
                }
            }
            when (vm.state.step) {
                1 -> PickStep(vm, catalog)
                2 -> RoomsStep(vm, catalog)
                3 -> AssignStep(vm)
                else -> ResultStep(vm, snackbar, scope, clipboard)
            }
        }
    }
}

// ── Step 1: 选择需求(按空间分组浏览 + 搜索) ──
@Composable
private fun PickStep(vm: PlannerViewModel, catalog: com.renovation.guardian.data.knowledge.DecoboxRequirementsJson) {
    var query by remember { mutableStateOf("") }
    var spaceKey by remember { mutableStateOf(catalog.spaceList.firstOrNull()?.key ?: "") }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, label = { Text("搜索需求") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        // 空间 chips
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            catalog.spaceList.take(5).forEach { sp ->
                FilterChip(selected = spaceKey == sp.key, onClick = { spaceKey = sp.key }, label = { Text(sp.name) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            catalog.spaceList.drop(5).forEach { sp ->
                FilterChip(selected = spaceKey == sp.key, onClick = { spaceKey = sp.key }, label = { Text(sp.name) })
            }
        }
        Text("已选 ${vm.state.picked.size} 项 · 去分配 →", color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { vm.setStep(3) }.padding(vertical = 4.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
            val space = catalog.spaceList.firstOrNull { it.key == spaceKey } ?: return@LazyColumn
            val typeList = catalog.typeListBySpace[spaceKey] ?: emptyList()
            val types = if (query.isBlank()) typeList else typeList.filter { t -> t.name.contains(query, ignoreCase = true) }
            types.forEach { type ->
                val typeName = type.name.removePrefix("(功能)").removePrefix("(收纳)").removePrefix("(设备)")
                item(key = type.key) {
                    Text(typeName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                val items = catalog.functionsByType[type.key] ?: emptyList()
                val filtered = if (query.isBlank()) items else items.filter { it.name.contains(query, ignoreCase = true) }
                item(key = "${type.key}-head") {
                    SectionCard {
                        Column {
                            filtered.forEachIndexed { i, item ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { vm.togglePick(pickOf(space.name, spaceKey, type, item)) }) {
                                    Checkbox(checked = vm.state.picked.any { p -> p.itemKey == item.key },
                                        onCheckedChange = { vm.togglePick(pickOf(space.name, spaceKey, type, item)) })
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun pickOf(spaceName: String, spaceKey: String, type: com.renovation.guardian.data.knowledge.RequirementTypeJson, it: RequirementItemJson): DemandPick =
    DemandPick(it.key, it.name, type.key, type.name, spaceKey, spaceName)

// ── Step 2: 空间 ──
@Composable
private fun RoomsStep(vm: PlannerViewModel, catalog: com.renovation.guardian.data.knowledge.DecoboxRequirementsJson) {
    var custom by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("空间列表", style = MaterialTheme.typography.titleMedium)
        // 预设空间一键添加
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            catalog.spaceList.forEach { sp ->
                FilterChip(
                    selected = vm.state.rooms.any { it.presetKey == sp.key },
                    onClick = {
                        if (vm.state.rooms.any { it.presetKey == sp.key }) {
                            vm.removeRoom(vm.state.rooms.first { it.presetKey == sp.key }.id)
                        } else {
                            vm.addRoom(sp.name, sp.key)
                        }
                    },
                    label = { Text(sp.name) },
                )
            }
        }
        vm.state.rooms.forEach { room ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(room.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (room.presetKey == null) {
                        IconButton(onClick = { vm.removeRoom(room.id) }) { Text("✕", color = MaterialTheme.colorScheme.error) }
                    } else {
                        IconButton(onClick = { vm.removeRoom(room.id) }) { Text("✕", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = custom, onValueChange = { custom = it }, label = { Text("自定义空间名") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = {
                val name = custom.trim()
                if (name.isNotBlank() && vm.state.rooms.none { it.name == name }) {
                    vm.addRoom(name, null)
                    custom = ""
                }
            }, enabled = custom.isNotBlank()) { Text("添加") }
        }
        Text("共 ${vm.state.rooms.size} 个空间 · 已选需求 ${vm.state.picked.size} 项",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Step 3: 分配需求到空间 ──
@Composable
private fun AssignStep(vm: PlannerViewModel) {
    val rooms = vm.state.rooms
    val picked = vm.state.picked
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (rooms.isEmpty()) {
            Text("请先添加空间(第 2 步)", style = MaterialTheme.typography.bodyMedium)
        } else if (picked.isEmpty()) {
            Text("需求车为空,请先选择需求(第 1 步)", style = MaterialTheme.typography.bodyMedium)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.autoAssign() }) { Text("按面积推荐分配") }
                OutlinedButton(onClick = { vm.setStep(4) }) { Text("跳过分配 →") }
            }
            Text("已选需求 ${picked.size} 项;勾选分配空间后可在第 4 步导出。", style = MaterialTheme.typography.bodySmall)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(picked, key = { it.itemKey }) { pick ->
                    SectionCard {
                        Column {
                            Text(pick.itemName, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            val assignedRooms = vm.state.assignments.filter { it.itemKey == pick.itemKey }
                            rooms.forEach { room ->
                                val a = assignedRooms.firstOrNull { it.roomId == room.id }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = a != null,
                                        onCheckedChange = { checked -> vm.toggleAssignment(pick, room.id, checked) },
                                    )
                                    Text(room.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    if (a != null) {
                                        Importance.entries.forEach { imp ->
                                            FilterChip(
                                                selected = a.importance == imp,
                                                onClick = { vm.setImportance(pick.itemKey, room.id, imp) },
                                                label = { Text(imp.label) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Step 4: 清单 ──
@Composable
private fun ResultStep(
    vm: PlannerViewModel,
    snackbar: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
) {
    val lines = vm.lines()
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("装修需求清单", style = MaterialTheme.typography.titleLarge)
        Text("按空间分组,共 ${lines.size} 行", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionCard {
            Column {
                lines.forEachIndexed { i, l ->
                    if (i > 0 && l.roomName != lines[i - 1].roomName) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Text("【${l.roomName}】${l.itemName}(${l.importance.label})", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                clipboard.setText(AnnotatedString(com.renovation.guardian.ui.planner.engine.PlannerTextBuilder.build(lines)))
                scope.launch { snackbar.showSnackbar("清单已复制") }
            }) { Text("复制文本") }
            OutlinedButton(onClick = { vm.setStep(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null); Text(" 重新规划") }
        }
    }
}
