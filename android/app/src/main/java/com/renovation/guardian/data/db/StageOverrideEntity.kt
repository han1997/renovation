package com.renovation.guardian.data.db

import kotlinx.serialization.Serializable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 整段完成覆盖。
 * 对应 Web `state.stageOverride[stageId] === 'done'`。
 */
@Entity(tableName = "stage_override")
@Serializable
data class StageOverrideEntity(
    @PrimaryKey @ColumnInfo(name = "stage_id") val stageId: String,
    @ColumnInfo(name = "override_state") val overrideState: String = STATE_DONE,
    @ColumnInfo(name = "set_at") val setAt: String,
) {
    companion object {
        const val STATE_DONE = "DONE"
    }
}