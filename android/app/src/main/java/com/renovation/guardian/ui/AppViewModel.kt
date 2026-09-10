package com.renovation.guardian.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.renovation.guardian.RenovationApp
import com.renovation.guardian.data.repo.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 写操作串行执行，只有仓库完成后才通知页面。 */
abstract class AppViewModel(application: Application) : AndroidViewModel(application) {
    protected val container: AppContainer by lazy { (application as RenovationApp).container }
    private val notices = Channel<String>(32, kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    val messages = notices.receiveAsFlow()
    private val busy = MutableStateFlow(false)
    val isBusy = busy.asStateFlow()
    private val writes = Mutex()
    protected fun notify(message: String) { notices.trySend(message) }
    private var pending = 0
    protected fun perform(success: String? = "已保存", onSuccess: () -> Unit = {}, singleFlight: Boolean = false, action: suspend () -> Unit): kotlinx.coroutines.Job {
        if (singleFlight && busy.value) return kotlinx.coroutines.Job().apply { complete() }
        pending++; busy.value = true
        return viewModelScope.launch {
            writes.withLock {
                try {
                    action(); onSuccess()
                    if (success != null) notices.trySend(success)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { notices.trySend(when (e) {
                    is kotlinx.serialization.SerializationException -> "数据格式不正确，原内容未修改"
                    is android.database.sqlite.SQLiteException -> "本地数据操作失败，请重试"
                    is ArithmeticException -> "金额或数量超出可计算范围"
                    else -> e.message?.take(120) ?: "操作失败，请重试"
                }) }
                finally { pending--; busy.value = pending > 0 }
            }
        }
    }
}
