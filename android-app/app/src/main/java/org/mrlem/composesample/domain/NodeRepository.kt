package org.mrlem.composesample.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.mrlem.composesample.data.db.EdgeDao
import org.mrlem.composesample.data.db.EdgeEntity
import org.mrlem.composesample.data.db.EdgeType
import org.mrlem.composesample.data.db.NodeDao
import org.mrlem.composesample.data.db.NodeEntity
import org.mrlem.composesample.data.db.NodeStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeRepository @Inject constructor(
    private val nodeDao: NodeDao,
    private val edgeDao: EdgeDao,
) {

    fun observeAll(): Flow<List<NodeEntity>> = nodeDao.observeAll()

    fun observeByStatus(status: NodeStatus): Flow<List<NodeEntity>> =
        nodeDao.observeByStatus(status)

    fun observeEdges(): Flow<List<EdgeEntity>> = edgeDao.observeAll()

    /**
     * ACTIVE ノードと、着手可能（IDEA かつ全 PREREQUISITE が DONE）なノードを返す。
     */
    fun observeActionable(): Flow<Pair<List<NodeEntity>, List<NodeEntity>>> =
        combine(nodeDao.observeAll(), edgeDao.observeAll()) { nodes, edges ->
            val nodeMap = nodes.associateBy { it.id }
            val incomingPrereqs = edges
                .filter { it.type == EdgeType.PREREQUISITE }
                .groupBy { it.toId }

            val active = nodes.filter { it.status == NodeStatus.ACTIVE }
            val ready = nodes.filter { node ->
                node.status == NodeStatus.IDEA &&
                    incomingPrereqs[node.id]?.all { edge ->
                        nodeMap[edge.fromId]?.status == NodeStatus.DONE
                    } ?: true
            }
            active to ready
        }

    suspend fun createNode(title: String): Long =
        nodeDao.insert(NodeEntity(title = title.trim()))

    suspend fun updateStatus(node: NodeEntity, status: NodeStatus) {
        val updated = when (status) {
            NodeStatus.ACTIVE -> node.copy(status = status, startedAt = System.currentTimeMillis())
            NodeStatus.DONE -> node.copy(status = status, doneAt = System.currentTimeMillis())
            else -> node.copy(status = status)
        }
        nodeDao.update(updated)
    }

    suspend fun addEdge(fromId: Long, toId: Long) =
        edgeDao.insert(EdgeEntity(fromId = fromId, toId = toId))

    suspend fun removeEdge(edge: EdgeEntity) = edgeDao.delete(edge)
}
