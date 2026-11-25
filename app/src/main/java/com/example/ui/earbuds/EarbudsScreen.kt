package com.example.ui.earbuds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EchoRepository
import com.example.ui.theme.EchoCyan
import com.example.ui.theme.EchoOrange
import com.example.ui.theme.EchoSurface
import com.example.ui.theme.EchoSurfaceVariant

@Composable
fun EarbudsScreen() {
    val buds by EchoRepository.budsState.collectAsState()
    var selectedEq by remember { mutableStateOf(buds.eqMode) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "TWS COMPANION",
                fontSize = 12.sp,
                color = EchoOrange,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "itel Buds Neo 3",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EchoSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Headphones, contentDescription = null, tint = EchoCyan)
                            Text(buds.deviceName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Badge(containerColor = EchoCyan) { Text("A2DP Connected") }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        EarbudBatteryWidget("Left Bud", "${buds.leftBattery}%")
                        EarbudBatteryWidget("Right Bud", "${buds.rightBattery}%")
                        EarbudBatteryWidget("Charging Case", "${buds.caseBattery}%")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EchoSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("AUDIO EQUALIZER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val eqModes = listOf("Balanced Bass", "Vocal Boost", "Podcast / Clear", "Dynamic Gaming")
                    eqModes.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mode, color = MaterialTheme.colorScheme.onSurface)
                            RadioButton(
                                selected = selectedEq == mode,
                                onClick = { selectedEq = mode },
                                colors = RadioButtonDefaults.colors(selectedColor = EchoOrange)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EarbudBatteryWidget(title: String, level: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = EchoSurfaceVariant,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(level, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EchoOrange)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
