package com.renovation.guardian.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.data.db.StageTaskView
import com.renovation.guardian.ui.components.*
import com.renovation.guardian.util.DateUtil
import com.renovation.guardian.util.MoneyUtil
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenStage: (String) -> Unit = {}, onOpenBudget: () -> Unit = {}) {
    val vm: HomeViewModel = viewModel()
    val ui by vm.uiState.collectAsState()
    val overdue by vm.overdueTasks.collectAsState(emptyList())
    val today by vm.todayTasks.collectAsState(emptyList())
    val upcoming by vm.upcomingTasks.collectAsState(emptyList())
    val host = remember { SnackbarHostState() }
    OperationFeedback(vm, host)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, vm) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { vm.refreshDate(); delay(30_000) } }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("装修进行时") }) }, snackbarHost = { SnackbarHost(host) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard(onClick = { onOpenStage(ui.currentStageId.orEmpty()) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("当前阶段", style = MaterialTheme.typography.labelMedium)
                        Text(when {
                            ui.currentStageName != null -> "${ui.currentStageEmoji.orEmpty()} ${ui.currentStageName}"
                            ui.totalTasks > 0 -> "全部完成"
                            else -> "暂无任务，可添加或恢复默认清单"
                        }, style = MaterialTheme.typography.titleLarge)
                        LinearProgressIndicator(progress = { (ui.overallPct / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text("已完成 ${ui.doneTasks} / ${ui.totalTasks} 项任务", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            taskGroup("逾期", overdue, vm, onOpenStage)
            taskGroup("今天", today, vm, onOpenStage)
            if (overdue.isEmpty() && today.isEmpty()) item {
                Text("今天没有待办", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { onOpenStage(ui.currentStageId.orEmpty()) }) { Text("到流程页安排任务日期") }
            }
            item {
                SectionCard(onClick = onOpenBudget) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("预算概览", style = MaterialTheme.typography.titleMedium)
                        MoneyLine("总预算上限", ui.totalPlannedCents)
                        MoneyLine("分类计划", ui.categoryPlannedCents)
                        MoneyLine("实际支出", ui.totalSpentCents)
                        if (ui.totalOverCents > 0) Text("总预算超支 ¥${MoneyUtil.formatFull(ui.totalOverCents)}", color = MaterialTheme.colorScheme.error)
                        if (ui.overspentCount > 0) Text("${ui.overspentCount} 个分类超出计划", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            taskGroup("未来 7 天", upcoming, vm, onOpenStage)
        }
    }
}

private fun LazyListScope.taskGroup(title: String, tasks: List<StageTaskView>, vm: HomeViewModel, onOpenStage: (String) -> Unit) {
    if (tasks.isEmpty()) return
    item { Text("$title · ${tasks.size} 项", style = MaterialTheme.typography.titleMedium) }
    items(tasks, key = { "$title-${it.source}-${it.id}" }) { task ->
        Row(modifier = Modifier.fillMaxWidth().clickable { onOpenStage(task.stageId) }, verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.done, onCheckedChange = { vm.toggleDone(task) })
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(task.text, style = MaterialTheme.typography.bodyLarge)
                task.dueDate?.let { Text(DateUtil.formatCN(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
