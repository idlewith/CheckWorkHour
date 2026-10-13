package com.idlewith.checkworkhour.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object WorkHourCalculator {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun parseDateTime(dateStr: String, timeStr: String): Long? {
        return try {
            val fullStr = "${dateStr.trim()} ${timeStr.trim()}"
            dateTimeFormat.parse(fullStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates work hour details given start and end timestamps.
     * Excludes:
     * - Lunch break: 12:00:00 to 13:30:00 (1.5h)
     * - Dinner break: 18:30:00 to 19:00:00 (0.5h)
     */
    fun calculateWorkHours(startMs: Long, endMs: Long): Triple<Double, Double, Double> {
        if (endMs <= startMs) {
            return Triple(0.0, 0.0, 0.0)
        }

        val grossSeconds = (endMs - startMs) / 1000.0
        val grossHours = grossSeconds / 3600.0

        var breakDeductedSeconds = 0L

        val cal = Calendar.getInstance()
        cal.timeInMillis = startMs

        val endCal = Calendar.getInstance()
        endCal.timeInMillis = endMs

        // Move day by day from start date to end date
        val currDayCal = Calendar.getInstance()
        currDayCal.timeInMillis = startMs
        currDayCal.set(Calendar.HOUR_OF_DAY, 0)
        currDayCal.set(Calendar.MINUTE, 0)
        currDayCal.set(Calendar.SECOND, 0)
        currDayCal.set(Calendar.MILLISECOND, 0)

        val lastDayCal = Calendar.getInstance()
        lastDayCal.timeInMillis = endMs
        lastDayCal.set(Calendar.HOUR_OF_DAY, 0)
        lastDayCal.set(Calendar.MINUTE, 0)
        lastDayCal.set(Calendar.SECOND, 0)
        lastDayCal.set(Calendar.MILLISECOND, 0)

        while (!currDayCal.after(lastDayCal)) {
            // Lunch interval: 12:00:00 to 13:30:00
            val lunchStart = currDayCal.clone() as Calendar
            lunchStart.set(Calendar.HOUR_OF_DAY, 12)
            lunchStart.set(Calendar.MINUTE, 0)
            lunchStart.set(Calendar.SECOND, 0)

            val lunchEnd = currDayCal.clone() as Calendar
            lunchEnd.set(Calendar.HOUR_OF_DAY, 13)
            lunchEnd.set(Calendar.MINUTE, 30)
            lunchEnd.set(Calendar.SECOND, 0)

            breakDeductedSeconds += calculateOverlapSeconds(
                startMs, endMs,
                lunchStart.timeInMillis, lunchEnd.timeInMillis
            )

            // Dinner interval: 18:30:00 to 19:00:00
            val dinnerStart = currDayCal.clone() as Calendar
            dinnerStart.set(Calendar.HOUR_OF_DAY, 18)
            dinnerStart.set(Calendar.MINUTE, 30)
            dinnerStart.set(Calendar.SECOND, 0)

            val dinnerEnd = currDayCal.clone() as Calendar
            dinnerEnd.set(Calendar.HOUR_OF_DAY, 19)
            dinnerEnd.set(Calendar.MINUTE, 0)
            dinnerEnd.set(Calendar.SECOND, 0)

            breakDeductedSeconds += calculateOverlapSeconds(
                startMs, endMs,
                dinnerStart.timeInMillis, dinnerEnd.timeInMillis
            )

            // Move to next day
            currDayCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val breakDeductedHours = breakDeductedSeconds / 3600.0
        val netHours = Math.max(0.0, grossHours - breakDeductedHours)

        return Triple(
            round2(netHours),
            round2(grossHours),
            round2(breakDeductedHours)
        )
    }

    private fun calculateOverlapSeconds(
        start1: Long, end1: Long,
        start2: Long, end2: Long
    ): Long {
        val overlapStart = Math.max(start1, start2)
        val overlapEnd = Math.min(end1, end2)
        return if (overlapEnd > overlapStart) {
            (overlapEnd - overlapStart) / 1000
        } else {
            0L
        }
    }

    private fun round2(value: Double): Double {
        return Math.round(value * 100.0) / 100.0
    }

    /**
     * Convert milliseconds time-of-day into HH:mm:ss format
     */
    fun formatTime(timeMs: Long): String {
        return timeFormat.format(Date(timeMs))
    }
}
