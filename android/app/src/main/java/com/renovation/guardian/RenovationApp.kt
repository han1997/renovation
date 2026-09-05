package com.renovation.guardian

import android.app.Application
import android.util.Log
import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.knowledge.KnowledgeCache
import com.renovation.guardian.data.repo.AppContainer
import com.renovation.guardian.data.repo.DefaultAppContainer
import com.renovation.guardian.util.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application 类。负责初始化 Room 数据库与只读知识缓存。
 *
 * - Room 数据库由 [AppDatabase] 单例管理；
 * - 知识数据（`assets/knowledge.json` / `prices.json`）由 [KnowledgeCache] 启动时一次性加载；
 * - 仓库层 [AppContainer] 由 [DefaultAppContainer] 提供；
 * - 启动时异步执行种子写入，保证流程页（阶段 / 模板任务 / 验收清单）有数据可渲染。
 */
class RenovationApp : Application() {

    lateinit var container: AppContainer
        private set

    /** 应用级协程作用域：SupervisorJob 保证子协程失败互不影响；持有实例避免使用 GlobalScope。 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        val knowledge = KnowledgeCache(this).also { it.load() }
        container = DefaultAppContainer(db, knowledge)
        // 种子写入必须在启动链路被调用（幂等：种子表非空时内部自动跳过）。
        // 若遗漏此调用，stage / task_template / checklist 三张种子表永远为空，
        // 流程页完全空白且无任何报错——勿在重构启动链路时删除。
        appScope.launch {
            try {
                container.seeder.seedIfEmpty(DateUtil.today())
            } catch (e: Exception) {
                // 种子失败不应崩溃启动流程；下次启动会因表空自动重试。
                Log.w(TAG, "seedIfEmpty failed", e)
            }
        }
    }

    private companion object {
        const val TAG = "RenovationApp"
    }
}