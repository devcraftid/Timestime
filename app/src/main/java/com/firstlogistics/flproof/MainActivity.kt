package com.firstlogistics.flproof

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.firstlogistics.flproof.ui.navigation.AppNavigation
import com.firstlogistics.flproof.ui.theme.FLProofTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FLProofTheme {
                AppNavigation()
            }
        }
    }
}
