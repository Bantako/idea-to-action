package org.mrlem.composesample.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    /** theme_id = null かつ IDEA/READY のノード（Capture タブ用） */
    fun observeUnorganized(): Flow<List<NodeEntity>> = nodeDao.observeUnorganized()

    fun observeEdges(): Flow<List<EdgeEntity>> = edgeDao.observeAll()

    /**
     * ACTIVE ノードと READY ノードを返す。
     */
    fun observeActionable(): Flow<Pair<List<NodeEntity>, List<NodeEntity>>> =
        nodeDao.observeAll().map { nodes ->
            nodes.filter { it.status == NodeStatus.ACTIVE } to
                nodes.filter { it.status == NodeStatus.READY }
        }

    suspend fun updateNode(node: NodeEntity, title: String, body: String) {
        nodeDao.update(node.copy(title = title.trim(), body = body.trim()))
    }

    suspend fun deleteNode(node: NodeEntity) {
        nodeDao.delete(node)
        recalculateReady()
    }

    suspend fun createNode(title: String): Long {
        val id = nodeDao.insert(NodeEntity(title = title.trim()))
        recalculateReady()
        return id
    }

    suspend fun updateStatus(node: NodeEntity, status: NodeStatus) {
        val updated = when (status) {
            NodeStatus.ACTIVE -> node.copy(status = status, startedAt = System.currentTimeMillis())
            NodeStatus.DONE -> node.copy(status = status, doneAt = System.currentTimeMillis())
            else -> node.copy(status = status)
        }
        nodeDao.update(updated)
        recalculateReady()
    }

    suspend fun addEdge(fromId: Long, toId: Long, type: EdgeType = EdgeType.PREREQUISITE) {
        edgeDao.insert(EdgeEntity(fromId = fromId, toId = toId, type = type))
        if (type == EdgeType.PREREQUISITE) recalculateReady()
    }

    suspend fun removeEdge(edge: EdgeEntity) {
        edgeDao.delete(edge)
        recalculateReady()
    }

    /**
     * PREREQUISITE エッジに基づいて IDEA/READY ステータスを再計算する。
     * - 全前提が DONE の IDEA ノード → READY に昇格
     * - 前提が未完了の READY ノード → IDEA に降格
     */
    private suspend fun recalculateReady() {
        val nodes = nodeDao.observeAll().first()
        val edges = edgeDao.observeAll().first()
        val nodeMap = nodes.associateBy { it.id }
        val incomingPrereqs = edges
            .filter { it.type == EdgeType.PREREQUISITE }
            .groupBy { it.toId }

        nodes.forEach { node ->
            val prereqs = incomingPrereqs[node.id] ?: emptyList()
            val allPrereqsDone = prereqs.all { nodeMap[it.fromId]?.status == NodeStatus.DONE }
            when {
                node.status == NodeStatus.IDEA && allPrereqsDone ->
                    nodeDao.update(node.copy(status = NodeStatus.READY))
                node.status == NodeStatus.READY && !allPrereqsDone ->
                    nodeDao.update(node.copy(status = NodeStatus.IDEA))
            }
        }
    }
}
