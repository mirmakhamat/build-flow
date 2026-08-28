package uz.buildflow.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.appcompat.app.AppCompatActivity
import uz.buildflow.app.core.preferences.LocalPrivacyMode
import uz.buildflow.app.core.theme.BuildFlowTheme
import uz.buildflow.app.presentation.navigation.AppNavigation

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skrinshot, ekran videoyozuvi va multitaskingda ko'rinishni bloklash
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val appContainer = (application as BuildFlowApp).container

        setContent {
            val isPrivacyMode by appContainer.userPreferences.isPrivacyMode.collectAsState()

            CompositionLocalProvider(LocalPrivacyMode provides isPrivacyMode) {
                BuildFlowTheme {
                    AppNavigation(container = appContainer)
                }
            }
        }
    }
}
