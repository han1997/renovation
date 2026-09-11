package han1997.renovation.ui.planner.engine

import han1997.renovation.domain.planner.*

import han1997.renovation.data.knowledge.*
import org.junit.Assert.*
import org.junit.Test

class PlannerRulesTest {
    private val pick = DemandPick("socket", "床头插座", "power", "电力配置", "bedroom", "卧室")
    private val rooms = listOf(PlannerRoom("r1", "bedroom", "主卧"), PlannerRoom("r2", "bedroom", "次卧"), PlannerRoom("r3", null, "工作室"))
    @Test fun itemMatchDoesNotRequireTypeMatch() {
        val catalog = DecoboxRequirementsJson(spaceList = listOf(SpaceJson("bedroom", "卧室")),
            typeListBySpace = mapOf("bedroom" to listOf(RequirementTypeJson("power", "电力配置"))),
            functionsByType = mapOf("power" to listOf(RequirementItemJson("socket", "床头插座"))))
        assertEquals(listOf("socket"), PlannerRules.search(catalog, "插座").map { it.itemKey })
        assertEquals(1, PlannerRules.search(catalog, "电力").size)
        assertTrue(PlannerRules.search(catalog, "插座", "kitchen").isEmpty())
    }
    @Test fun recommendationsMatchAllRoomsButNeverOverwriteManualWork() {
        val state = PlannerState(picked = listOf(pick), rooms = rooms)
        assertEquals(listOf("r1", "r2"), PlannerRules.recommend(state).map { it.roomId })
        val manual = Assignment(pick.itemKey, pick.itemName, "r3", "工作室", Importance.CRITICAL)
        assertTrue(PlannerRules.recommend(state.copy(assignments = listOf(manual))).isEmpty())
        assertEquals(Importance.CRITICAL, PlannerRules.normalize(state.copy(assignments = listOf(manual))).assignments.single().importance)
    }
    @Test fun staleReferencesAreRemovedAndNamesAreDerived() {
        val a = Assignment(pick.itemKey, "旧名称", "r1", "旧房间", Importance.MUST)
        val state = PlannerState(picked = listOf(pick), rooms = rooms, assignments = listOf(a, a))
        val clean = PlannerRules.normalize(state)
        assertEquals(1, clean.assignments.size)
        assertEquals("主卧", clean.assignments.single().roomName)
        assertEquals("床头插座", clean.assignments.single().itemName)
        assertTrue(PlannerRules.normalize(state.copy(picked = emptyList())).assignments.isEmpty())
        assertTrue(PlannerRules.normalize(state.copy(rooms = emptyList())).assignments.isEmpty())
    }
    @Test fun unassignedNeedsRemainVisibleWithoutAnyRooms() {
        val lines = PlannerRules.lines(PlannerState(picked = listOf(pick)))
        assertEquals("未分配", lines.single().roomName)
        assertNull(lines.single().importance)
        assertTrue(PlannerTextBuilder.build(lines).contains("床头插座"))
    }
    @Test fun unknownCatalogReferencesAreRejectedInsteadOfBecomingInvisiblePicks() {
        assertThrows(IllegalArgumentException::class.java) {
            PlannerRules.validateCatalogReferences(PlannerState(picked = listOf(pick)), DecoboxRequirementsJson())
        }
    }

}
