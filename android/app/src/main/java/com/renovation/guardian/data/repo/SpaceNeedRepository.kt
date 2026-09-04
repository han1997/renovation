package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.SpaceNeedEntity
import com.renovation.guardian.data.db.SpaceNeedWithStages
import com.renovation.guardian.data.db.TaskEntity
import com.renovation.guardian.util.IdGen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SpaceNeedRepository(
    private val db: AppDatabase,
    private val taskRepo: TaskRepository,
) {
    fun observeAll(): Flow<List<SpaceNeedWithStages>> = db.spaceNeedDao().observeAll()

    suspend fun getById(id: String): SpaceNeedEntity? = db.spaceNeedDao().getById(id)

    suspend fun stageIdsFor(spaceId: String): List<String> = db.spaceNeedDao().stageIdsFor(spaceId)

    /**
     * 添加一个预设空间需求（含派生任务）。
     */
    suspend fun addPreset(
        presetId: String,
        name: String,
        emoji: String,
        desc: String?,
        stageIds: List<String>,
        budgetCategoryId: String?,
        budgetNote: String?,
        tasks: List<Pair<String, String>>, // (stageId, text)
        today: String,
    ): SpaceNeedEntity {
        val spaceId = IdGen.new("sp")
        val space = SpaceNeedEntity(
            id = spaceId,
            presetId = presetId,
            name = name,
            emoji = emoji,
            description = desc,
            budgetCategoryId = budgetCategoryId,
            budgetNote = budgetNote,
            isCustom = false,
            createdAt = today,
        )
        db.spaceNeedDao().add(space, stageIds)
        syncDerivedTasks(space, tasks, today)
        return space
    }

    suspend fun addCustom(
        name: String,
        stageIds: List<String>,
        budgetCategoryId: String?,
        budgetNote: String?,
        today: String,
    ): SpaceNeedEntity {
        val spaceId = IdGen.new("sp")
        val space = SpaceNeedEntity(
            id = spaceId,
            presetId = null,
            name = name,
            emoji = "🪟",
            description = null,
            budgetCategoryId = budgetCategoryId,
            budgetNote = budgetNote,
            isCustom = true,
            createdAt = today,
        )
        db.spaceNeedDao().add(space, stageIds)
        val tasks = stageIds.map { sid -> sid to "$name（${stageNameOf(sid)}）" }
        syncDerivedTasks(space, tasks, today)
        return space
    }

    suspend fun updateCustom(
        spaceId: String,
        name: String,
        stageIds: List<String>,
        budgetCategoryId: String?,
        budgetNote: String?,
        today: String,
    ) {
        val existing = db.spaceNeedDao().getById(spaceId) ?: return
        db.spaceNeedDao().add(existing.copy(name = name, budgetCategoryId = budgetCategoryId, budgetNote = budgetNote), stageIds)
        val tasks = stageIds.map { sid -> sid to "$name（${stageNameOf(sid)}）" }
        syncDerivedTasks(existing.copy(name = name), tasks, today)
    }

    suspend fun delete(spaceId: String) {
        // 已打卡的派生转普通任务；未打卡的直接删
        db.taskDao().unmarkDerivedDoneForSpace(spaceId)
        db.taskDao().deleteUndoneDerivedForSpace(spaceId)
        db.spaceNeedDao().delete(spaceId)
    }

    /**
     * 同步派生任务：
     * - 旧派生 + done=true → 清 spaceId 保留为普通任务；
     * - 旧派生 + done=false → 删除；
     * - 新派生 → INSERT（避免与已保留的重复）。
     */
    private suspend fun syncDerivedTasks(
        space: SpaceNeedEntity,
        newTasks: List<Pair<String, String>>,
        today: String,
    ) {
        val sid = space.id
        // Step 1: 把已打卡的派生任务"转普通"
        db.taskDao().unmarkDerivedDoneForSpace(sid)
        // Step 2: 删未打卡的旧派生
        db.taskDao().deleteUndoneDerivedForSpace(sid)
        // Step 3: 重新 INSERT；用 stageId + text 防重复
        val existingTasks = db.taskDao().listAllForStage("*").let { /* 我们需要全表，这里换路径 */ emptyList<TaskEntity>() }
        val allTasks = mutableListOf<TaskEntity>()
        for ((stageId, text) in newTasks) {
            // 判定是否已被"保留"（stageId + text + done == true）
            // 这里改用更稳的方式：查 task 表里 stageId=stageId AND text=text AND done=1 的所有记录，
            // 跳过其中已存在的"已保留为普通任务"的那条
            val preserved = findPreservedTask(stageId, text)
            if (preserved == null) {
                allTasks += TaskEntity(
                    id = IdGen.new("ct"),
                    stageId = stageId,
                    sourceType = TaskEntity.TYPE_DERIVED,
                    text = text,
                    tip = null,
                    spaceId = sid,
                    dueDate = null,
                    done = false,
                    doneAt = null,
                    createdAt = today,
                )
            }
        }
        if (allTasks.isNotEmpty()) {
            db.taskDao().upsertAll(allTasks)
        }
    }

    private suspend fun findPreservedTask(stageId: String, text: String): TaskEntity? {
        // 简化：直接走 db.taskDao() 一次性查询 stage 下所有 task，客户端过滤
        return db.taskDao().listAllForStage(stageId).firstOrNull { it.text == text && it.done && it.spaceId == null }
    }

    private suspend fun stageNameOf(stageId: String): String =
        db.stageDao().getById(stageId)?.name ?: stageId
}