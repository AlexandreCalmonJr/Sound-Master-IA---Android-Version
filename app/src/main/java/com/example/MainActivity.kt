package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.SoundMasterDatabase
import com.example.data.SoundMasterRepository
import com.example.ui.SoundMasterUi
import com.example.ui.SoundMasterViewModel
import com.example.ui.SoundMasterViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Database & Repository
    val database = SoundMasterDatabase.getDatabase(applicationContext)
    val repository = SoundMasterRepository(database.recordingDao(), applicationContext)
    
    // Create ViewModel using factory
    val factory = SoundMasterViewModelFactory(application, repository)
    val viewModel = ViewModelProvider(this, factory)[SoundMasterViewModel::class.java]

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        SoundMasterUi(viewModel = viewModel)
      }
    }
  }
}
