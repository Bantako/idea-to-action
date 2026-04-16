package org.mrlem.composesample.domain

import kotlinx.coroutines.flow.Flow
import org.mrlem.composesample.data.db.DailyLogDao
import org.mrlem.composesample.data.db.DailyLogEntity
import org.mrlem.composesample.data.db.StepEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao,
) {

    fun observeToday(): Flow<List<DailyLogEntity>> =
        dailyLogDao.observeByDate(today())

    fun observeByProject(projectId: Long): Flow<List<DailyLogEntity>> =
        dailyLogDao.observeByProject(projectId)

    suspend fun createFromStep(step: StepEntity) {
        dailyLogDao.insert(
            DailyLogEntity(
                date = today(),
                stepId = step.id,
                what = step.title,
            )
        )
    }

    suspend fun createManual(what: String) {
        dailyLogDao.insert(
            DailyLogEntity(
                date = today(),
                what = what.trim(),
            )
        )
    }

    suspend fun delete(log: DailyLogEntity) = dailyLogDao.delete(log)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
