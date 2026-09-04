package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.ChecklistItemCheckEntity
import com.renovation.guardian.data.db.ChecklistItemEntity
import com.renovation.guardian.data.db.ChecklistEntity
import com.renovation.guardian.data.db.ContactEntity
import com.renovation.guardian.data.db.ExpenseEntity
import com.renovation.guardian.data.db.HouseProfileEntity
import com.renovation.guardian.data.db.NoteEntity
import com.renovation.guardian.data.db.QuickNoteEntity
import com.renovation.guardian.data.db.SpaceNeedEntity
import com.renovation.guardian.data.db.SpaceNeedStageEntity
import com.renovation.guardian.data.db.SpaceExportTuple
import com.renovation.guardian.data.db.StageOverrideEntity
import com.renovation.guardian.data.db.TaskCompletionEntity
import com.renovation.guardian.data.db.TaskEntity
import com.renovation.guardian.data.db.exportSpaces
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.room.withTransaction

/**
 * JSON 备份 / 恢复。
 *
 * - 顶层结构尽量与 Web `Store.state` 对齐（"profile / tasksDone / taskDates / ...")，
 *   字段命名用下划线 / 驼峰以与 Web 一致；
 * - 金额用元（float）；导入时用 `MoneyUtil.fromYuan` 折算为 cents 写入 Room。
 *
 * CSV 导出走 [BudgetRepository] / 业务层。
 */
class ImportExportRepository(private val db: AppDatabase) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun exportJson(): String {
        val profile = db.houseProfileDao().get()
        val completions = db.taskDao().exportCompletions()
        val tasks = db.taskDao().exportCustomTasks()
        val overrides = db.taskDao().exportOverrides()
        val cats = db.budgetCategoryDao().listAll()
        val expenses = db.expenseDao().exportAll()
        val checks = db.checklistDao().exportChecks()
        val contacts = db.contactDao().exportAll()
        val notes = db.noteDao().exportAll()
        val quickNotes = db.quickNoteDao().exportAll()
        val spaces = db.spaceNeedDao().exportSpaces()

        val payload = ExportPayload(
                schemaVersion = 1,
                ver = 1,
                profile = profile?.toExport(),
                tasksDone = completions.associate { it.templateId to it.done },
                taskDates = completions.mapNotNull { c -> c.dueDate?.let { c.templateId to it } }.toMap() +
                        tasks.mapNotNull { t -> t.dueDate?.let { t.id to it } }.toMap(),
                customTasks = tasks.map { it.toExport() },
                stageOverride = overrides.associate { it.stageId to it.overrideState },
                budgetCategories = cats.map { it.toExport() },
                expenses = expenses.map { it.toExport() },
                checks = checks.associate { it.itemId to it.done },
                notes = notes.map { it.toExport() },
                contacts = contacts.map { it.toExport() },
                quickNotes = quickNotes.map { it.toExport() },
                spaces = spaces.map { it.toSpaceExport() },
            )
        return json.encodeToString(ExportPayload.serializer(), payload)
    }

    suspend fun importJson(text: String): ImportResult {
        return try {
            val payload = json.decodeFromString(ExportPayload.serializer(), text)
            // 兜底：允许缺字段
            db.withTransaction {
                runBlockingImport(payload)
            }
            ImportResult(success = true)
        } catch (e: Throwable) {
            ImportResult(success = false, error = e.message ?: "unknown error")
        }
    }

    private suspend fun runBlockingImport(p: ExportPayload) {
        // House profile
        p.profile?.let { prof ->
            db.houseProfileDao().upsert(prof.toEntity())
        } ?: db.houseProfileDao().deleteAll()

        // Clear dynamic tables first
        db.taskDao().clearCompletions()
        db.taskDao().clearTasks()
        db.taskDao().clearOverrides()
        db.budgetCategoryDao().clear()
        db.expenseDao().clear()
        db.checklistDao().clearChecks()
        db.noteDao().clearAll()
        db.contactDao().clearAll()
        db.quickNoteDao().clearAll()
        db.spaceNeedDao().clearAll()

        // Reinsert
        p.tasksDone.forEach { (templateId, done) ->
            db.taskDao().upsertCompletion(
                TaskCompletionEntity(templateId, p.taskDates[templateId], done, null),
            )
        }
        p.taskDates.filterKeys { id -> id !in p.tasksDone.keys }.forEach { (id, date) ->
            // 仅当 id 属于模板任务时插入；customTasks 的日期由 task.dueDate 承担
            db.taskDao().upsertCompletion(TaskCompletionEntity(id, date, false, null))
        }
        p.customTasks.forEach { task ->
            db.taskDao().upsert(task.toEntity())
        }
        p.stageOverride.forEach { (stageId, state) ->
            db.taskDao().insertOverride(StageOverrideEntity(stageId, state, "1970-01-01"))
        }
        p.budgetCategories.forEach { cat ->
            db.budgetCategoryDao().upsert(cat.toEntity())
        }
        p.expenses.forEach { exp ->
            db.expenseDao().upsert(exp.toEntity())
        }
        p.checks.forEach { (itemId, done) ->
            db.checklistDao().upsertCheck(ChecklistItemCheckEntity(itemId, done, null))
        }
        p.notes.forEach { n ->
            db.noteDao().upsert(n.toEntity())
        }
        p.contacts.forEach { c ->
            db.contactDao().upsert(c.toEntity())
        }
        p.quickNotes.forEach { q ->
            db.quickNoteDao().upsert(q.toEntity())
        }
        p.spaces.forEach { s ->
            val space = s.toSpaceEntity()
            db.spaceNeedDao().upsert(space)
            val stageIds = s.stageIds
            stageIds.forEachIndexed { idx, sid ->
                db.spaceNeedDao().upsertStages(listOf(SpaceNeedStageEntity(space.id, sid, idx)))
            }
        }
    }
}

data class ImportResult(val success: Boolean, val error: String? = null)

@Serializable
private data class ExportPayload(
    @kotlinx.serialization.SerialName("schema_version") val schemaVersion: Int = 1,
    val ver: Int = 1,
    val profile: ProfileExport? = null,
    val tasksDone: Map<String, Boolean> = emptyMap(),
    val taskDates: Map<String, String> = emptyMap(),
    val customTasks: List<TaskExport> = emptyList(),
    val stageOverride: Map<String, String> = emptyMap(),
    val budgetCategories: List<BudgetCategoryExport> = emptyList(),
    val expenses: List<ExpenseExport> = emptyList(),
    val checks: Map<String, Boolean> = emptyMap(),
    val notes: List<NoteExport> = emptyList(),
    val contacts: List<ContactExport> = emptyList(),
    val quickNotes: List<QuickNoteExport> = emptyList(),
    val spaces: List<SpaceExport> = emptyList(),
)

@Serializable
private data class ProfileExport(
    val area: Double,
    val tier: String,
    val mode: String,
    val grade: String,
    val startDate: String? = null,
    val totalBudget: Double = 0.0,
    val styleId: String? = null,
    val createdAt: String,
) {
    fun toEntity() = HouseProfileEntity(
        id = HouseProfileEntity.SINGLE_ROW_ID,
        areaM2 = area,
        tierId = tier,
        modeId = mode,
        gradeId = grade,
        startDate = startDate,
        totalBudgetCents = (totalBudget * 100).toLong(),
        styleId = styleId,
        styleQuizAt = null,
        createdAt = createdAt,
    )
}

private fun HouseProfileEntity.toExport() = ProfileExport(
    area = areaM2,
    tier = tierId,
    mode = modeId,
    grade = gradeId,
    startDate = startDate,
    totalBudget = totalBudgetCents / 100.0,
    styleId = styleId,
    createdAt = createdAt,
)

@Serializable
private data class TaskExport(
    val id: String,
    val stageId: String,
    val text: String,
    val tip: String? = null,
    val spaceId: String? = null,
    val date: String? = null,
    val sourceType: String = TaskEntity.TYPE_CUSTOM,
    val createdAt: String,
) {
    fun toEntity() = TaskEntity(
        id = id,
        stageId = stageId,
        sourceType = sourceType,
        text = text,
        tip = tip,
        spaceId = spaceId,
        dueDate = date,
        done = false,
        doneAt = null,
        createdAt = createdAt,
    )
}

private fun TaskEntity.toExport() = TaskExport(
    id = id,
    stageId = stageId,
    text = text,
    tip = tip,
    spaceId = spaceId,
    date = dueDate,
    sourceType = sourceType,
    createdAt = createdAt,
)

@Serializable
private data class BudgetCategoryExport(
    val id: String,
    val name: String,
    val emoji: String,
    val planned: Double,
    val orderIndex: Int,
    val isFromTemplate: Boolean = false,
    val createdAt: String,
) {
    fun toEntity() = com.renovation.guardian.data.db.BudgetCategoryEntity(
        id = id,
        name = name,
        emoji = emoji,
        plannedCents = (planned * 100).toLong(),
        orderIndex = orderIndex,
        isFromTemplate = isFromTemplate,
        createdAt = createdAt,
    )
}

private fun com.renovation.guardian.data.db.BudgetCategoryEntity.toExport() = BudgetCategoryExport(
    id = id,
    name = name,
    emoji = emoji,
    planned = plannedCents / 100.0,
    orderIndex = orderIndex,
    isFromTemplate = isFromTemplate,
    createdAt = createdAt,
)

@Serializable
private data class ExpenseExport(
    val id: String,
    val name: String,
    val amount: Double,
    val catId: String,
    val date: String,
    val note: String? = null,
    val createdAt: String,
) {
    fun toEntity() = ExpenseEntity(
        id = id,
        name = name,
        amountCents = (amount * 100).toLong(),
        categoryId = catId,
        date = date,
        note = note,
        createdAt = createdAt,
    )
}

private fun ExpenseEntity.toExport() = ExpenseExport(
    id = id,
    name = name,
    amount = amountCents / 100.0,
    catId = categoryId,
    date = date,
    note = note,
    createdAt = createdAt,
)

@Serializable
private data class NoteExport(val id: String, val title: String, val text: String, val updatedAt: String) {
    fun toEntity() = NoteEntity(id, title, text, updatedAt, updatedAt)
}

private fun NoteEntity.toExport() = NoteExport(id, title, body, updatedAt)

@Serializable
private data class ContactExport(val id: String, val name: String, val role: String? = null, val phone: String? = null, val note: String? = null) {
    fun toEntity() = ContactEntity(id, name, role, phone, note, "1970-01-01")
}

private fun ContactEntity.toExport() = ContactExport(id, name, role, phone, note)

/** 随手记导出/导入模型；旧备份缺 `quickNotes` 字段时取默认空列表，导入跳过。 */
@Serializable
private data class QuickNoteExport(
    val id: String,
    val content: String,
    val type: String,
    val category: String? = null,
    val stageId: String? = null,
    val isDone: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
) {
    fun toEntity() = QuickNoteEntity(
        id = id,
        content = content,
        type = type,
        category = category,
        stageId = stageId,
        isDone = isDone,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

private fun QuickNoteEntity.toExport() = QuickNoteExport(
    id = id,
    content = content,
    type = type,
    category = category,
    stageId = stageId,
    isDone = isDone,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

@Serializable
private data class SpaceExport(
    val id: String,
    val presetId: String? = null,
    val name: String,
    val emoji: String,
    val desc: String? = null,
    val stageIds: List<String> = emptyList(),
    val budgetCat: String? = null,
    val budgetNote: String? = null,
    val custom: Boolean = false,
    val createdAt: String,
) {
    fun toSpaceEntity() = SpaceNeedEntity(
        id = id,
        presetId = presetId,
        name = name,
        emoji = emoji,
        description = desc,
        budgetCategoryId = budgetCat,
        budgetNote = budgetNote,
        isCustom = custom,
        createdAt = createdAt,
    )
}

private fun SpaceExportTuple.toSpaceExport() = SpaceExport(
    id = space.id,
    presetId = space.presetId,
    name = space.name,
    emoji = space.emoji,
    desc = space.description,
    stageIds = stageIds,
    budgetCat = space.budgetCategoryId,
    budgetNote = space.budgetNote,
    custom = space.isCustom,
    createdAt = space.createdAt,
)