package org.mrlem.composesample.inbox

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.mrlem.android.core.feature.nav.Navigator
import org.mrlem.android.core.feature.ui.NavProvider
import org.mrlem.composesample.R
import org.mrlem.composesample.coaching.BatchCoachingKey
import org.mrlem.composesample.coaching.CoachingKey
import org.mrlem.composesample.theme.ThemeKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxNavProvider @Inject constructor(
    private val navigator: Navigator,
) : NavProvider() {

    override val navBarItem = BottomBarItem(
        index = 2,
        labelResId = R.string.inbox_bottomnav_label,
        icon = Icons.Filled.Edit,
        key = InboxKey,
    )

    override val entryBuilders: EntryProviderScope<NavKey>.(SnackbarHostState, PaddingValues) -> Unit =
        { _, innerPadding ->
            entry<InboxKey> {
                InboxScreen(
                    onEntryClick = { id ->
                        navigator.navigate(Navigator.Operation.Push(InboxEntryKey(id)))
                    },
                    onBatchCoaching = { ids ->
                        navigator.navigate(Navigator.Operation.Push(BatchCoachingKey.from(ids)))
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            entry<InboxEntryKey> { key ->
                val viewModel = hiltViewModel<InboxDetailViewModel, InboxDetailViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                )
                InboxDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navigator.navigate(Navigator.Operation.Pop)
                    },
                    onStartCoaching = { entryId ->
                        navigator.navigate(Navigator.Operation.Push(CoachingKey(entryId)))
                    },
                    onNavigateToTheme = { themeId ->
                        navigator.navigate(Navigator.Operation.Push(ThemeKey(themeId)))
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
}
