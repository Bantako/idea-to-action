package org.mrlem.composesample.inbox

import kotlinx.serialization.Serializable
import org.mrlem.android.core.feature.nav.MainNavKey

@Serializable
data class InboxEntryKey(val entryId: String) : MainNavKey
