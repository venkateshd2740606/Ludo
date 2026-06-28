package com.ludo.di

import com.ludo.data.repository.ChallengeRepositoryImpl
import com.ludo.data.repository.GameRepositoryImpl
import com.ludo.data.repository.PreferencesRepositoryImpl
import com.ludo.data.repository.ProgressionRepositoryImpl
import com.ludo.domain.repository.ChallengeRepository
import com.ludo.domain.repository.GameRepository
import com.ludo.domain.repository.PreferencesRepository
import com.ludo.domain.repository.ProgressionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository
    @Binds @Singleton abstract fun bindChallengeRepository(impl: ChallengeRepositoryImpl): ChallengeRepository
    @Binds @Singleton abstract fun bindProgressionRepository(impl: ProgressionRepositoryImpl): ProgressionRepository
    @Binds @Singleton abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}
