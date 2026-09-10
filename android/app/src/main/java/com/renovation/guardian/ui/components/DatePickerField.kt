package com.renovation.guardian.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.renovation.guardian.util.DateUtil
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** 只读日期选择框（YYYY-MM-DD ↔ 中文展示）。 */
@Composable
fun DatePickerField(
    value: String?,
    onDateSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    var show by rememberSaveable { mutableStateOf(false) }
    val initialMillis = remember(value) {
        value?.let {
            try {
                Instant.parse("${it}T00:00:00Z").toEpochMilliseconds()
            } catch (_: Throwable) {
                null
            }
        }
    }
    OutlinedTextField(
        value = if (value != null) DateUtil.formatCN(value) else "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            androidx.compose.foundation.layout.Row {
                if (value != null && onClear != null) TextButton(onClick = onClear) { Text("清除") }
                androidx.compose.material3.IconButton(onClick = { show = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "选择日期")
                }
            }
        },
        modifier = modifier.fillMaxWidth().clickable { show = true },
        textStyle = MaterialTheme.typography.bodyLarge,
    )
    if (show) {
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val iso = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date.toString()
                        onDateSelected(iso)
                    }
                    show = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = state, modifier = Modifier.padding(8.dp))
        }
    }
}
