package com.example.tiketin.model

import java.io.Serializable

data class BusDepartureModel(
    val busDepartures: List<BusDeparture>
): Serializable

data class BusDeparture(
    val checkpoint_end: Int,
    val checkpoint_start: Int,
    val created_at: String,
    val description: String,
    val id: Int,
    val multiplier: Double,
    val name: String,
    val updated_at: String
): Serializable