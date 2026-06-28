package com.ludo.di

import android.content.Context
import androidx.room.Room
import com.ludo.data.local.database.LudoDatabase
import com.ludo.data.local.database.dao.AchievementDao
import com.ludo.data.local.database.dao.ChallengeDao
import com.ludo.data.local.database.dao.EconomyDao
import com.ludo.data.local.database.dao.GameDao
import com.ludo.data.local.database.dao.ProfileDao
import com.ludo.data.local.database.dao.StatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LudoDatabase =
        Room.databaseBuilder(context, LudoDatabase::class.java, "ludo.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideGameDao(db: LudoDatabase): GameDao = db.gameDao()
    @Provides fun provideStatsDao(db: LudoDatabase): StatsDao = db.statsDao()
    @Provides fun provideAchievementDao(db: LudoDatabase): AchievementDao = db.achievementDao()
    @Provides fun provideChallengeDao(db: LudoDatabase): ChallengeDao = db.challengeDao()
    @Provides fun provideEconomyDao(db: LudoDatabase): EconomyDao = db.economyDao()
    @Provides fun provideProfileDao(db: LudoDatabase): ProfileDao = db.profileDao()
}
