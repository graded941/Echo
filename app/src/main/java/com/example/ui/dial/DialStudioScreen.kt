package com.example.ui.dial

import android.util.Log
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import com.example.ui.theme.EchoCyan
import com.example.ui.theme.EchoOrange
import com.example.ui.theme.EchoSurface
import com.example.ui.theme.EchoSurfaceVariant

sealed interface AiDialState {
    data object Idle : AiDialState
    data object Loading : AiDialState
    data class Success(val description: String) : AiDialState
    data class Error(val message: String) : AiDialState
}

class DialStudioViewModel : ViewModel() {
    private val model by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val _uiState = MutableStateFlow<AiDialState>(AiDialState.Idle)
    val uiState: StateFlow<AiDialState> = _uiState.asStateFlow()

    fun generateAiDialConcept(promptStyle: String) {
        viewModelScope.launch {
            _uiState.value = AiDialState.Loading
            try {
                val response = withTimeout(60_000L) {
                    model.generateContent("Create a detailed smartwatch watchface aesthetic and color palette concept for an ISW-O23 rectangular AMOLED screen based on this style: $promptStyle. Return brief design instructions.")
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    _uiState.value = AiDialState.Success(text)
                } else {
                    _uiState.value = AiDialState.Error("Model returned empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.value = AiDialState.Error("Generation timed out after 60 seconds.")
            } catch (e: Exception) {
                Log.e("DialStudioViewModel", "AI Dial generation failed", e)
                _uiState.value = AiDialState.Error(e.localizedMessage ?: "Failed to generate AI dial.")
            }
        }
    }
}

@Composable
fun DialStudioScreen(viewModel: DialStudioViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var styleInput by remember { mutableStateOf("Cyberpunk Neon AMOLED with HR & Steps rings") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "DIAL STUDIO & GEMINI AI",
                fontSize = 12.sp,
                color = EchoOrange,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Watch Face Customisation",
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SLOT 7: CUSTOM PHOTO DIAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Badge(containerColor = EchoCyan) { Text("Ready") }
                    }
                    Text("ISW-O23 390×450px Rectangular AMOLED", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Supports centre complication clusters, custom photos, and Gemini AI background generation.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("GEMINI AI DIAL GENERATOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EchoOrange)
                    
                    OutlinedTextField(
                        value = styleInput,
                        onValueChange = { styleInput = it },
                        label = { Text("Describe Dial Style") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { viewModel.generateAiDialConcept(styleInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = EchoOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate AI Dial Concept")
                    }

                    when (val state = uiState) {
                        is AiDialState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = EchoOrange)
                            }
                        }
                        is AiDialState.Success -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EchoSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Generated Concept:", fontWeight = FontWeight.Bold, color = EchoCyan)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(state.description, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                }
                            }
                        }
                        is AiDialState.Error -> {
                            Text(state.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
