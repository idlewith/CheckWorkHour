package com.idlewith.checkworkhour.repository

import android.content.Context
import android.content.SharedPreferences
import com.idlewith.checkworkhour.model.MonthlyStats
import com.idlewith.checkworkhour.model.WorkRecord
import com.idlewith.checkworkhour.util.WorkHourCalculator
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WorkRecordRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("work_hour_records_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_RECORDS = "key_work_records_json"
    }

    fun getAllRecords(): List<WorkRecord> {
        val jsonStr = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        val list = mutableListOf<WorkRecord>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    WorkRecord(
                        id = obj.optString("id"),
                        date = obj.optString("date"),
                        startTime = obj.optString("startTime"),
                        endTime = obj.optString("endTime"),
                        startTimestamp = obj.optLong("startTimestamp"),
                        endTimestamp = obj.optLong("endTimestamp"),
                        netWorkHours = obj.optDouble("netWorkHours"),
                        grossHours = obj.optDouble("grossHours"),
                        breakHoursDeducted = obj.optDouble("breakHoursDeducted"),
                        note = obj.optString("note", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedBy { it.date }
    }

    private fun saveRecords(records: List<WorkRecord>) {
        try {
            val jsonArray = JSONArray()
            for (r in records) {
                val obj = JSONObject()
                obj.put("id", r.id)
                obj.put("date", r.date)
                obj.put("startTime", r.startTime)
                obj.put("endTime", r.endTime)
                obj.put("startTimestamp", r.startTimestamp)
                obj.put("endTimestamp", r.endTimestamp)
                obj.put("netWorkHours", r.netWorkHours)
                obj.put("grossHours", r.grossHours)
                obj.put("breakHoursDeducted", r.breakHoursDeducted)
                obj.put("note", r.note)
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_RECORDS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRecordsForMonth(yearMonth: String): List<WorkRecord> {
        return getAllRecords().filter { it.date.startsWith(yearMonth) }
    }

    fun addOrUpdateRecord(record: WorkRecord) {
        val records = getAllRecords().toMutableList()
        val index = records.indexOfFirst { it.id == record.id || it.date == record.date }
        if (index >= 0) {
            records[index] = record
        } else {
            records.add(record)
        }
        saveRecords(records)
    }

    fun deleteRecord(id: String) {
        val records = getAllRecords().filter { it.id != id }
        saveRecords(records)
    }

    fun calculateMonthlyStats(yearMonth: String): MonthlyStats {
        val records = getRecordsForMonth(yearMonth)
        if (records.isEmpty()) {
            return MonthlyStats(
                yearMonth = yearMonth,
                totalDaysWorked = 0,
                totalNetWorkHours = 0.0,
                averageDailyHours = 0.0,
                avgStartTime = "--:--:--",
                avgEndTime = "--:--:--"
            )
        }

        val totalDays = records.size
        val totalHours = records.sumOf { it.netWorkHours }
        val avgHours = Math.round((totalHours / totalDays) * 100.0) / 100.0

        var sumStartSec = 0L
        var sumEndSec = 0L

        for (r in records) {
            sumStartSec += parseSecondsOfDay(r.startTime)
            sumEndSec += parseSecondsOfDay(r.endTime)
        }

        val avgStartSec = sumStartSec / totalDays
        val avgEndSec = sumEndSec / totalDays

        val avgStartStr = formatSecondsOfDay(avgStartSec)
        val avgEndStr = formatSecondsOfDay(avgEndSec)

        return MonthlyStats(
            yearMonth = yearMonth,
            totalDaysWorked = totalDays,
            totalNetWorkHours = Math.round(totalHours * 100.0) / 100.0,
            averageDailyHours = avgHours,
            avgStartTime = avgStartStr,
            avgEndTime = avgEndStr
        )
    }

    private fun parseSecondsOfDay(timeStr: String): Long {
        return try {
            val parts = timeStr.split(":").map { it.trim().toInt() }
            val h = parts[0]
            val m = parts[1]
            val s = if (parts.size > 2) parts[2] else 0
            (h * 3600 + m * 60 + s).toLong()
        } catch (_: Exception) {
            9 * 3600L
        }
    }

    private fun formatSecondsOfDay(seconds: Long): String {
        val h = (seconds / 3600) % 24
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }

    fun generateSampleDataForMonth(yearMonth: String) {
        val records = getAllRecords().toMutableList()
        records.removeAll { it.date.startsWith(yearMonth) }

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        try {
            val date = sdf.parse(yearMonth)
            if (date != null) {
                cal.time = date
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1

        for (d in 1..maxDays) {
            val dayCal = Calendar.getInstance()
            dayCal.set(year, month - 1, d)
            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)

            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                continue
            }

            val dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, d)
            val startMin = 50 + (d % 15)
            val startH = 8 + (startMin / 60)
            val startM = startMin % 60
            val startStr = String.format(Locale.getDefault(), "%02d:%02d:15", startH, startM)

            val endMin = 30 + ((d * 7) % 45)
            val endH = 18 + (endMin / 60)
            val endM = endMin % 60
            val endStr = String.format(Locale.getDefault(), "%02d:%02d:45", endH, endM)

            val startMs = WorkHourCalculator.parseDateTime(dateStr, startStr) ?: continue
            val endMs = WorkHourCalculator.parseDateTime(dateStr, endStr) ?: continue

            val (netHours, grossHours, breakDeducted) = WorkHourCalculator.calculateWorkHours(startMs, endMs)

            records.add(
                WorkRecord(
                    date = dateStr,
                    startTime = startStr,
                    endTime = endStr,
                    startTimestamp = startMs,
                    endTimestamp = endMs,
                    netWorkHours = netHours,
                    grossHours = grossHours,
                    breakHoursDeducted = breakDeducted,
                    note = "示例打卡"
                )
            )
        }

        saveRecords(records)
    }

    /**
     * Exports all work records into clean plain text format.
     */
    fun exportToPlainText(): String {
        val records = getAllRecords()
        if (records.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("# 工时记录纯文本备份 (共 ${records.size} 条)\n")
        sb.append("# 格式: 日期, 上班时间, 下班时间\n\n")
        for (r in records) {
            sb.append("${r.date}, ${r.startTime}, ${r.endTime}\n")
        }
        return sb.toString()
    }

    /**
     * Imports work records from plain text (CSV / Line-by-line / JSON).
     * Returns the count of successfully imported records.
     */
    fun importFromPlainText(text: String): Int {
        if (text.isBlank()) return 0
        var count = 0
        val recordsToSave = getAllRecords().toMutableList()

        val trimmed = text.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val date = obj.optString("date")
                    val start = obj.optString("startTime")
                    val end = obj.optString("endTime")
                    if (date.isNotBlank() && start.isNotBlank() && end.isNotBlank()) {
                        val startMs = WorkHourCalculator.parseDateTime(date, start)
                        val endMs = WorkHourCalculator.parseDateTime(date, end)
                        if (startMs != null && endMs != null && endMs > startMs) {
                            val (net, gross, brk) = WorkHourCalculator.calculateWorkHours(startMs, endMs)
                            val rec = WorkRecord(
                                date = date,
                                startTime = start,
                                endTime = end,
                                startTimestamp = startMs,
                                endTimestamp = endMs,
                                netWorkHours = net,
                                grossHours = gross,
                                breakHoursDeducted = brk
                            )
                            val idx = recordsToSave.indexOfFirst { it.date == date }
                            if (idx >= 0) recordsToSave[idx] = rec else recordsToSave.add(rec)
                            count++
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val lines = text.split("\n")
            val lineRegex = Regex("""(\d{4}-\d{2}-\d{2})[,\s\t]+([0-2]?\d:[0-5]\d(:[0-5]\d)?)[,\s\t]+([0-2]?\d:[0-5]\d(:[0-5]\d)?)""")
            for (line in lines) {
                val trimmedLine = line.trim()
                if (trimmedLine.startsWith("#") || trimmedLine.isBlank()) continue
                val match = lineRegex.find(trimmedLine)
                if (match != null) {
                    val date = match.groupValues[1]
                    var start = match.groupValues[2]
                    var end = match.groupValues[4]
                    if (start.length == 5) start = "$start:00"
                    if (end.length == 5) end = "$end:00"

                    val startMs = WorkHourCalculator.parseDateTime(date, start)
                    val endMs = WorkHourCalculator.parseDateTime(date, end)
                    if (startMs != null && endMs != null && endMs > startMs) {
                        val (net, gross, brk) = WorkHourCalculator.calculateWorkHours(startMs, endMs)
                        val rec = WorkRecord(
                            date = date,
                            startTime = start,
                            endTime = end,
                            startTimestamp = startMs,
                            endTimestamp = endMs,
                            netWorkHours = net,
                            grossHours = gross,
                            breakHoursDeducted = brk
                        )
                        val idx = recordsToSave.indexOfFirst { it.date == date }
                        if (idx >= 0) recordsToSave[idx] = rec else recordsToSave.add(rec)
                        count++
                    }
                }
            }
        }

        if (count > 0) {
            saveRecords(recordsToSave)
        }
        return count
    }
}
