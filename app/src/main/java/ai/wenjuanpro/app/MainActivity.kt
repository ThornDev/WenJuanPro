package ai.wenjuanpro.app

import ai.wenjuanpro.app.ui.WenJuanProNavHost
import ai.wenjuanpro.app.ui.theme.WenJuanProTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WenJuanProTheme {
                WenJuanProNavHost()
            }
        }
    }
}
