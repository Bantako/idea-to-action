package org.mrlem.composesample.features.overview.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.mrlem.android.core.feature.nav.Navigator
import org.mrlem.android.core.feature.nav.Navigator.Operation.Push
import org.mrlem.android.core.feature.ui.NavProvider
import org.mrlem.composesample.features.library.nav.BookmarkKey
import org.mrlem.composesample.features.overview.nav.OverviewKey
import se.ansman.dagger.auto.AutoBindIntoSet
import javax.inject.Inject

@AutoBindIntoSet
class OverviewNavProvider @Inject constructor(
    private val navigator: Navigator,
) : NavProvider() {

    override val navBarItem = null

    override val entryBuilders: EntryProviderScope<NavKey>.(SnackbarHostState, PaddingValues) -> Unit =
        { _, innerPadding ->
            entry<OverviewKey> {
                OverviewScreen(
                    modifier = Modifier
                        .padding(innerPadding),
                    onSuggestionClick = { itemId ->
                        navigator.navigate(Push(BookmarkKey(itemId = itemId)))
                    },
                )
            }
        }
}