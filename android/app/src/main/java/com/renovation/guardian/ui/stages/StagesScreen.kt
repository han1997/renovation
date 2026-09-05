package com.renovation.guardian.ui.stages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.data.db.StageEntity
import com.renovation.guardian.data.db.TaskEntity
import com.renovation.guardian.ui.components.ConfirmDeleteDialog
import com.renovation.guardian.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StagesScreen() {
    val vm: StagesViewModel = viewModel()
    val stages by vm.stages.collectAsState(initial = emptyList())
    val progress by vm.progress.collectAsState(initial = emptyList())
    var expandedId by remember { mutableStateOf<String?>(null) }
    var deleteCustomTask by remember { mutableStateOf<TaskEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("装修流程") }) },
    ) { inner ->
        if (stages.isEmpty()) {
            // 首次启动时种子写入是异步的：表为空可能是「还没写完」也可能是「写入失败」。
            // 统一给加载占位，避免整页白屏（种子写入完成后 Flow 会自动刷新出列表）。
            Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        "正在准备数据…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp).imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(stages, key = { it.id }) { stage ->
                    val st = progress.firstOrNull { it.stageId == stage.id }
                    val pct = st?.pct ?: 0
                    val expanded = expandedId == stage.id
                    StageCard(
                        stage = stage,
                        pct = pct,
                        isDone = st?.isDone ?: false,
                        expanded = expanded,
                        onClick = { expandedId = if (expanded) null else stage.id },
                    )
                    if (expanded) {
                        val detailFlow = remember(stage.id) { vm.observeStageDetail(stage) }
                        val detail by detailFlow.collectAsState(
                            initial = StageDetailUi(stage, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                        )
                        StageDetailContent(
                            detail = detail,
                            vm = vm,
                            onRequestDeleteCustom = { deleteCustomTask = it },
                        )
                    }
                }
            }
        }
    }

    deleteCustomTask?.let { task ->
        ConfirmDeleteDialog(
            title = "删除任务",
            text = "确定删除「${task.text}」？删除后不可恢复。",
            onConfirm = { vm.deleteCustom(task.id); deleteCustomTask = null },
            onDismiss = { deleteCustomTask = null },
        )
    }
}

@Composable
private fun StageCard(
    stage: StageEntity,
    pct: Int,
    isDone: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    SectionCard(onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stage.emoji, style = MaterialTheme.typography.titleLarge)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stage.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stage.phase, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (isDone) "已完成" else "$pct%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { (pct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(stage.duration, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun StageDetailContent(
    detail: StageDetailUi,
    vm: StagesViewModel,
    onRequestDeleteCustom: (TaskEntity) -> Unit,
) {
    var newTask by remember { mutableStateOf("") }

    Column {
        SectionCard {
            DetailSectionTitle("🎯 目标")
            Text(detail.stage.goal, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }

        if (detail.warnings.isNotEmpty()) {
            SectionCard(modifier = Modifier.padding(top = 12.dp)) {
                DetailSectionTitle("⚠️ 避坑要点")
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.warnings.forEach {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        if (detail.buy.isNotEmpty()) {
            SectionCard(modifier = Modifier.padding(top = 12.dp)) {
                DetailSectionTitle("🛒 需购材料")
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.buy.forEach {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                it.item,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            it.note?.let { n ->
                                Text(
                                    "（$n）",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        SectionCard(modifier = Modifier.padding(top = 12.dp)) {
            DetailSectionTitle("✅ 任务清单")
            Column(modifier = Modifier.padding(top = 4.dp)) {
                detail.templates.forEachIndexed { index, t ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = t.done, onCheckedChange = { vm.toggleTemplate(t.id, it) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.text, style = MaterialTheme.typography.bodyLarge)
                            t.tip?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
                if (detail.templates.isNotEmpty() && detail.customTasks.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
                detail.customTasks.forEachIndexed { index, c ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = c.done, onCheckedChange = { vm.toggleCustom(c.id, it) })
                        Text(c.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRequestDeleteCustom(c) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                OutlinedTextField(
                    value = newTask,
                    onValueChange = { newTask = it },
                    label = { Text("添加自定义任务") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newTask.isNotBlank()) {
                                vm.addCustom(detail.stage.id, newTask)
                                newTask = ""
                            }
                        },
                    ),
                )
                IconButton(
                    onClick = {
                        if (newTask.isNotBlank()) {
                            vm.addCustom(detail.stage.id, newTask)
                            newTask = ""
                        }
                    },
                    enabled = newTask.isNotBlank(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "添加")
                }
            }
        }

        detail.checklists.forEach { cl ->
            SectionCard(modifier = Modifier.padding(top = 12.dp)) {
                DetailSectionTitle("${cl.emoji} ${cl.name}")
                if (cl.items.isEmpty()) {
                    Text(
                        "（暂无条目）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    cl.items.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.done, onCheckedChange = { vm.toggleChecklistItem(item.id, it) })
                            Text(item.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}
