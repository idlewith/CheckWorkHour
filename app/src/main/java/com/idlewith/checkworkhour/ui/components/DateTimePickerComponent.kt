package com.idlewith.checkworkhour.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WorkRecordEditorCard(
    context: Context,
    initialDate: String,
    initialStartTime: String = "09:00:00",
    initialEndTime: String = "18:30:00",
    onSaveRecord: (date: String, startTime: String, endTime: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var dateInput by remember(initialDate) { mutableStateOf(initialDate) }
    var startTimeInput by remember(initialStartTime) { mutableStateOf(initialStartTime) }
    var endTimeInput by remember(initialEndTime) { mutableStateOf(initialEndTime) }

    val cal = remember { Calendar.getInstance() }

    // DatePickerDialog Helper
    val showDatePicker = {
        try {
            val parts = dateInput.split("-").map { it.toInt() }
            val y = parts[0]
            val m = parts[1] - 1
            val d = parts[2]
            cal.set(y, m, d)
        } catch (_: Exception) {
            cal.time = Date()
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dateInput = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // TimePickerDialog Helper for Start Time
    val showStartTimePicker = {
        try {
            val parts = startTimeInput.split(":").map { it.toInt() }
            cal.set(Calendar.HOUR_OF_DAY, parts[0])
            cal.set(Calendar.MINUTE, parts[1])
        } catch (_: Exception) {
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
        }

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                startTimeInput = String.format(Locale.getDefault(), "%02d:%02d:00", hourOfDay, minute)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    // TimePickerDialog Helper for End Time
    val showEndTimePicker = {
        try {
            val parts = endTimeInput.split(":").map { it.toInt() }
            cal.set(Calendar.HOUR_OF_DAY, parts[0])
            cal.set(Calendar.MINUTE, parts[1])
        } catch (_: Exception) {
            cal.set(Calendar.HOUR_OF_DAY, 18)
            cal.set(Calendar.MINUTE, 30)
        }

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                endTimeInput = String.format(Locale.getDefault(), "%02d:%02d:00", hourOfDay, minute)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⏱️ 录入 / 修改打卡时间 (支持选择或手动编辑)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("日期 (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = showDatePicker) {
                    Text("选择日期")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Start Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = startTimeInput,
                    onValueChange = { startTimeInput = it },
                    label = { Text("上班时间 (HH:mm:ss)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = showStartTimePicker) {
                    Text("选时间")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // End Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = endTimeInput,
                    onValueChange = { endTimeInput = it },
                    label = { Text("下班时间 (HH:mm:ss)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = showEndTimePicker) {
                    Text("选时间")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Preset button & Save button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        startTimeInput = "09:00:00"
                        endTimeInput = "18:30:00"
                    }
                ) {
                    Text("恢复默认 (09:00 - 18:30)", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onSaveRecord(dateInput.trim(), startTimeInput.trim(), endTimeInput.trim())
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text("保存打卡记录", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
