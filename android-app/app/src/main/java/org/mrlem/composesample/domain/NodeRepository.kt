package org.mrlem.composesample.domain

import kotlinx.coroutines.flow.Flow
import org.mrlem.composesample.data.db.NodeDao
import org.mrlem.composesample.data.db.NodeEntity
import org.mrlem.composesample.data.db.NodeStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeRepository @Inject constructor(
    private val nodeDao: NodeDao,
) {

    fun observeAll(): Flow<List<NodeEntity>> = nodeDao.observeAll()

    fun observeByStatus(status: NodeStatus): Flow<List<NodeEntity>> =
        nodeDao.observeByStatus(status)

    fun observeUnorganized(): Flow<List<NodeEntity>> = nodeDao.observeUnorganized()

    suspend fun updateNode(node: NodeEntity, title: String, body: String) {
        nodeDao.update(node.copy(title = title.trim(), body = body.trim()))
    }

    suspend fun deleteNode(node: NodeEntity) {
        nodeDao.delete(node)
    }

    suspend fun createNode(title: String): Long {
        return nodeDao.insert(NodeEntity(title = title.trim()))
    }

    suspend fun updateStatus(node: NodeEntity, status: NodeStatus) {
        val updated = when (status) {
            NodeStatus.ACTIVE -> node.copy(status = status, startedAt = System.currentTimeMillis())
            NodeStatus.DONE -> node.copy(status = status, doneAt = System.currentTimeMillis())
            else -> node.copy(status = status)
        }
        nodeDao.update(updated)
    }
}
