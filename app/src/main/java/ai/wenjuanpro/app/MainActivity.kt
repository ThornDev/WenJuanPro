package ai.wenjuanpro.app

import ai.wenjuanpro.app.ui.WenJuanProNavHost
import ai.wenjuanpro.app.ui.theme.WenJuanProTheme
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FLAG_SECURE: blocks system screenshots, returns black frames to
        // screen recorders, and hides the window from the recent-apps
        // thumbnail. Debug builds keep it off so researchers can grab
        // screenshots while iterating.
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        setContent {
            WenJuanProTheme {
                WenJuanProNavHost()
            }
        }
    }
}
