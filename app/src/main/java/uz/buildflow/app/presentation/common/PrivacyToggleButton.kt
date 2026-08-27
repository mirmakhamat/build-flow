package uz.buildflow.app.presentation.common

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
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

/**
 * Context dan FragmentActivity ni topish uchun yordamchi funksiya.
 */
fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
