package com.renovation.guardian.util

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object ImageShareUtil {
    fun cacheBitmap(context: Context, bitmap: Bitmap, name: String): Uri {
        val dir = File(context.cacheDir, "exports").apply { check(isDirectory || mkdirs()) }
        dir.listFiles()?.filter { it.isFile && it.lastModified() < System.currentTimeMillis() - 86_400_000 }?.forEach { it.delete() }
        val file = File(dir, "$name.png")
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { "此系统请使用图片分享" }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RenovationGuardian")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values))
        try {
            requireNotNull(resolver.openOutputStream(uri)).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            check(resolver.update(uri, values, null, null) > 0)
            return uri
        } catch (e: Exception) { resolver.delete(uri, null, null); throw e }
    }
    fun shareUris(context: Context, uris: List<Uri>) {
        require(uris.isNotEmpty())
        val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/png"
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first()) else putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            clipData = ClipData.newUri(context.contentResolver, "装修清单", uris.first()).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享清单图片"))
    }
}
