package org.mrlem.composesample.coaching

import kotlinx.serialization.Serializable
import org.mrlem.android.core.feature.nav.MainNavKey

@Serializable
data class BatchCoachingKey(val entryIdsJoined: String) : MainNavKey {
    val entryIds: List<String> get() = entryIdsJoined.split(",").filter { it.isNotEmpty() }

    companion object {
        fun from(entryIds: List<String>) = BatchCoachingKey(entryIds.joinToString(","))
    }
}
