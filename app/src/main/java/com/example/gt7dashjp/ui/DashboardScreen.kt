package com.example.gt7dashjp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.gt7dashjp.viewmodel.TelemetryViewModel

@Composable
fun DashboardScreen(viewModel: TelemetryViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showIpError by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()

    val bg          = if (dark) Color(0xFF121212) else Color(0xFFF2F2F7)
    val cardBg      = if (dark) Color(0xFF1E1E1E) else Color.White
    val textColor   = if (dark) Color.White       else Color.Black
    val headerBg    = if (dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val borderColor = if (dark) Color(0x26FFFFFF) else Color(0x1F000000)

    if (showIpError) {
        AlertDialog(
            onDismissRequest = { showIpError = false },
            confirmButton = {
                TextButton(onClick = { showIpError = false }) { Text("OK") }
            },
            title = { Text("Invalid IP Address") },
            text = { Text("Please enter a valid IP address (e.g. 192.168.1.100)") }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            GT7Drawer(
                uiState = uiState,
                dark = dark,
                onIpChanged = viewModel::onIpChanged,
                onStartClicked = {
                    if (viewModel.isValidIp(uiState.ipFieldText)) {
                        viewModel.startListening()
                        scope.launch { drawerState.close() }
                    } else {
                        showIpError = true
                    }
                },
                onStopClicked = {
                    viewModel.stopListening()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .safeDrawingPadding()
        ) {
            // App Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Settings",
                            tint = textColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "GT7 Dashboard",
                        fontSize = 20.sp,
                        fontWeight = FontWeight(600),
                        color = textColor,
                        letterSpacing = (-0.3).sp
                    )
                }
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // RPM Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "%,d".format(uiState.rpm.toInt()),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "RPM",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            letterSpacing = (-1).sp
                        )
                    }
                }
            }
        }
    }
}
