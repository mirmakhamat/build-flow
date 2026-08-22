package uz.buildflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import uz.buildflow.app.core.theme.BuildFlowTheme
import uz.buildflow.app.presentation.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as BuildFlowApp).container

        setContent {
            BuildFlowTheme {
                AppNavigation(container = appContainer)
            }
        }
    }
}
