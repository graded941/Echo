package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun DashboardScreen(
    onNavigateToAiDial: () -> Unit,
    onNavigateToEarbuds: () -> Unit,
    onNavigateToActivity: () -> Unit
) {
    val watch by EchoRepository.watchState.collectAsState()
    val buds by EchoRepository.budsState.collectAsState()
    val fitness by EchoRepository.fitnessData.collectAsState()

    var quickActionResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ECHO COMPANION",
                        fontSize = 12.sp,
                        color = EchoOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "itel Ecosystem",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = EchoSurfaceVariant,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Watch, contentDescription = "Device Status", tint = EchoCyan)
                    }
                }
            }
        }

        item {
            Text("CONNECTED HARDWARE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            
            // 3 Quick Status Cards for ISW-O23 Watch, P55 Phone, and Buds Neo 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ISW-O23 Watch Card
                DeviceStatusCard(
                    title = "ISW-O23 Watch",
                    subtitle = "${watch.batteryPercent}% • BLE 5.0",
                    status = "Connected",
                    icon = Icons.Default.Watch,
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
                // P55 Phone Card
                DeviceStatusCard(
                    title = "itel P55 Phone",
                    subtitle = "92% • Transsion 14",
                    status = "Host Hub",
                    icon = Icons.Default.PhoneAndroid,
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
                // Buds Neo 3 Card
                DeviceStatusCard(
                    title = "Buds Neo 3",
                    subtitle = "L:${buds.leftBattery}% R:${buds.rightBattery}%",
                    status = "A2DP Active",
                    icon = Icons.Default.Headphones,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToEarbuds
                )
            }
        }

        item {
            // Fitness Rings Summary
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
                        Text("TODAY'S ACTIVITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onNavigateToActivity) {
                            Text("View Strava Logs", color = EchoOrange)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricRing(title = "Steps", value = "${fitness.steps}", subtitle = "/${fitness.stepGoal}", color = EchoOrange)
                        MetricRing(title = "Calories", value = "${fitness.calories}", subtitle = "kcal", color = EchoCyan)
                        MetricRing(title = "Body Battery", value = "${fitness.bodyBattery}%", subtitle = "Energy", color = Color(0xFF10B981))
                    }
                }
            }
        }

        item {
            // Quick Actions
            Text("QUICK ACTIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.AutoAwesome,
                    label = "AI Dial Studio",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAiDial
                )
                QuickActionButton(
                    icon = Icons.Default.CameraAlt,
                    label = "Camera Shutter",
                    modifier = Modifier.weight(1f),
                    onClick = { quickActionResult = "Camera shutter triggered on phone!" }
                )
                QuickActionButton(
                    icon = Icons.Default.PhoneAndroid,
                    label = "Find Phone",
                    modifier = Modifier.weight(1f),
                    onClick = { quickActionResult = "Phone ringing & flashlight active!" }
                )
            }
        }

        quickActionResult?.let { message ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EchoSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(message, color = EchoCyan, fontWeight = FontWeight.Medium)
                        IconButton(onClick = { quickActionResult = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceStatusCard(
    title: String,
    subtitle: String,
    status: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EchoSurface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = EchoOrange, modifier = Modifier.size(24.dp))
                Badge(containerColor = EchoCyan.copy(alpha = 0.2f)) {
                    Text(status, fontSize = 9.sp, color = EchoCyan, fontWeight = FontWeight.Bold)
                }
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun MetricRing(title: String, value: String, subtitle: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
                    Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EchoSurface),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = EchoOrange)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}
