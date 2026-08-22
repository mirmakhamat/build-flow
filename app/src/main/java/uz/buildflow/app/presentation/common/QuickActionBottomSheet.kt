package uz.buildflow.app.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionBottomSheet(
    onDismiss: () -> Unit,
    onAddAttendance: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddBonus: () -> Unit,
    onAddWorker: () -> Unit,
    onAddObject: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tezkor Amallar",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            QuickActionItem(
                icon = Icons.Default.FactCheck,
                title = "Kunlik Davomat Kiritish",
                subtitle = "Ishchilarning bugungi ish kunini qayd qilish",
                iconColor = DeepBluePrimary,
                onClick = { onDismiss(); onAddAttendance() }
            )

            QuickActionItem(
                icon = Icons.Default.ReceiptLong,
                title = "Xarajat Kiritish",
                subtitle = "Yo'l kira, material, ovqatlanish va boshqa chiqimlar",
                iconColor = RoseExpense,
                onClick = { onDismiss(); onAddExpense() }
            )

            QuickActionItem(
                icon = Icons.Default.TrendingUp,
                title = "Obyektga Pul Kirimi (Tushum)",
                subtitle = "Mijozdan olingan navbatdagi to'lov",
                iconColor = EmeraldSuccess,
                onClick = { onDismiss(); onAddIncome() }
            )

            QuickActionItem(
                icon = Icons.Default.CardGiftcard,
                title = "Ishchiga Bonus Berish",
                subtitle = "Kunlik yoki umumiy rag'batlantirish",
                iconColor = AmberWarning,
                onClick = { onDismiss(); onAddBonus() }
            )

            QuickActionItem(
                icon = Icons.Default.PersonAdd,
                title = "Yangi Ishchi Qo'shish",
                subtitle = "Obyektga yangi usta yoki shogird biriktirish",
                iconColor = DeepBlueLight,
                onClick = { onDismiss(); onAddWorker() }
            )

            QuickActionItem(
                icon = Icons.Default.Apartment,
                title = "Yangi Obyekt Yaratish",
                subtitle = "Yangi qurilish yoki ta'mirlash loyihasi",
                iconColor = DeepBlueDark,
                onClick = { onDismiss(); onAddObject() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}
