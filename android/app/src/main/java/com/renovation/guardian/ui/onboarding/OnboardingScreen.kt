package com.renovation.guardian.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.R
import com.renovation.guardian.ui.components.*
import com.renovation.guardian.util.MoneyUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** 三步设置，共享表单校验，底部操作始终在可见安全区。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val vm: OnboardingViewModel = viewModel()
    var step by rememberSaveable { mutableIntStateOf(1) }
    var area by rememberSaveable { mutableStateOf<Double?>(null) }
    var tier by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("") }
    var grade by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf<String?>(null) }
    var budget by rememberSaveable { mutableLongStateOf(0L) }
    var budgetRevision by rememberSaveable { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val form = remember(step) { FormState() }
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun save(defaults: Boolean) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                if (defaults) vm.finishCents(90.0, "t2", "clear", "mid", null, MoneyUtil.fromYuan(vm.suggestTotalYuan(90.0, "t2", "clear", "mid")))
                else vm.finishCents(requireNotNull(area), tier, mode, grade, date, budget)
                onFinished()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { host.showSnackbar("保存失败，输入已保留，请重试") }
            finally { busy = false }
        }
    }
    val valid = form.valid && when (step) {
        1 -> area != null && tier.isNotBlank()
        2 -> mode.isNotBlank()
        else -> grade.isNotBlank()
    }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.wizard_step_label, step, 3)) }) },
        snackbarHost = { SnackbarHost(host) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            key(step) {
                CompositionLocalProvider(LocalFormState provides form) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when (step) {
                            1 -> {
                                Text("你家的基本情况", style = MaterialTheme.typography.titleLarge)
                                Text("用于预算参考和装修计划，所有数据保存在本机。")
                                NumberField("建筑面积（㎡）", area) { area = it }
                                ChoiceField("城市能级", tier, vm.tiers.map { it.id to it.name }) { tier = it }
                                DatePickerField(date, { date = it }, "计划开工日期（可选）", onClear = { date = null })
                            }
                            2 -> {
                                Text("装修方式", style = MaterialTheme.typography.titleLarge)
                                ChoiceField("选择装修方式", mode, vm.modes.map { it.id to it.name }) { mode = it }
                                vm.modes.firstOrNull { it.id == mode }?.let { Text(it.short) }
                                Text("用于生成初始预算分类；已有支出不会因后续设置变化而被删除。", style = MaterialTheme.typography.bodySmall)
                            }
                            else -> {
                                Text("档次与预算上限", style = MaterialTheme.typography.titleLarge)
                                ChoiceField("装修档次", grade, vm.grades.map { it.id to it.name }) { grade = it }
                                if (grade.isNotBlank()) {
                                    val recommended = MoneyUtil.fromYuan(vm.suggestTotalYuan(area!!, tier, mode, grade))
                                    Text("参考预算 ¥${MoneyUtil.formatFull(recommended)}")
                                    OutlinedButton(onClick = { budget = recommended; budgetRevision++ }) { Text("采用参考预算") }
                                }
                                key(budgetRevision) { MoneyField("总预算上限（元）", budget) { budget = it } }
                                Text("这是你的支出上限，不会因应用报价而自动改动，可在房屋设置中修改。", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (step > 1) OutlinedButton(enabled = !busy, onClick = { step-- }) { Text("上一步") }
                else TextButton(enabled = !busy, onClick = { save(true) }) { Text("使用默认设置") }
                Button(enabled = valid && !busy, onClick = { if (step < 3) step++ else save(false) }) {
                    Text(if (busy) "正在保存…" else if (step < 3) "下一步" else "完成设置")
                }
            }
        }
    }
}
