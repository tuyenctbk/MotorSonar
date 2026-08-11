package com.soloprono.motorsonar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soloprono.motorsonar.data.AppDatabase
import com.soloprono.motorsonar.data.EngineScanRepository
import com.soloprono.motorsonar.ui.DashboardScreen
import com.soloprono.motorsonar.ui.EngineSoundViewModel
import com.soloprono.motorsonar.ui.EngineSoundViewModelFactory
import com.soloprono.motorsonar.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Instantiate Room Database and Repository
        val database = AppDatabase.getDatabase(this)
        val repository = EngineScanRepository(
            database.engineScanDao(),
            database.maintenanceActivityDao(),
            database.diagnosticRecordDao()
        )
        
        setContent {
            val viewModel: EngineSoundViewModel = viewModel(
                factory = EngineSoundViewModelFactory(repository)
            )
            
            LaunchedEffect(Unit) {
                viewModel.loadThemeMode(this@MainActivity)
            }

            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
