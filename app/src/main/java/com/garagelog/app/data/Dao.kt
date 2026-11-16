package com.garagelog.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GarageDao {
    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    fun observeVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    suspend fun getAllVehicles(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Long): VehicleEntity?

    @Insert
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC, id DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM maintenance_tasks ORDER BY id ASC")
    fun observeTasks(): Flow<List<MaintenanceTaskEntity>>

    @Query("SELECT * FROM maintenance_tasks")
    suspend fun getAllTasks(): List<MaintenanceTaskEntity>

    @Insert
    suspend fun insertTask(task: MaintenanceTaskEntity): Long

    @Update
    suspend fun updateTask(task: MaintenanceTaskEntity)

    @Delete
    suspend fun deleteTask(task: MaintenanceTaskEntity)

    @Query("SELECT * FROM mileage_logs ORDER BY dateMillis ASC, mileage ASC")
    fun observeMileageLogs(): Flow<List<MileageLogEntity>>

    @Insert
    suspend fun insertMileageLog(log: MileageLogEntity): Long
}
