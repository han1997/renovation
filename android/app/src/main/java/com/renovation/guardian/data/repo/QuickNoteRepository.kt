package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.QuickNoteEntity
import com.renovation.guardian.util.IdGen
import kotlinx.coroutines.flow.Flow

/**
 * 随手记仓库：购物愿望 / 阶段备忘的增删改查。
 * 日期由调用方注入（`todayProvider`），与 [NoteRepository] 模式一致。
 */
class QuickNoteRepository(private val db: AppDatabase) {

    fun observeAll(): Flow<List<QuickNoteEntity>> = db.quickNoteDao().observeAll()

    suspend fun getById(id: String): QuickNoteEntity? = db.quickNoteDao().getById(id)

    /**
     * 新增或更新随手记。
     * - 新增时生成 `qn_` 前缀 id，createdAt / updatedAt 均为 today；
     * - 编辑时保留原 id / createdAt / isDone，仅刷新 updatedAt。
     */
    suspend fun upsert(
        content: String,
        type: String,
        category: String?,
        stageId: String?,
        existingId: String?,
        today: String,
    ) {
        val existing = existingId?.let { db.quickNoteDao().getById(it) }
        db.quickNoteDao().upsert(
            QuickNoteEntity(
                id = existing?.id ?: IdGen.new("qn"),
                content = content,
                type = type,
                category = category?.takeIf { it.isNotBlank() },
                stageId = stageId?.takeIf { it.isNotBlank() },
                isDone = existing?.isDone ?: false,
                createdAt = existing?.createdAt ?: today,
                updatedAt = today,
            ),
        )
    }

    /** 勾选 / 取消完成，并刷新 updatedAt。 */
    suspend fun setDone(id: String, done: Boolean, today: String) {
        val existing = db.quickNoteDao().getById(id) ?: return
        db.quickNoteDao().upsert(existing.copy(isDone = done, updatedAt = today))
    }

    suspend fun delete(id: String) = db.quickNoteDao().delete(id)
}
