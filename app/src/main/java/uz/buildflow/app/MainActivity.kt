package uz.buildflow.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import uz.buildflow.app.core.preferences.LocalPrivacyMode
import uz.buildflow.app.core.theme.BuildFlowTheme
import uz.buildflow.app.presentation.navigation.AppNavigation

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
