package com.renovation.guardian.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * 通用「删除确认」对话框：全 app 的删除入口统一复用，避免误触一键直删。
 *
 * @param title 对话框标题（如「删除支出」）。
 * @param text 说明文案，默认提示不可恢复；可传入带对象名的定制文案。
 * @param onConfirm 用户确认删除。
 * @param onDismiss 取消 / 点外部关闭。
 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    text: String = "删除后不可恢复，确定删除吗？",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
