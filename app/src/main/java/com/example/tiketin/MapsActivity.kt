package com.example.tiketin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tiketin.api.ApiRetrofit
import com.example.tiketin.model.Checkpoint
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

class MapsActivity : ComponentActivity() {
    private val api by lazy { ApiRetrofit().apiEndPoint }
    private val selectedCheckpoints = mutableListOf<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val checkpointStart = intent.getIntExtra("checkpoint_start", 0)
        val checkpointEnd = intent.getIntExtra("checkpoint_end", 0)
        setContent {
            val checkpoints = remember { mutableStateListOf<Checkpoint>() }
            val coroutineScope = rememberCoroutineScope()

            // Fetch checkpoints
            LaunchedEffect(Unit) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val response = api.getCheckpoint(checkpointStart,  checkpointEnd).awaitResponse()
                        if (response.isSuccessful) {
                            response.body()?.checkpoints?.let {
                                checkpoints.addAll(it)
                            }
                        } else {
                            Log.e("MapsActivity", "Failed to fetch checkpoints")
                        }
                    } catch (e: Exception) {
                        Log.e("MapsActivity", "Error: ${e.message}")
                    }
                }
            }

            MapsScreen(
                checkpoints = checkpoints,
                onCheckpointSelected = { checkpoint ->
                    returnSelectedCheckpoints(checkpoint)
                }
            )
        }
    }

    private fun returnSelectedCheckpoints(value: Checkpoint) {
        val resultIntent = Intent().putExtra("checkpoint", value)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    @Composable
    fun MapsScreen(
        checkpoints: List<Checkpoint>,
        onCheckpointSelected: (Checkpoint) -> Unit
    ) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(-6.200000, 106.816666), // Jakarta
                10f
            )
        }

        var selectedCheckpoint by remember { mutableStateOf<Checkpoint?>(null) }
        var showBottomSheet by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(title = "Select Checkpoints", onBackClick = { finish() })

            Box(modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    // Render markers for all checkpoints
                    checkpoints.forEach { checkpoint ->
                        Marker(
                            state = MarkerState(position = LatLng(checkpoint.latitude, checkpoint.longitude)),
                            title = checkpoint.name,
                            snippet = checkpoint.description,
                            onClick = {
                                selectedCheckpoint = checkpoint
                                showBottomSheet = true
                                true
                            }
                        )
                    }
                }

                if (selectedCheckpoint != null) {
                    CheckpointDetailBottomSheet(
                        checkpoint = selectedCheckpoint!!,
                        isVisible = showBottomSheet,
                        onConfirm = {
                            onCheckpointSelected(selectedCheckpoint!!)
                            showBottomSheet = false
                        },
                        onDismiss = { showBottomSheet = false }
                    )
                }
            }


        }
    }
}


@Composable
fun CheckpointDetailBottomSheet(
    checkpoint: Checkpoint,
    isVisible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it } // Start at the bottom of the screen
        ),
        exit = slideOutVertically(
            targetOffsetY = { it } // Exit to the bottom of the screen
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 0.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = checkpoint.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Description: ${checkpoint.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Latitude: ${checkpoint.latitude}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Longitude: ${checkpoint.longitude}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Confirm", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    onBackClick: () -> Unit = {},
    onHelpClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onHelpClick) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Help",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black
        )
    )
}


