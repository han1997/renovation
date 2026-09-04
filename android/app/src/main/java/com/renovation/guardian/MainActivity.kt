package com.renovation.guardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.renovation.guardian.ui.nav.AppRoot
import com.renovation.guardian.ui.theme.RenovationTheme

/**
 * Activity 入口。负责：
 * - `enableEdgeToEdge()`（targetSdk 35 默认要求 edge-to-edge）；
 * - 在 Compose 树根注入 [RenovationTheme]（Material 3 + 动态取色）；
 * - 装载 [AppRoot]（NavHost + 5 Tab 底栏）。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RenovationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRoot()
                }
            }
        }
    }
}