package com.example.tiketin.model

import java.io.Serializable

data class Bus(
    val capacity: Int,
    val `class`: String,
    val created_at: String,
    val description: String,
    val id: Int,
    val name: String,
    val price: Int,
    val updated_at: String
): Serializable