package org.mrlem.composesample.domain

import kotlinx.coroutines.flow.Flow
import org.mrlem.composesample.data.db.StepDao
import org.mrlem.composesample.data.db.StepEntity
import org.mrlem.composesample.data.db.StepStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepRepository @Inject constructor(
    private val stepDao: StepDao,
) {

    fun observeByProject(projectId: Long): Flow<List<StepEntity>> =
        stepDao.observeByProject(projectId)

    suspend fun create(projectId: Long, title: String): Long =
        stepDao.insert(StepEntity(projectId = projectId, title = title.trim()))

    suspend fun markDone(step: StepEntity) {
        stepDao.update(step.copy(status = StepStatus.DONE, doneAt = System.currentTimeMillis()))
    }

    suspend fun delete(step: StepEntity) = stepDao.delete(step)
}
