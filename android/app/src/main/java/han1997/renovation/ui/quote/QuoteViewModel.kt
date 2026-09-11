package han1997.renovation.ui.quote

import android.app.Application
import androidx.compose.runtime.*
import han1997.renovation.data.db.QuotePlanEntity
import han1997.renovation.data.knowledge.DecoboxStateJson
import han1997.renovation.data.repo.BudgetApplyPreview
import han1997.renovation.ui.AppViewModel
import han1997.renovation.ui.quote.engine.*
import han1997.renovation.domain.quote.*
import han1997.renovation.util.IdGen
import kotlin.math.round

class QuoteViewModel(application: Application) : AppViewModel(application) {
    val catalog get() = container.knowledge.decoboxCatalog
    val plans = container.quotePlanRepo.observeAllPlans()
    val categories = container.budgetRepo.observeCategories()
    var state by mutableStateOf(QuotePlanState()); private set
    var editing by mutableStateOf(false); private set
    var editingPlanId by mutableStateOf<String?>(null); private set
    var planName by mutableStateOf(""); private set
    var editorRevision by mutableStateOf(0); private set
    private var savedState: QuotePlanState? = null
    val dirty get() = editing && (savedState == null || state.copy(wizardStep = 1) != savedState?.copy(wizardStep = 1))
    private val computed = derivedStateOf {
        try { catalog?.let { QuoteCalculator.calculate(it, state) } } catch (_: Exception) { null }
    }
    val result get() = computed.value
    fun update(change: (QuotePlanState) -> QuotePlanState) { state = change(state) }
    fun setStep(step: Int) { state = state.copy(wizardStep = step.coerceIn(1, 4)) }
    fun setMode(mode: QuoteMode) { state = state.copy(mode = mode, wizardStep = 1); editorRevision++ }
    fun newPlan() = perform(success = null, singleFlight = true) {
        val c = requireNotNull(catalog) { "报价目录不可用" }
        val profile = container.houseProfileRepo.get()
        state = QuotePlanState(house = HouseInfo(profile?.areaM2 ?: c.defaultHouse.totalArea,
            profile?.totalBudgetCents ?: han1997.renovation.util.MoneyUtil.fromYuan(c.defaultHouse.budget), c.defaultHouse.ceilingHeight))
        editingPlanId = null; savedState = null; planName = "新报价方案"; editing = true; editorRevision++
    }
    fun loadPlan(id: String) = perform(success = null, singleFlight = true) {
        val plan = requireNotNull(container.quotePlanRepo.getPlan(id)) { "方案已不存在" }
        val decoded = DecoboxStateJson.json.decodeFromString(QuotePlanState.serializer(), plan.stateJson)
        QuoteValidation.validate(decoded, catalog)
        state = decoded.copy(partialWorks = decoded.partialWorks.map { w -> w.copy(qtyRows = w.qtyRows.map { q -> if (q.id.isBlank()) q.copy(id = IdGen.new("pq")) else q }) }, rooms = decoded.rooms.map { r -> r.copy(mains = r.mains.map { if (it.id.isBlank()) it.copy(id = IdGen.new("qm")) else it }) })
        savedState = state; editingPlanId = plan.id; planName = plan.name; editing = true; editorRevision++
    }
    fun closeEditor() { editing = false }
    fun updateRoom(id: String, change: (QuoteRoomState) -> QuoteRoomState) { update { it.copy(rooms = it.rooms.map { r -> if (r.id == id) change(r) else r }) } }
    fun addRoom() { update { it.copy(rooms = it.rooms + QuoteRoomState(IdGen.new("qr"), "房间 ${it.rooms.size + 1}", 10.0, isDefault = false)) } }
    fun removeRoom(id: String) { update { it.copy(rooms = it.rooms.filterNot { r -> r.id == id }) } }
    fun recommendRooms() {
        val c = catalog ?: return
        val presets = c.roomPresets.firstOrNull { it.maxArea == null || state.house.totalArea <= it.maxArea }?.rooms.orEmpty()
        update { s -> s.copy(rooms = presets.map { QuoteRoomState(IdGen.new("qr"), it.roomName, round(s.house.totalArea * it.ratio * 10) / 10) }) }
        editorRevision++
    }
    fun unconfiguredRooms(): List<QuoteRoomState> = if (state.mode == QuoteMode.PARTIAL) emptyList() else state.rooms.filter {
        it.wall.categoryId.isBlank() && it.floor.categoryId.isBlank() && (it.ceilingPlan == CeilingPlan.NONE || it.ceiling.categoryId.isBlank()) &&
            it.mains.isEmpty() && it.extras.values.none { extra -> extra.enabled }
    }
    fun completionError(): String? {
        try { QuoteValidation.validate(state, catalog) } catch (e: Exception) { return e.message ?: "报价配置无效" }
        if (!state.house.totalArea.isFinite() || state.house.totalArea <= 0 || state.house.ceilingHeight <= 0) return "请填写有效房屋信息"
        if (state.mode != QuoteMode.PARTIAL && (state.rooms.isEmpty() || state.rooms.any { it.name.isBlank() || it.area <= 0 || !it.area.isFinite() })) return "请添加空间并填写有效名称与面积"
        if (state.mode == QuoteMode.PARTIAL && state.partialWorks.none { it.enabled && it.qtyRows.any { q -> q.area > 0 } }) return "请启用局改事项并填写施工面积"
        if (result == null || result?.lines.isNullOrEmpty()) return "请先配置选材或工程项目"
        return null
    }
    fun savePlan(name: String, asNew: Boolean = false, onSaved: () -> Unit = {}) = perform(onSuccess = onSaved, singleFlight = true) {
        require(name.isNotBlank()) { "请填写方案名称" }
        check(completionError() == null) { completionError().orEmpty() }
        val snapshot = state
        val text = DecoboxStateJson.json.encodeToString(QuotePlanState.serializer(), snapshot)
        val id = editingPlanId.takeUnless { asNew }
        if (id == null) editingPlanId = container.quotePlanRepo.savePlan(name.trim(), snapshot.mode.name.lowercase(), text, container.todayProvider()).id
        else container.quotePlanRepo.updatePlanState(id, text, container.todayProvider(), name.trim(), snapshot.mode.name.lowercase())
        planName = name.trim(); savedState = snapshot
    }
    fun renamePlan(plan: QuotePlanEntity, name: String, onSaved: () -> Unit) = perform(onSuccess = onSaved, singleFlight = true) { container.quotePlanRepo.renamePlan(plan.id, name, container.todayProvider()) }
    fun deletePlan(id: String) = perform("已删除") { container.quotePlanRepo.deletePlan(id) }
    fun applyBudget(preview: BudgetApplyPreview, onDone: () -> Unit) = perform("已更新分类计划，总预算上限和支出未变", onDone, singleFlight = true) {
        check(result?.estimatedTotalCents == preview.estimatedTotalCents) { "报价已变化，请重新预览" }
        container.quotePlanRepo.applyBudgetPreview(preview)
    }
}
