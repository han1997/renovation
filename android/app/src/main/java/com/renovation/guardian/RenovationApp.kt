package com.renovation.guardian

import android.app.Application
import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.knowledge.KnowledgeCache
import com.renovation.guardian.data.repo.AppContainer
import com.renovation.guardian.data.repo.DefaultAppContainer

/**
 * Application 类。负责初始化 Room 数据库与只读知识缓存。
 *
 * - Room 数据库由 [AppDatabase] 单例管理；
 * - 知识数据（`assets/knowledge.json` / `prices.json`）由 [KnowledgeCache] 启动时一次性加载；
 * - 仓库层 [AppContainer] 由 [DefaultAppContainer] 提供。
 */
class RenovationApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        val knowledge = KnowledgeCache(this).also { it.load() }
        container = DefaultAppContainer(db, knowledge)
    }
}