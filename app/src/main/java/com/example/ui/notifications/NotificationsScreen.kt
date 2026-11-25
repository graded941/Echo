package com.example.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun NotificationsScreen() {
    val alarms by EchoRepository.alarms.collectAsState()
    var whatsappFilter by remember { mutableStateOf(true) }
    var callsFilter by remember { mutableStateOf(true) }
    var dndActive by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "NOTIFICATIONS & ALARMS",
                fontSize = 12.sp,
                color = EchoOrange,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Smart Wake & Filtering",
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("NOTIFICATION LISTENER SERVICE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("WhatsApp Rich Parser & Quick Reply", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = whatsappFilter,
                            onCheckedChange = { whatsappFilter = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EchoOrange)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Incoming Calls & Contact Caller ID", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = callsFilter,
                            onCheckedChange = { callsFilter = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EchoOrange)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Do Not Disturb (DND) Sync", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = dndActive,
                            onCheckedChange = { dndActive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EchoOrange)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SMART WAKE ALARMS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { /* Add Alarm */ }) {
                    Text("Add Alarm", color = EchoOrange)
                }
            }
        }

        items(alarms) { alarm ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EchoSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(alarm.time, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(alarm.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (alarm.smartWake) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Badge(containerColor = EchoCyan) { Text("Smart Wake ±30min") }
                        }
                    }
                    Switch(
                        checked = alarm.enabled,
                        onCheckedChange = { EchoRepository.toggleAlarm(alarm.id) },
                        colors = SwitchDefaults.colors(checkedThumbColor = EchoOrange)
                    )
                }
            }
        }
    }
}
