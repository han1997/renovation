package com.renovation.guardian.ui.quote

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.renovation.guardian.data.knowledge.DecoboxCatalogJson
import com.renovation.guardian.ui.AppViewModel
import com.renovation.guardian.ui.quote.engine.HouseInfo
import com.renovation.guardian.ui.quote.engine.QuoteCalculator
import com.renovation.guardian.ui.quote.engine.QuoteMode
import com.renovation.guardian.ui.quote.engine.QuotePlanState
import com.renovation.guardian.ui.quote.engine.QuoteResult
import com.renovation.guardian.ui.quote.engine.QuoteRoomState
import kotlinx.coroutines.launch

/**
 * 逐空间报价向导状态 + 计算。state 以可变状态持有,由 [QuoteScreen] 渲染;
 * 每次改动即时重算 [result]。方案保存走 [com.renovation.guardian.data.repo.QuotePlanRepository]。
 */
class QuoteViewModel(application: Application) : AppViewModel(application) {

    val catalog: DecoboxCatalogJson?
        get() = container.knowledge.decoboxCatalog

    var state by mutableStateOf(QuotePlanState())
        private set

    var editingPlanId by mutableStateOf<String?>(null)
        private set

    val result: QuoteResult?
        get() = catalog?.let { QuoteCalculator.calculate(it, state) }

    private val today: String get() = container.todayProvider()

    fun newPlan() {
        val c = catalog ?: return
        state = QuotePlanState(
            mode = QuoteMode.FULL,
            house = HouseInfo(
                totalArea = c.defaultHouse.totalArea,
                budgetCents = com.renovation.guardian.util.MoneyUtil.fromYuan(c.defaultHouse.budget),
                ceilingHeight = c.defaultHouse.ceilingHeight,
            ),
        )
        editingPlanId = null
    }

    fun loadPlan(id: String) {
        viewModelScope.launch {
            val plan = container.quotePlanRepo.getPlan(id) ?: return@launch
            val decoded = runCatching {
                com.renovation.guardian.data.knowledge.DecoboxStateJson.json.decodeFromString(
                    QuotePlanState.serializer(), plan.stateJson,
                )
            }.getOrNull() ?: return@launch
            state = decoded
            editingPlanId = id
        }
    }

    fun setMode(mode: QuoteMode) {
        state = state.copy(mode = mode, wizardStep = 1)
    }

    fun setHouse(totalArea: Double, budgetCents: Long, ceilingHeight: Double) {
        state = state.copy(house = HouseInfo(totalArea, budgetCents, ceilingHeight))
    }

    fun setStep(step: Int) {
        state = state.copy(wizardStep = step)
    }

    // ---- 空间 ----
    fun setRooms(rooms: List<QuoteRoomState>) {
        state = state.copy(rooms = rooms)
    }

    fun updateRoom(index: Int, transform: (QuoteRoomState) -> QuoteRoomState) {
        val rooms = state.rooms.toMutableList()
        if (index in rooms.indices) rooms[index] = transform(rooms[index])
        state = state.copy(rooms = rooms)
    }

    fun setHouseWorks(id: String, enabled: Boolean, priceOverride: Int?) {
        val cur = state.houseWorks[id] ?: com.renovation.guardian.ui.quote.engine.HouseWorkInput()
        val map = state.houseWorks.toMutableMap()
        map[id] = cur.copy(enabled = enabled, priceOverride = priceOverride)
        state = state.copy(houseWorks = map)
    }

    /** 保存当前方案为新方案或覆盖。 */
    fun savePlan(name: String, onDone: (Boolean) -> Unit) {
        val json = com.renovation.guardian.data.knowledge.DecoboxStateJson.json.encodeToString(
            QuotePlanState.serializer(), state,
        )
        viewModelScope.launch {
            val id = editingPlanId
            if (id != null) {
                container.quotePlanRepo.updatePlanState(id, json, today)
            } else {
                container.quotePlanRepo.savePlan(name, state.mode.name.lowercase(), json, today)
            }
            onDone(true)
        }
    }

    fun deletePlan(id: String) {
        viewModelScope.launch { container.quotePlanRepo.deletePlan(id) }
    }

    /** 写入预算(按大项覆盖 budget_category.planned_cents + 同步总预算)。 */
    fun writeToBudget(onDone: (Boolean) -> Unit) {
        val r = result ?: return
        viewModelScope.launch {
            val cats = container.quotePlanRepo.listBudgetCategories().associateBy { it.id }
            val s = r.summary
            val writes = linkedMapOf<String, Long>()
            when {
                "b-full" in cats -> writes["b-full"] = r.totalCents
                "b-whole" in cats -> writes["b-whole"] = r.totalCents
                "b-main" in cats -> {
                    if ("b-labor" in cats) writes["b-labor"] = s.laborAuxCents
                    if ("b-aux" in cats) writes["b-aux"] = 0L
                    if ("b-main" in cats) writes["b-main"] = s.includedMainCents + s.managementCents + s.houseWorkCents
                }
                else -> {
                    if ("b-construct" in cats) writes["b-construct"] = s.laborAuxCents + s.includedMainCents
                    if ("b-main" in cats) writes["b-main"] = s.managementCents
                    if ("b-misc" in cats) writes["b-misc"] = s.houseWorkCents
                }
            }
            container.quotePlanRepo.writeQuoteToBudget(writes, r.totalCents)
            onDone(true)
        }
    }
}
