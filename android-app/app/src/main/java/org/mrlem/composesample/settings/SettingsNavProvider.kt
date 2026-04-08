package org.mrlem.composesample.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.mrlem.android.core.feature.nav.MainNavKey
import org.mrlem.android.core.feature.ui.NavProvider
import org.mrlem.composesample.R
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data object SettingsKey : MainNavKey

@Singleton
class SettingsNavProvider @Inject constructor() : NavProvider() {

    override val navBarItem = BottomBarItem(
        index = 5,
        labelResId = R.string.settings_bottomnav_label,
        icon = Icons.Filled.Info,
        key = SettingsKey,
    )

    override val entryBuilders: EntryProviderScope<NavKey>.(SnackbarHostState, PaddingValues) -> Unit =
        { _, innerPadding ->
            entry<SettingsKey> {
                SettingsScreen(modifier = Modifier.padding(innerPadding))
            }
        }
}
