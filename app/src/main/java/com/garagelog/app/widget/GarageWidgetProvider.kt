package com.garagelog.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.garagelog.app.MainActivity
import com.garagelog.app.R
import com.garagelog.app.data.AppDatabase
import com.garagelog.app.data.VehicleType
import com.garagelog.app.domain.DAY_MS
import com.garagelog.app.domain.evaluateTask
import com.garagelog.app.domain.formatInt
import com.garagelog.app.domain.formatRemainingShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GarageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).garageDao()
                val vehicles = dao.getAllVehicles()
                val tasks = dao.getAllTasks()
                val now = System.currentTimeMillis()

                val vehicleMap = vehicles.associateBy { it.id }
                val evaluated = tasks.mapNotNull { t ->
                    val v = vehicleMap[t.vehicleId] ?: return@mapNotNull null
                    evaluateTask(t, v, now)
                }.sortedBy { it.urgencyScore }

                val primaryVehicle = evaluated.firstOrNull()?.vehicle ?: vehicles.firstOrNull()

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_garage)

                    val openAppIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val openAppPending = PendingIntent.getActivity(
                        context,
                        10,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, openAppPending)

                    val openMileageIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(EXTRA_OPEN_MILEAGE, true)
                        primaryVehicle?.let { putExtra(EXTRA_VEHICLE_ID, it.id) }
                    }
                    val openMileagePending = PendingIntent.getActivity(
                        context,
                        11,
                        openMileageIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_update_mileage, openMileagePending)

                    if (primaryVehicle == null) {
                        views.setTextViewText(R.id.widget_vehicle_title, "Гараж пуст")
                        views.setTextViewText(R.id.widget_mileage_reminder, "Откройте приложение и добавьте ТС")
                        views.setTextColor(R.id.widget_mileage_reminder, Color.parseColor("#FFD54F"))
                        views.setTextViewText(R.id.widget_next_task_1, "Нажмите, чтобы начать учет")
                        views.setViewVisibility(R.id.widget_next_task_2, View.GONE)
                    } else {
                        val icon = if (primaryVehicle.type == VehicleType.CAR) "🚗" else "🏍️"
                        views.setTextViewText(
                            R.id.widget_vehicle_title,
                            "$icon ${primaryVehicle.name} • ${formatInt(primaryVehicle.currentMileage)} км"
                        )

                        val daysAgo = ((now - primaryVehicle.mileageUpdatedAt) / DAY_MS).toInt()
                        if (daysAgo >= 3) {
                            views.setTextViewText(
                                R.id.widget_mileage_reminder,
                                "⚠ Внесите пробег (не обновлялся $daysAgo дн.)"
                            )
                            views.setTextColor(R.id.widget_mileage_reminder, Color.parseColor("#FFD54F"))
                        } else {
                            val label = if (daysAgo == 0) "Пробег обновлен сегодня" else "Пробег обновлен $daysAgo дн. назад"
                            views.setTextViewText(R.id.widget_mileage_reminder, label)
                            views.setTextColor(R.id.widget_mileage_reminder, Color.parseColor("#A5D6A7"))
                        }

                        val topTasks = evaluated.take(2)
                        if (topTasks.isEmpty()) {
                            views.setTextViewText(R.id.widget_next_task_1, "Нет задач ТО — добавьте в приложении")
                            views.setTextColor(R.id.widget_next_task_1, Color.parseColor("#E0E6ED"))
                            views.setViewVisibility(R.id.widget_next_task_2, View.GONE)
                        } else {
                            val first = topTasks[0]
                            val prefix1 = if (vehicles.size > 1) "${first.vehicle.name}: " else ""
                            views.setTextViewText(
                                R.id.widget_next_task_1,
                                "$prefix1${first.task.title} — ${formatRemainingShort(first)}"
                            )
                            val color1 = when {
                                first.isOverdue -> Color.parseColor("#FF8A80")
                                first.isDueSoon -> Color.parseColor("#FFD54F")
                                else -> Color.parseColor("#E0E6ED")
                            }
                            views.setTextColor(R.id.widget_next_task_1, color1)

                            if (topTasks.size > 1) {
                                val second = topTasks[1]
                                val prefix2 = if (vehicles.size > 1) "${second.vehicle.name}: " else ""
                                views.setViewVisibility(R.id.widget_next_task_2, View.VISIBLE)
                                views.setTextViewText(
                                    R.id.widget_next_task_2,
                                    "$prefix2${second.task.title} — ${formatRemainingShort(second)}"
                                )
                            } else {
                                views.setViewVisibility(R.id.widget_next_task_2, View.GONE)
                            }
                        }
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_MILEAGE = "extra_open_mileage"
        const val EXTRA_VEHICLE_ID = "extra_vehicle_id"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, GarageWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, GarageWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
