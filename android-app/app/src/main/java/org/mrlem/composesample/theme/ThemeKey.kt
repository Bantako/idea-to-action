package org.mrlem.composesample.theme

import kotlinx.serialization.Serializable
import org.mrlem.android.core.feature.nav.MainNavKey

@Serializable
data class ThemeKey(val themeId: String) : MainNavKey
