package com.garagelog.app.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.garagelog.app.GarageApp
import com.garagelog.app.MainActivity
import com.garagelog.app.R
import com.garagelog.app.data.AppDatabase
import com.garagelog.app.domain.DAY_MS
import com.garagelog.app.domain.evaluateTask
import com.garagelog.app.domain.formatRemainingShort
import com.garagelog.app.widget.GarageWidgetProvider
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).garageDao()
        val vehicles = dao.getAllVehicles().associateBy { it.id }
        val tasks = dao.getAllTasks()
        val now = System.currentTimeMillis()

        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (canNotify) {
            val manager = NotificationManagerCompat.from(applicationContext)

            tasks.mapNotNull { task ->
                val v = vehicles[task.vehicleId] ?: return@mapNotNull null
                evaluateTask(task, v, now)
            }.filter { it.isOverdue || it.isDueSoon }
                .forEachIndexed { idx, info ->
                    val prefix = if (info.isOverdue) "СРОЧНО (просрочено)" else "Скоро ТО"
                    sendNotification(
                        manager = manager,
                        id = 1000 + info.task.id.toInt() + idx,
                        title = "$prefix: ${info.vehicle.name} — ${info.task.title}",
                        text = formatRemainingShort(info).replaceFirstChar { it.uppercase() },
                        openMileage = false
                    )
                }

            vehicles.values.forEach { vehicle ->
                val daysSinceUpdate = ((now - vehicle.mileageUpdatedAt) / DAY_MS).toInt()
                if (daysSinceUpdate >= 7) {
                    sendNotification(
                        manager = manager,
                        id = 2000 + vehicle.id.toInt(),
                        title = "Внесите пробег: ${vehicle.name}",
                        text = "Пробег не обновлялся уже $daysSinceUpdate дн.",
                        openMileage = true
                    )
                }
            }
        }

        GarageWidgetProvider.updateAllWidgets(applicationContext)
        return Result.success()
    }

    private fun sendNotification(
        manager: NotificationManagerCompat,
        id: Int,
        title: String,
        text: String,
        openMileage: Boolean
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(GarageWidgetProvider.EXTRA_OPEN_MILEAGE, openMileage)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            GarageApp.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_fg)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
        }
    }

    companion object {
        private const val WORK_NAME = "garage_daily_reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
