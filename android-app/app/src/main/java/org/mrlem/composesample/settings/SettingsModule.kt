package org.mrlem.composesample.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.mrlem.android.core.feature.ui.NavProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds @IntoSet @Singleton
    abstract fun bindSettingsNavProvider(provider: SettingsNavProvider): NavProvider
}
