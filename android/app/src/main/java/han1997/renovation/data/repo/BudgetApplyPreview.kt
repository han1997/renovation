package han1997.renovation.data.repo

/** 已确认的分类差额快照，仓库在事务中校验原值，防止覆盖预览后的编辑。 */
data class CategoryBudgetChange(val categoryId: String, val name: String, val beforeCents: Long, val afterCents: Long)
data class BudgetApplyPreview(val changes: List<CategoryBudgetChange>, val estimatedTotalCents: Long)
