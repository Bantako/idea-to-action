package org.mrlem.composesample.domain

import kotlinx.coroutines.flow.Flow
import org.mrlem.composesample.data.db.ProjectDao
import org.mrlem.composesample.data.db.ProjectEntity
import org.mrlem.composesample.data.db.ProjectStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
) {

    fun observeActive(): Flow<List<ProjectEntity>> = projectDao.observeActive()

    fun observeFocused(): Flow<List<ProjectEntity>> = projectDao.observeFocused()

    suspend fun create(title: String): Long =
        projectDao.insert(ProjectEntity(title = title.trim()))

    suspend fun updateBackground(project: ProjectEntity, background: String) {
        projectDao.update(project.copy(background = background.trim().ifEmpty { null }))
    }

    suspend fun setFocus(project: ProjectEntity) {
        projectDao.update(project.copy(focusedAt = System.currentTimeMillis()))
    }

    suspend fun clearFocus(project: ProjectEntity) {
        projectDao.update(project.copy(focusedAt = null))
    }

    suspend fun pause(project: ProjectEntity) {
        projectDao.update(project.copy(status = ProjectStatus.PAUSED))
    }

    suspend fun archive(project: ProjectEntity) {
        projectDao.update(project.copy(status = ProjectStatus.ARCHIVED))
    }
}
