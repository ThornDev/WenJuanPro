package ai.wenjuanpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ai.wenjuanpro.app.ui.WenJuanProHomeScreen
import ai.wenjuanpro.app.ui.theme.WenJuanProTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WenJuanProTheme {
                WenJuanProHomeScreen()
            }
        }
    }
}
