package com.garagelog.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class VehicleType(val label: String) {
    CAR("Автомобиль"),
    MOTORCYCLE("Мотоцикл")
}

enum class ExpenseCategory(val label: String) {
    FUEL("Топливо"),
    MAINTENANCE("ТО и сервис"),
    PARTS("Запчасти"),
    INSURANCE("Страховка / Налог"),
    OTHER("Прочее")
}

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: VehicleType,
    val currentMileage: Int,
    val mileageUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateMillis: Long,
    val amount: Double,
    val category: ExpenseCategory,
    val mileageAtExpense: Int,
    val note: String = ""
)

@Entity(
    tableName = "maintenance_tasks",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class MaintenanceTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val title: String,
    val intervalKm: Int? = null,
    val intervalDays: Int? = null,
    val lastDoneMileage: Int,
    val lastDoneDateMillis: Long,
    val notifyBeforeKm: Int = 500,
    val notifyBeforeDays: Int = 14
)

@Entity(
    tableName = "mileage_logs",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class MileageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val mileage: Int,
    val dateMillis: Long
)

class Converters {
    @TypeConverter
    fun fromVehicleType(value: VehicleType): String = value.name

    @TypeConverter
    fun toVehicleType(value: String): VehicleType = VehicleType.valueOf(value)

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.valueOf(value)
}
