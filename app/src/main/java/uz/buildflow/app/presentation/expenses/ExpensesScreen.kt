package uz.buildflow.app.presentation.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.Expense
import uz.buildflow.app.presentation.common.EmptyStateView
import uz.buildflow.app.presentation.common.MetricCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel,
    onBackToObjects: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Obyekt Xarajatlari",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (onBackToObjects != null) {
                        IconButton(onClick = onBackToObjects) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Barcha Obyektlar"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yangilash (Refresh)",
                            tint = DeepBluePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddExpense() },
                containerColor = DeepBluePrimary,
                contentColor = SurfaceLight,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Xarajat qo'shish")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MetricCard(
                    title = "Ushbu Obyekt Xarajatlari Jami",
                    amount = CurrencyFormatter.formatAmount(uiState.totalExpense),
                    accentColor = RoseExpense,
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    modifier = Modifier.padding(16.dp)
                )

                if (uiState.expenses.isEmpty() && !uiState.isLoading) {
                    EmptyStateView(
                        title = "Xarajatlar yo'q",
                        description = "Yangi xarajat kiritish uchun pastdagi '+' tugmasini bosing.",
                        icon = Icons.Default.Receipt,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.expenses, key = { it.id }) { exp ->
                            ExpenseItemCard(
                                expense = exp,
                                onClick = { viewModel.openEditExpense(exp) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddSheetOpen) {
        AddExpenseSheet(
            existingExpense = uiState.selectedExpense,
            categories = uiState.categories,
            onDismiss = { viewModel.closeAddExpense() },
            onDelete = { exp -> viewModel.deleteExpense(exp) },
            onAddNewCategory = { name -> viewModel.addNewCategory(name) },
            onSave = { cat, amt, date, desc, workerId ->
                viewModel.saveExpense(cat, amt, date, desc, workerId)
            }
        )
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                if (!expense.description.isNullOrBlank()) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Text(
                    text = DateUtil.formatToDisplay(expense.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CurrencyFormatter.formatAmount(expense.amount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RoseExpense
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Tahrirlash",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
