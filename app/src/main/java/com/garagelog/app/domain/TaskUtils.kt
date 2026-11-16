package com.garagelog.app.domain

import com.garagelog.app.data.MaintenanceTaskEntity
import com.garagelog.app.data.VehicleEntity
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val DAY_MS = 86_400_000L

data class TaskStatusInfo(
    val task: MaintenanceTaskEntity,
    val vehicle: VehicleEntity,
    val remainingKm: Int?,
    val remainingDays: Int?,
    val isOverdue: Boolean,
    val isDueSoon: Boolean,
    val progress: Float,
    val urgencyScore: Int
)

fun evaluateTask(
    task: MaintenanceTaskEntity,
    vehicle: VehicleEntity,
    nowMillis: Long = System.currentTimeMillis()
): TaskStatusInfo {
    val remKm = task.intervalKm?.let { (task.lastDoneMileage + it) - vehicle.currentMileage }
    val remDays = task.intervalDays?.let { interval ->
        val dueMillis = task.lastDoneDateMillis + interval.toLong() * DAY_MS
        ((dueMillis - nowMillis) / DAY_MS).toInt()
    }

    val overdueKm = remKm != null && remKm <= 0
    val overdueDays = remDays != null && remDays <= 0
    val isOverdue = overdueKm || overdueDays

    val soonKm = remKm != null && remKm <= task.notifyBeforeKm
    val soonDays = remDays != null && remDays <= task.notifyBeforeDays
    val isDueSoon = !isOverdue && (soonKm || soonDays)

    val kmProgress = task.intervalKm?.takeIf { it > 0 }?.let { interval ->
        ((vehicle.currentMileage - task.lastDoneMileage).toFloat() / interval).coerceIn(0f, 1f)
    }
    val dayProgress = task.intervalDays?.takeIf { it > 0 }?.let { interval ->
        ((nowMillis - task.lastDoneDateMillis).toFloat() / (interval.toLong() * DAY_MS)).coerceIn(0f, 1f)
    }
    val progress = listOfNotNull(kmProgress, dayProgress).maxOrNull() ?: 0f

    val kmScore = remKm ?: Int.MAX_VALUE
    val dayScore = remDays?.let { it * 40 } ?: Int.MAX_VALUE
    val urgencyScore = minOf(kmScore, dayScore)

    return TaskStatusInfo(
        task = task,
        vehicle = vehicle,
        remainingKm = remKm,
        remainingDays = remDays,
        isOverdue = isOverdue,
        isDueSoon = isDueSoon,
        progress = progress,
        urgencyScore = urgencyScore
    )
}

fun formatRemainingShort(info: TaskStatusInfo): String {
    val parts = mutableListOf<String>()
    info.remainingKm?.let { km ->
        if (km <= 0) parts += "просрочено на ${formatInt(-km)} км"
        else parts += "осталось ${formatInt(km)} км"
    }
    info.remainingDays?.let { days ->
        if (days <= 0) parts += "просрочено на ${-days} дн."
        else parts += "осталось $days дн."
    }
    return if (parts.isEmpty()) "без интервала" else parts.joinToString(" • ")
}

fun formatInt(value: Int): String {
    val symbols = DecimalFormatSymbols(Locale("ru", "RU")).apply {
        groupingSeparator = ' '
    }
    return DecimalFormat("#,###", symbols).format(value)
}

fun formatMoney(value: Double): String {
    val symbols = DecimalFormatSymbols(Locale("ru", "RU")).apply {
        groupingSeparator = ' '
        decimalSeparator = ','
    }
    val pattern = if (value % 1.0 == 0.0) "#,##0" else "#,##0.00"
    return "${DecimalFormat(pattern, symbols).format(value)} ₽"
}

fun formatDate(millis: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU")).format(Date(millis))
}
