package com.renovation.guardian.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.data.db.StageTaskView
import com.renovation.guardian.ui.components.MoneyText
import com.renovation.guardian.ui.components.SectionCard
import com.renovation.guardian.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val vm: HomeViewModel = viewModel()
    val ui by vm.uiState.collectAsState()
    val overdue by vm.overdueTasks.collectAsState(emptyList())
    val today by vm.todayTasks.collectAsState(emptyList())
    val upcoming by vm.upcomingTasks.collectAsState(emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("装修进行时") }) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Column {
                        Text("当前阶段", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        // totalTasks == 0 说明种子还没写入（或写入失败），此时不能误报「全部完成 🎉」，
                        // 给中性占位文案，等 Flow 刷新后自动变为正常展示。
                        val headline = when {
                            ui.totalTasks > 0 && ui.currentStageName != null -> "${ui.currentStageEmoji ?: ""} ${ui.currentStageName}"
                            ui.totalTasks > 0 -> "全部完成 🎉"
                            else -> "正在准备你的装修计划…"
                        }
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LinearProgressIndicator(
                                progress = { (ui.overallPct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.weight(1f),
                            )
                            Text(" ${ui.overallPct}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(
                            text = if (ui.totalTasks > 0) "已完成 ${ui.doneTasks} / ${ui.totalTasks} 项任务" else "任务数据加载中，稍等片刻…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                SectionCard {
                    Column {
                        Text("预算概览", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("总预算", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(ui.totalPlannedCents, style = MaterialTheme.typography.bodyLarge)
                            }
                            Column {
                                Text("已支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(ui.totalSpentCents, style = MaterialTheme.typography.bodyLarge)
                            }
                            Column {
                                Text("超支", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(ui.totalOverCents, color = if (ui.totalOverCents > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        if (ui.overspentCount > 0) {
                            Text(
                                "⚠️ 有 ${ui.overspentCount} 个分类超出预算",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            // 三组任务全空时给一条聚合引导，避免三行「暂无任务」堆叠
            val hasAnyTask = overdue.isNotEmpty() || today.isNotEmpty() || upcoming.isNotEmpty()
            if (hasAnyTask) {
                actionGroup("逾期", overdue, vm) { MaterialTheme.colorScheme.error }
                actionGroup("今天", today, vm) { MaterialTheme.colorScheme.primary }
                actionGroup("未来 7 天", upcoming, vm) { MaterialTheme.colorScheme.onSurfaceVariant }
            } else {
                item {
                    SectionCard {
                        Text(
                            "近 7 天暂无安排，去流程页看看接下来的任务吧",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.actionGroup(
    title: String,
    tasks: List<StageTaskView>,
    vm: HomeViewModel,
    color: @Composable () -> androidx.compose.ui.graphics.Color,
) {
    item {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
    }
    if (tasks.isEmpty()) {
        item {
            Text("暂无任务", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        items(tasks, key = { it.id }) { task ->
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.done, onCheckedChange = { vm.toggleDone(task) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.text, style = MaterialTheme.typography.bodyLarge)
                        val due = task.dueDate?.let { DateUtil.formatCN(it) }
                        if (due != null) {
                            Text(due, style = MaterialTheme.typography.bodySmall, color = color())
                        }
                    }
                }
            }
        }
    }
}


