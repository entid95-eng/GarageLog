package com.garagelog.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.garagelog.app.data.AppDatabase
import com.garagelog.app.data.ExpenseCategory
import com.garagelog.app.data.ExpenseEntity
import com.garagelog.app.data.MaintenanceTaskEntity
import com.garagelog.app.data.MileageLogEntity
import com.garagelog.app.data.VehicleEntity
import com.garagelog.app.data.VehicleType
import com.garagelog.app.widget.GarageWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategoryReportItem(
    val category: ExpenseCategory,
    val total: Double
)

data class VehicleReportItem(
    val vehicle: VehicleEntity,
    val total: Double
)

data class AnnualReport(
    val year: Int,
    val totalAmount: Double,
    val expenseCount: Int,
    val drivenKm: Int,
    val costPerKm: Double?,
    val byCategory: List<CategoryReportItem>,
    val byVehicle: List<VehicleReportItem>
)

class GarageViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).garageDao()

    val vehicles = dao.observeVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses = dao.observeExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks = dao.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mileageLogs = dao.observeMileageLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun refreshWidget() {
        GarageWidgetProvider.updateAllWidgets(getApplication())
    }

    fun addVehicle(name: String, type: VehicleType, currentMileage: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = dao.insertVehicle(
                VehicleEntity(
                    name = name.trim(),
                    type = type,
                    currentMileage = currentMileage,
                    mileageUpdatedAt = now
                )
            )
            dao.insertMileageLog(
                MileageLogEntity(vehicleId = id, mileage = currentMileage, dateMillis = now)
            )
            refreshWidget()
        }
    }

    fun deleteVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch {
            dao.deleteVehicle(vehicle)
            refreshWidget()
        }
    }

    fun updateMileage(vehicleId: Long, newMileage: Int) {
        viewModelScope.launch {
            val vehicle = dao.getVehicleById(vehicleId) ?: return@launch
            val now = System.currentTimeMillis()
            dao.updateVehicle(
                vehicle.copy(
                    currentMileage = maxOf(vehicle.currentMileage, newMileage),
                    mileageUpdatedAt = now
                )
            )
            dao.insertMileageLog(
                MileageLogEntity(vehicleId = vehicleId, mileage = newMileage, dateMillis = now)
            )
            refreshWidget()
        }
    }

    fun addExpense(
        vehicleId: Long,
        amount: Double,
        category: ExpenseCategory,
        mileage: Int,
        dateMillis: Long,
        note: String
    ) {
        viewModelScope.launch {
            dao.insertExpense(
                ExpenseEntity(
                    vehicleId = vehicleId,
                    dateMillis = dateMillis,
                    amount = amount,
                    category = category,
                    mileageAtExpense = mileage,
                    note = note.trim()
                )
            )
            val vehicle = dao.getVehicleById(vehicleId)
            if (vehicle != null && mileage > vehicle.currentMileage) {
                dao.updateVehicle(
                    vehicle.copy(
                        currentMileage = mileage,
                        mileageUpdatedAt = System.currentTimeMillis()
                    )
                )
            }
            if (mileage > 0) {
                dao.insertMileageLog(
                    MileageLogEntity(vehicleId = vehicleId, mileage = mileage, dateMillis = dateMillis)
                )
            }
            refreshWidget()
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            dao.deleteExpense(expense)
        }
    }

    fun addTask(
        vehicleId: Long,
        title: String,
        intervalKm: Int?,
        intervalDays: Int?,
        lastDoneMileage: Int,
        lastDoneDateMillis: Long,
        notifyBeforeKm: Int,
        notifyBeforeDays: Int
    ) {
        viewModelScope.launch {
            dao.insertTask(
                MaintenanceTaskEntity(
                    vehicleId = vehicleId,
                    title = title.trim(),
                    intervalKm = intervalKm,
                    intervalDays = intervalDays,
                    lastDoneMileage = lastDoneMileage,
                    lastDoneDateMillis = lastDoneDateMillis,
                    notifyBeforeKm = notifyBeforeKm,
                    notifyBeforeDays = notifyBeforeDays
                )
            )
            refreshWidget()
        }
    }

    fun completeTask(
        task: MaintenanceTaskEntity,
        currentMileage: Int,
        costAmount: Double?,
        note: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.updateTask(
                task.copy(
                    lastDoneMileage = currentMileage,
                    lastDoneDateMillis = now
                )
            )
            val vehicle = dao.getVehicleById(task.vehicleId)
            if (vehicle != null && currentMileage >= vehicle.currentMileage) {
                dao.updateVehicle(
                    vehicle.copy(
                        currentMileage = currentMileage,
                        mileageUpdatedAt = now
                    )
                )
            }
            dao.insertMileageLog(
                MileageLogEntity(vehicleId = task.vehicleId, mileage = currentMileage, dateMillis = now)
            )
            if (costAmount != null && costAmount > 0.0) {
                dao.insertExpense(
                    ExpenseEntity(
                        vehicleId = task.vehicleId,
                        dateMillis = now,
                        amount = costAmount,
                        category = ExpenseCategory.MAINTENANCE,
                        mileageAtExpense = currentMileage,
                        note = if (note.isBlank()) task.title else "${task.title}: ${note.trim()}"
                    )
                )
            }
            refreshWidget()
        }
    }

    fun deleteTask(task: MaintenanceTaskEntity) {
        viewModelScope.launch {
            dao.deleteTask(task)
            refreshWidget()
        }
    }

    fun buildAnnualReport(
        year: Int,
        vehicleFilterId: Long?,
        allVehicles: List<VehicleEntity>,
        allExpenses: List<ExpenseEntity>,
        allMileageLogs: List<MileageLogEntity>
    ): AnnualReport {
        val cal = Calendar.getInstance()
        fun yearOf(millis: Long): Int {
            cal.timeInMillis = millis
            return cal.get(Calendar.YEAR)
        }

        val yearExpenses = allExpenses.filter {
            yearOf(it.dateMillis) == year &&
                (vehicleFilterId == null || it.vehicleId == vehicleFilterId)
        }

        val total = yearExpenses.sumOf { it.amount }

        val byCategory = ExpenseCategory.entries.mapNotNull { cat ->
            val sum = yearExpenses.filter { it.category == cat }.sumOf { it.amount }
            if (sum > 0.0) CategoryReportItem(cat, sum) else null
        }.sortedByDescending { it.total }

        val byVehicle = allVehicles.mapNotNull { v ->
            if (vehicleFilterId != null && v.id != vehicleFilterId) return@mapNotNull null
            val sum = yearExpenses.filter { it.vehicleId == v.id }.sumOf { it.amount }
            if (sum > 0.0) VehicleReportItem(v, sum) else null
        }.sortedByDescending { it.total }

        val targetVehicles = if (vehicleFilterId == null) {
            allVehicles.map { it.id }
        } else {
            listOf(vehicleFilterId)
        }

        var drivenKm = 0
        for (vId in targetVehicles) {
            val points = mutableListOf<Int>()
            allMileageLogs
                .filter { it.vehicleId == vId && yearOf(it.dateMillis) == year }
                .forEach { points += it.mileage }
            yearExpenses
                .filter { it.vehicleId == vId && it.mileageAtExpense > 0 }
                .forEach { points += it.mileageAtExpense }
            if (points.size >= 2) {
                drivenKm += (points.max() - points.min()).coerceAtLeast(0)
            }
        }

        val costPerKm = if (drivenKm > 0 && total > 0.0) total / drivenKm else null

        return AnnualReport(
            year = year,
            totalAmount = total,
            expenseCount = yearExpenses.size,
            drivenKm = drivenKm,
            costPerKm = costPerKm,
            byCategory = byCategory,
            byVehicle = byVehicle
        )
    }
}
