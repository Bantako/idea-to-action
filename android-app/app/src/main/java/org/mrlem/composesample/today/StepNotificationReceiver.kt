package org.mrlem.composesample.today

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.mrlem.composesample.Application
import org.mrlem.composesample.R

class StepNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_STEP_TITLE = "stepTitle"
        const val EXTRA_SCHEDULED_STEP_ID = "scheduledStepId"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val stepTitle = intent.getStringExtra(EXTRA_STEP_TITLE) ?: return
        val scheduledStepId = intent.getStringExtra(EXTRA_SCHEDULED_STEP_ID) ?: return

        val notification = NotificationCompat.Builder(context, Application.CHANNEL_TASK)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("着手する時間です")
            .setContentText(stepTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(scheduledStepId.hashCode(), notification)
    }
}
