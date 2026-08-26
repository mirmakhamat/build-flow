package uz.buildflow.app.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.domain.model.ObjectFinancialSummary
import uz.buildflow.app.presentation.common.MetricCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    summary: ObjectFinancialSummary?,
    onBackToObjects: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Moliyaviy Hisobot",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToObjects) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Obyektlar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Umumiy Xulosa Kartochkasi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = summary?.objectName ?: "Obyekt Xulosasi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    HorizontalDivider(color = BorderColor)

                    // 1. Mijoz va Daromad qismi
                    Text(
                        text = "Mijoz To'lovlari va Qoldiq",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                    BreakdownRow(
                        title = "Obyekt umumiy narxi",
                        amount = summary?.totalPrice ?: 0.0,
                        isCustomText = (summary?.totalPrice ?: 0.0) <= 0.0,
                        customText = "Kiritilmagan"
                    )
                    BreakdownRow(title = "Tushgan pul (Olingan avanslar)", amount = summary?.totalReceivedIncome ?: 0.0, customColor = EmeraldSuccess)
                    BreakdownRow(
                        title = "Mijozdan qolgan summa (Qoldiq)",
                        amount = summary?.remainingReceivable ?: 0.0,
                        customColor = if ((summary?.remainingReceivable ?: 0.0) > 0) AmberWarning else EmeraldSuccess,
                        isCustomText = (summary?.totalPrice ?: 0.0) <= 0.0,
                        customText = "—"
                    )

                    HorizontalDivider(color = BorderColor)

                    // 2. Ishchilar qismi
                    Text(
                        text = "Ishchilar Hisob-kitobi",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                    BreakdownRow(title = "Hisoblangan jami ish haqi", amount = summary?.totalWorkerSalary ?: 0.0)
                    BreakdownRow(title = "Hisoblangan jami bonuslar", amount = summary?.totalBonuses ?: 0.0)
                    BreakdownRow(title = "Ishchilarga berilgan to'lovlar (Jami)", amount = summary?.totalPaidToWorkers ?: 0.0, customColor = EmeraldSuccess)
                    
                    if ((summary?.totalPaidByOtherObjectsForThisWorkers ?: 0.0) > 0) {
                        BreakdownRow(
                            title = "  ↳ Boshqa obyekt hisobidan qoplangan",
                            amount = summary?.totalPaidByOtherObjectsForThisWorkers ?: 0.0,
                            customColor = DeepBluePrimary
                        )
                    }

                    if ((summary?.totalPaidForOtherObjectsWorkers ?: 0.0) > 0) {
                        BreakdownRow(
                            title = "  ↳ Boshqa obyekt ishchilariga to'lab berilgan",
                            amount = summary?.totalPaidForOtherObjectsWorkers ?: 0.0,
                            customColor = AmberWarning
                        )
                    }

                    BreakdownRow(
                        title = "Ishchilarga qolgan qarz",
                        amount = summary?.totalWorkerDebt ?: 0.0,
                        customColor = if ((summary?.totalWorkerDebt ?: 0.0) > 0) RoseExpense else EmeraldSuccess
                    )

                    HorizontalDivider(color = BorderColor)

                    // 3. Qo'shimcha Xarajatlar
                    Text(
                        text = "Qo'shimcha Xarajatlar Taqsimoti",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                    if (summary?.categoryBreakdowns.isNullOrEmpty()) {
                        BreakdownRow(title = "Boshqa xarajatlar", amount = summary?.totalOtherExpenses ?: 0.0)
                    } else {
                        summary?.categoryBreakdowns?.forEach { item ->
                            BreakdownRow(title = item.categoryName, amount = item.totalAmount)
                        }
                    }

                    if ((summary?.totalExpensesPaidByOtherObjects ?: 0.0) > 0) {
                        BreakdownRow(
                            title = "  ↳ Boshqa obyekt hisobidan to'langan xarajat",
                            amount = summary?.totalExpensesPaidByOtherObjects ?: 0.0,
                            customColor = DeepBluePrimary
                        )
                    }

                    if ((summary?.totalExpensesPaidForOtherObjects ?: 0.0) > 0) {
                        BreakdownRow(
                            title = "  ↳ Boshqa obyekt uchun to'lab berilgan xarajat",
                            amount = summary?.totalExpensesPaidForOtherObjects ?: 0.0,
                            customColor = AmberWarning
                        )
                    }

                    HorizontalDivider(color = BorderColor)

                    // 4. Yakuniy Moliyaviy Natija va Kassa
                    BreakdownRow(
                        title = "Jami Obyekt Xarajatlari",
                        amount = summary?.totalExpenses ?: 0.0,
                        customColor = RoseExpense,
                        isBold = true
                    )
                    BreakdownRow(
                        title = "Kassadan chiqqan jami pul (Chiqim)",
                        amount = summary?.totalCashOutflow ?: 0.0,
                        customColor = RoseExpense,
                        isBold = true
                    )
                    BreakdownRow(
                        title = "Qo'ldagi pul (Kassa balansi)",
                        amount = summary?.cashBalance ?: 0.0,
                        customColor = if ((summary?.cashBalance ?: 0.0) >= 0) EmeraldSuccess else RoseExpense,
                        isBold = true
                    )
                    BreakdownRow(
                        title = "Taxminiy Sof Foyda",
                        amount = summary?.estimatedProfit ?: 0.0,
                        customColor = if ((summary?.estimatedProfit ?: 0.0) >= 0) EmeraldSuccess else RoseExpense,
                        isBold = true,
                        isCustomText = (summary?.totalPrice ?: 0.0) <= 0.0,
                        customText = "Kiritilmagan"
                    )
                }
            }
        }
    }
}

@Composable
fun BreakdownRow(
    title: String,
    amount: Double,
    customColor: Color = TextPrimary,
    isBold: Boolean = false,
    isCustomText: Boolean = false,
    customText: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = if (isBold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
            color = if (isBold) TextPrimary else TextSecondary
        )
        Text(
            text = if (isCustomText) customText else CurrencyFormatter.formatAmount(amount),
            style = if (isBold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = customColor
        )
    }
}
