package com.renovation.guardian.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.R
import com.renovation.guardian.ui.components.DatePickerField
import com.renovation.guardian.ui.components.PrimaryButton
import com.renovation.guardian.ui.components.SectionCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val vm: OnboardingViewModel = viewModel()
    var step by remember { mutableStateOf(1) }

    var areaText by remember { mutableStateOf("") }
    var tierId by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<String?>(null) }

    var modeId by remember { mutableStateOf("") }
    var gradeId by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wizard_step_label, step, 3)) },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp),
        ) {
            when (step) {
                1 -> Step1(
                    areaText = areaText,
                    onArea = { areaText = it },
                    tierId = tierId,
                    onTier = { tierId = it },
                    startDate = startDate,
                    onDate = { startDate = it },
                    tiers = vm.tiers,
                )
                2 -> Step2(
                    modeId = modeId,
                    onMode = { modeId = it },
                    modes = vm.modes,
                )
                3 -> Step3(
                    gradeId = gradeId,
                    onGrade = { gradeId = it },
                    grades = vm.grades,
                    totalText = totalText,
                    onTotal = { totalText = it },
                    suggested = if (modeId.isNotBlank() && gradeId.isNotBlank() && areaText.toDoubleOrNull() != null) {
                        vm.suggestTotalYuan(areaText.toDouble(), tierId, modeId, gradeId).let { "%.0f".format(it) }
                    } else null,
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 1) {
                    TextButton(onClick = { step-- }) { Text(stringResource(R.string.wizard_back)) }
                } else {
                    TextButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                vm.finish(90.0, "t2", "clear", "mid", null, vm.suggestTotalYuan(90.0, "t2", "clear", "mid"))
                                busy = false
                                onFinished()
                            }
                        },
                    ) { Text(stringResource(R.string.wizard_skip)) }
                }

                PrimaryButton(
                    text = if (step < 3) stringResource(R.string.wizard_next) else stringResource(R.string.wizard_finish),
                    onClick = {
                        when (step) {
                            1 -> if (areaText.toDoubleOrNull() != null && tierId.isNotBlank()) step = 2
                            2 -> if (modeId.isNotBlank()) step = 3
                            3 -> {
                                val area = areaText.toDoubleOrNull() ?: 0.0
                                val total = totalText.toDoubleOrNull() ?: 0.0
                                if (area > 0 && gradeId.isNotBlank() && total >= 0) {
                                    scope.launch {
                                        busy = true
                                        vm.finish(area, tierId, modeId, gradeId, startDate, total)
                                        busy = false
                                        onFinished()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    enabled = !busy,
                )
            }
        }
    }
}

@Composable
private fun Step1(
    areaText: String,
    onArea: (String) -> Unit,
    tierId: String,
    onTier: (String) -> Unit,
    startDate: String?,
    onDate: (String) -> Unit,
    tiers: List<com.renovation.guardian.data.knowledge.TierJson>,
) {
    Column {
        Text("你家的基本情况", style = MaterialTheme.typography.titleLarge)
        Text("用来推算装修周期与预算范围。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(12.dp))
        OutlinedTextField(
            value = areaText,
            onValueChange = onArea,
            label = { Text("建筑面积（㎡）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(12.dp))
        Text("所在城市能级", style = MaterialTheme.typography.titleSmall)
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
            items(tiers) { t ->
                SelectableCard(
                    selected = tierId == t.id,
                    title = t.name,
                    subtitle = null,
                    emoji = "🏙️",
                    onClick = { onTier(t.id) },
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        DatePickerField(value = startDate, onDateSelected = onDate, label = "计划开工日期（可选）")
    }
}

@Composable
private fun Step2(
    modeId: String,
    onMode: (String) -> Unit,
    modes: List<com.renovation.guardian.data.knowledge.ModeJson>,
) {
    Column {
        Text("装修方式", style = MaterialTheme.typography.titleLarge)
        Text("决定预算模板如何切分。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            modes.forEach { m ->
                SelectableCard(
                    selected = modeId == m.id,
                    title = "${m.emoji} ${m.name}",
                    subtitle = m.short,
                    emoji = null,
                    onClick = { onMode(m.id) },
                )
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun Step3(
    gradeId: String,
    onGrade: (String) -> Unit,
    grades: List<com.renovation.guardian.data.knowledge.GradeJson>,
    totalText: String,
    onTotal: (String) -> Unit,
    suggested: String?,
) {
    Column {
        Text("装修档次与总预算", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(12.dp))
        grades.forEach { g ->
            SelectableCard(
                selected = gradeId == g.id,
                title = "${g.emoji} ${g.name}",
                subtitle = g.desc,
                emoji = null,
                onClick = { onGrade(g.id) },
            )
            Spacer(Modifier.size(8.dp))
        }
        Spacer(Modifier.size(12.dp))
        OutlinedTextField(
            value = totalText,
            onValueChange = onTotal,
            label = { Text("总预算（元）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        if (suggested != null) {
            Text(
                "参考推荐总预算：约 ¥$suggested（可在我的页修改）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    title: String,
    subtitle: String?,
    emoji: String?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (emoji != null) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (selected) {
                androidx.compose.material3.Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
