package org.mrlem.composesample

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.mrlem.composesample.feature.capture.CaptureScreen
import org.mrlem.composesample.feature.projects.ProjectDetailScreen
import org.mrlem.composesample.feature.projects.ProjectsScreen
import org.mrlem.composesample.feature.today.TodayScreen

@Composable
fun AppContent() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; selectedProjectId = null },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text("Today") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; selectedProjectId = null },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Capture") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; selectedProjectId = null },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Projects") },
                )
            }
        },
    ) { contentPadding ->
        val pid = selectedProjectId
        when {
            selectedTab == 0 -> TodayScreen(contentPadding, onNavigateToProjects = { selectedTab = 2 })
            selectedTab == 1 -> CaptureScreen(contentPadding)
            selectedTab == 2 && pid != null -> ProjectDetailScreen(
                contentPadding = contentPadding,
                projectId = pid,
                onBack = { selectedProjectId = null },
            )
            else -> ProjectsScreen(
                contentPadding = contentPadding,
                onProjectClick = { projectId -> selectedProjectId = projectId },
            )
        }
    }
}
