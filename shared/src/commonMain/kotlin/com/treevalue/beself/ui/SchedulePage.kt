package com.treevalue.beself.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.Pages
import com.treevalue.beself.backend.ScheduleBackend
import com.treevalue.beself.backend.getLang
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter

enum class ScheduleType { NORMAL, CYCLIC, SEQUENCE }
enum class RepeatMode { ONCE, DAILY, SPECIFIC_DAYS }

internal val timeSize = 10.sp

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
    val cyclicTasks: List<CyclicTask> = emptyList(),
)

/** 工具：若 end <= start，则将 end 顺延到下一天，避免负时长 */
private fun normalizeEnd(start: LocalDateTime, end: LocalDateTime): LocalDateTime {
    return if (!end.isAfter(start)) end.plusDays(1) else end
}

private val DateFmt = DateTimeFormatter.ofPattern("d/M/yy") // 如 1/10/26
private val TimeFmt = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun ChoiceTag(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
) {
    OutlinedButton(
        onClick = onClick,
        border = if (selected) ButtonDefaults.outlinedBorder else ButtonDefaults.outlinedBorder,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun SchedulePage(
    onBackClicked: () -> Unit,
    backend: ScheduleBackend = ScheduleBackend.getInstance(scope = GlobalScope),
) {
    // 使用后端的日程列表（每次 recomposition 派生）
    val schedules by remember { derivedStateOf { backend.getAllSchedules() } }

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
            val todaySchedules = schedules.filter { it.startTime.toLocalDate() == now }
                .sortedBy { it.startTime }

            val nearestIndex = todaySchedules.indexOfFirst { it.endTime.isAfter(currentDateTime) }
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
        }.sortedBy { it.startTime.toLocalTime() } // 按开始时间排序
    }

    fun addSchedule(schedule: ScheduleItem) {
        backend.addSchedule(schedule)
    }

    fun deleteSchedule(id: String) {
        backend.deleteSchedule(id)
        selectedIds = selectedIds - id
    }

    fun batchDeleteSchedules() {
        selectedIds.forEach { id -> backend.deleteSchedule(id) }
        selectedIds = emptySet()
    }

    fun copySchedules(startTime: LocalDateTime) {
        if (selectedIds.isEmpty()) return
        val selected = schedules.filter { it.id in selectedIds }.sortedBy { it.startTime }
        if (selected.isEmpty()) return
        val firstStart = selected.first().startTime
        val delta = Duration.between(firstStart, startTime)
        selected.forEach { s ->
            val newStart = s.startTime.plus(delta)
            val newEnd = s.endTime.plus(delta)
            addSchedule(
                s.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    startTime = newStart,
                    endTime = normalizeEnd(newStart, newEnd)
                )
            )
        }
        selectedIds = emptySet()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClicked, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            Pages.FunctionPage.Back.getLang(),
                            tint = MaterialTheme.colors.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        Pages.SchedulePage.ScheduleManagement.getLang(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onBackground
                    )
                }
                // 右上角：删除（在复制前）+ 复制
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { if (selectedIds.isNotEmpty()) batchDeleteSchedules() },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            Pages.SchedulePage.Delete.getLang(),
                            tint = if (selectedIds.isNotEmpty()) Color(0xFFFF5252)
                            else MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { if (selectedIds.isNotEmpty()) showCopyDialog = true },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            Pages.SchedulePage.Copy.getLang(),
                            tint = if (selectedIds.isNotEmpty()) MaterialTheme.colors.primary
                            else MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 主滚动区域（按天垂直显示）
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // 向上展开按钮（显示前一天）
                if (!showPreviousDay) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clickable { showPreviousDay = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
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
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(horizontal = 4.dp)
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

                // 日程列表（按开始时间排序，垂直展示）
                items((0 until visibleDays).toList()) { dayOffset ->
                    val date = startDate.plusDays(dayOffset.toLong())
                    DayScheduleSection(
                        date = date,
                        schedules = getSchedulesForDate(date),
                        selectedIds = selectedIds,
                        onSelectChanged = { id, selected ->
                            selectedIds = if (selected) selectedIds + id else selectedIds - id
                        },
                        onEdit = { id, newSchedule -> backend.updateSchedule(id, newSchedule) },
                        onDelete = { deleteSchedule(it) }
                    )
                    if (dayOffset < visibleDays - 1) {
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                        )
                    }
                }
            }

            // 底部固定按钮区
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 展开按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 向下展开一天
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable {
                                if (visibleDays < 7) {
                                    visibleDays++
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
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
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(horizontal = 4.dp)
                            )
                            Divider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = MaterialTheme.colors.primary.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // 一键展开到 7 天
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable { visibleDays = 7 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
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
                                    Icons.Default.ExpandMore, null,
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Icon(
                                    Icons.Default.ExpandMore, null,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        Pages.SchedulePage.AddSchedule.getLang(),
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { item -> ScheduleBackend.getInstance(GlobalScope).addSchedule(item); showAddDialog = false }
        )
    }
    if (showCopyDialog) {
        CopyScheduleDialog(
            onDismiss = { showCopyDialog = false },
            onConfirm = { copySchedules(it); showCopyDialog = false }
        )
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
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 日期标题
        Text(
            text = "${date.monthValue}${Pages.SchedulePage.MonthDay.getLang()}${date.dayOfMonth} " +
                    "${Pages.SchedulePage.Day.getLang()}${
                        when (date.dayOfWeek) {
                            DayOfWeek.MONDAY -> Pages.SchedulePage.Monday.getLang()
                            DayOfWeek.TUESDAY -> Pages.SchedulePage.Tuesday.getLang()
                            DayOfWeek.WEDNESDAY -> Pages.SchedulePage.Wednesday.getLang()
                            DayOfWeek.THURSDAY -> Pages.SchedulePage.Thursday.getLang()
                            DayOfWeek.FRIDAY -> Pages.SchedulePage.Friday.getLang()
                            DayOfWeek.SATURDAY -> Pages.SchedulePage.Saturday.getLang()
                            DayOfWeek.SUNDAY -> Pages.SchedulePage.Sunday.getLang()
                        }
                    }",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (schedules.isEmpty()) {
            Text(
                Pages.SchedulePage.NoSchedules.getLang(),
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                schedules.forEach { schedule ->
                    ScheduleItemCard(
                        schedule = schedule,
                        isSelected = selectedIds.contains(schedule.id),
                        onSelectChanged = { onSelectChanged(schedule.id, it) },
                        onEdit = { onEdit(schedule.id, it) },
                        onDelete = { onDelete(schedule.id) }
                    )
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
    var showPickerStart by remember { mutableStateOf(false) }
    var showPickerEnd by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
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

                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = schedule.startTime.format(DateTimeFormatter.ofPattern("d/M HH:mm")),
                        fontSize = timeSize,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = schedule.endTime.format(DateTimeFormatter.ofPattern("d/M HH:mm")),
                        fontSize = timeSize,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.primary
                    )
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
                        onClick = { showNoteExpanded = !showNoteExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (showNoteExpanded) Icons.Default.ExpandLess else Icons.AutoMirrored.Filled.Notes,
                            Pages.SchedulePage.NoteLabel.getLang(),
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 编辑/确认按钮
                IconButton(
                    onClick = {
                        if (isEditing && editedName.isNotBlank()) {
                            val ns = editedStartTime
                            val ne = normalizeEnd(ns, editedEndTime) // 修复跨天负时长
                            onEdit(
                                schedule.copy(
                                    name = editedName,
                                    note = editedNote,
                                    startTime = ns,
                                    endTime = ne
                                )
                            )
                        }
                        isEditing = !isEditing
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        Pages.SchedulePage.Edit.getLang(),
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 删除按钮（单条）
                if (!isEditing) {
                    IconButton(
                        onClick = { handleDeleteClick() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            Pages.SchedulePage.Delete.getLang(),
                            tint = deleteButtonColor,
                            modifier = Modifier.size(18.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    placeholder = { Text(Pages.SchedulePage.NoteOptional.getLang()) },
                    colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    // 开始/结束时间选择器（新：日期+时间一体）
    if (showPickerStart) {
        DateTimePickerDialog(
            initial = editedStartTime,
            onDismiss = { showPickerStart = false },
            onConfirm = { dt ->
                editedStartTime = dt
                showPickerStart = false
            }
        )
    }
    if (showPickerEnd) {
        DateTimePickerDialog(
            initial = editedEndTime,
            onDismiss = { showPickerEnd = false },
            onConfirm = { dt ->
                editedEndTime = dt
                showPickerEnd = false
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
    var startDateTime by remember { mutableStateOf(LocalDateTime.now()) } // 默认“现在”
    var endDateTime by remember {
        mutableStateOf(LocalDateTime.now().plusDays(1)) // 默认“明天同一时间”
    }
    var cyclicTasks by remember { mutableStateOf(listOf<CyclicTask>()) }
    var selectedWeekDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    var showPickerStart by remember { mutableStateOf(false) }
    var showPickerEnd by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // 计算循环任务总时长 & 计划时长（分钟）
    val totalCyclicDuration = cyclicTasks.sumOf { it.duration }
    val scheduleDuration = Duration.between(startDateTime, normalizeEnd(startDateTime, endDateTime))
        .toMinutes().toInt()
    val isTimeValid = selectedType != ScheduleType.CYCLIC || totalCyclicDuration <= scheduleDuration

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Pages.SchedulePage.AddSchedule.getLang(), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Pages.SchedulePage.ScheduleName.getLang()) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    TextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(Pages.SchedulePage.NoteLabel.getLang()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Column {
                        Text(
                            Pages.SchedulePage.ScheduleType.getLang(),
                            fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScheduleType.values().forEach { type ->
                                val label = when (type) {
                                    ScheduleType.NORMAL -> Pages.SchedulePage.Normal.getLang()
                                    ScheduleType.CYCLIC -> Pages.SchedulePage.Cyclic.getLang()
                                    ScheduleType.SEQUENCE -> Pages.SchedulePage.Sequence.getLang()
                                }
                                ChoiceTag(
                                    selected = selectedType == type,
                                    onClick = {
                                        selectedType = type
                                        // 普通模式只能一次，序列/循环不开启 ONCE
                                        when (type) {
                                            ScheduleType.NORMAL -> selectedRepeatMode = RepeatMode.ONCE
                                            ScheduleType.SEQUENCE -> {
                                                if (selectedRepeatMode == RepeatMode.ONCE) {
                                                    selectedRepeatMode = RepeatMode.DAILY
                                                }
                                            }

                                            ScheduleType.CYCLIC -> { /* 不改 repeatMode */
                                            }
                                        }
                                    },
                                    text = label
                                )
                            }
                        }
                    }
                }

                // 重复模式 - 普通模式不显示，序列模式不显示 "一次"
                if (selectedType != ScheduleType.NORMAL) {
                    item {
                        Column {
                            Text(
                                Pages.SchedulePage.RepeatMode.getLang(),
                                fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RepeatMode.values().forEach { mode ->
                                    if (selectedType != ScheduleType.SEQUENCE || mode != RepeatMode.ONCE) {
                                        val label = when (mode) {
                                            RepeatMode.ONCE -> Pages.SchedulePage.Once.getLang()
                                            RepeatMode.DAILY -> Pages.SchedulePage.Daily.getLang()
                                            RepeatMode.SPECIFIC_DAYS -> Pages.SchedulePage.SpecificDays.getLang()
                                        }
                                        ChoiceTag(
                                            selected = selectedRepeatMode == mode,
                                            onClick = { selectedRepeatMode = mode },
                                            text = label
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedRepeatMode == RepeatMode.SPECIFIC_DAYS) {
                    item {
                        Column {
                            Text(
                                Pages.SchedulePage.SelectWeekdays.getLang(),
                                fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Column {
                                // 两行展示一周
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DayOfWeek.values().take(4).forEach { day ->
                                        val dayText = when (day) {
                                            DayOfWeek.MONDAY -> Pages.SchedulePage.Mon.getLang()
                                            DayOfWeek.TUESDAY -> Pages.SchedulePage.Tue.getLang()
                                            DayOfWeek.WEDNESDAY -> Pages.SchedulePage.Wed.getLang()
                                            DayOfWeek.THURSDAY -> Pages.SchedulePage.Thu.getLang()
                                            else -> ""
                                        }
                                        ChoiceTag(
                                            selected = selectedWeekDays.contains(day),
                                            onClick = {
                                                selectedWeekDays =
                                                    if (selectedWeekDays.contains(day)) selectedWeekDays - day
                                                    else selectedWeekDays + day
                                            },
                                            text = dayText
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DayOfWeek.values().drop(4).forEach { day ->
                                        val dayText = when (day) {
                                            DayOfWeek.FRIDAY -> Pages.SchedulePage.Fri.getLang()
                                            DayOfWeek.SATURDAY -> Pages.SchedulePage.Sat.getLang()
                                            DayOfWeek.SUNDAY -> Pages.SchedulePage.Sun.getLang()
                                            else -> ""
                                        }
                                        ChoiceTag(
                                            selected = selectedWeekDays.contains(day),
                                            onClick = {
                                                selectedWeekDays =
                                                    if (selectedWeekDays.contains(day)) selectedWeekDays - day
                                                    else selectedWeekDays + day
                                            },
                                            text = dayText
                                        )
                                    }
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
                                Text(
                                    Pages.SchedulePage.CyclicTaskList.getLang(),
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${Pages.SchedulePage.TotalDuration.getLang()}: $totalCyclicDuration / $scheduleDuration ${Pages.SchedulePage.Minutes.getLang()}",
                                    fontSize = 12.sp,
                                    color = if (isTimeValid) MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                    else Color.Red
                                )
                            }
                            if (!isTimeValid) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    Pages.SchedulePage.ErrorTaskDurationExceeds.getLang(),
                                    fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .background(MaterialTheme.colors.surface, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (cyclicTasks.isEmpty()) {
                                    Text(
                                        Pages.SchedulePage.NoTasksAddBelow.getLang(),
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
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(task.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "${task.duration} ${Pages.SchedulePage.Minutes.getLang()}",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        cyclicTasks = cyclicTasks.filter { it.id != task.id }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Remove,
                                                        Pages.SchedulePage.Delete.getLang(),
                                                        tint = Color.Red,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showAddTaskDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Pages.SchedulePage.AddTask.getLang(), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 时间设置（一个区域内展示“开始/结束”两块；每块两行：上面日期 d/M/yy，下面时间 HH:mm）
                item {
                    Column {
                        Text(
                            Pages.SchedulePage.TimeSettings.getLang(),
                            fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPickerStart = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(Pages.SchedulePage.Start.getLang(), fontSize = 12.sp)
                                    Text(
                                        startDateTime.format(DateFmt),
                                        fontSize = timeSize,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(startDateTime.format(TimeFmt), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = { showPickerEnd = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(Pages.SchedulePage.End.getLang(), fontSize = 12.sp)
                                    Text(
                                        endDateTime.format(DateFmt),
                                        fontSize = timeSize,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(endDateTime.format(TimeFmt), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && isTimeValid) {
                        val ns = startDateTime
                        val ne = normalizeEnd(ns, endDateTime) // 修复跨天负时长
                        val schedule = ScheduleItem(
                            name = name,
                            note = note,
                            startTime = ns,
                            endTime = ne,
                            type = selectedType,
                            repeatMode = selectedRepeatMode,
                            weekDays = selectedWeekDays,
                            cyclicTasks = if (selectedType == ScheduleType.CYCLIC) cyclicTasks else emptyList()
                        )
                        onConfirm(schedule)
                    }
                },
                enabled = name.isNotBlank() && isTimeValid
            ) {
                Text(Pages.BlockSitePage.OK.getLang())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Pages.AddSitePage.Cancel.getLang())
            }
        },
        backgroundColor = MaterialTheme.colors.surface
    )

    if (showPickerStart) {
        DateTimePickerDialog(
            initial = startDateTime,
            onDismiss = { showPickerStart = false },
            onConfirm = { dt ->
                startDateTime = dt
                showPickerStart = false
            }
        )
    }
    if (showPickerEnd) {
        DateTimePickerDialog(
            initial = endDateTime,
            onDismiss = { showPickerEnd = false },
            onConfirm = { dt ->
                endDateTime = dt
                showPickerEnd = false
            }
        )
    }

    // 添加循环任务对话框
    if (showAddTaskDialog) {
        AddCyclicTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { task ->
                cyclicTasks = cyclicTasks + task
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun AddCyclicTaskDialog(onDismiss: () -> Unit, onConfirm: (CyclicTask) -> Unit) {
    var taskName by remember { mutableStateOf("") }
    var taskDuration by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Pages.SchedulePage.AddCyclicTask.getLang(), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text(Pages.SchedulePage.TaskName.getLang()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                TextField(
                    value = taskDuration,
                    onValueChange = { taskDuration = it.filter { c -> c.isDigit() } },
                    label = { Text(Pages.SchedulePage.DurationMinutes.getLang()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val duration = taskDuration.toIntOrNull()
                    if (taskName.isNotBlank() && duration != null && duration > 0) {
                        onConfirm(CyclicTask(name = taskName, duration = duration))
                    }
                },
                enabled = taskName.isNotBlank() && (taskDuration.toIntOrNull() ?: 0) > 0
            ) {
                Text(Pages.BlockSitePage.OK.getLang())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Pages.AddSitePage.Cancel.getLang())
            }
        },
        backgroundColor = MaterialTheme.colors.surface
    )
}

@Composable
fun CopyScheduleDialog(onDismiss: () -> Unit, onConfirm: (LocalDateTime) -> Unit) {
    var selectedDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Pages.SchedulePage.SelectCopyStartTime.getLang(), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    Pages.SchedulePage.SelectCopyTimeDescription.getLang(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Pages.SchedulePage.StartTime.getLang(), fontSize = 12.sp)
                        Text(selectedDateTime.format(DateFmt), fontSize = timeSize, fontWeight = FontWeight.Medium)
                        Text(selectedDateTime.format(TimeFmt), fontSize = timeSize, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDateTime) }) {
                Text(Pages.BlockSitePage.OK.getLang())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Pages.AddSitePage.Cancel.getLang())
            }
        },
        backgroundColor = MaterialTheme.colors.surface
    )

    if (showPicker) {
        DateTimePickerDialog(
            initial = selectedDateTime,
            onDismiss = { showPicker = false },
            onConfirm = { dt ->
                selectedDateTime = dt
                showPicker = false
            }
        )
    }
}

@Composable
fun DateTimePickerDialog(
    initial: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
) {
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.monthValue) } // 1..12
    var day by remember { mutableStateOf(initial.dayOfMonth) }
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }

    fun clampDay(y: Int, m: Int, d: Int): Int {
        val max = YearMonth.of(y, m).lengthOfMonth()
        return d.coerceIn(1, max)
    }

    fun updateMonth(delta: Int) {
        var m = month + delta
        var y = year
        while (m < 1) {
            m += 12; y -= 1
        }
        while (m > 12) {
            m -= 12; y += 1
        }
        month = m
        year = y
        day = clampDay(year, month, day)
    }

    fun updateYear(delta: Int) {
        year += delta
        day = clampDay(year, month, day)
    }

    fun updateDay(delta: Int) {
        val base = LocalDate.of(year, month, day).plusDays(delta.toLong())
        year = base.year
        month = base.monthValue
        day = base.dayOfMonth
    }

    fun updateHour(delta: Int) {
        val total = (hour + delta).mod(24)
        hour = if (total < 0) total + 24 else total
    }

    fun updateMinute(delta: Int) {
        val total = (minute + delta).mod(60)
        minute = if (total < 0) total + 60 else total
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {},
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp).padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 日
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { updateDay(+1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, null, tint = MaterialTheme.colors.primary)
                    }
                    Text(
                        String.format("%02d", day),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    IconButton(onClick = { updateDay(-1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colors.primary)
                    }
                    Text("D", fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }

                // 月
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { updateMonth(+1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, null, tint = MaterialTheme.colors.primary)
                    }
                    Text(
                        String.format("%02d", month),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    IconButton(onClick = { updateMonth(-1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colors.primary)
                    }
                    Text("M", fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }

                // 年（后两位显示，但内部用四位）
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { updateYear(+1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, null, tint = MaterialTheme.colors.primary)
                    }
                    Text(
                        String.format("%02d", year % 100),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    IconButton(onClick = { updateYear(-1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colors.primary)
                    }
                    Text("Y", fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }

                // 小时
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { updateHour(+1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, null, tint = MaterialTheme.colors.primary)
                    }
                    Text(
                        String.format("%02d", hour),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    IconButton(onClick = { updateHour(-1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colors.primary)
                    }
                    Text("h", fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }

                // 分钟
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { updateMinute(+1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, null, tint = MaterialTheme.colors.primary)
                    }
                    Text(
                        String.format("%02d", minute),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    IconButton(onClick = { updateMinute(-1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colors.primary)
                    }
                    Text("m", fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dt = LocalDateTime.of(year, month, day, hour, minute)
                onConfirm(dt)
            }) {
                Text(Pages.BlockSitePage.OK.getLang())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Pages.AddSitePage.Cancel.getLang())
            }
        },
        backgroundColor = MaterialTheme.colors.surface
    )
}
