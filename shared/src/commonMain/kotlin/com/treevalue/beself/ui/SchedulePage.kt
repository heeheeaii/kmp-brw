package com.treevalue.beself.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.ScheduleBackend
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter

enum class ScheduleType { NORMAL, CYCLIC, SEQUENCE }
enum class RepeatMode { ONCE, DAILY, SPECIFIC_DAYS }

data class CyclicTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val duration: Int, // 分钟
)

data class ScheduleItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val note: String = "",
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val type: ScheduleType,
    val repeatMode: RepeatMode = RepeatMode.ONCE,
    val weekDays: Set<DayOfWeek> = emptySet(),
    val cyclicTasks: List<CyclicTask> = emptyList(), // 循环任务列表
    val lane: Int = 0,
)

@Composable
fun SchedulePage(
    onBackClicked: () -> Unit,
    backend: ScheduleBackend = ScheduleBackend.getInstance(scope = GlobalScope),
) {
    // 使用后端的日程列表
    val schedules by remember {
        derivedStateOf { backend.getAllSchedules() }
    }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var visibleDays by remember { mutableStateOf(1) }
    var showPreviousDay by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val now = remember { LocalDate.now() }
    val startDate = if (showPreviousDay) now.minusDays(1) else now

    // 自动定位到当前时间最近的日程
    LaunchedEffect(schedules) {
        if (schedules.isNotEmpty()) {
            val currentDateTime = LocalDateTime.now()
            val todaySchedules = schedules.filter {
                it.startTime.toLocalDate() == now
            }.sortedBy { it.startTime }

            val nearestIndex = todaySchedules.indexOfFirst {
                it.endTime.isAfter(currentDateTime)
            }

            if (nearestIndex >= 0 && !showPreviousDay) {
                listState.scrollToItem(nearestIndex + 1) // +1 for expand button
            }
        }
    }

    fun getSchedulesForDate(date: LocalDate): List<ScheduleItem> {
        return schedules.filter { schedule ->
            when (schedule.repeatMode) {
                RepeatMode.ONCE -> schedule.startTime.toLocalDate() == date
                RepeatMode.DAILY -> true
                RepeatMode.SPECIFIC_DAYS -> schedule.weekDays.contains(date.dayOfWeek)
            }
        }.sortedBy { it.startTime.toLocalTime() }
    }

    fun addSchedule(schedule: ScheduleItem) {
        val schedulesAtSameTime = schedules.filter { existing ->
            existing.startTime.toLocalDate() == schedule.startTime.toLocalDate() && !(schedule.endTime.isBefore(existing.startTime) || schedule.endTime.isEqual(
                existing.startTime
            ) || schedule.startTime.isAfter(existing.endTime) || schedule.startTime.isEqual(existing.endTime))
        }
        val usedLanes = schedulesAtSameTime.map { it.lane }.toSet()
        var newLane = 0
        while (usedLanes.contains(newLane)) newLane++

        // 使用后端添加
        backend.addSchedule(schedule.copy(lane = newLane))
    }

    fun deleteSchedule(id: String) {
        backend.deleteSchedule(id)
        selectedIds = selectedIds - id
    }

    fun moveSchedule(id: String, direction: Int) {
        val schedule = schedules.find { it.id == id } ?: return
        val date = schedule.startTime.toLocalDate()
        val daySchedules = getSchedulesForDate(date).filter { it.lane == schedule.lane }
        val currentIndex = daySchedules.indexOfFirst { it.id == id }

        if (direction < 0 && currentIndex > 0) {
            // 上移：结束时间 = 上一个的开始时间
            val prevSchedule = daySchedules[currentIndex - 1]
            val newEndTime = prevSchedule.startTime
            val duration = Duration.between(schedule.startTime, schedule.endTime)
            val newStartTime = newEndTime.minus(duration)
            backend.updateSchedule(id, schedule.copy(startTime = newStartTime, endTime = newEndTime))
        } else if (direction > 0 && currentIndex < daySchedules.size - 1) {
            // 下移：开始时间 = 下一个的结束时间
            val nextSchedule = daySchedules[currentIndex + 1]
            val newStartTime = nextSchedule.endTime
            val duration = Duration.between(schedule.startTime, schedule.endTime)
            val newEndTime = newStartTime.plus(duration)
            backend.updateSchedule(id, schedule.copy(startTime = newStartTime, endTime = newEndTime))
        }
    }

    fun copySchedules(startTime: LocalDateTime) {
        if (selectedIds.isEmpty()) return
        val selectedSchedules = schedules.filter { it.id in selectedIds }.sortedBy { it.startTime }
        if (selectedSchedules.isEmpty()) return
        val firstScheduleStart = selectedSchedules.first().startTime
        val timeOffset = Duration.between(firstScheduleStart, startTime)

        val newSchedules = selectedSchedules.map { schedule ->
            schedule.copy(
                id = java.util.UUID.randomUUID().toString(),
                startTime = schedule.startTime.plus(timeOffset),
                endTime = schedule.endTime.plus(timeOffset)
            )
        }

        // 批量添加
        newSchedules.forEach { addSchedule(it) }
        selectedIds = emptySet()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部标题栏
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClicked, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colors.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "日程管理",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onBackground
                    )
                }
                IconButton(
                    onClick = { if (selectedIds.isNotEmpty()) showCopyDialog = true },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        "复制",
                        tint = if (selectedIds.isNotEmpty()) MaterialTheme.colors.primary
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 主滚动区域
            LazyColumn(
                state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                // 向上展开按钮
                if (!showPreviousDay) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(40.dp).clickable { showPreviousDay = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                            ) {
                                Divider(
                                    modifier = Modifier.weight(1f),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colors.primary.copy(alpha = 0.3f)
                                )
                                Icon(
                                    Icons.Default.ExpandLess,
                                    null,
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(16.dp).padding(horizontal = 4.dp)
                                )
                                Divider(
                                    modifier = Modifier.weight(1f),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colors.primary.copy(alpha = 0.3f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // 日程列表
                items((0 until visibleDays).toList()) { dayOffset ->
                    val date = startDate.plusDays(dayOffset.toLong())
                    DayScheduleSection(date = date,
                        schedules = getSchedulesForDate(date),
                        selectedIds = selectedIds,
                        onSelectChanged = { id, selected ->
                            selectedIds = if (selected) selectedIds + id else selectedIds - id
                        },
                        onEdit = { id, newSchedule ->
                            backend.updateSchedule(id, newSchedule)
                        },
                        onDelete = { deleteSchedule(it) },
                        onMove = { id, direction -> moveSchedule(id, direction) })
                    if (dayOffset < visibleDays - 1) {
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                        )
                    }
                }
            }

            // 底部固定按钮区
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 展开按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 打开明天日程按钮（向下展开一天）
                    Box(
                        modifier = Modifier.weight(1f).height(36.dp).clickable {
                            if (visibleDays < 7) {
                                visibleDays++
                                scope.launch {
                                    delay(100)
                                    listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                                }
                            }
                        }, contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Divider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = MaterialTheme.colors.primary.copy(alpha = 0.3f)
                            )
                            Icon(
                                Icons.Default.ExpandMore,
                                null,
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(16.dp).padding(horizontal = 4.dp)
                            )
                            Divider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = MaterialTheme.colors.primary.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // 展开到7天按钮
                    Box(
                        modifier = Modifier.weight(1f).height(36.dp).clickable {
                            visibleDays = 7
                            scope.launch {
                                delay(100)
                                listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                            }
                        }, contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Divider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = MaterialTheme.colors.primary.copy(alpha = 0.3f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.ExpandMore,
                                    null,
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Icon(
                                    Icons.Default.ExpandMore,
                                    null,
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Divider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = MaterialTheme.colors.primary.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                // 添加日程按钮
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "添加日程", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleDialog(onDismiss = { showAddDialog = false }, onConfirm = { addSchedule(it); showAddDialog = false })
    }
    if (showCopyDialog) {
        CopyScheduleDialog(onDismiss = { showCopyDialog = false },
            onConfirm = { copySchedules(it); showCopyDialog = false })
    }
}

@Composable
fun DayScheduleSection(
    date: LocalDate,
    schedules: List<ScheduleItem>,
    selectedIds: Set<String>,
    onSelectChanged: (String, Boolean) -> Unit,
    onEdit: (String, ScheduleItem) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
) {
    val maxLane = schedules.maxOfOrNull { it.lane } ?: 0
    val laneColors = remember {
        listOf(
            Color(0xFFFFE0B2), // 浅橙色
            Color(0xFFFFCCBC), // 浅红色
            Color(0xFFFFF9C4), // 浅黄色
            Color(0xFFFFE0E0), // 浅粉色
            Color(0xFFFFD180)  // 浅杏色
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 日期标题
        Text(
            text = "${date.monthValue}月${date.dayOfMonth}日 ${
                when (date.dayOfWeek) {
                    DayOfWeek.MONDAY -> "周一"
                    DayOfWeek.TUESDAY -> "周二"
                    DayOfWeek.WEDNESDAY -> "周三"
                    DayOfWeek.THURSDAY -> "周四"
                    DayOfWeek.FRIDAY -> "周五"
                    DayOfWeek.SATURDAY -> "周六"
                    DayOfWeek.SUNDAY -> "周日"
                }
            }",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (schedules.isEmpty()) {
            Text(
                "暂无日程",
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            // 并行日程展示（泳道）
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth()
            ) {
                for (lane in 0..maxLane) {
                    val laneSchedules = schedules.filter { it.lane == lane }
                    Column(
                        modifier = Modifier.width(340.dp)
                            .background(laneColors[lane % laneColors.size].copy(alpha = 0.15f)).padding(8.dp)
                    ) {
                        laneSchedules.forEach { schedule ->
                            ScheduleItemCard(schedule = schedule,
                                isSelected = selectedIds.contains(schedule.id),
                                onSelectChanged = { onSelectChanged(schedule.id, it) },
                                onEdit = { onEdit(schedule.id, it) },
                                onDelete = { onDelete(schedule.id) },
                                onMoveUp = { onMove(schedule.id, -1) },
                                onMoveDown = { onMove(schedule.id, 1) })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    if (lane < maxLane) {
                        // 虚线分隔泳道
                        Box(
                            modifier = Modifier.width(2.dp).fillMaxHeight()
                                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleItemCard(
    schedule: ScheduleItem,
    isSelected: Boolean,
    onSelectChanged: (Boolean) -> Unit,
    onEdit: (ScheduleItem) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var showNoteExpanded by remember { mutableStateOf(false) }
    var deleteClickTime by remember { mutableStateOf(0L) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var editedName by remember(schedule) { mutableStateOf(schedule.name) }
    var editedNote by remember(schedule) { mutableStateOf(schedule.note) }
    var editedStartTime by remember(schedule) { mutableStateOf(schedule.startTime) }
    var editedEndTime by remember(schedule) { mutableStateOf(schedule.endTime) }
    var showTimePickerStart by remember { mutableStateOf(false) }
    var showTimePickerEnd by remember { mutableStateOf(false) }

    // 删除按钮颜色：绿色 -> 红色
    val deleteButtonColor by animateColorAsState(
        targetValue = if (showDeleteConfirm) Color(0xFFFF5252) else Color(0xFF4CAF50)
    )

    fun handleDeleteClick() {
        val currentTime = System.currentTimeMillis()
        if (showDeleteConfirm && currentTime - deleteClickTime < 1000) {
            onDelete()
        } else {
            showDeleteConfirm = true
            deleteClickTime = currentTime
            scope.launch {
                delay(1000)
                showDeleteConfirm = false
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f)
        else MaterialTheme.colors.surface
    ) {
        Column {
            // 主要内容行
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 多选框
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectChanged,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary),
                    modifier = Modifier.size(24.dp)
                )

                // 时间显示/编辑
                if (isEditing) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        // 开始时间
                        Text(
                            text = editedStartTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.clickable { showTimePickerStart = true }
                        )
                        // 结束时间
                        Text(
                            text = editedEndTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.clickable { showTimePickerEnd = true }
                        )
                    }
                } else {
                    Column {
                        Text(
                            text = schedule.startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                        Text(
                            text = schedule.endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }

                // 名称显示/编辑
                if (isEditing) {
                    TextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 18.sp, fontWeight = FontWeight.Bold
                        ),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = schedule.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 备注按钮
                if (!isEditing && schedule.note.isNotEmpty()) {
                    IconButton(
                        onClick = { showNoteExpanded = !showNoteExpanded }, modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (showNoteExpanded) Icons.Default.ExpandLess else Icons.AutoMirrored.Filled.Notes,
                            "备注",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 编辑/确认按钮
                IconButton(
                    onClick = {
                        if (isEditing && editedName.isNotBlank()) {
                            onEdit(
                                schedule.copy(
                                    name = editedName,
                                    note = editedNote,
                                    startTime = editedStartTime,
                                    endTime = editedEndTime
                                )
                            )
                        }
                        isEditing = !isEditing
                    }, modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        "编辑",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 移动按钮（非编辑模式）
                if (!isEditing) {
                    IconButton(
                        onClick = onMoveUp, modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            "上移",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown, modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            "下移",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 删除按钮
                if (!isEditing) {
                    IconButton(
                        onClick = { handleDeleteClick() }, modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete, "删除", tint = deleteButtonColor, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 备注展开区域
            if (showNoteExpanded && schedule.note.isNotEmpty()) {
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                )
                Text(
                    text = schedule.note,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // 编辑模式的备注输入
            if (isEditing) {
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                )
                TextField(
                    value = editedNote,
                    onValueChange = { editedNote = it },
                    placeholder = { Text("备注（可选）") },
                    colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    // 开始时间选择器
    if (showTimePickerStart) {
        TimePickerDialog(
            initialTime = editedStartTime,
            title = "设置开始时间",
            onDismiss = { showTimePickerStart = false },
            onConfirm = { time ->
                editedStartTime = time
                showTimePickerStart = false
            }
        )
    }

    // 结束时间选择器
    if (showTimePickerEnd) {
        TimePickerDialog(
            initialTime = editedEndTime,
            title = "设置结束时间",
            onDismiss = { showTimePickerEnd = false },
            onConfirm = { time ->
                editedEndTime = time
                showTimePickerEnd = false
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AddScheduleDialog(onDismiss: () -> Unit, onConfirm: (ScheduleItem) -> Unit) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ScheduleType.NORMAL) }
    var selectedRepeatMode by remember { mutableStateOf(RepeatMode.ONCE) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.now()) }
    var endTime by remember { mutableStateOf(LocalTime.now().plusHours(1)) }
    var cyclicTasks by remember { mutableStateOf(listOf<CyclicTask>()) }
    var selectedWeekDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    var showTimePickerStart by remember { mutableStateOf(false) }
    var showTimePickerEnd by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // 计算循环任务总时长
    val totalCyclicDuration = cyclicTasks.sumOf { it.duration }
    val scheduleDuration = Duration.between(
        LocalDateTime.of(selectedDate, startTime), LocalDateTime.of(selectedDate, endTime)
    ).toMinutes().toInt()
    val isTimeValid = selectedType != ScheduleType.CYCLIC || totalCyclicDuration <= scheduleDuration

    AlertDialog(onDismissRequest = onDismiss, title = { Text("添加日程", fontWeight = FontWeight.Bold) }, text = {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(500.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("日程名称*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Column {
                    Text("日程类型", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScheduleType.values().forEach { type ->
                            FilterChip(selected = selectedType == type, onClick = {
                                selectedType = type
                                // 普通模式只能一次，周期模式不能一次
                                when (type) {
                                    ScheduleType.NORMAL -> selectedRepeatMode = RepeatMode.ONCE
                                    ScheduleType.SEQUENCE -> {
                                        if (selectedRepeatMode == RepeatMode.ONCE) {
                                            selectedRepeatMode = RepeatMode.DAILY
                                        }
                                    }

                                    ScheduleType.CYCLIC -> {}
                                }
                            }, content = {
                                Text(
                                    when (type) {
                                        ScheduleType.NORMAL -> "普通"
                                        ScheduleType.CYCLIC -> "循环"
                                        ScheduleType.SEQUENCE -> "周期"
                                    }, fontSize = 12.sp
                                )
                            })
                        }
                    }
                }
            }

            // 重复模式 - 普通模式不显示，周期模式不显示"一次"
            if (selectedType != ScheduleType.NORMAL) {
                item {
                    Column {
                        Text("重复模式", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RepeatMode.values().forEach { mode ->
                                // 周期模式不显示"一次"选项
                                if (selectedType != ScheduleType.SEQUENCE || mode != RepeatMode.ONCE) {
                                    FilterChip(selected = selectedRepeatMode == mode,
                                        onClick = { selectedRepeatMode = mode },
                                        content = {
                                            Text(
                                                when (mode) {
                                                    RepeatMode.ONCE -> "一次"
                                                    RepeatMode.DAILY -> "每天"
                                                    RepeatMode.SPECIFIC_DAYS -> "特定日"
                                                }, fontSize = 12.sp
                                            )
                                        })
                                }
                            }
                        }
                    }
                }
            }
            if (selectedRepeatMode == RepeatMode.SPECIFIC_DAYS) {
                item {
                    Column {
                        Text("选择星期", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DayOfWeek.values().forEach { day ->
                                val dayText = when (day) {
                                    DayOfWeek.MONDAY -> "一"
                                    DayOfWeek.TUESDAY -> "二"
                                    DayOfWeek.WEDNESDAY -> "三"
                                    DayOfWeek.THURSDAY -> "四"
                                    DayOfWeek.FRIDAY -> "五"
                                    DayOfWeek.SATURDAY -> "六"
                                    DayOfWeek.SUNDAY -> "日"
                                }
                                FilterChip(selected = selectedWeekDays.contains(day), onClick = {
                                    selectedWeekDays = if (selectedWeekDays.contains(day)) selectedWeekDays - day
                                    else selectedWeekDays + day
                                }, content = { Text(dayText, fontSize = 12.sp) })
                            }
                        }
                    }
                }
            }

            // 循环模式 - 任务列表
            if (selectedType == ScheduleType.CYCLIC) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("循环任务列表", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "总时长: $totalCyclicDuration / $scheduleDuration 分钟",
                                fontSize = 12.sp,
                                color = if (isTimeValid) MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                else Color.Red
                            )
                        }

                        if (!isTimeValid) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "错误：任务总时长超过日程时长",
                                fontSize = 12.sp,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 任务列表
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).background(
                                MaterialTheme.colors.surface, RoundedCornerShape(8.dp)
                            ).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (cyclicTasks.isEmpty()) {
                                Text(
                                    "暂无任务，点击下方添加按钮添加",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                cyclicTasks.forEach { task ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation = 1.dp,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    task.name, fontSize = 14.sp, fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "${task.duration} 分钟",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    cyclicTasks = cyclicTasks.filter { it.id != task.id }
                                                }, modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Remove,
                                                    "删除",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 添加任务按钮
                            OutlinedButton(
                                onClick = { showAddTaskDialog = true }, modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("添加任务", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            item {
                Column {
                    Text("时间设置", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTimePickerStart = true }, modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("开始", fontSize = 12.sp)
                                Text(
                                    startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showTimePickerEnd = true }, modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("结束", fontSize = 12.sp)
                                Text(
                                    endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }, confirmButton = {
        TextButton(
            onClick = {
                if (name.isNotBlank() && isTimeValid) {
                    val schedule = ScheduleItem(
                        name = name,
                        note = note,
                        startTime = LocalDateTime.of(selectedDate, startTime),
                        endTime = LocalDateTime.of(selectedDate, endTime),
                        type = selectedType,
                        repeatMode = selectedRepeatMode,
                        weekDays = selectedWeekDays,
                        cyclicTasks = if (selectedType == ScheduleType.CYCLIC) cyclicTasks else emptyList()
                    )
                    onConfirm(schedule)
                }
            }, enabled = name.isNotBlank() && isTimeValid
        ) {
            Text("确定")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("取消")
        }
    })

    if (showTimePickerStart) {
        TimePickerDialog(
            initialTime = LocalDateTime.of(selectedDate, startTime),
            title = "设置开始时间",
            onDismiss = { showTimePickerStart = false },
            onConfirm = { time ->
                startTime = time.toLocalTime()
                showTimePickerStart = false
            })
    }

    if (showTimePickerEnd) {
        TimePickerDialog(
            initialTime = LocalDateTime.of(selectedDate, endTime),
            title = "设置结束时间",
            onDismiss = { showTimePickerEnd = false },
            onConfirm = { time ->
                endTime = time.toLocalTime()
                showTimePickerEnd = false
            })
    }

    // 添加循环任务对话框
    if (showAddTaskDialog) {
        AddCyclicTaskDialog(onDismiss = { showAddTaskDialog = false }, onConfirm = { task ->
            cyclicTasks = cyclicTasks + task
            showAddTaskDialog = false
        })
    }
}

@Composable
fun AddCyclicTaskDialog(onDismiss: () -> Unit, onConfirm: (CyclicTask) -> Unit) {
    var taskName by remember { mutableStateOf("") }
    var taskDuration by remember { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("添加循环任务", fontWeight = FontWeight.Bold) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text("任务名称*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            TextField(
                value = taskDuration,
                onValueChange = { taskDuration = it.filter { c -> c.isDigit() } },
                label = { Text("持续时间(分钟)*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }, confirmButton = {
        TextButton(
            onClick = {
                val duration = taskDuration.toIntOrNull()
                if (taskName.isNotBlank() && duration != null && duration > 0) {
                    onConfirm(CyclicTask(name = taskName, duration = duration))
                }
            }, enabled = taskName.isNotBlank() && (taskDuration.toIntOrNull() ?: 0) > 0
        ) {
            Text("确定")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("取消")
        }
    })
}

@Composable
fun CopyScheduleDialog(onDismiss: () -> Unit, onConfirm: (LocalDateTime) -> Unit) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("选择复制开始时间", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "选择新的开始时间，将保持原有时间间隔",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("开始时间", fontSize = 12.sp)
                        Text(
                            selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalDateTime.of(selectedDate, selectedTime)) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        })

    if (showTimePicker) {
        TimePickerDialog(
            initialTime = LocalDateTime.of(selectedDate, selectedTime),
            title = "选择复制开始时间",
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                selectedTime = time.toLocalTime()
                showTimePicker = false
            })
    }
}

@Composable
fun TimePickerDialog(
    initialTime: LocalDateTime,
    title: String = "选择时间",
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
) {
    var hour by remember { mutableStateOf(initialTime.hour) }
    var minute by remember { mutableStateOf(initialTime.minute) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(title, fontWeight = FontWeight.Bold) }, text = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 小时选择器
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { hour = (hour + 1) % 24 }) {
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = MaterialTheme.colors.primary)
                }
                Text(
                    text = String.format("%02d", hour),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
                IconButton(onClick = { hour = (hour - 1 + 24) % 24 }) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colors.primary)
                }
            }

            Text(
                ":", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 分钟选择器
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { minute = (minute + 1) % 60 }) {
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = MaterialTheme.colors.primary)
                }
                Text(
                    text = String.format("%02d", minute),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
                IconButton(onClick = { minute = (minute - 1 + 60) % 60 }) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colors.primary)
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = { onConfirm(initialTime.withHour(hour).withMinute(minute)) }) {
            Text("确定")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("取消")
        }
    })
}
