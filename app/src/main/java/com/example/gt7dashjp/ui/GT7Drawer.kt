package com.example.gt7dashjp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gt7dashjp.viewmodel.StatusType
import com.example.gt7dashjp.viewmodel.TelemetryUiState

@Composable
fun GT7Drawer(
    uiState: TelemetryUiState,
    dark: Boolean,
    onIpChanged: (String) -> Unit,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit
) {
    val drawerBg   = if (dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val textColor  = if (dark) Color.White       else Color.Black
    val borderColor = if (dark) Color(0x26FFFFFF) else Color(0x1F000000)
    val inputBg    = if (dark) Color(0xFF2C2C2E) else Color.White
    val subText    = if (dark) Color(0x80FFFFFF) else Color(0x73000000)
    val accentGreen = Color(0xFF4CAF50)
    val stopRed     = Color(0xFFFF3B30)

    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
        drawerContainerColor = drawerBg,
        modifier = Modifier.width(300.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 20.dp, end = 20.dp, bottom = 18.dp)
        ) {
            Text(
                text = "GT7 Dashboard Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = (-0.5).sp
            )
        }
        HorizontalDivider(color = borderColor, thickness = 1.dp)

        // Body
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // IP Input
            OutlinedTextField(
                value = uiState.ipFieldText,
                onValueChange = { input ->
                    onIpChanged(input.filter { it.isDigit() || it == '.' })
                },
                label = { Text("PS5 IP Address", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 17.sp
                ),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = borderColor,
                    focusedLabelColor = subText,
                    unfocusedLabelColor = subText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Start / Stop Button
            Button(
                onClick = if (uiState.isListening) onStopClicked else onStartClicked,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isListening) stopRed else accentGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (uiState.isListening) "Stop Receiving" else "Start Receiving",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp
                )
            }

            // Status Info
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Packets: ${uiState.packetCount}",
                    fontSize = 14.sp,
                    color = textColor
                )
                val statusColor = when (uiState.statusType) {
                    StatusType.RECEIVING  -> accentGreen
                    StatusType.CONNECTING -> Color(0xFFE65100)
                    StatusType.IDLE, StatusType.ERROR -> stopRed
                }
                Text(
                    text = uiState.status,
                    fontSize = 14.sp,
                    color = statusColor,
                    lineHeight = 21.sp
                )
            }
        }
    }
}
