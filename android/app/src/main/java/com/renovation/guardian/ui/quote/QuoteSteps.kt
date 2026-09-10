package com.renovation.guardian.ui.quote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.renovation.guardian.ui.components.*
import com.renovation.guardian.ui.quote.engine.*
import com.renovation.guardian.domain.quote.*
import com.renovation.guardian.util.IdGen

@Composable
internal fun HouseStep(vm: QuoteViewModel) {
    val house = vm.state.house
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("房屋信息", style = MaterialTheme.typography.titleLarge)
            NumberField("建筑面积（㎡）", house.totalArea) { n -> vm.update { it.copy(house = it.house.copy(totalArea = n!!)) } }
            NumberField("完成面层高（m）", house.ceilingHeight) { n -> vm.update { it.copy(house = it.house.copy(ceilingHeight = n!!)) } }
            MoneyField("本方案预算（元）", house.budgetCents) { n -> vm.update { it.copy(house = it.house.copy(budgetCents = n)) } }
            Text("默认读取房屋总预算。这里的修改只影响当前方案，不改变房屋预算上限。", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun RoomsStep(vm: QuoteViewModel) {
    var recommend by remember { mutableStateOf(false) }
    Text("划分空间", style = MaterialTheme.typography.titleLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.addRoom() }) { Text("添加空间") }
        OutlinedButton(onClick = { if (vm.state.rooms.isEmpty()) vm.recommendRooms() else recommend = true }) { Text("按面积推荐") }
    }
    if (vm.state.rooms.isEmpty()) Text("先添加空间，或按建筑面积生成可编辑的建议。")
    vm.state.rooms.forEach { room -> key(room.id) {
        SectionCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(room.name, { name -> vm.updateRoom(room.id) { it.copy(name = name) } }, label = { Text("空间名称") },
                isError = room.name.isBlank(), supportingText = { if (room.name.isBlank()) Text("请输入名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            NumberField("地面面积（㎡）", room.area) { area -> vm.updateRoom(room.id) { it.copy(area = area!!) } }
            DeleteAction("删除空间", "同时删除「${room.name}」的选材与工程配置。") { vm.removeRoom(room.id) }
        } }
    } }
    if (recommend) ConfirmDeleteDialog(title = "重新推荐空间", text = "将替换当前空间与选材，可在生成后继续修改。",
        onConfirm = { vm.recommendRooms(); recommend = false }, onDismiss = { recommend = false })
}

@Composable
internal fun MaterialsStep(vm: QuoteViewModel) {
    val c = vm.catalog ?: return
    val form = LocalFormState.current
    var selected by rememberSaveable { mutableStateOf(vm.state.rooms.firstOrNull()?.id.orEmpty()) }
    val room = vm.state.rooms.firstOrNull { it.id == selected } ?: vm.state.rooms.firstOrNull()
    if (room == null) { Text("请返回上一步添加空间。" ); return }
    ChoiceField("正在配置的空间", room.id, vm.state.rooms.map { it.id to it.name }, enabled = form?.valid != false) { selected = it }
    key(room.id) {
        SectionCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SurfaceEditor("墙面", room.wall, c.wall) { v -> vm.updateRoom(room.id) { it.copy(wall = v) } }
            HorizontalDivider()
            SurfaceEditor("地面", room.floor, c.floor) { v -> vm.updateRoom(room.id) { it.copy(floor = v) } }
            HorizontalDivider()
            ChoiceField("吊顶方案", room.ceilingPlan, CeilingPlan.entries.map { it to it.label }) { plan -> vm.updateRoom(room.id) {
                it.copy(ceilingPlan = plan, partialCeilingArea = if (it.partialCeilingArea > 0) it.partialCeilingArea else QuoteCalculator.suggestPartialCeiling(it.area),
                    ceiling = if (it.ceiling.categoryId.isBlank()) c.ceiling.firstOrNull()?.let { cat -> surfaceDefault(cat) } ?: it.ceiling else it.ceiling)
            } }
            if (room.ceilingPlan == CeilingPlan.PARTIAL) NumberField("局部吊顶面积（㎡）", room.partialCeilingArea, maxValue = minOf(room.area, QuoteCalculator.suggestPartialCeiling(room.area))) { n -> vm.updateRoom(room.id) { it.copy(partialCeilingArea = n!!) } }
            if (room.ceilingPlan != CeilingPlan.NONE) SurfaceEditor("顶面", room.ceiling, c.ceiling, maxArea = if (room.ceilingPlan == CeilingPlan.PARTIAL) minOf(room.area, QuoteCalculator.suggestPartialCeiling(room.area)) else room.area) { v -> vm.updateRoom(room.id) { it.copy(ceiling = v) } }
            else Text("墙面选乳胶漆时，不吊顶的顶面按同款漆及工艺计价。", style = MaterialTheme.typography.bodySmall)
        } }
        SectionCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("空间附加工程", style = MaterialTheme.typography.titleMedium)
            c.spaceExtras.forEach { extra ->
                val value = room.extras[extra.id] ?: ExtraInput(waterproofFloorArea = room.area, wallDemolitionArea = room.area)
                fun change(v: ExtraInput) { vm.updateRoom(room.id) { it.copy(extras = it.extras + (extra.id to v)) } }
                ToggleRow("${extra.label}（¥${extra.unitPrice}/${extra.unit}）", value.enabled) { change(value.copy(enabled = it)) }
                if (value.enabled) key(extra.id) { when (extra.id) {
                    "waterproof" -> {
                        ToggleRow("按空间估算防水面积", value.waterproofUseEstimate) { change(value.copy(waterproofUseEstimate = it)) }
                        if (!value.waterproofUseEstimate) {
                            NumberField("防水地面面积（㎡）", value.waterproofFloorArea, allowZero = true) { change(value.copy(waterproofFloorArea = it!!)) }
                            NumberField("防水墙面面积（㎡）", value.waterproofWallArea, allowZero = true) { change(value.copy(waterproofWallArea = it!!)) }
                        } else Text("按空间墙地面积估算，实际施工范围不同请关闭估算手动填写。", style = MaterialTheme.typography.bodySmall)
                    }
                    "pipeWrap" -> NumberField("包管根数", value.pipeWrapCount.toDouble(), integer = true) { change(value.copy(pipeWrapCount = it!!.toInt())) }
                    else -> NumberField("拆改面积（㎡）", value.wallDemolitionArea, allowZero = true) { change(value.copy(wallDemolitionArea = it!!)) }
                } }
            }
        } }
        var mainId by rememberSaveable { mutableStateOf(c.otherMains.firstOrNull()?.id.orEmpty()) }
        SectionCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("其他主材", style = MaterialTheme.typography.titleMedium)
            ChoiceField("添加主材类型", mainId, c.otherMains.map { it.id to it.label }) { mainId = it }
            OutlinedButton(onClick = {
                c.otherMains.firstOrNull { it.id == mainId }?.let { main -> main.types.firstOrNull()?.let { type ->
                    vm.updateRoom(room.id) { it.copy(mains = it.mains + mainDefault(c, main, type)) }
                } }
            }) { Text("添加主材") }
        } }
        room.mains.forEach { selection -> key(selection.id) {
            val main = c.otherMains.firstOrNull { it.id == selection.mainId }
            if (main != null) SectionCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(main.label, style = MaterialTheme.typography.titleMedium)
                MainEditor(c, main, selection) { updated -> vm.updateRoom(room.id) { it.copy(mains = it.mains.map { m -> if (m.id == selection.id) updated else m }) } }
                DeleteAction("删除主材", "从本空间移除这项主材？") { vm.updateRoom(room.id) { it.copy(mains = it.mains.filterNot { m -> m.id == selection.id }) } }
            } }
        } }
    }
    HouseWorksStep(vm)
}

@Composable
private fun HouseWorksStep(vm: QuoteViewModel) {
    val c = vm.catalog ?: return
    SectionCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("全屋工程", style = MaterialTheme.typography.titleMedium)
        val labels = mapOf("plumbing" to "水电工程", "hauling" to "垃圾清运", "cleaning" to "保洁", "protection" to "成品保护")
        c.houseWorks.forEach { (id, price) ->
            val value = vm.state.houseWorks[id] ?: HouseWorkInput()
            val label = labels[id] ?: id
            ToggleRow("$label（默认 ¥$price${if (id == "plumbing") "/㎡" else "/项"}）", value.enabled) { enabled ->
                vm.update { it.copy(houseWorks = it.houseWorks + (id to value.copy(enabled = enabled))) }
            }
            if (value.enabled) key(id) { NumberField("$label 自定义单价（元）", value.priceOverride?.toDouble(), optional = true, allowZero = true, integer = true) { n ->
                vm.update { it.copy(houseWorks = it.houseWorks + (id to value.copy(priceOverride = n?.toInt()))) }
            } }
        }
        if (vm.state.mode == QuoteMode.FULL) NumberField("管理费率（%，留空按目录）", vm.state.managementRateOverride?.toDouble(), optional = true, allowZero = true, integer = true, maxValue = 100.0) { n ->
            vm.update { it.copy(managementRateOverride = n?.toInt()) }
        }
    } }
}

@Composable
internal fun PartialWorksStep(vm: QuoteViewModel) {
    val c = vm.catalog ?: return
    Text("局改事项与施工数量", style = MaterialTheme.typography.titleLarge)
    c.partialItems.forEach { item -> key(item.id) {
        val input = vm.state.partialWorks.firstOrNull { it.itemId == item.id } ?: PartialWorkInput(itemId = item.id, tierId = item.tiers.firstOrNull()?.id.orEmpty(),
            qtyRows = listOf(PartialQtyRow("施工区域", vm.state.house.totalArea, IdGen.new("pq"))))
        fun change(v: PartialWorkInput) { vm.update { it.copy(partialWorks = it.partialWorks.filterNot { w -> w.itemId == item.id } + v) } }
        SectionCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleRow(item.label, input.enabled) { change(input.copy(enabled = it)) }
            if (input.enabled) {
                ChoiceField("工艺 / 材料档位", input.tierId, item.tiers.map { it.id to it.label }) { change(input.copy(tierId = it, floorSelection = null)) }
                item.tiers.firstOrNull { it.id == input.tierId }?.note?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                input.qtyRows.forEachIndexed { index, row -> key(row.id) {
                    OutlinedTextField(row.roomName, { name -> change(input.copy(qtyRows = input.qtyRows.mapIndexed { i, q -> if (i == index) q.copy(roomName = name) else q })) },
                        label = { Text("施工区域") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    NumberField("施工面积（㎡）", row.area) { n -> change(input.copy(qtyRows = input.qtyRows.mapIndexed { i, q -> if (i == index) q.copy(area = n!!) else q })) }
                    DeleteAction("删除施工区域", "删除此区域的施工数量？") { change(input.copy(qtyRows = input.qtyRows.filterIndexed { i, _ -> i != index })) }
                } }
                OutlinedButton(onClick = { change(input.copy(qtyRows = input.qtyRows + PartialQtyRow("区域 ${input.qtyRows.size + 1}", 10.0, IdGen.new("pq")))) }) { Text("添加施工区域") }
                if (item.id == "wall-refresh") {
                    NumberField("工艺自定义单价（元/㎡）", input.priceOverride?.toDouble(), optional = true, allowZero = true, integer = true) { change(input.copy(priceOverride = it?.toInt())) }
                    if (input.tierId == "full") c.wall.firstOrNull { it.id == "wall-paint" }?.let { paint ->
                        VariantPicker(paint, input.paintVariantId, input.paintSpecId) { variant, spec -> change(input.copy(paintVariantId = variant, paintSpecId = spec)) }
                        // 默认项也必须写入状态，而不是仅在下拉框显示默认值。
                        LaunchedEffect(input.tierId, input.paintSpecId) {
                            if (input.paintSpecId == null) change(input.copy(paintVariantId = paint.variants.firstOrNull()?.id, paintSpecId = paint.variants.firstOrNull()?.specs?.firstOrNull()?.id))
                        }
                        ChoiceField("乳胶漆采购方式", input.paintSourcing, Sourcing.entries.map { it to it.label }) { change(input.copy(paintSourcing = it)) }
                    }
                } else {
                    val cat = c.floor.firstOrNull { it.id == item.tiers.firstOrNull { t -> t.id == input.tierId }?.catalogCategoryId }
                    if (cat != null) SurfaceEditor("局改地面", (input.floorSelection ?: surfaceDefault(cat, Sourcing.INCLUDED)).copy(categoryId = cat.id), listOf(cat), allowNone = false, areaAndCraft = false) { change(input.copy(floorSelection = it)) }
                }
            }
        } }
    } }
}

@Composable
internal fun PartialFeesStep(vm: QuoteViewModel) {
    Text("局改附加费用", style = MaterialTheme.typography.titleLarge)
    mapOf("demo" to "拆旧", "protect" to "成品保护", "hauling" to "垃圾清运").forEach { (id, label) -> key(id) {
        val value = vm.state.partialFees[id] ?: PartialFeeInput()
        fun change(v: PartialFeeInput) { vm.update { it.copy(partialFees = it.partialFees + (id to v)) } }
        SectionCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleRow(label, value.enabled) { change(value.copy(enabled = it)) }
            if (value.enabled) {
                ToggleRow("自定义费用", value.amountOverrideCents != null) { change(value.copy(amountOverrideCents = if (it) 0L else null)) }
                if (value.amountOverrideCents != null) MoneyField("$label（元）", value.amountOverrideCents) { change(value.copy(amountOverrideCents = it)) }
                else Text("根据已填施工数量与目录规则计算，可在清单核对。", style = MaterialTheme.typography.bodySmall)
            }
        } }
    } }
}
