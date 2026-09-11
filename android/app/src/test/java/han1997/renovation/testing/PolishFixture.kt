package han1997.renovation.testing

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import han1997.renovation.RenovationApp
import han1997.renovation.data.db.AppDatabase
import han1997.renovation.data.knowledge.KnowledgeCache
import han1997.renovation.data.repo.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor

class PolishFixture(dispatcher: CoroutineDispatcher? = null) : AutoCloseable {
    val cache = KnowledgeCache(ApplicationProvider.getApplicationContext()).also { it.load() }
    val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().apply {
        if (dispatcher != null) { setQueryExecutor(dispatcher.asExecutor()); setTransactionExecutor(dispatcher.asExecutor()) }
    }.build()
    var today = "2026-09-10"
    private val real = DefaultAppContainer(db, cache)
    val container: AppContainer = object : AppContainer by real { override val todayProvider = { today } }
    val app = mockk<RenovationApp>(relaxed = true).also { every { it.container } returns container }
    suspend fun initialize() {
        container.seeder.seedIfEmpty(today)
        container.houseProfileRepo.finishOnboarding(90.0, "t2", "half", "mid", today, 200000.0, emptyList(), emptyList())
    }
    override fun close() { db.close() }
}
