package com.example.tiketin.model

import java.io.Serializable

data class CheckpointModel(
    val checkpoints: List<Checkpoint>,
): Serializable

data class Checkpoint(
    val created_at: String,
    val description: String,
    val id: Int,
    val image: Any,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val updated_at: String
): Serializable
