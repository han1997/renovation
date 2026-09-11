package han1997.renovation

import android.app.Application
import android.util.Log
import han1997.renovation.data.db.AppDatabase
import han1997.renovation.data.knowledge.KnowledgeCache
import han1997.renovation.data.repo.AppContainer
import han1997.renovation.data.repo.DefaultAppContainer
import han1997.renovation.util.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application 类。负责初始化 Room 数据库与只读知识缓存。
 *
 * - Room 数据库由 [AppDatabase] 单例管理；
 * - 知识数据（`assets/knowledge.json` / `prices.json`）由 [KnowledgeCache] 启动时一次性加载；
 * - 仓库层 [AppContainer] 由 [DefaultAppContainer] 提供；
 * - 启动时异步执行种子写入，保证流程页（阶段 / 模板任务 / 验收清单）有数据可渲染。
 */
sealed interface InitializationState {
    data object Loading : InitializationState
    data object Ready : InitializationState
    data class Failed(val message: String) : InitializationState
}

class RenovationApp : Application() {
    private val initializationState = MutableStateFlow<InitializationState>(InitializationState.Loading)
    val initialization = initializationState.asStateFlow()
    private var initializationJob: Job? = null

    lateinit var container: AppContainer
        private set

    /** 应用级协程作用域：SupervisorJob 保证子协程失败互不影响；持有实例避免使用 GlobalScope。 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        val knowledge = KnowledgeCache(this)
        container = DefaultAppContainer(db, knowledge)
        retryInitialization()
    }

    fun retryInitialization() {
        if (initializationJob?.isActive == true) return
        initializationState.value = InitializationState.Loading
        initializationJob = appScope.launch {
            try {
                container.seeder.seedIfEmpty(DateUtil.today())
                initializationState.value = InitializationState.Ready
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.w(TAG, "初始化失败", e)
                initializationState.value = InitializationState.Failed("本地资料加载失败，请重试")
            }
        }
    }

    private companion object {
        const val TAG = "RenovationApp"
    }
}