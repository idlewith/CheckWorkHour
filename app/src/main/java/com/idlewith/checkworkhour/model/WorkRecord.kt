package com.idlewith.checkworkhour.model

import java.util.UUID

/**
 * Daily Attendance Work Record Data Model
 */
data class WorkRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // "YYYY-MM-DD" e.g., "2026-03-25"
    val startTime: String, // "HH:mm:ss" e.g., "09:00:00"
    val endTime: String, // "HH:mm:ss" e.g., "18:30:00"
    val startTimestamp: Long,
    val endTimestamp: Long,
    val netWorkHours: Double,
    val grossHours: Double,
    val breakHoursDeducted: Double,
    val note: String = ""
)

/**
 * Monthly Statistics Summary Data Model
 */
data class MonthlyStats(
    val yearMonth: String, // "YYYY-MM"
    val totalDaysWorked: Int,
    val totalNetWorkHours: Double,
    val averageDailyHours: Double,
    val avgStartTime: String,
    val avgEndTime: String
)
