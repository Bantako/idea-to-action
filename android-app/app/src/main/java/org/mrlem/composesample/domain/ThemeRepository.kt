package org.mrlem.composesample.domain

import kotlinx.coroutines.flow.Flow
import org.mrlem.composesample.data.db.NodeDao
import org.mrlem.composesample.data.db.ThemeDao
import org.mrlem.composesample.data.db.ThemeEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    private val themeDao: ThemeDao,
    private val nodeDao: NodeDao,
) {

    fun observeAll(): Flow<List<ThemeEntity>> = themeDao.observeAll()

    suspend fun create(name: String): Long =
        themeDao.insert(ThemeEntity(name = name.trim()))

    suspend fun rename(theme: ThemeEntity, newName: String) =
        themeDao.update(theme.copy(name = newName.trim()))

    suspend fun delete(theme: ThemeEntity) {
        // テーマ削除時は所属ノードの themeId を null に戻す
        nodeDao.clearTheme(theme.id)
        themeDao.delete(theme)
    }

    suspend fun assignNode(nodeId: Long, themeId: Long?) =
        nodeDao.updateTheme(nodeId, themeId)

    suspend fun assignNodes(nodeIds: List<Long>, themeId: Long?) {
        nodeIds.forEach { nodeDao.updateTheme(it, themeId) }
    }
}
