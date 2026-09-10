package com.renovation.guardian.ui.budget

import com.renovation.guardian.util.MoneyUtil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.renovation.guardian.ui.components.ConfirmDeleteDialog
import com.renovation.guardian.ui.components.DatePickerField
import com.renovation.guardian.ui.components.MoneyText
import com.renovation.guardian.ui.components.SectionCard
import com.renovation.guardian.ui.nav.LocalExportActions
import com.renovation.guardian.util.DateUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(onOpenQuote: () -> Unit = {}) {
    val vm: BudgetViewModel = viewModel()
    val categories by vm.categories.collectAsState(initial = emptyList())
    val csvRows by vm.csvRows.collectAsState(initial = emptyList())
    val exportActions = LocalExportActions.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddCat by rememberSaveable { mutableStateOf(false) }
    var editCatId by rememberSaveable { mutableStateOf<String?>(null) }
    val editCat = categories.firstOrNull { it.id == editCatId }
    var addExpenseFor by rememberSaveable { mutableStateOf<String?>(null) }
    var editExpenseId by rememberSaveable { mutableStateOf<String?>(null) }
    val expenseFlow = remember(editExpenseId) { editExpenseId?.let(vm::observeExpense) ?: kotlinx.coroutines.flow.flowOf(null) }
    val editExpense by expenseFlow.collectAsState(initial = null)
    var deleteCatId by remember { mutableStateOf<String?>(null) }
    var deleteExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    val profile by vm.profile.collectAsState(initial = null)
    com.renovation.guardian.ui.components.OperationFeedback(vm, snackbar)
    val totalPlanned = categories.sumOf { it.plannedCents }
    val totalSpent = categories.sumOf { it.spentCents }
    val totalOver = categories.sumOf { it.overCents }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预算") },
                actions = {
                    IconButton(onClick = {
                        exportActions.exportCsv("renovation-budget.csv", buildCsv(csvRows))

                    }) {
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
            // 底部留出 FAB 高度，避免最后一个卡片被遮挡
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
        ) {
            item {
                SectionCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onOpenQuote() }.padding(vertical = 4.dp),
                    ) {
                        Text("🧮", style = MaterialTheme.typography.titleLarge)
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text("逐空间报价", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "整装 / 半包 / 局改预算计算器,按空间逐项选材并生成清单",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("进入 ›", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                SectionCard {
                    Column {
                        Text("总预算上限 ¥${MoneyUtil.formatFull(profile?.totalBudgetCents ?: 0L)}", style = MaterialTheme.typography.titleMedium)
                        if (totalSpent > (profile?.totalBudgetCents ?: 0L)) Text("实际支出已超过总预算上限", color = MaterialTheme.colorScheme.error)
                        if (totalPlanned > (profile?.totalBudgetCents ?: 0L)) Text("分类计划合计超过上限，请检查分配", color = MaterialTheme.colorScheme.error)
                        com.renovation.guardian.ui.components.MoneyLine("分类计划", totalPlanned)
                        com.renovation.guardian.ui.components.MoneyLine("实际支出", totalSpent)
                        if (totalOver > 0) {
                            Text("分类超支合计 ¥${com.renovation.guardian.util.MoneyUtil.formatFull(totalOver)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }

            if (categories.isEmpty()) item { Text("暂无预算分类，点击右下角添加分类，或先生成报价。") }
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
                            TextButton(onClick = { editCatId = cat.id }) { Text("编辑") }
                            TextButton(onClick = { deleteCatId = cat.id }) { Text("删除") }
                        }
                    }
                }
                if (expanded) {
                    // remember 住 Flow 实例，避免每次重组重建 Room Flow 导致重复查库
                    val expensesFlow = remember(cat.id) { vm.observeExpenses(cat.id) }
                    val expenses by expensesFlow.collectAsState(initial = emptyList())
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
                                    IconButton(onClick = { editExpenseId = ex.id }) { Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    IconButton(onClick = { deleteExpense = ex }) { Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
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
            vm.addCategoryCents(name, emoji, planned) { showAddCat = false }

        }
    }
    editCat?.let { c ->
        CategoryDialog(initial = c, onDismiss = { editCatId = null }) { name, emoji, planned ->
            vm.updateCategoryCents(c.id, name, planned) { editCatId = null }

        }
    }
    addExpenseFor?.let { cid ->
        ExpenseDialog(initial = null, categoryId = cid, today = vm.today(), onDismiss = { addExpenseFor = null }) { name, amount, catId, date, note ->
            vm.addExpenseCents(name, amount, catId, date, note) { addExpenseFor = null }

        }
    }
    editExpense?.let { ex ->
        ExpenseDialog(initial = ex, categoryId = ex.categoryId, today = vm.today(), onDismiss = { editExpenseId = null }) { name, amount, catId, date, note ->
            vm.updateExpenseCents(ex.id, name, amount, catId, date, note) { editExpenseId = null }

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
    deleteExpense?.let { ex ->
        ConfirmDeleteDialog(
            title = "删除支出",
            text = "确定删除「${ex.name}」这笔记录？删除后不可恢复。",
            onConfirm = {
                vm.deleteExpense(ex.id)
                deleteExpense = null

            },
            onDismiss = { deleteExpense = null },
        )
    }
}

@Composable
private fun CategoryDialog(
    initial: CategoryWithSpent?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, plannedCents: Long) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var emoji by rememberSaveable { mutableStateOf(initial?.emoji ?: "💰") }
    var planned by rememberSaveable { mutableStateOf(initial?.plannedCents?.let { MoneyUtil.input(it) } ?: "") }

    // 必填校验：名称为空 / 预算非法时给可见错误提示，不静默失败
    val nameError = name.isBlank()
    val plannedValue = MoneyUtil.parseYuan(planned)
    val plannedError = plannedValue == null || plannedValue < 0
    val canSave = name.isNotBlank() && !plannedError

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), emoji.ifBlank { "💰" }, plannedValue ?: 0L) },
                enabled = canSave,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (initial == null) "新增分类" else "编辑分类") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("请填写分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("图标 emoji") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = planned,
                    onValueChange = { planned = it },
                    label = { Text("预算（元）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = plannedError,
                    supportingText = { if (plannedError) Text("请输入不小于 0 的金额") },
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
    today: String,
    onDismiss: () -> Unit,
    onSave: (name: String, amountCents: Long, categoryId: String, date: String, note: String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var amount by rememberSaveable { mutableStateOf(initial?.amountCents?.let { MoneyUtil.input(it) } ?: "") }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var date by rememberSaveable { mutableStateOf(initial?.date ?: today) }

    // 必填校验：项目名 / 金额（> 0）非法时给可见错误提示，不静默失败
    val nameError = name.isBlank()
    val amountValue = MoneyUtil.parseYuan(amount)
    val amountError = amountValue == null || amountValue <= 0
    val canSave = name.isNotBlank() && amountValue != null && amountValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), amountValue ?: 0L, categoryId, date, note.ifBlank { null }) },
                enabled = canSave,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (initial == null) "新增支出" else "编辑支出") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("项目") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("请填写项目名称") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额（元）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = {
                        Text(
                            when {
                                amountError -> "请输入大于 0 的金额"
                                amount.isBlank() -> "必填"
                                else -> ""
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                DatePickerField(value = date, onDateSelected = { date = it }, label = "日期")
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}
