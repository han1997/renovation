package han1997.renovation.ui.nav

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface FileActionResult {
    data class Success(val message: String) : FileActionResult
    data object Cancelled : FileActionResult
    data class Failure(val message: String) : FileActionResult
}
interface ExportActions {
    fun exportCsv(fileName: String, content: String)
    fun exportJson(fileName: String, content: String)
    fun importJson(onImported: (String) -> Unit)
}
val LocalExportActions = staticCompositionLocalOf<ExportActions> { error("ExportActions not provided") }

data class FileRequest(val kind: String, val name: String = "")

/** SAF 宿主持有操作状态，旋转时不丢待写内容，也不把大文件塞进 Bundle。 */
class FileActionsViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val requestChannel = Channel<FileRequest>(Channel.BUFFERED)
    val requests = requestChannel.receiveAsFlow()
    private val resultChannel = Channel<FileActionResult>(Channel.BUFFERED)
    val results = resultChannel.receiveAsFlow()
    private var imported: ((String) -> Unit)? = null

    fun export(kind: String, name: String, content: String) {
        if (saved.get<String>("kind") != null) return
        saved["kind"] = kind
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    File.createTempFile("document-", ".txt", getApplication<Application>().cacheDir).also { it.writeText(content, Charsets.UTF_8) }
                }
                saved["path"] = file.absolutePath
                requestChannel.send(FileRequest(kind, name))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { resultChannel.send(FileActionResult.Failure("准备导出失败，请重试")); clear() }
        }
    }
    fun import(onImported: (String) -> Unit) {
        if (saved.get<String>("kind") != null) return
        saved["kind"] = "import"
        imported = onImported
        requestChannel.trySend(FileRequest("import"))
    }
    fun finish(uri: Uri?) {
        val kind = saved.get<String>("kind") ?: return
        viewModelScope.launch {
            try {
                if (uri == null) { resultChannel.send(FileActionResult.Cancelled); return@launch }
                val resolver = getApplication<Application>().contentResolver
                if (kind == "import") {
                    val text = withContext(Dispatchers.IO) {
                        requireNotNull(resolver.openInputStream(uri)).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    }
                    val callback = imported
                    if (callback == null) resultChannel.send(FileActionResult.Failure("页面已恢复，请重新选择备份")) else callback(text)
                } else {
                    val file = File(requireNotNull(saved.get<String>("path")))
                    withContext(Dispatchers.IO) {
                        requireNotNull(resolver.openOutputStream(uri, "wt")).use { output -> file.inputStream().use { it.copyTo(output) } }
                    }
                    resultChannel.send(FileActionResult.Success("导出完成"))
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { resultChannel.send(FileActionResult.Failure("文件读写失败，请检查可用空间和访问权限")) }
            finally { clear() }
        }
    }
    fun launcherFailed() {
        resultChannel.trySend(FileActionResult.Failure("无法打开系统文件选择器，请检查系统文件应用"))
        clear()
    }
    private fun clear() {
        saved.get<String>("path")?.let { File(it).delete() }
        saved.remove<String>("path"); saved.remove<String>("kind"); imported = null
    }
}

@Composable
fun rememberExportActions(vm: FileActionsViewModel = viewModel()): ExportActions {
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv"), vm::finish)
    val json = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json"), vm::finish)
    val input = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), vm::finish)
    LaunchedEffect(vm) {
        vm.requests.collect { try { when (it.kind) {
            "csv" -> csv.launch(it.name)
            "json" -> json.launch(it.name)
            else -> input.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
        } } catch (_: android.content.ActivityNotFoundException) { vm.launcherFailed() } }
    }
    return remember(vm) { object : ExportActions {
        override fun exportCsv(fileName: String, content: String) = vm.export("csv", fileName, content)
        override fun exportJson(fileName: String, content: String) = vm.export("json", fileName, content)
        override fun importJson(onImported: (String) -> Unit) = vm.import(onImported)
    } }
}
