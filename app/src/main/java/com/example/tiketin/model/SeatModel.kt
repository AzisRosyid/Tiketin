package com.example.tiketin.model

data class SeatModel(
    val seats: List<Seat>
)

data class Seat(
    val id: Int,
    val status: String
)
