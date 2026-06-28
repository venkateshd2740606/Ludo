package com.ludo.domain.repository

import com.ludo.domain.model.Achievement
import com.ludo.domain.model.ChallengeRecord
import com.ludo.domain.model.ChallengeType
import com.ludo.domain.model.LudoGame
import com.ludo.domain.model.LudoLevel
import com.ludo.domain.model.Difficulty
import com.ludo.domain.model.EconomyState
import com.ludo.domain.model.PuzzleProfile
import com.ludo.domain.model.UserStats
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    suspend fun createNewGame(difficulty: Difficulty, levelNumber: Int): LudoGame
    suspend fun createGameFromSeed(seed: Long, levelNumber: Int, difficulty: Difficulty): LudoGame
    suspend fun createTutorialGame(tutorialIndex: Int): LudoGame?
    suspend fun createEndlessGame(wave: Int): LudoGame
    suspend fun saveGame(game: LudoGame): Long
    suspend fun getGame(gameId: Long): LudoGame?
    suspend fun getInProgressGame(): LudoGame?
    fun observeInProgressGame(): Flow<LudoGame?>
    suspend fun completeGame(game: LudoGame): LudoGame
    suspend fun abandonGame(gameId: Long)
    suspend fun getLevel(seed: Long, levelNumber: Int, difficulty: Difficulty): LudoLevel
}

interface ChallengeRepository {
    suspend fun getChallenge(type: ChallengeType, key: String): ChallengeRecord?
    suspend fun createChallenge(type: ChallengeType, key: String, difficulty: Difficulty): ChallengeRecord
    suspend fun resolveActiveChallenge(type: ChallengeType): ChallengeRecord
    fun observeActiveChallenge(type: ChallengeType): Flow<ChallengeRecord?>
    suspend fun completeChallenge(record: ChallengeRecord, timeSeconds: Long, moves: Int): ChallengeRecord
    fun observeChallengeHistory(type: ChallengeType): Flow<List<ChallengeRecord>>
    suspend fun getCurrentStreak(type: ChallengeType): Int
    suspend fun getChallengeGame(record: ChallengeRecord): LudoGame
}

interface ProgressionRepository {
    fun observeStats(): Flow<UserStats>
    suspend fun getStats(): UserStats
    suspend fun updateStatsAfterGame(game: LudoGame)
    suspend fun grantChallengeRewards(rewardCoins: Int, rewardXp: Int)
    fun observePuzzleProfile(): Flow<PuzzleProfile>
    suspend fun getPuzzleProfile(): PuzzleProfile
    fun observeAchievements(): Flow<List<Achievement>>
    suspend fun checkAndUnlockAchievements(
        game: LudoGame,
        sameDevicePlayed: Boolean = false
    ): List<Achievement>
    fun observeEconomy(): Flow<EconomyState>
    suspend fun getEconomy(): EconomyState
    suspend fun spendCoins(amount: Int): Boolean
    suspend fun earnCoins(amount: Int)
    suspend fun unlockTheme(themeId: String): Boolean
}

interface PreferencesRepository {
    fun getUserPreferences(): Flow<com.ludo.domain.model.UserPreferences>
    suspend fun updatePreferences(transform: (com.ludo.domain.model.UserPreferences) -> com.ludo.domain.model.UserPreferences)
    suspend fun getCampaignLevel(difficulty: Difficulty): Int
    suspend fun advanceCampaignLevel(difficulty: Difficulty): Int
}
