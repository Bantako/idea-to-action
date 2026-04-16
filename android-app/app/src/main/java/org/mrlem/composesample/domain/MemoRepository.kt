package org.mrlem.composesample.domain

import kotlinx.coroutines.flow.Flow
import org.mrlem.composesample.data.db.MemoDao
import org.mrlem.composesample.data.db.MemoEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoRepository @Inject constructor(
    private val memoDao: MemoDao,
) {

    fun observeUnorganized(): Flow<List<MemoEntity>> = memoDao.observeUnorganized()

    fun observeByProject(projectId: Long): Flow<List<MemoEntity>> =
        memoDao.observeByProject(projectId)

    suspend fun create(text: String): Long =
        memoDao.insert(MemoEntity(text = text.trim()))

    suspend fun delete(memo: MemoEntity) = memoDao.delete(memo)

    suspend fun linkToProject(memo: MemoEntity, projectId: Long) {
        memoDao.update(memo.copy(projectId = projectId))
    }
}
