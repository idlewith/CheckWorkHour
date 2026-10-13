package com.idlewith.checkworkhour

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlewith.checkworkhour.model.MonthlyStats
import com.idlewith.checkworkhour.model.WorkRecord
import com.idlewith.checkworkhour.repository.WorkRecordRepository
import com.idlewith.checkworkhour.ui.components.WorkRecordEditorCard
import com.idlewith.checkworkhour.ui.theme.CheckWorkHourTheme
import com.idlewith.checkworkhour.util.WorkHourCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var repository: WorkRecordRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = WorkRecordRepository(applicationContext)
        enableEdgeToEdge()

        setContent {
            CheckWorkHourTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        repository = repository,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(repository: WorkRecordRepository, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val cal = remember { Calendar.getInstance() }
    val yearMonthSdf = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val dateSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var currentYearMonth by remember { mutableStateOf(yearMonthSdf.format(Date())) }
    val todayStr by remember { mutableStateOf(dateSdf.format(Date())) }

    var monthRecords by remember { mutableStateOf<List<WorkRecord>>(emptyList()) }
    var stats by remember { mutableStateOf<MonthlyStats?>(null) }

    // Dialog state for Import / Export
    var showImportExportDialog by remember { mutableStateOf(false) }

    fun refreshData() {
        monthRecords = repository.getRecordsForMonth(currentYearMonth)
        stats = repository.calculateMonthlyStats(currentYearMonth)
    }

    LaunchedEffect(currentYearMonth) {
        refreshData()
    }

    val navigateMonth = { delta: Int ->
        try {
            val d = yearMonthSdf.parse(currentYearMonth)
            if (d != null) {
                cal.time = d
                cal.add(Calendar.MONTH, delta)
                currentYearMonth = yearMonthSdf.format(cal.time)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Title & Import/Export Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 月度平均工时计算器",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = { showImportExportDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("导入/导出纯文本", fontSize = 12.sp)
                }
            }
        }

        // Month Selector Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { navigateMonth(-1) },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("◀ 上一月")
                    }

                    Text(
                        text = "${currentYearMonth.replace("-", "年 ")}月",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    OutlinedButton(
                        onClick = { navigateMonth(1) },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("下一月 ▶")
                    }
                }
            }
        }

        // Average Monthly Work Hours Summary Card
        if (stats != null) {
            val s = stats!!
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E88E5)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "本月平均日工时",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f 小时", s.averageDailyHours),
                            color = Color.Yellow,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "出勤天数", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(text = "${s.totalDaysWorked} 天", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "月总工时", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(text = String.format(Locale.getDefault(), "%.1f 小时", s.totalNetWorkHours), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "平均打卡", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(text = "${s.avgStartTime} ~ ${s.avgEndTime}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Rule Explanation Info Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "ℹ️ 工时规则：一天跨度为 05:00:00 至次日 04:59:59。自动扣除午休 12:00-13:30 (1.5h) 与 晚休 18:30-19:00 (0.5h)。默认打卡：09:00:00 ~ 18:30:00。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Add / Edit Record Card Component
        item {
            WorkRecordEditorCard(
                context = context,
                initialDate = todayStr,
                initialStartTime = "09:00:00",
                initialEndTime = "18:30:00",
                onSaveRecord = { date, start, end ->
                    val startMs = WorkHourCalculator.parseDateTime(date, start)
                    val endMs = WorkHourCalculator.parseDateTime(date, end)

                    if (startMs == null || endMs == null) {
                        Toast.makeText(context, "日期或时间格式错误，请检查！", Toast.LENGTH_SHORT).show()
                        return@WorkRecordEditorCard
                    }

                    if (endMs <= startMs) {
                        Toast.makeText(context, "下班时间必须晚于上班时间！", Toast.LENGTH_SHORT).show()
                        return@WorkRecordEditorCard
                    }

                    val (netHours, grossHours, breakDeducted) = WorkHourCalculator.calculateWorkHours(startMs, endMs)

                    val record = WorkRecord(
                        date = date,
                        startTime = start,
                        endTime = end,
                        startTimestamp = startMs,
                        endTimestamp = endMs,
                        netWorkHours = netHours,
                        grossHours = grossHours,
                        breakHoursDeducted = breakDeducted
                    )

                    repository.addOrUpdateRecord(record)
                    Toast.makeText(context, "已保存 $date 打卡，净工时: $netHours 小时", Toast.LENGTH_SHORT).show()

                    val monthOfRecord = date.take(7)
                    if (monthOfRecord != currentYearMonth) {
                        currentYearMonth = monthOfRecord
                    } else {
                        refreshData()
                    }
                }
            )
        }

        // Header for Daily List & Preset Generator Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "明细记录 (${monthRecords.size}条)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                OutlinedButton(
                    onClick = {
                        repository.generateSampleDataForMonth(currentYearMonth)
                        refreshData()
                        Toast.makeText(context, "已生成 ${currentYearMonth} 月示例打卡数据", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("一键生成本月示例数据", fontSize = 11.sp)
                }
            }
        }

        // Record List Items
        if (monthRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无 ${currentYearMonth} 月的打卡数据，可手动录入或点击上方“一键生成示例”。",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(monthRecords, key = { it.id }) { record ->
                RecordListItem(
                    record = record,
                    onDelete = {
                        repository.deleteRecord(record.id)
                        refreshData()
                        Toast.makeText(context, "已删除 ${record.date} 打卡记录", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Import / Export Dialog
    if (showImportExportDialog) {
        ImportExportDialog(
            context = context,
            repository = repository,
            onDismiss = {
                showImportExportDialog = false
                refreshData()
            }
        )
    }
}

@Composable
fun ImportExportDialog(
    context: Context,
    repository: WorkRecordRepository,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf(repository.exportToPlainText()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "📋 纯文本导入与导出工时记录", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "下方为当前所有打卡记录纯文本。您可以直接复制保存，也可以在此粘贴纯文本（或CSV格式：YYYY-MM-DD, HH:mm:ss, HH:mm:ss）进行导入：",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("在此粘贴导出文本或输入CSV数据...") },
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = repository.importFromPlainText(textInput)
                    if (count > 0) {
                        Toast.makeText(context, "成功导入 $count 条打卡记录！", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "未识别到有效打卡格式文本，请检查！", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("解析并导入")
            }
        },
        dismissButton = {
            Row {
                OutlinedButton(
                    onClick = {
                        val exported = repository.exportToPlainText()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("WorkHourRecords", exported)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制工时记录纯文本到剪贴板！", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("复制全部文本")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    )
}

@Composable
fun RecordListItem(
    record: WorkRecord,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.date} (${getDayOfWeekName(record.date)})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "打卡: ${record.startTime} ~ ${record.endTime}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.breakHoursDeducted > 0) {
                    Text(
                        text = "毛工时: ${record.grossHours}h | 已扣除休息: ${record.breakHoursDeducted}h",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${record.netWorkHours}h",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF2E7D32)
                )

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("删除", color = Color.Red, fontSize = 11.sp)
                }
            }
        }
    }
}

fun getDayOfWeekName(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr)
        if (date != null) {
            val cal = Calendar.getInstance()
            cal.time = date
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "周一"
                Calendar.TUESDAY -> "周二"
                Calendar.WEDNESDAY -> "周三"
                Calendar.THURSDAY -> "周四"
                Calendar.FRIDAY -> "周五"
                Calendar.SATURDAY -> "周六"
                Calendar.SUNDAY -> "周日"
                else -> ""
            }
        } else ""
    } catch (_: Exception) {
        ""
    }
}
