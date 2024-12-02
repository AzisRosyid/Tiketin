package com.example.tiketin.model


data class BusScheduleModel(
    val busSchedules: List<BusSchedule>
)

data class BusSchedule(
    val bus: Bus,
    val bus_departure: BusDeparture,
    val created_at: String,
    val day: Int,
    val description: String,
    val id: Int,
    val time: String,
    val updated_at: String
)