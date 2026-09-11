package han1997.renovation.data.db

import kotlinx.serialization.Serializable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 逐空间报价方案。方案状态(四步向导的完整 state)以 JSON blob 存 [stateJson],
 * 结构演进不受 schema 约束(第一版无跨方案查询需求,见 PRD ADR)。
 */
@Entity(tableName = "quote_plan")
@Serializable
data class QuotePlanEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    /** full / semi / partial */
    @ColumnInfo(name = "mode") val mode: String,
    @ColumnInfo(name = "state_json") val stateJson: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

/** 需求规划当前状态(单行表,id 恒为 1)。 */
@Entity(tableName = "planner_state")
@Serializable
data class PlannerStateEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "state_json") val stateJson: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
