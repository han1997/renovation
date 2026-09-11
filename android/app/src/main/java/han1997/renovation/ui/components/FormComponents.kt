package han1997.renovation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import han1997.renovation.ui.AppViewModel
import han1997.renovation.util.MoneyUtil

@Composable
fun OperationFeedback(vm: AppViewModel, host: SnackbarHostState) {
    LaunchedEffect(vm, host) { vm.messages.collectLatest { host.showSnackbar(it) } }
}

/** 当前步骤的非法草稿阻止继续，避免显示新输入却用旧数值计算。 */
class FormState {
    private val invalid = mutableStateMapOf<Any, Boolean>()
    val valid: Boolean get() = invalid.isEmpty()
    fun set(key: Any, valid: Boolean) { if (valid) invalid.remove(key) else invalid[key] = true }
    fun remove(key: Any) { invalid.remove(key) }
}
val LocalFormState = staticCompositionLocalOf<FormState?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceField(label: String, value: T, options: List<Pair<T, String>>, enabled: Boolean = true, onChange: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(value = options.firstOrNull { it.first == value }?.second ?: "请选择", onValueChange = {},
            readOnly = true, enabled = enabled, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onChange(id); expanded = false }) }
        }
    }
}

@Composable
fun NumberField(label: String, value: Double?, optional: Boolean = false, allowZero: Boolean = false,
                integer: Boolean = false, maxValue: Double = 100000000.0, onChange: (Double?) -> Unit) {
    val form = LocalFormState.current
    val token = remember { Any() }
    var text by rememberSaveable(label) { mutableStateOf(value?.let { if (integer) it.toLong().toString() else it.toString() } ?: "") }
    val parsed = text.toDoubleOrNull()
    val valid = (optional && text.isBlank()) || (parsed != null && parsed.isFinite() &&
        (if (allowZero) parsed >= 0 else parsed > 0) && parsed <= maxValue && (!integer || parsed == parsed.toInt().toDouble()))
    SideEffect { form?.set(token, valid) }
    DisposableEffect(form, token) { onDispose { form?.remove(token) } }
    OutlinedTextField(value = text, onValueChange = {
        text = it
        val n = it.toDoubleOrNull()
        if (optional && it.isBlank()) onChange(null)
        else if (n != null && n.isFinite() && (if (allowZero) n >= 0 else n > 0) && n <= maxValue && (!integer || n == n.toInt().toDouble())) onChange(n)
    }, label = { Text(label) }, singleLine = true, isError = !valid,
        supportingText = { if (!valid) Text("请输入${if (integer) "整数" else "数值"}，范围${if (allowZero) "含 0" else "大于 0"}且不超过 $maxValue") else if (optional) Text("留空使用目录或估算值") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}

@Composable
fun MoneyField(label: String, cents: Long, onChange: (Long) -> Unit) {
    val form = LocalFormState.current
    val token = remember { Any() }
    var text by rememberSaveable(label) { mutableStateOf(MoneyUtil.input(cents)) }
    val parsed = MoneyUtil.parseYuan(text)
    val valid = parsed != null && parsed >= 0
    SideEffect { form?.set(token, valid) }
    DisposableEffect(form, token) { onDispose { form?.remove(token) } }
    OutlinedTextField(value = text, onValueChange = { text = it; MoneyUtil.parseYuan(it)?.takeIf { c -> c >= 0 }?.let(onChange) },
        label = { Text(label) }, isError = !valid, singleLine = true,
        supportingText = { if (!valid) Text("请输入非负金额，最多两位小数") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Checkbox(checked = checked, onCheckedChange = null)
    }
}
