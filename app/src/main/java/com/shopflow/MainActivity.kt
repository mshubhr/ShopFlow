package com.shopflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shopflow.ui.ShopFlowApp
import com.shopflow.ui.theme.ShopFlowTheme
import com.shopflow.viewmodel.ShopViewModel
import com.shopflow.viewmodel.ShopViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val application = application as ShopFlowApplication
            val factory = remember { ShopViewModelFactory(application.repository) }
            val viewModel: ShopViewModel = viewModel(factory = factory)
            ShopFlowTheme { ShopFlowApp(viewModel) }
        }
    }
}
