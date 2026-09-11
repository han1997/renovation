package han1997.renovation.ui.quote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import han1997.renovation.data.knowledge.*
import han1997.renovation.ui.components.*
import han1997.renovation.ui.quote.engine.*
import han1997.renovation.domain.quote.*
import han1997.renovation.util.IdGen

internal fun surfaceDefault(cat: SurfaceCategory, sourcing: Sourcing = Sourcing.SELF) = SurfaceSelection(
    categoryId = cat.id, variantId = cat.variants.firstOrNull()?.id, specId = cat.variants.firstOrNull()?.specs?.firstOrNull()?.id,
    craftIds = cat.craftSteps.map { it.id }, sourcing = sourcing)

@Composable
internal fun SurfaceEditor(label: String, selection: SurfaceSelection, categories: List<SurfaceCategory>, maxArea: Double = 100000000.0, allowNone: Boolean = true, areaAndCraft: Boolean = true, onChange: (SurfaceSelection) -> Unit) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    ChoiceField("$label 材料", selection.categoryId, (if (allowNone) listOf("" to "不配置此项") else emptyList()) + categories.map { it.id to it.label }) { id ->
        onChange(categories.firstOrNull { it.id == id }?.let { surfaceDefault(it, selection.sourcing).copy(areaOverride = selection.areaOverride) } ?: SurfaceSelection())
    }
    val cat = categories.firstOrNull { it.id == selection.categoryId } ?: return
    key(cat.id) {
        VariantPicker(cat, selection.variantId, selection.specId, onVariant = { variant, spec ->
            onChange(selection.copy(variantId = variant, specId = spec, priceOverrideYuan = null))
        })
        ChoiceField("$label 采购方式", selection.sourcing, Sourcing.entries.map { it to it.label }) { onChange(selection.copy(sourcing = it)) }
        if (areaAndCraft) {
        if (cat.fixedCrafts) Text("已含工艺：${cat.craftSteps.joinToString("、") { it.label }}", style = MaterialTheme.typography.bodySmall)
        else cat.craftSteps.forEach { craft ->
            ToggleRow("${craft.label}（¥${craft.unitPrice}/㎡）", craft.id in selection.craftIds) { enabled ->
                onChange(selection.copy(craftIds = if (enabled) (selection.craftIds + craft.id).distinct() else selection.craftIds - craft.id))
            }
        }
        NumberField("$label 实际面积（㎡）", selection.areaOverride, optional = true, allowZero = true, maxValue = maxArea) { onChange(selection.copy(areaOverride = it)) }
        } else Text("施工面积取各区域数量，安装工艺按目录固定计入。", style = MaterialTheme.typography.bodySmall)
        key(selection.variantId, selection.specId) {
            NumberField("$label 自定义主材单价（元）", selection.priceOverrideYuan?.toDouble(), optional = true, allowZero = true, integer = true) {
                onChange(selection.copy(priceOverrideYuan = it?.toInt()))
            }
        }
    }
}

@Composable
internal fun VariantPicker(cat: SurfaceCategory, variantId: String?, specId: String?, onVariant: (String?, String?) -> Unit) {
    val variant = cat.variants.firstOrNull { it.id == variantId } ?: cat.variants.firstOrNull()
    ChoiceField("品牌 / 系列", variant?.id, cat.variants.map { it.id as String? to (it.brand + " " + it.series).trim().ifBlank { it.id } }) { id ->
        onVariant(id, cat.variants.firstOrNull { it.id == id }?.specs?.firstOrNull()?.id)
    }
    ChoiceField("材料规格", specId ?: variant?.specs?.firstOrNull()?.id, variant?.specs.orEmpty().map { it.id as String? to "${it.label} · ¥${it.unitPrice}/${it.unitLabel}" }) { onVariant(variant?.id, it) }
}

@Composable
internal fun DeleteAction(label: String = "删除", description: String, onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    TextButton(onClick = { confirm = true }) { Text(label, color = MaterialTheme.colorScheme.error) }
    if (confirm) ConfirmDeleteDialog(title = label, text = description,
        onConfirm = { onDelete(); confirm = false }, onDismiss = { confirm = false })
}

internal fun mainDefault(c: DecoboxCatalogJson, main: MainMaterial, type: MainMaterialType, id: String = IdGen.new("qm"), sourcing: Sourcing = Sourcing.SELF): MainSelection {
    val door = c.doors.firstOrNull()
    val heater = c.heaterBrands.firstOrNull()?.id
    val toilet = c.toiletBrands.firstOrNull()?.id
    return MainSelection(mainId = main.id, typeId = type.id, specId = if (type.id == "door-wood") door?.series?.firstOrNull()?.id.orEmpty() else type.specs.firstOrNull()?.id.orEmpty(),
        doorBrandId = door?.id, doorGlassId = c.doorGlasses[type.specs.firstOrNull()?.id]?.firstOrNull()?.id,
        windowsillEdgeId = c.windowsillEdges.firstOrNull()?.id,
        heaterBrandId = heater, heaterTierId = c.heaterTiers.firstOrNull { it.id in c.heaterMatrix[heater].orEmpty() }?.id,
        toiletBrandId = toilet, toiletKindId = c.toiletKinds.firstOrNull { it.id in c.toiletMatrix[toilet].orEmpty() }?.id,
        sourcing = sourcing, id = id)
}

@Composable
internal fun MainEditor(c: DecoboxCatalogJson, main: MainMaterial, value: MainSelection, onChange: (MainSelection) -> Unit) {
    val type = main.types.firstOrNull { it.id == value.typeId } ?: return
    ChoiceField("${main.label} 类型", value.typeId, main.types.map { it.id to it.label }) { id ->
        main.types.firstOrNull { it.id == id }?.let { onChange(mainDefault(c, main, it, value.id, value.sourcing)) }
    }
    key(value.typeId) {
        when {
            main.id == "door" && type.id == "door-wood" -> {
                val brand = c.doors.firstOrNull { it.id == value.doorBrandId } ?: c.doors.firstOrNull()
                ChoiceField("木门品牌", brand?.id, c.doors.map { it.id as String? to it.label }) { id ->
                    onChange(value.copy(doorBrandId = id, specId = c.doors.firstOrNull { it.id == id }?.series?.firstOrNull()?.id.orEmpty()))
                }
                ChoiceField("系列", value.specId, brand?.series.orEmpty().map { it.id to "${it.label} · ¥${it.unitPrice}" }) { onChange(value.copy(specId = it)) }
                val frames = brand?.series?.firstOrNull { it.id == value.specId }?.framePrices.orEmpty()
                if (frames.isNotEmpty()) ChoiceField("门套", value.doorFrame, frames.map { it.key to "${if (it.key == "double") "双包" else "单包"} · ¥${it.value}" }) { onChange(value.copy(doorFrame = it)) }
            }
            main.id == "sanitary" && type.id == "sanitary-heater" -> {
                ChoiceField("风暖品牌", value.heaterBrandId, c.heaterBrands.map { it.id as String? to it.label }) { id ->
                    onChange(value.copy(heaterBrandId = id, heaterTierId = c.heaterTiers.firstOrNull { it.id in c.heaterMatrix[id].orEmpty() }?.id))
                }
                ChoiceField("风暖档位", value.heaterTierId, c.heaterTiers.filter { it.id in c.heaterMatrix[value.heaterBrandId].orEmpty() }.map { it.id as String? to "${it.label} · ¥${c.heaterMatrix[value.heaterBrandId]?.get(it.id)}" }) { onChange(value.copy(heaterTierId = it)) }
            }
            main.id == "sanitary" && type.id == "sanitary-toilet" -> {
                ChoiceField("坐便品牌", value.toiletBrandId, c.toiletBrands.map { it.id as String? to it.label }) { id ->
                    onChange(value.copy(toiletBrandId = id, toiletKindId = c.toiletKinds.firstOrNull { it.id in c.toiletMatrix[id].orEmpty() }?.id))
                }
                ChoiceField("坐便类型", value.toiletKindId, c.toiletKinds.filter { it.id in c.toiletMatrix[value.toiletBrandId].orEmpty() }.map { it.id as String? to "${it.label} · ¥${c.toiletMatrix[value.toiletBrandId]?.get(it.id)}" }) { onChange(value.copy(toiletKindId = it)) }
            }
            else -> ChoiceField("规格", value.specId, type.specs.map { it.id to "${it.label} · ¥${it.unitPrice}/${it.unitLabel}" }) {
                onChange(value.copy(specId = it, doorGlassId = c.doorGlasses[it]?.firstOrNull()?.id))
            }
        }
        if (main.id == "door") {
            NumberField("门洞高度（mm）", value.doorHeightMm.toDouble(), integer = true) { onChange(value.copy(doorHeightMm = it!!.toInt())) }
            NumberField("门洞宽度（mm）", value.doorWidthMm.toDouble(), integer = true) { onChange(value.copy(doorWidthMm = it!!.toInt())) }
            if (type.id == "door-metal-slide") ChoiceField("玻璃", value.doorGlassId, c.doorGlasses[value.specId].orEmpty().map { it.id as String? to "${it.label} · ¥${it.unitPrice}" }) { onChange(value.copy(doorGlassId = it)) }
        }
        if (main.id == "windowsill") ChoiceField("窗台石边型", value.windowsillEdgeId, c.windowsillEdges.map { it.id as String? to "${it.label} · ¥${it.unitPrice}/m" }) { onChange(value.copy(windowsillEdgeId = it)) }
        NumberField(if (main.id == "windowsill") "计价长度（m，含左右耳）" else "计价数量", value.qtyOverride, optional = true) { onChange(value.copy(qtyOverride = it)) }
        NumberField("自定义完整单价（元）", value.priceOverride?.toDouble(), optional = true, allowZero = true, integer = true) { onChange(value.copy(priceOverride = it?.toInt())) }
        ChoiceField("采购方式", value.sourcing, Sourcing.entries.map { it to it.label }) { onChange(value.copy(sourcing = it)) }
    }
}
