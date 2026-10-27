package com.qrpro.scanner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.qrpro.scanner.R
import com.qrpro.scanner.data.AppState
import com.qrpro.scanner.data.Scan
import com.qrpro.scanner.data.ScanParser
import com.qrpro.scanner.util.Feedback

@Composable
fun MainScreen(state: AppState) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var current by remember { mutableStateOf<Scan?>(null) }
    var lastCode by remember { mutableStateOf("") }
    var lastTime by remember { mutableLongStateOf(0L) }

    fun handleCode(raw: String, format: String) {
        val now = System.currentTimeMillis()
        if (current != null) return
        // Ignore the same code re-detected right after the sheet was closed.
        if (raw == lastCode && now - lastTime < 2500) return
        Feedback.scanSuccess(context, state.vibrate, state.beep)
        state.addHistory(raw, format)
        current = Scan(ScanParser.parse(raw), format, now)
    }

    val tabs = listOf(
        Pair(R.string.tab_scan, Icons.Outlined.QrCodeScanner),
        Pair(R.string.tab_history, Icons.Outlined.History),
        Pair(R.string.tab_settings, Icons.Outlined.Settings),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.second, contentDescription = null) },
                        label = { Text(stringResource(item.first)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                0 -> ScannerScreen(
                    paused = current != null,
                    bottomPadding = padding.calculateBottomPadding(),
                    onCode = { raw, format -> handleCode(raw, format) },
                )

                1 -> HistoryScreen(
                    state = state,
                    modifier = Modifier.padding(padding),
                    onOpen = { item ->
                        current = Scan(ScanParser.parse(item.raw), item.format, item.time)
                    },
                )

                else -> SettingsScreen(state = state, modifier = Modifier.padding(padding))
            }
        }
    }

    current?.let { scan ->
        ResultSheet(
            scan = scan,
            onDismiss = {
                lastCode = scan.parsed.raw
                lastTime = System.currentTimeMillis()
                current = null
            },
        )
    }
}
