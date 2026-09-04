package uz.buildflow.app.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.buildflow.app.BuildConfig
import uz.buildflow.app.R
import uz.buildflow.app.core.theme.*

@Composable
fun MainDrawerSheet(
    deviceId: String,
    onNavigateToObjects: () -> Unit,
    onNavigateToGlobalReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onExportDatabase: () -> Unit,
    onImportDatabase: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val companyName = stringResource(R.string.company_name)

    ModalDrawerSheet(
        modifier = modifier.width(310.dp),
        drawerContainerColor = SurfaceLight,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Header (Kompaniya va Qurilma info)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepBluePrimary)
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BusinessCenter,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = companyName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ID: $deviceId",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(text = "•", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                            Text(text = "Faol", color = EmeraldSuccess, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Asosiy Menyu Bandlari
            DrawerSectionTitle("ASOSIY BO'LIMLAR")

            DrawerMenuItem(
                icon = Icons.Default.Apartment,
                title = "Faol Obyektlar",
                subtitle = "Barcha qurilish obyektlari",
                onClick = onNavigateToObjects
            )

            DrawerMenuItem(
                icon = Icons.Default.Assessment,
                title = "Kompaniya Umumiy Hisoboti",
                subtitle = "Barcha obyektlar yig'ma tahlili",
                onClick = onNavigateToGlobalReports
            )

            HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp))

            // 3. Ma'lumotlar va Zaxira
            DrawerSectionTitle("MA'LUMOTLAR VA ZAXIRA")

            DrawerMenuItem(
                icon = Icons.Default.SaveAlt,
                title = "Baza Zaxirasini Saqlash",
                subtitle = "Eksport va Telegramga yuborish",
                onClick = onExportDatabase
            )

            DrawerMenuItem(
                icon = Icons.Default.FileUpload,
                title = "Baza Nusxasidan Tiklash",
                subtitle = "Zaxira faylini import qilish",
                onClick = onImportDatabase
            )

            DrawerMenuItem(
                icon = Icons.Default.Refresh,
                title = "Ma'lumotlarni Yangilash",
                subtitle = "Barcha holatlarni qayta o'qish",
                onClick = onRefresh
            )

            HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp))

            // 4. Sozlamalar
            DrawerSectionTitle("TIZIM")

            DrawerMenuItem(
                icon = Icons.Default.Settings,
                title = "Sozlamalar va Xavfsizlik",
                subtitle = "App Lock, kesh va ma'lumotlar",
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.weight(1f))

            // 5. Footer (Dinamik Versiya)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$companyName v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = TextSecondary,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DeepBlueLight.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = DeepBluePrimary, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
