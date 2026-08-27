package uz.buildflow.app.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import uz.buildflow.app.core.preferences.LocalPrivacyMode
import uz.buildflow.app.core.theme.DeepBluePrimary

@Composable
fun PrivacyToggleButton(
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPrivacyMode = LocalPrivacyMode.current
    IconButton(
        onClick = onToggle,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (isPrivacyMode) "Summalarni ko'rsatish" else "Summalarni berkitish",
            tint = DeepBluePrimary
        )
    }
}
