package dev.photopia.core.engine.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.photopia.core.engine.Compositor
import dev.photopia.core.engine.GlCompositor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    @Provides
    @Singleton
    fun provideCompositor(impl: GlCompositor): Compositor = impl
}
