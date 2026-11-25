package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WatchState(
    val connected: Boolean = true,
    val deviceName: String = "itel ISW-O23 Storm",
    val batteryPercent: Int = 84,
    val firmwareVersion: String = "v1.2.4_ota"
)

data class BudsState(
    val connected: Boolean = true,
    val deviceName: String = "itel Buds Neo 3",
    val leftBattery: Int = 90,
    val rightBattery: Int = 85,
    val caseBattery: Int = 75,
    val eqMode: String = "Balanced Bass"
)

data class FitnessData(
    val steps: Int = 8420,
    val stepGoal: Int = 10000,
    val calories: Int = 540,
    val activeMinutes: Int = 42,
    val bodyBattery: Int = 78,
    val heartRate: Int = 72,
    val restingHr: Int = 58,
    val spo2: Int = 98,
    val hrvRmssd: Double = 64.5,
    val stressScore: Int = 24,
    val sleepHours: Double = 7.5,
    val sleepQuality: Int = 88
)

data class WorkoutSession(
    val id: Long,
    val sportType: String,
    val durationSec: Int,
    val distanceKm: Double,
    val avgHr: Int,
    val date: String,
    val syncedToStrava: Boolean
)

data class AlarmItem(
    val id: Int,
    val time: String,
    val enabled: Boolean,
    val smartWake: Boolean,
    val label: String
)

object EchoRepository {
    private val _watchState = MutableStateFlow(WatchState())
    val watchState: StateFlow<WatchState> = _watchState.asStateFlow()

    private val _budsState = MutableStateFlow(BudsState())
    val budsState: StateFlow<BudsState> = _budsState.asStateFlow()

    private val _fitnessData = MutableStateFlow(FitnessData())
    val fitnessData: StateFlow<FitnessData> = _fitnessData.asStateFlow()

    private val _workouts = MutableStateFlow(
        listOf(
            WorkoutSession(1, "Outdoor Run", 1820, 5.24, 152, "Today, 07:30 AM", true),
            WorkoutSession(2, "Cycling", 3600, 18.5, 138, "Yesterday, 05:15 PM", false),
            WorkoutSession(3, "Outdoor Walk", 2400, 3.1, 115, "Nov 23, 06:00 PM", true)
        )
    )
    val workouts: StateFlow<List<WorkoutSession>> = _workouts.asStateFlow()

    private val _alarms = MutableStateFlow(
        listOf(
            AlarmItem(1, "07:00 AM", true, true, "Morning Wake-up"),
            AlarmItem(2, "08:30 AM", false, false, "Meeting Reminder")
        )
    )
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    fun toggleAlarm(id: Int) {
        _alarms.value = _alarms.value.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
    }

    fun syncStrava(id: Long) {
        _workouts.value = _workouts.value.map { if (it.id == id) it.copy(syncedToStrava = true) else it }
    }
}
