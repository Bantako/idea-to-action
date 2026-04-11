package org.mrlem.composesample.review

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.mrlem.android.core.feature.nav.Navigator
import org.mrlem.android.core.feature.ui.NavProvider
import org.mrlem.composesample.R
import org.mrlem.composesample.coaching.ReviewCoachingKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewNavProvider @Inject constructor(
    private val navigator: Navigator,
) : NavProvider() {

    override val navBarItem: BottomBarItem? = null

    override val entryBuilders: EntryProviderScope<NavKey>.(SnackbarHostState, PaddingValues) -> Unit =
        { _, innerPadding ->
            entry<ReviewKey> {
                ReviewScreen(
                    onNavigateToReviewCoaching = {
                        navigator.navigate(Navigator.Operation.Push(ReviewCoachingKey))
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
}
