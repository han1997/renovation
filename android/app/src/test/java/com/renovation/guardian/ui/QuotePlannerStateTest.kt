package com.renovation.guardian.ui

import androidx.lifecycle.ViewModelStore
import com.renovation.guardian.testing.PolishFixture
import com.renovation.guardian.data.knowledge.DecoboxStateJson
import com.renovation.guardian.ui.quote.QuoteViewModel
import com.renovation.guardian.ui.quote.engine.*
import com.renovation.guardian.domain.quote.*
import com.renovation.guardian.ui.planner.PlannerViewModel
import com.renovation.guardian.ui.planner.engine.*
import com.renovation.guardian.domain.planner.*
import com.renovation.guardian.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class QuotePlannerStateTest {
    @Test fun repeatedSaveUpdatesOnePlanAndModeMetadataStaysInSync() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            PolishFixture(dispatcher).use { f ->
                f.initialize()
                val vm = QuoteViewModel(f.app)
                val store = ViewModelStore().apply { put("quote", vm) }
                try {
                    vm.newPlan(); advanceUntilIdle()
                    val cat = f.cache.decoboxCatalog!!.floor.first()
                    val variant = cat.variants.first()
                    vm.update { it.copy(rooms = listOf(QuoteRoomState("r", "客厅", 20.0, floor = SurfaceSelection(cat.id, variant.id, variant.specs.first().id, sourcing = Sourcing.SELF)))) }
                    assertNull(vm.completionError())
                    vm.savePlan("方案一"); vm.savePlan("重复点击")
                    advanceUntilIdle()
                    assertEquals(1, f.db.quotePlanDao().listAll().size)
                    val id = vm.editingPlanId!!
                    assertFalse(vm.dirty)
                    vm.setMode(QuoteMode.SEMI)
                    vm.savePlan("修改后的方案"); advanceUntilIdle()
                    assertEquals(id, vm.editingPlanId)
                    assertEquals("semi", f.db.quotePlanDao().getById(id)!!.mode)
                    assertEquals("修改后的方案", f.db.quotePlanDao().getById(id)!!.name)
                    vm.savePlan("副本", asNew = true); advanceUntilIdle()
                    assertEquals(2, f.db.quotePlanDao().listAll().size)
                    vm.closeEditor(); vm.loadPlan(id); advanceUntilIdle()
                    assertEquals(QuoteMode.SEMI, vm.state.mode)
                    assertFalse(vm.dirty)
                } finally { store.clear(); advanceUntilIdle() }
            }
        } finally { Dispatchers.resetMain() }
    }
    @Test fun rapidPlannerChangesPersistTheLatestConsistentSnapshot() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            PolishFixture(dispatcher).use { f ->
                f.initialize()
                val vm = PlannerViewModel(f.app)
                val store = ViewModelStore().apply { put("planner", vm) }
                try {
                    advanceUntilIdle()
                    val picks = PlannerRules.search(f.cache.decoboxRequirements!!, "").take(20)
                    picks.forEach(vm::togglePick)
                    vm.addRoom("空间一", picks.first().spaceKey)
                    val room = vm.state.rooms.single()
                    vm.toggleAssignment(picks.first(), room.id, true)
                    vm.setImportance(picks.first().itemKey, room.id, Importance.CRITICAL)
                    vm.renameRoom(room.id, "新空间名")
                    vm.togglePick(picks.last())
                    advanceUntilIdle()
                    val stored = f.db.plannerStateDao().get()!!
                    val restored = DecoboxStateJson.json.decodeFromString(PlannerState.serializer(), stored.stateJson)
                    assertEquals(vm.state, restored)
                    assertEquals(19, restored.picked.size)
                    assertEquals("新空间名", restored.assignments.single().roomName)
                    assertEquals(Importance.CRITICAL, restored.assignments.single().importance)
                    assertFalse(vm.saving)
                    vm.togglePick(picks.first()); advanceUntilIdle()
                    assertTrue(vm.state.assignments.isEmpty())
                    assertTrue(DecoboxStateJson.json.decodeFromString(PlannerState.serializer(), f.db.plannerStateDao().get()!!.stateJson).assignments.isEmpty())
                } finally { store.clear(); advanceUntilIdle() }
            }
        } finally { Dispatchers.resetMain() }
    }
    @Test fun dateRefreshMovesTasksToOverdueWithoutRecreatingViewModel() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            PolishFixture(dispatcher).use { f ->
                f.initialize()
                val task = f.db.taskTemplateDao().listAll().first()
                f.container.taskRepo.setTemplateDueDate(task.id, f.today)
                val vm = HomeViewModel(f.app)
                val store = ViewModelStore().apply { put("home", vm) }
                try {
                    assertEquals(listOf(task.id), vm.todayTasks.first().map { it.id })
                    f.today = "2026-09-11"; vm.refreshDate()
                    assertTrue(vm.todayTasks.first().isEmpty())
                    assertEquals(listOf(task.id), vm.overdueTasks.first().map { it.id })
                } finally { store.clear(); advanceUntilIdle() }
            }
        } finally { Dispatchers.resetMain() }
    }
}
