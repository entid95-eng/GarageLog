package com.garagelog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.garagelog.app.data.ExpenseCategory
import com.garagelog.app.data.ExpenseEntity
import com.garagelog.app.data.MaintenanceTaskEntity
import com.garagelog.app.data.MileageLogEntity
import com.garagelog.app.data.VehicleEntity
import com.garagelog.app.data.VehicleType
import com.garagelog.app.domain.DAY_MS
import com.garagelog.app.domain.TaskStatusInfo
import com.garagelog.app.domain.evaluateTask
import com.garagelog.app.domain.formatDate
import com.garagelog.app.domain.formatInt
import com.garagelog.app.domain.formatMoney
import com.garagelog.app.domain.formatRemainingShort
import java.util.Calendar

enum class AppTab(val label: String) {
    GARAGE("Гараж"),
    EXPENSES("Траты"),
    MAINTENANCE("ТО и работы"),
    REPORTS("Отчеты")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageRootScreen(
    viewModel: GarageViewModel,
    openMileageSignal: Int,
    widgetVehicleId: Long?
) {
    val vehicles by viewModel.vehicles.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val mileageLogs by viewModel.mileageLogs.collectAsState()

    var currentTab by remember { mutableStateOf(AppTab.GARAGE) }
    var showAddVehicle by remember { mutableStateOf(false) }
    var mileageTargetVehicle by remember { mutableStateOf<VehicleEntity?>(null) }
    var expenseTargetVehicle by remember { mutableStateOf<VehicleEntity?>(null) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    var completingTask by remember { mutableStateOf<TaskStatusInfo?>(null) }

    LaunchedEffect(openMileageSignal, vehicles) {
        if (openMileageSignal > 0 && vehicles.isNotEmpty()) {
            mileageTargetVehicle = vehicles.firstOrNull { it.id == widgetVehicleId } ?: vehicles.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTab.label, fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    val icon = when (tab) {
                        AppTab.GARAGE -> Icons.Default.DirectionsCar
                        AppTab.EXPENSES -> Icons.Default.ReceiptLong
                        AppTab.MAINTENANCE -> Icons.Default.Build
                        AppTab.REPORTS -> Icons.Default.BarChart
                    }
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) }
                    )
                }
            }
        },
        floatingActionButton = {
            when (currentTab) {
                AppTab.GARAGE -> FloatingActionButton(onClick = { showAddVehicle = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить ТС")
                }
                AppTab.EXPENSES -> if (vehicles.isNotEmpty()) {
                    FloatingActionButton(onClick = {
                        expenseTargetVehicle = vehicles.first()
                        showAddExpense = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить трату")
                    }
                }
                AppTab.MAINTENANCE -> if (vehicles.isNotEmpty()) {
                    FloatingActionButton(onClick = { showAddTask = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
                    }
                }
                AppTab.REPORTS -> Unit
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentTab) {
                AppTab.GARAGE -> GarageTab(
                    vehicles = vehicles,
                    tasks = tasks,
                    onUpdateMileage = { mileageTargetVehicle = it },
                    onAddExpense = {
                        expenseTargetVehicle = it
                        showAddExpense = true
                    },
                    onDeleteVehicle = { viewModel.deleteVehicle(it) }
                )
                AppTab.EXPENSES -> ExpensesTab(
                    vehicles = vehicles,
                    expenses = expenses,
                    onDeleteExpense = { viewModel.deleteExpense(it) }
                )
                AppTab.MAINTENANCE -> MaintenanceTab(
                    vehicles = vehicles,
                    tasks = tasks,
                    onCompleteTask = { completingTask = it },
                    onDeleteTask = { viewModel.deleteTask(it) }
                )
                AppTab.REPORTS -> ReportsTab(
                    vehicles = vehicles,
                    expenses = expenses,
                    mileageLogs = mileageLogs,
                    viewModel = viewModel
                )
            }
        }
    }

    if (showAddVehicle) {
        AddVehicleDialog(
            onDismiss = { showAddVehicle = false },
            onSave = { name, type, mileage ->
                viewModel.addVehicle(name, type, mileage)
                showAddVehicle = false
            }
        )
    }

    mileageTargetVehicle?.let { target ->
        UpdateMileageDialog(
            vehicles = vehicles,
            initialVehicle = target,
            onDismiss = { mileageTargetVehicle = null },
            onSave = { vId, km ->
                viewModel.updateMileage(vId, km)
                mileageTargetVehicle = null
            }
        )
    }

    if (showAddExpense && vehicles.isNotEmpty()) {
        AddExpenseDialog(
            vehicles = vehicles,
            initialVehicle = expenseTargetVehicle ?: vehicles.first(),
            onDismiss = { showAddExpense = false },
            onSave = { vId, amount, cat, km, date, note ->
                viewModel.addExpense(vId, amount, cat, km, date, note)
                showAddExpense = false
            }
        )
    }

    if (showAddTask && vehicles.isNotEmpty()) {
        AddTaskDialog(
            vehicles = vehicles,
            onDismiss = { showAddTask = false },
            onSave = { vId, title, intKm, intDays, lastKm, lastDate, beforeKm, beforeDays ->
                viewModel.addTask(vId, title, intKm, intDays, lastKm, lastDate, beforeKm, beforeDays)
                showAddTask = false
            }
        )
    }

    completingTask?.let { info ->
        CompleteTaskDialog(
            info = info,
            onDismiss = { completingTask = null },
            onConfirm = { km, cost, note ->
                viewModel.completeTask(info.task, km, cost, note)
                completingTask = null
            }
        )
    }
}

@Composable
private fun GarageTab(
    vehicles: List<VehicleEntity>,
    tasks: List<MaintenanceTaskEntity>,
    onUpdateMileage: (VehicleEntity) -> Unit,
    onAddExpense: (VehicleEntity) -> Unit,
    onDeleteVehicle: (VehicleEntity) -> Unit
) {
    var deletingVehicle by remember { mutableStateOf<VehicleEntity?>(null) }

    if (vehicles.isEmpty()) {
        EmptyState("В гараже пока пусто.\nНажмите «+», чтобы добавить автомобиль или мотоцикл.")
        return
    }

    val now = System.currentTimeMillis()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vehicles, key = { it.id }) { vehicle ->
            val vTasks = tasks.filter { it.vehicleId == vehicle.id }
                .map { evaluateTask(it, vehicle, now) }
                .sortedBy { it.urgencyScore }
            val nextTask = vTasks.firstOrNull()
            val daysSinceUpdate = ((now - vehicle.mileageUpdatedAt) / DAY_MS).toInt()

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (vehicle.type == VehicleType.CAR)
                                Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vehicle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(vehicle.type.label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        IconButton(onClick = { deletingVehicle = vehicle }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Пробег: ${formatInt(vehicle.currentMileage)} км",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (daysSinceUpdate == 0) "Обновлен сегодня"
                        else "Обновлен $daysSinceUpdate дн. назад",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (daysSinceUpdate >= 3) Color(0xFFD84315) else Color.Gray
                    )

                    if (nextTask != null) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = when {
                                nextTask.isOverdue -> Color(0xFFFFEBEE)
                                nextTask.isDueSoon -> Color(0xFFFFF8E1)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Ближайшее: ${nextTask.task.title} (${formatRemainingShort(nextTask)})",
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    nextTask.isOverdue -> Color(0xFFC62828)
                                    nextTask.isDueSoon -> Color(0xFFEF6C00)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onUpdateMileage(vehicle) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Пробег")
                        }
                        OutlinedButton(
                            onClick = { onAddExpense(vehicle) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Трата")
                        }
                    }
                }
            }
        }
    }

    deletingVehicle?.let { v ->
        AlertDialog(
            onDismissRequest = { deletingVehicle = null },
            title = { Text("Удалить ${v.name}?") },
            text = { Text("Удалятся также все траты и задачи ТО по этому транспорту.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteVehicle(v)
                    deletingVehicle = null
                }) { Text("Удалить", color = Color(0xFFC62828)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingVehicle = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun ExpensesTab(
    vehicles: List<VehicleEntity>,
    expenses: List<ExpenseEntity>,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    if (vehicles.isEmpty()) {
        EmptyState("Сначала добавьте автомобиль или мотоцикл во вкладке «Гараж».")
        return
    }

    var filterVehicleId by remember { mutableStateOf<Long?>(null) }
    val vehicleMap = vehicles.associateBy { it.id }
    val filtered = expenses.filter { filterVehicleId == null || it.vehicleId == filterVehicleId }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filterVehicleId == null,
                    onClick = { filterVehicleId = null },
                    label = { Text("Все ТС") }
                )
            }
            items(vehicles) { v ->
                FilterChip(
                    selected = filterVehicleId == v.id,
                    onClick = { filterVehicleId = v.id },
                    label = { Text(v.name) }
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyState("Трат пока нет.\nНажмите «+», чтобы записать заправку, ТО или покупку.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { exp ->
                    val vName = vehicleMap[exp.vehicleId]?.name ?: "ТС"
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${exp.category.label} • $vName",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                val kmStr = if (exp.mileageAtExpense > 0) " • ${formatInt(exp.mileageAtExpense)} км" else ""
                                Text(
                                    "${formatDate(exp.dateMillis)}$kmStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                if (exp.note.isNotBlank()) {
                                    Text(exp.note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text(
                                formatMoney(exp.amount),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { onDeleteExpense(exp) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceTab(
    vehicles: List<VehicleEntity>,
    tasks: List<MaintenanceTaskEntity>,
    onCompleteTask: (TaskStatusInfo) -> Unit,
    onDeleteTask: (MaintenanceTaskEntity) -> Unit
) {
    if (vehicles.isEmpty()) {
        EmptyState("Сначала добавьте автомобиль или мотоцикл во вкладке «Гараж».")
        return
    }

    var filterVehicleId by remember { mutableStateOf<Long?>(null) }
    val vehicleMap = vehicles.associateBy { it.id }
    val now = System.currentTimeMillis()

    val evaluated = tasks
        .filter { filterVehicleId == null || it.vehicleId == filterVehicleId }
        .mapNotNull { t -> vehicleMap[t.vehicleId]?.let { v -> evaluateTask(t, v, now) } }
        .sortedBy { it.urgencyScore }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filterVehicleId == null,
                    onClick = { filterVehicleId = null },
                    label = { Text("Все ТС") }
                )
            }
            items(vehicles) { v ->
                FilterChip(
                    selected = filterVehicleId == v.id,
                    onClick = { filterVehicleId = v.id },
                    label = { Text(v.name) }
                )
            }
        }

        if (evaluated.isEmpty()) {
            EmptyState("Задач ТО пока нет.\nНажмите «+», чтобы добавить замену масла, сход-развал или другие работы.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(evaluated, key = { it.task.id }) { info ->
                    val accentColor = when {
                        info.isOverdue -> Color(0xFFC62828)
                        info.isDueSoon -> Color(0xFFEF6C00)
                        else -> Color(0xFF2E7D32)
                    }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        info.task.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        info.vehicle.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(onClick = { onDeleteTask(info.task) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Gray)
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = formatRemainingShort(info).replaceFirstChar { it.uppercase() },
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { info.progress },
                                modifier = Modifier.fillMaxWidth().height(7.dp),
                                color = accentColor
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Последний раз: ${formatInt(info.task.lastDoneMileage)} км (${formatDate(info.task.lastDoneDateMillis)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { onCompleteTask(info) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Отметить выполненным")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsTab(
    vehicles: List<VehicleEntity>,
    expenses: List<ExpenseEntity>,
    mileageLogs: List<MileageLogEntity>,
    viewModel: GarageViewModel
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var filterVehicleId by remember { mutableStateOf<Long?>(null) }

    val report = remember(selectedYear, filterVehicleId, vehicles, expenses, mileageLogs) {
        viewModel.buildAnnualReport(selectedYear, filterVehicleId, vehicles, expenses, mileageLogs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { selectedYear-- }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий год")
            }
            Text(
                "$selectedYear год",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            IconButton(onClick = { selectedYear++ }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий год")
            }
        }

        if (vehicles.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterVehicleId == null,
                        onClick = { filterVehicleId = null },
                        label = { Text("Все ТС") }
                    )
                }
                items(vehicles) { v ->
                    FilterChip(
                        selected = filterVehicleId == v.id,
                        onClick = { filterVehicleId = v.id },
                        label = { Text(v.name) }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Итого за $selectedYear год", style = MaterialTheme.typography.titleMedium)
                Text(
                    formatMoney(report.totalAmount),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Записей расходов:", color = Color.Gray)
                    Text("${report.expenseCount}", fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Пробег за год (по записям):", color = Color.Gray)
                    Text("${formatInt(report.drivenKm)} км", fontWeight = FontWeight.SemiBold)
                }
                report.costPerKm?.let { cpk ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Стоимость 1 км:", color = Color.Gray)
                        Text(formatMoney(cpk), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (report.byCategory.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("По категориям", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    report.byCategory.forEach { item ->
                        val fraction = if (report.totalAmount > 0) (item.total / report.totalAmount).toFloat() else 0f
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.category.label)
                                Text(formatMoney(item.total), fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (report.byVehicle.isNotEmpty() && filterVehicleId == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("По транспорту", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    report.byVehicle.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.vehicle.name)
                            Text(formatMoney(item.total), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (report.expenseCount == 0) {
            Text(
                "За $selectedYear год расходов не найдено.",
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onSave: (String, VehicleType, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(VehicleType.CAR) }
    var mileage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить транспорт") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VehicleType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название (напр. Kia Rio / MT-07)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = mileage,
                    onValueChange = { mileage = it.filter(Char::isDigit) },
                    label = { Text("Текущий пробег (км)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onSave(name, type, mileage.toIntOrNull() ?: 0) }
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun UpdateMileageDialog(
    vehicles: List<VehicleEntity>,
    initialVehicle: VehicleEntity,
    onDismiss: () -> Unit,
    onSave: (Long, Int) -> Unit
) {
    var selectedVehicle by remember { mutableStateOf(initialVehicle) }
    var mileage by remember(selectedVehicle.id) {
        mutableStateOf(selectedVehicle.currentMileage.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Обновить пробег") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (vehicles.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(vehicles) { v ->
                            FilterChip(
                                selected = selectedVehicle.id == v.id,
                                onClick = { selectedVehicle = v },
                                label = { Text(v.name) }
                            )
                        }
                    }
                } else {
                    Text(selectedVehicle.name, fontWeight = FontWeight.SemiBold)
                }
                OutlinedTextField(
                    value = mileage,
                    onValueChange = { mileage = it.filter(Char::isDigit) },
                    label = { Text("Новый пробег (км)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val km = mileage.toIntOrNull()
            Button(
                enabled = km != null && km >= selectedVehicle.currentMileage,
                onClick = { if (km != null) onSave(selectedVehicle.id, km) }
            ) { Text("Обновить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddExpenseDialog(
    vehicles: List<VehicleEntity>,
    initialVehicle: VehicleEntity,
    onDismiss: () -> Unit,
    onSave: (Long, Double, ExpenseCategory, Int, Long, String) -> Unit
) {
    var selectedVehicle by remember { mutableStateOf(initialVehicle) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.FUEL) }
    var mileage by remember(selectedVehicle.id) {
        mutableStateOf(selectedVehicle.currentMileage.toString())
    }
    var note by remember { mutableStateOf("") }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая трата") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (vehicles.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(vehicles) { v ->
                            FilterChip(
                                selected = selectedVehicle.id == v.id,
                                onClick = { selectedVehicle = v },
                                label = { Text(v.name) }
                            )
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.replace(',', '.') },
                    label = { Text("Сумма (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = mileage,
                    onValueChange = { mileage = it.filter(Char::isDigit) },
                    label = { Text("Пробег при оплате (км)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Дата: ${formatDate(dateMillis)}")
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Комментарий (необязательно)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val parsedAmount = amount.toDoubleOrNull()
            Button(
                enabled = parsedAmount != null && parsedAmount > 0.0,
                onClick = {
                    if (parsedAmount != null) {
                        onSave(
                            selectedVehicle.id,
                            parsedAmount,
                            category,
                            mileage.toIntOrNull() ?: 0,
                            dateMillis,
                            note
                        )
                    }
                }
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTaskDialog(
    vehicles: List<VehicleEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, String, Int?, Int?, Int, Long, Int, Int) -> Unit
) {
    val presets = listOf("Замена масла", "Сход-развал", "Тормозные колодки", "Фильтры", "Цепь / ГРМ", "Страховка")
    var selectedVehicle by remember { mutableStateOf(vehicles.first()) }
    var title by remember { mutableStateOf("") }
    var intervalKm by remember { mutableStateOf("10000") }
    var intervalDays by remember { mutableStateOf("365") }
    var lastMileage by remember(selectedVehicle.id) {
        mutableStateOf(selectedVehicle.currentMileage.toString())
    }
    var notifyBeforeKm by remember { mutableStateOf("500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Напоминание о ТО") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (vehicles.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(vehicles) { v ->
                            FilterChip(
                                selected = selectedVehicle.id == v.id,
                                onClick = { selectedVehicle = v },
                                label = { Text(v.name) }
                            )
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = title == preset,
                            onClick = { title = preset },
                            label = { Text(preset) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название работы") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = intervalKm,
                    onValueChange = { intervalKm = it.filter(Char::isDigit) },
                    label = { Text("Интервал в км (пусто — не учитывать)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = intervalDays,
                    onValueChange = { intervalDays = it.filter(Char::isDigit) },
                    label = { Text("Интервал в днях (пусто — не учитывать)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lastMileage,
                    onValueChange = { lastMileage = it.filter(Char::isDigit) },
                    label = { Text("На каком пробеге делали в прошлый раз") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notifyBeforeKm,
                    onValueChange = { notifyBeforeKm = it.filter(Char::isDigit) },
                    label = { Text("Предупредить за (км)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val kmInt = intervalKm.toIntOrNull()?.takeIf { it > 0 }
            val daysInt = intervalDays.toIntOrNull()?.takeIf { it > 0 }
            Button(
                enabled = title.isNotBlank() && (kmInt != null || daysInt != null),
                onClick = {
                    onSave(
                        selectedVehicle.id,
                        title,
                        kmInt,
                        daysInt,
                        lastMileage.toIntOrNull() ?: selectedVehicle.currentMileage,
                        System.currentTimeMillis(),
                        notifyBeforeKm.toIntOrNull() ?: 500,
                        14
                    )
                }
            ) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun CompleteTaskDialog(
    info: TaskStatusInfo,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double?, String) -> Unit
) {
    var mileage by remember { mutableStateOf(info.vehicle.currentMileage.toString()) }
    var cost by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выполнено: ${info.task.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${info.vehicle.name} — отсчет сбросится на текущий пробег и сегодняшнюю дату.")
                OutlinedTextField(
                    value = mileage,
                    onValueChange = { mileage = it.filter(Char::isDigit) },
                    label = { Text("Пробег выполнения (км)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it.replace(',', '.') },
                    label = { Text("Стоимость в ₽ (если хотите сразу записать в траты)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка (необязательно)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    mileage.toIntOrNull() ?: info.vehicle.currentMileage,
                    cost.toDoubleOrNull(),
                    note
                )
            }) { Text("Подтвердить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
