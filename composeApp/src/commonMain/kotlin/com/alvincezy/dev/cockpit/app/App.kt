package com.alvincezy.dev.cockpit.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvincezy.dev.cockpit.adbops.AdbOperationsArea
import com.alvincezy.dev.cockpit.app.theme.AppTheme

@Composable
fun App() {
    AppScaffold { modifier ->
        AdbOperationsArea(modifier = modifier)
    }
}

/**
 * App shell with the ADB operations area provided as a slot, so the surrounding
 * layout can be previewed/tested without depending on Koin injection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    adbArea: @Composable (Modifier) -> Unit,
) {
    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Cockpit") },
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Cockpit Desktop Toolbox",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "调试 · 演示 · 操作",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )

                adbArea(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                )
            }
        }
    }
}
