package com.renovation.guardian.ui.guide

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.data.knowledge.KnowledgeJson
import com.renovation.guardian.ui.AppViewModel
import com.renovation.guardian.ui.components.SectionCard

class GuideViewModel(application: Application) : AppViewModel(application) {
    val knowledge: KnowledgeJson? get() = container.knowledge.knowledge
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen() {
    val vm: GuideViewModel = viewModel()
    val knowledge = vm.knowledge
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("避坑", "验收清单", "风格", "建材日历", "百科")

    Scaffold(
        topBar = { TopAppBar(title = { Text("装修指南") }) },
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            ScrollableTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            if (knowledge == null) {
                Text("资料加载中…", modifier = Modifier.padding(16.dp))
                return@Column
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (tab) {
                    0 -> tipsTab(knowledge)
                    1 -> checklistsTab(knowledge)
                    2 -> stylesTab(knowledge)
                    3 -> timelineTab(knowledge)
                    4 -> glossaryTab(knowledge)
                }
            }
        }
    }
}

private fun LazyListScope.tipsTab(k: KnowledgeJson) {
    items(k.tips, key = { it.title + it.topic }) { tip ->
        SectionCard {
            Column {
                Text(tip.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("级别：${tip.level} · ${tip.topic}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(tip.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

private fun LazyListScope.checklistsTab(k: KnowledgeJson) {
    items(k.checklists, key = { it.id }) { cl ->
        SectionCard {
            Column {
                Text("${cl.emoji} ${cl.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                cl.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                cl.items.forEach { item ->
                    Text("☐ ${item.text}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

private fun LazyListScope.stylesTab(k: KnowledgeJson) {
    items(k.styles, key = { it.id }) { s ->
        SectionCard {
            Column {
                Text("${s.emoji} ${s.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(s.tagline, style = MaterialTheme.typography.bodyMedium)
                Text("参考造价：${s.cost}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                s.desc.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
                Text("适合：${s.fit}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                if (s.elements.isNotEmpty()) {
                    Text("元素", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                    s.elements.forEach { Text("· $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (s.pitfalls.isNotEmpty()) {
                    Text("避坑", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.error)
                    s.pitfalls.forEach { Text("· $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

private fun LazyListScope.timelineTab(k: KnowledgeJson) {
    items(k.materialTimeline, key = { it.period + it.whenText }) { m ->
        SectionCard {
            Column {
                Text("${m.emoji} ${m.period}（${m.whenText}）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                m.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                m.items.forEach { item ->
                    Text("· ${item.name}（${item.lead}）${item.note?.let { " — $it" } ?: ""}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

private fun LazyListScope.glossaryTab(k: KnowledgeJson) {
    items(k.glossary, key = { it.term }) { g ->
        SectionCard {
            Column {
                Text(g.term, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(g.def, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
