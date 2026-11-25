package com.example.ui.health

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EchoRepository
import com.example.ui.theme.EchoCyan
import com.example.ui.theme.EchoOrange
import com.example.ui.theme.EchoSurface
import com.example.ui.theme.EchoSurfaceVariant

@Composable
fun HealthScreen() {
    val fitness by EchoRepository.fitnessData.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "BIOMETRIC HEALTH",
                fontSize = 12.sp,
                color = EchoOrange,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ISW-O23 Sensors & Health Connect",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HealthMetricCard(
                    title = "Heart Rate",
                    value = "${fitness.heartRate}",
                    unit = "BPM",
                    subtext = "Resting: ${fitness.restingHr} BPM",
                    icon = Icons.Default.Favorite,
                    modifier = Modifier.weight(1f)
                )
                HealthMetricCard(
                    title = "Blood Oxygen",
                    value = "${fitness.spo2}%",
                    unit = "SpO₂",
                    subtext = "Optimal range",
                    icon = Icons.Default.Air,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HealthMetricCard(
                    title = "Heart Rate Var.",
                    value = "${fitness.hrvRmssd}",
                    unit = "ms",
                    subtext = "RMSSD Recovery",
                    icon = Icons.Default.ShowChart,
                    modifier = Modifier.weight(1f)
                )
                HealthMetricCard(
                    title = "Stress Level",
                    value = "${fitness.stressScore}",
                    unit = "/100",
                    subtext = "Low stress",
                    icon = Icons.Default.Psychology,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EchoSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SLEEP STAGING & RECOVERY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Badge(containerColor = EchoCyan) { Text("${fitness.sleepQuality}% Score") }
                    }
                    Text("${fitness.sleepHours} Hours Total Sleep", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Deep sleep: 2.5h • REM: 1.8h • Light sleep: 3.2h", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    LinearProgressIndicator(
                        progress = { 0.88f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = EchoCyan,
                        trackColor = EchoSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EchoSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = EchoOrange, modifier = Modifier.size(32.dp))
                    Column {
                        Text("Health Connect Synchronization", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("All metrics automatically synced to Android 14 Health Connect.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun HealthMetricCard(title: String, value: String, unit: String, subtext: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EchoSurface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = EchoOrange, modifier = Modifier.size(18.dp))
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(unit, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(subtext, fontSize = 12.sp, color = EchoCyan)
        }
    }
}
