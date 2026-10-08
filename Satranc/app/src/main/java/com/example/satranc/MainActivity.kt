package com.example.satranc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.satranc.ui.SalonApp
import com.example.satranc.ui.SalonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdManager.initialize(this)
        setContent {
            SalonTheme {
                val model: GameModel = viewModel()
                SalonApp(model)
            }
        }
    }
}
