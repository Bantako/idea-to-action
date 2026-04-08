package org.mrlem.composesample.coaching

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.mrlem.android.core.feature.nav.Navigator
import org.mrlem.android.core.feature.ui.NavProvider
import org.mrlem.composesample.inbox.InboxEntryKey
import org.mrlem.composesample.theme.ThemeFocusKey
import org.mrlem.composesample.theme.ThemeFocusScreen
import org.mrlem.composesample.theme.ThemeFocusViewModel
import org.mrlem.composesample.theme.ThemeKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoachingNavProvider @Inject constructor(
    private val navigator: Navigator,
) : NavProvider() {

    override val navBarItem = null

    override val entryBuilders: EntryProviderScope<NavKey>.(SnackbarHostState, PaddingValues) -> Unit =
        { _, innerPadding ->
            entry<CoachingKey> { key ->
                val viewModel = hiltViewModel<CoachingViewModel, CoachingViewModel.Factory>(
                    creationCallback = { factory -> factory.create(InboxEntryKey(key.inboxEntryId)) },
                )
                CoachingScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigator.navigate(Navigator.Operation.Pop) },
                    onNavigateToTheme = { themeId ->
                        navigator.navigate(Navigator.Operation.Push(ThemeKey(themeId)))
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            entry<ReviewCoachingKey> {
                val viewModel = hiltViewModel<ReviewCoachingViewModel>()
                ReviewCoachingScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigator.navigate(Navigator.Operation.Pop) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            entry<BatchCoachingKey> { key ->
                val viewModel = hiltViewModel<BatchCoachingViewModel, BatchCoachingViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                )
                CoachingScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigator.navigate(Navigator.Operation.Pop) },
                    onNavigateToTheme = { themeId ->
                        navigator.navigate(Navigator.Operation.Push(ThemeKey(themeId)))
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            entry<ThemeFocusKey> {
                val viewModel = hiltViewModel<ThemeFocusViewModel>()
                ThemeFocusScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigator.navigate(Navigator.Operation.Pop) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
}
