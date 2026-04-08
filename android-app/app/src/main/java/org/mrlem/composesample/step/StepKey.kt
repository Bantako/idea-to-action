package org.mrlem.composesample.step

import kotlinx.serialization.Serializable
import org.mrlem.android.core.feature.nav.MainNavKey

@Serializable
data class StepKey(val stepId: String) : MainNavKey
