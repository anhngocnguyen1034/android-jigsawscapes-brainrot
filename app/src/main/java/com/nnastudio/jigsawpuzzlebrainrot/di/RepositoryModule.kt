package com.nnastudio.jigsawpuzzlebrainrot.di

import com.nnastudio.jigsawpuzzlebrainrot.data.repository.ProgressRepositoryImpl
import com.nnastudio.jigsawpuzzlebrainrot.data.repository.PuzzleImageRepositoryImpl
import com.nnastudio.jigsawpuzzlebrainrot.data.repository.PuzzleRepositoryImpl
import com.nnastudio.jigsawpuzzlebrainrot.data.repository.SavedGameRepositoryImpl
import com.nnastudio.jigsawpuzzlebrainrot.data.repository.SettingsRepositoryImpl
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.ProgressRepository
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.PuzzleImageRepository
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.PuzzleRepository
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.SavedGameRepository
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPuzzleRepository(impl: PuzzleRepositoryImpl): PuzzleRepository

    @Binds
    @Singleton
    abstract fun bindPuzzleImageRepository(impl: PuzzleImageRepositoryImpl): PuzzleImageRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindSavedGameRepository(impl: SavedGameRepositoryImpl): SavedGameRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
