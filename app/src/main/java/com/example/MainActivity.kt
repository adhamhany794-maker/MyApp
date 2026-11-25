package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.repositories.SmartCompanionRepository
import com.example.ui.SmartCompanionApp
import com.example.viewmodels.SmartCompanionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val prefs = PreferencesManager(applicationContext)
        val repository = SmartCompanionRepository(db, prefs)
        val viewModel = ViewModelProvider(
            this,
            SmartCompanionViewModel.Factory(repository)
        )[SmartCompanionViewModel::class.java]

        setContent {
            SmartCompanionApp(viewModel = viewModel)
        }
    }
}
