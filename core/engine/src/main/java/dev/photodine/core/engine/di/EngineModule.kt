package dev.photodine.core.engine.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.photodine.core.engine.Compositor
import dev.photodine.core.engine.GlCompositor
import dev.photodine.core.engine.history.HistoryManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    @Provides
    @Singleton
    fun provideCompositor(impl: GlCompositor): Compositor = impl

    @Provides
    @Singleton
    fun provideHistoryManager(): HistoryManager = HistoryManager()
}
