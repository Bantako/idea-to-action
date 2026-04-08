package org.mrlem.composesample.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.mrlem.composesample.theme.Theme as AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StepScheduleSheet(
    onDismiss: () -> Unit,
    onConfirm: (date: String, startTime: Int?, durationMinutes: Int?, notificationEnabled: Boolean) -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val today = remember { dateFormat.format(Date()) }
    val tomorrow = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        dateFormat.format(cal.time)
    }
    var selectedDate by remember { mutableStateOf(today) }
    var startEnabled by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf("") }
    var startMinute by remember { mutableStateOf("") }
    var durationEnabled by remember { mutableStateOf(false) }
    var durationInput by remember { mutableStateOf("") }
    var notificationEnabled by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.size.medium)
                .navigationBarsPadding(),
        ) {
            Text("スケジュールに追加", style = MaterialTheme.typography.titleMedium)

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.size.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.size.medium),
            ) {
                listOf("今日" to today, "明日" to tomorrow).forEach { (label, date) ->
                    if (selectedDate == date) {
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                        ) { Text(label) }
                    } else {
                        OutlinedButton(
                            onClick = { selectedDate = date },
                            modifier = Modifier.weight(1f),
                        ) { Text(label) }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.size.medium),
            ) {
                Text("開始時刻を設定する", modifier = Modifier.weight(1f))
                Switch(checked = startEnabled, onCheckedChange = { startEnabled = it })
            }
            if (startEnabled) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.size.small),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTheme.size.small),
                ) {
                    TextField(
                        value = startHour,
                        onValueChange = { v ->
                            startHour = v.filter { it.isDigit() }.take(2)
                        },
                        label = { Text("時") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = startMinute,
                        onValueChange = { v ->
                            startMinute = v.filter { it.isDigit() }.take(2)
                        },
                        label = { Text("分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.size.medium),
            ) {
                Text("所要時間を設定する", modifier = Modifier.weight(1f))
                Switch(checked = durationEnabled, onCheckedChange = { durationEnabled = it })
            }
            if (durationEnabled) {
                TextField(
                    value = durationInput,
                    onValueChange = { v ->
                        durationInput = v.filter { it.isDigit() }
                    },
                    label = { Text("分") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTheme.size.small),
                )
            }

            if (startEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTheme.size.medium),
                ) {
                    Text("開始時刻に通知する", modifier = Modifier.weight(1f))
                    Switch(checked = notificationEnabled, onCheckedChange = { notificationEnabled = it })
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.size.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.size.medium),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("キャンセル")
                }
                Button(
                    onClick = {
                        val startTime = if (startEnabled) {
                            val h = startHour.toIntOrNull()?.coerceIn(0, 23) ?: 0
                            val m = startMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            h * 60 + m
                        } else null
                        val duration = if (durationEnabled) durationInput.toIntOrNull() else null
                        onConfirm(selectedDate, startTime, duration, notificationEnabled && startEnabled)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("追加")
                }
            }
        }
    }
}
