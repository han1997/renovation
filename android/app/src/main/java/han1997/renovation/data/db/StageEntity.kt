package han1997.renovation.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 14 阶段目录（只读种子，由 `assets/knowledge.json` 启动时写入）。
 *
 * 列表性子表（tasks / warnings / buy / acceptIds）以 JSON 字符串持久化在单行，
 * 与"v1 不拆关联表"的简化方案一致。
 */
@Entity(tableName = "stage")
data class StageEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "phase") val phase: String,
    @ColumnInfo(name = "emoji") val emoji: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "duration") val duration: String,
    @ColumnInfo(name = "goal") val goal: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "warnings_json") val warningsJson: String,
    @ColumnInfo(name = "buy_json") val buyJson: String,
    @ColumnInfo(name = "accept_ids_json") val acceptIdsJson: String,
)