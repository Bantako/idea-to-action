package org.mrlem.composesample.coaching

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.mrlem.android.core.feature.ui.NavProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoachingModule {
    @Binds @IntoSet @Singleton
    abstract fun bindCoachingNavProvider(provider: CoachingNavProvider): NavProvider
}
