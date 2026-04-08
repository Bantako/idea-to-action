package org.mrlem.composesample.review

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.mrlem.android.core.feature.ui.NavProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewModule {

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindReviewNavProvider(provider: ReviewNavProvider): NavProvider
}
