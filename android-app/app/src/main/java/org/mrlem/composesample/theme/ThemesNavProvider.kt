package org.mrlem.composesample.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.mrlem.android.core.feature.nav.Navigator
import org.mrlem.android.core.feature.ui.NavProvider
import org.mrlem.composesample.R
import org.mrlem.composesample.step.StepDetailScreen
import org.mrlem.composesample.step.StepDetailViewModel
import org.mrlem.composesample.step.StepKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemesNavProvider @Inject constructor(
    private val navigator: Navigator,
) : NavProvider() {

    override val navBarItem = BottomBarItem(
        index = 3,
        labelResId = R.string.themes_bottomnav_label,
        icon = Icons.Filled.Star,
        key = ThemesKey,
    )

    override val entryBuilders: EntryProviderScope<NavKey>.(SnackbarHostState, PaddingValues) -> Unit =
        { _, innerPadding ->
            entry<ThemesKey> {
                ThemesScreen(
                    onThemeClick = { id ->
                        navigator.navigate(Navigator.Operation.Push(ThemeKey(id)))
                    },
                    onFocusCoaching = {
                        navigator.navigate(Navigator.Operation.Push(ThemeFocusKey))
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            entry<ThemeKey> { key ->
                val viewModel = hiltViewModel<ThemeDetailViewModel, ThemeDetailViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                )
                ThemeDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigator.navigate(Navigator.Operation.Pop) },
                    onNavigateToStep = { stepId ->
                        navigator.navigate(Navigator.Operation.Push(StepKey(stepId)))
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            entry<StepKey> { key ->
                val viewModel = hiltViewModel<StepDetailViewModel, StepDetailViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                )
                StepDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigator.navigate(Navigator.Operation.Pop) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
}
