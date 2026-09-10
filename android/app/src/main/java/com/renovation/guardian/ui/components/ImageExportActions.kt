package com.renovation.guardian.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.renovation.guardian.util.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageExportActions(title: String, subtitle: String, rows: List<LongImageRow>, enabled: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme
    val style = LongImageStyle(1080, colors.surface.toArgb(), colors.surfaceContainerLow.toArgb(), colors.outlineVariant.toArgb(),
        colors.onSurface.toArgb(), colors.onSurface.toArgb(), colors.onSurfaceVariant.toArgb(), 48f, 32f)
    fun export(gallery: Boolean) {
        busy = true; message = null
        scope.launch {
            try {
                val uris = withContext(Dispatchers.IO) {
                    val written = mutableListOf<android.net.Uri>()
                    val id = java.util.UUID.randomUUID().toString()
                    try {
                        LongImageRenderer.renderPages(title, subtitle, rows, style).forEachIndexed { index, bitmap ->
                            try {
                                coroutineContext.ensureActive()
                                written += if (gallery) ImageShareUtil.saveToGallery(context, bitmap, "renovation-$id-${index + 1}")
                                    else ImageShareUtil.cacheBitmap(context, bitmap, "renovation-$id-${index + 1}")
                            } finally { bitmap.recycle() }
                        }
                        written
                    } catch (e: Exception) {
                        if (gallery) written.forEach { context.contentResolver.delete(it, null, null) }
                        throw e
                    }
                }
                if (!gallery) ImageShareUtil.shareUris(context, uris)
                message = if (gallery) "已保存 ${uris.size} 张图片" else "已打开分享面板，共 ${uris.size} 张图片"
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "图片导出失败，请检查可用空间或分享应用后重试" }
            finally { busy = false }
        }
    }
    Column {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = enabled && !busy, onClick = { export(false) }) { Text(if (busy) "正在生成…" else "分享图片") }
            if (Build.VERSION.SDK_INT >= 29) OutlinedButton(enabled = enabled && !busy, onClick = { export(true) }) { Text("保存到相册") }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
