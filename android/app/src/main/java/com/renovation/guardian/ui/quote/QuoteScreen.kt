package com.renovation.guardian.ui.quote

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.data.knowledge.MaterialVariant
import com.renovation.guardian.data.knowledge.SurfaceCategory
import com.renovation.guardian.ui.components.SectionCard
import com.renovation.guardian.ui.quote.engine.CeilingPlan
import com.renovation.guardian.ui.quote.engine.QuoteMode
import com.renovation.guardian.ui.quote.engine.QuoteRoomState
import com.renovation.guardian.ui.quote.engine.Sourcing
import com.renovation.guardian.ui.quote.engine.SurfaceSelection
import com.renovation.guardian.util.MoneyUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteScreen(onBack: () -> Unit = {}) {
    val vm: QuoteViewModel = viewModel()
    val clipboard = LocalClipboardManager.current
    val catalog = vm.catalog
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showResult by remember { mutableStateOf(false) }
    val result = vm.result

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("逐空间报价") },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 模式选择
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuoteMode.entries.forEach { m ->
                    FilterChip(
                        selected = vm.state.mode == m,
                        onClick = { vm.setMode(m) },
                        label = { Text(m.label) },
                    )
                }
            }
            when {
                showResult && result != null -> ResultStep(vm, result, snackbar, scope, clipboard) { showResult = false }
                else -> {
                    HouseStep(vm)
                    SpacesStep(vm, catalog.wall + catalog.ceiling + catalog.floor)
                    HouseWorksStep(vm)
                    Button(
                        onClick = { showResult = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null); Text("  生成清单") }
                }
            }
        }
    }
}

// ── Step 1: 房屋信息 ──
@Composable
private fun HouseStep(vm: QuoteViewModel) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("房屋信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            var area by remember { mutableStateOf(vm.state.house.totalArea.toString()) }
            var budget by remember { mutableStateOf(MoneyUtil.format(vm.state.house.budgetCents)) }
            var height by remember { mutableStateOf(vm.state.house.ceilingHeight.toString()) }
            OutlinedTextField(
                value = area, onValueChange = { area = it },
                label = { Text("总建面(m2)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = budget, onValueChange = { budget = it },
                label = { Text("预算(元)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = height, onValueChange = { height = it },
                label = { Text("完成面层高(m)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    vm.setHouse(
                        area.toDoubleOrNull() ?: 0.0,
                        (budget.replace(",", "").toDoubleOrNull() ?: 0.0).let { MoneyUtil.fromYuan(it) },
                        height.toDoubleOrNull() ?: 2.4,
                    )
                },
                enabled = (area.toDoubleOrNull() ?: 0.0) > 0,
            ) { Text("应用") }
        }
    }
}

// ── Step 2: 空间划分 + 逐空间选材 ──
@Composable
private fun SpacesStep(vm: QuoteViewModel, allCats: List<SurfaceCategory>) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("空间与选材", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            vm.state.rooms.forEachIndexed { idx, room ->
                RoomRow(vm, idx, room, allCats)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { addRecommendedRoom(vm) }) { Text("按面积推荐") }
                OutlinedButton(onClick = { addEmptyRoom(vm) }) { Text("添加房间") }
            }
        }
    }
}

@Composable
private fun RoomRow(vm: QuoteViewModel, idx: Int, room: QuoteRoomState, allCats: List<SurfaceCategory>) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 6.dp)) {
            Text(room.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("${(room.area * 10).toInt() / 10.0}m2", style = MaterialTheme.typography.bodySmall)
            Text(if (expanded) "▲" else "▼", color = MaterialTheme.colorScheme.primary)
        }
        if (expanded) {
            RoomEditor(vm, idx, room, allCats)
        }
    }
}

@Composable
private fun RoomEditor(vm: QuoteViewModel, idx: Int, room: QuoteRoomState, allCats: List<SurfaceCategory>) {
    var name by remember { mutableStateOf(room.name) }
    var area by remember { mutableStateOf(room.area.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("房间名") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("面积") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
        }
        // 墙面
        SurfacePicker(label = "墙面", sel = room.wall,
            onChange = { s -> vm.updateRoom(idx) { it.copy(wall = s) } },
            cats = allCats.filter { it.id.startsWith("wall") },
        )
        // 地面
        SurfacePicker(label = "地面", sel = room.floor,
            onChange = { s -> vm.updateRoom(idx) { it.copy(floor = s) } },
            cats = allCats.filter { it.id.startsWith("floor") },
        )
        Button(onClick = {
            vm.updateRoom(idx) {
                it.copy(name = name.ifBlank { it.name }, area = area.toDoubleOrNull() ?: it.area)
            }
        }) { Text("应用房间设置") }
    }
}

@Composable
private fun SurfacePicker(
    label: String,
    sel: SurfaceSelection,
    onChange: (SurfaceSelection) -> Unit,
    cats: List<SurfaceCategory>,
) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val category = cats.firstOrNull { it.id == sel.categoryId } ?: cats.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cats.forEach { c ->
                FilterChip(selected = sel.categoryId == c.id, onClick = {
                    onChange(sel.copy(categoryId = c.id, variantId = c.variants.firstOrNull()?.id,
                        specId = c.variants.firstOrNull()?.specs?.firstOrNull()?.id))
                }, label = { Text(c.label) })
            }
        }
        if (category != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                category.variants.forEach { v ->
                    FilterChip(selected = sel.variantId == v.id, onClick = {
                        onChange(sel.copy(variantId = v.id, specId = v.specs.firstOrNull()?.id))
                    }, label = { Text(variantLabel(v)) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = sel.sourcing == Sourcing.INCLUDED,
                onClick = { onChange(sel.copy(sourcing = Sourcing.INCLUDED)) }, label = { Text("施工方代购") })
            FilterChip(selected = sel.sourcing == Sourcing.SELF,
                onClick = { onChange(sel.copy(sourcing = Sourcing.SELF)) }, label = { Text("自购") })
        }
    }
}

private fun variantLabel(v: MaterialVariant): String = v.brand.ifBlank { v.id } + if (v.series.isNotBlank()) "·${v.series}" else ""

private fun addRecommendedRoom(vm: QuoteViewModel) {
    val rooms = vm.state.rooms.toMutableList()
    rooms += QuoteRoomState(
        id = "r${System.currentTimeMillis()}",
        name = "空间${rooms.size + 1}",
        area = (vm.state.house.totalArea / 3).let { (it * 10).toInt() / 10.0 },
    )
    vm.setRooms(rooms)
}

private fun addEmptyRoom(vm: QuoteViewModel) {
    val rooms = vm.state.rooms.toMutableList()
    rooms += QuoteRoomState(id = "r${System.currentTimeMillis()}", name = "空间${rooms.size + 1}", area = 0.0)
    vm.setRooms(rooms)
}

// ── 全屋工程(整装/半包) ──
@Composable
private fun HouseWorksStep(vm: QuoteViewModel) {
    if (vm.state.mode == QuoteMode.PARTIAL) return
    val names = mapOf("plumbing" to "水电改造", "hauling" to "垃圾清运", "cleaning" to "开荒保洁", "protection" to "成品保护")
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("全屋工程", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            names.forEach { (id, label) ->
                val input = vm.state.houseWorks[id]
                FilterChip(selected = input?.enabled == true, onClick = { vm.setHouseWorks(id, input?.enabled != true, null) }, label = { Text(label) })
            }
        }
    }
}

// ── 结果页 ──
@Composable
private fun ResultStep(
    vm: QuoteViewModel,
    result: com.renovation.guardian.ui.quote.engine.QuoteResult,
    snackbar: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    onBackEdit: () -> Unit,
) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("报价清单", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val total = result.totalCents
            Text("预计总价:¥${MoneyUtil.format(total)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            if (result.overBudget) {
                Text("⚠ 超出预算:¥${MoneyUtil.format(result.overBudgetCents)}", color = MaterialTheme.colorScheme.error)
            } else {
                Text("预算剩余:¥${MoneyUtil.format(result.budgetRemainCents)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            result.lines.take(30).forEach { l ->
                Text("${l.label}  ${l.quantityText}${l.unit} × ¥${MoneyUtil.format(l.unitPriceCents)} = ¥${MoneyUtil.format(l.subtotalCents)}${if (l.sourcing == Sourcing.SELF) " (自购)" else ""}",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (result.lines.size > 30) {
                Text("…共 ${result.lines.size} 行", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onBackEdit() }) { Text("返回修改") }
                Button(onClick = {
                    val text = com.renovation.guardian.ui.quote.export.QuoteTextBuilder.build(
                        result, vm.state.mode.label, vm.state.house.totalArea, vm.state.house.ceilingHeight,
                    )
                    clipboard.setText(AnnotatedString(text))
                    scope.launch { snackbar.showSnackbar("清单已复制") }
                }) { Text("复制文本") }
                Button(onClick = { vm.savePlan("方案 ${vm.state.mode.label}", { ok -> scope.launch { snackbar.showSnackbar(if (ok) "已保存" else "保存失败") } }) }) { Text("保存方案") }
                Button(onClick = {
                    vm.writeToBudget { ok -> scope.launch { snackbar.showSnackbar(if (ok) "已写入预算" else "写入失败") } }
                }) { Text("写入预算") }
            }
        }
    }
}
