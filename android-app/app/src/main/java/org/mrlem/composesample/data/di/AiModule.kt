package org.mrlem.composesample.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.mrlem.composesample.BuildConfig
import org.mrlem.composesample.data.ai.AiService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    fun provideAiService(httpClient: OkHttpClient): AiService =
        AiService(httpClient, BuildConfig.ANTHROPIC_API_KEY)
}
