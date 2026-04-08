package org.mrlem.composesample.today

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun schedule(entity: ScheduledStepEntity, stepTitle: String) {
        if (!entity.notificationEnabled || entity.startTime == null) return
        val triggerTime = calculateTriggerTime(entity.date, entity.startTime)
        if (triggerTime <= System.currentTimeMillis()) return

        val intent = Intent(context, StepNotificationReceiver::class.java).apply {
            putExtra(StepNotificationReceiver.EXTRA_STEP_TITLE, stepTitle)
            putExtra(StepNotificationReceiver.EXTRA_SCHEDULED_STEP_ID, entity.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entity.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancel(scheduledStepId: String) {
        val intent = Intent(context, StepNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduledStepId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent)
    }

    private fun calculateTriggerTime(date: String, startTimeMinutes: Int): Long {
        val parts = date.split("-")
        val calendar = Calendar.getInstance().apply {
            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(),
                startTimeMinutes / 60, startTimeMinutes % 60, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
