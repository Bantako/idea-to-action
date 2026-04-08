package org.mrlem.composesample.today

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.mrlem.android.core.feature.ui.NavProvider
import org.mrlem.composesample.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodayNavProvider @Inject constructor() : NavProvider() {

    override val startKey: NavKey = TodayKey

    override val navBarItem = BottomBarItem(
        index = 1,
        labelResId = R.string.today_bottomnav_label,
        icon = Icons.Filled.DateRange,
        key = TodayKey,
    )

    override val entryBuilders: EntryProviderScope<NavKey>.(SnackbarHostState, PaddingValues) -> Unit =
        { _, innerPadding ->
            entry<TodayKey> {
                TodayScreen(
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
}
