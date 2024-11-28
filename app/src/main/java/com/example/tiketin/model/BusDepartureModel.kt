package com.example.tiketin.model

data class BusDepartureModel(
    val busDepartures: List<BusDeparture>
)

data class BusDeparture(
    val checkpoint_end: Checkpoint,
    val checkpoint_start: Checkpoint,
    val created_at: String,
    val description: String,
    val id: Int,
    val multiplier: Double,
    val name: String,
    val updated_at: String
)