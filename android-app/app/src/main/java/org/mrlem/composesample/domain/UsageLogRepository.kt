package org.mrlem.composesample.domain

import org.mrlem.composesample.data.db.UsageLogDao
import org.mrlem.composesample.data.db.UsageLogEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageLogRepository @Inject constructor(
    private val usageLogDao: UsageLogDao,
) {

    suspend fun record(event: String, metadata: String? = null) {
        usageLogDao.insert(UsageLogEntity(event = event, metadata = metadata))
    }

    suspend fun queryFrom(fromMs: Long): List<UsageLogEntity> =
        usageLogDao.queryFrom(fromMs)
}
