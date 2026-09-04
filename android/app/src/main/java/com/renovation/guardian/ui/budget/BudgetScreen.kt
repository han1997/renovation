package com.renovation.guardian.ui.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renovation.guardian.data.db.CategoryWithSpent
import com.renovation.guardian.data.db.ExpenseEntity
import com.renovation.guardian.ui.components.BudgetProgressBar
import com.renovation.guardian.ui.components.DatePickerField
import com.renovation.guardian.ui.components.MoneyText
import com.renovation.guardian.ui.components.SectionCard
import com.renovation.guardian.ui.nav.LocalExportActions
import com.renovation.guardian.util.DateUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen() {
    val vm: BudgetViewModel = viewModel()
    val categories by vm.categories.collectAsState(initial = emptyList())
    val csvRows by vm.csvRows.collectAsState(initial = emptyList())
    val exportActions = LocalExportActions.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var expandedId by remember { mutableStateOf<String?>(null) }
    var showAddCat by remember { mutableStateOf(false) }
    var editCat by remember { mutableStateOf<CategoryWithSpent?>(null) }
    var addExpenseFor by remember { mutableStateOf<String?>(null) }
    var editExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var deleteCatId by remember { mutableStateOf<String?>(null) }

    val totalPlanned = categories.sumOf { it.plannedCents }
    val totalSpent = categories.sumOf { it.spentCents }
    val totalOver = categories.sumOf { it.overCents }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预算") },
                actions = {
                    IconButton(onClick = { exportActions.exportCsv("renovation-budget.csv", buildCsv(csvRows)) }) {
                        Icon(Icons.Filled.Upload, contentDescription = "导出 CSV")
                    }
                },
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = { showAddCat = true }) {
                Icon(Icons.Filled.Add, contentDescription = "新增分类")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Column {
                        Text("总览", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("预算", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(totalPlanned, style = MaterialTheme.typography.bodyLarge)
                            }
                            Column {
                                Text("已支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(totalSpent, style = MaterialTheme.typography.bodyLarge)
                            }
                            Column {
                                Text("超支", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(totalOver, color = if (totalOver > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        if (totalOver > 0) {
                            Text("⚠️ 已超出预算 ¥${com.renovation.guardian.util.MoneyUtil.formatFull(totalOver)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }

            items(categories, key = { it.id }) { cat ->
                val expanded = expandedId == cat.id
                SectionCard(onClick = { expandedId = if (expanded) null else cat.id }) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.emoji, style = MaterialTheme.typography.titleLarge)
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(cat.name, style = MaterialTheme.typography.titleMedium)
                                Text("${com.renovation.guardian.util.MoneyUtil.formatFull(cat.spentCents)} / ${com.renovation.guardian.util.MoneyUtil.formatFull(cat.plannedCents)} 元", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (cat.overCents > 0) {
                                Text("超支", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        BudgetProgressBar(cat.pct, cat.overCents, modifier = Modifier.padding(top = 8.dp))
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            TextButton(onClick = { addExpenseFor = cat.id }) { Text("+ 支出") }
                            TextButton(onClick = { editCat = cat }) { Text("编辑") }
                            TextButton(onClick = { deleteCatId = cat.id }) { Text("删除") }
                        }
                    }
                }
                if (expanded) {
                    val expenses by vm.observeExpenses(cat.id).collectAsState(initial = emptyList())
                    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                        if (expenses.isEmpty()) {
                            Text("暂无支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            expenses.forEach { ex ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ex.name, style = MaterialTheme.typography.bodyMedium)
                                        Text("${DateUtil.formatCN(ex.date)}  ·  ¥${com.renovation.guardian.util.MoneyUtil.formatFull(ex.amountCents)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { editExpense = ex }) { Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    IconButton(onClick = { vm.deleteExpense(ex.id) }) { Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCat) {
        CategoryDialog(initial = null, onDismiss = { showAddCat = false }) { name, emoji, planned ->
            vm.addCategory(name, emoji, planned)
            showAddCat = false
        }
    }
    editCat?.let { c ->
        CategoryDialog(initial = c, onDismiss = { editCat = null }) { name, emoji, planned ->
            vm.updateCategory(c.id, name, planned)
            editCat = null
        }
    }
    addExpenseFor?.let { cid ->
        ExpenseDialog(initial = null, categoryId = cid, onDismiss = { addExpenseFor = null }) { name, amount, catId, date, note ->
            vm.addExpense(name, amount, catId, date, note)
            addExpenseFor = null
        }
    }
    editExpense?.let { ex ->
        ExpenseDialog(initial = ex, categoryId = ex.categoryId, onDismiss = { editExpense = null }) { name, amount, catId, date, note ->
            vm.updateExpense(ex.id, name, amount, catId, date, note)
            editExpense = null
        }
    }
    deleteCatId?.let { cid ->
        AlertDialog(
            onDismissRequest = { deleteCatId = null },
            title = { Text("删除分类") },
            text = { Text("该分类下如有支出将无法删除。确定删除？") },
            confirmButton = { TextButton(onClick = {
                vm.deleteCategory(cid) { ok ->
                    if (!ok) scope.launch { snackbar.showSnackbar("分类下还有支出，无法删除") }
                }
                deleteCatId = null
            }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleteCatId = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun CategoryDialog(
    initial: CategoryWithSpent?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, plannedYuan: Double) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "💰") }
    var planned by remember { mutableStateOf(initial?.plannedCents?.let { (it / 100.0).toString() } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(name, emoji, planned.toDoubleOrNull() ?: 0.0) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (initial == null) "新增分类" else "编辑分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("图标 emoji") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = planned,
                    onValueChange = { planned = it },
                    label = { Text("预算（元）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun ExpenseDialog(
    initial: ExpenseEntity?,
    categoryId: String,
    onDismiss: () -> Unit,
    onSave: (name: String, amountYuan: Double, categoryId: String, date: String, note: String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.amountCents?.let { (it / 100.0).toString() } ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: DateUtil.today()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(name, amount.toDoubleOrNull() ?: 0.0, categoryId, date, note.ifBlank { null }) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (initial == null) "新增支出" else "编辑支出") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("项目") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额（元）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                DatePickerField(value = date, onDateSelected = { date = it }, label = "日期")
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}
