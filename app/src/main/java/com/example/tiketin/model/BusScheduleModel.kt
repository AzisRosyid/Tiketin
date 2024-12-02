package com.example.tiketin.model

import java.io.Serializable


data class BusScheduleModel(
    val busSchedules: List<BusSchedule>,
    val busSchedule: BusSchedule,
): Serializable

data class BusSchedule(
    val bus: Bus,
    val bus_departure: BusDeparture,
    val created_at: String,
    val day: Int,
    val description: String,
    val id: Int,
    val time: String,
    val updated_at: String
): Serializable