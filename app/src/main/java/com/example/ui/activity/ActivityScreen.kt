package com.example.ui.activity

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
import com.example.data.WorkoutSession
import com.example.ui.theme.EchoCyan
import com.example.ui.theme.EchoOrange
import com.example.ui.theme.EchoSurface
import com.example.ui.theme.EchoSurfaceVariant

@Composable
fun ActivityScreen() {
    val workouts by EchoRepository.workouts.collectAsState()
    var syncingId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STRAVA & GPS TRACKING",
                        fontSize = 12.sp,
                        color = EchoOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Activity History",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Button(
                    onClick = { /* Start Outdoor Sport */ },
                    colors = ButtonDefaults.buttonColors(containerColor = EchoOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Workout")
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
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = EchoOrange, modifier = Modifier.size(32.dp))
                    Column {
                        Text("Strava API v3 Connected", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("GPX tracks and split analysis automatically uploaded.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Text("RECENT SESSIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(workouts) { workout ->
            WorkoutCard(
                workout = workout,
                onSyncStrava = {
                    syncingId = workout.id
                    EchoRepository.syncStrava(workout.id)
                }
            )
        }
    }
}

@Composable
fun WorkoutCard(workout: WorkoutSession, onSyncStrava: () -> Unit) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (workout.sportType) {
                            "Outdoor Run" -> Icons.Default.DirectionsRun
                            "Cycling" -> Icons.Default.DirectionsBike
                            else -> Icons.Default.DirectionsWalk
                        },
                        contentDescription = null,
                        tint = EchoOrange
                    )
                    Text(workout.sportType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(workout.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Distance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${workout.distanceKm} km", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("Duration", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${workout.durationSec / 60} min", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("Avg HR", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${workout.avgHr} bpm", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            HorizontalDivider(color = EchoSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (workout.syncedToStrava) {
                    Badge(containerColor = EchoCyan) { Text("Synced to Strava") }
                } else {
                    OutlinedButton(
                        onClick = onSyncStrava,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Upload to Strava", color = EchoOrange, fontSize = 12.sp)
                    }
                }
                TextButton(onClick = { /* View GPX Map */ }) {
                    Text("View Route", color = EchoCyan, fontSize = 12.sp)
                }
            }
        }
    }
}
