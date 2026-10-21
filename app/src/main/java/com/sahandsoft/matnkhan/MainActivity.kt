package com.sahandsoft.matnkhan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahandsoft.matnkhan.data.ThemeMode
import com.sahandsoft.matnkhan.ui.MainScreen
import com.sahandsoft.matnkhan.ui.theme.MatnKhanTheme
import com.sahandsoft.matnkhan.vm.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // The whole app is designed right-to-left for Persian users.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MatnKhanTheme(darkTheme = darkTheme) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}
