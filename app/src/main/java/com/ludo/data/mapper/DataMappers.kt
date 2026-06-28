package com.ludo.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ludo.data.local.database.entity.AchievementEntity
import com.ludo.data.local.database.entity.ChallengeEntity
import com.ludo.data.local.database.entity.EconomyEntity
import com.ludo.data.local.database.entity.GameEntity
import com.ludo.data.local.database.entity.ProfileEntity
import com.ludo.data.local.database.entity.StatsEntity
import com.ludo.domain.model.PuzzleArchetype
import com.ludo.domain.model.PuzzleProfile
import com.ludo.domain.model.PuzzleProfileMetrics
import com.ludo.domain.model.SkillCategory
import com.ludo.domain.model.Achievement
import com.ludo.domain.model.ChallengeRecord
import com.ludo.domain.model.ChallengeType
import com.ludo.domain.model.LudoGame
import com.ludo.domain.model.LudoLevel
import com.ludo.domain.model.LudoLevelMode
import com.ludo.domain.model.LudoPlayer
import com.ludo.domain.model.Difficulty
import com.ludo.domain.model.EconomyState
import com.ludo.domain.model.GameStatus
import com.ludo.domain.model.UserStats
import com.ludo.engine.LudoEngine

object DataMappers {
    private val gson = Gson()
    private val intListType = object : TypeToken<List<List<Int>>>() {}.type

    fun toEntity(game: LudoGame): GameEntity = GameEntity(
        id = game.id,
        seed = game.level.seed,
        levelNumber = game.level.levelNumber,
        difficulty = game.level.difficulty.name,
        status = game.status.name,
        tubeStateJson = gson.toJson(
            GameStateJson(
                tokens = game.tokens,
                currentPlayer = game.currentPlayer.name,
                lastDiceRoll = game.lastDiceRoll,
                diceRollCount = game.diceRollCount,
                extraRoll = game.extraRoll,
                winner = game.winner?.name
            )
        ),
        selectedTubeId = game.selectedTokenIndex ?: -1,
        moves = game.moves,
        hintsUsed = game.hintsUsed,
        elapsedSeconds = game.elapsedSeconds,
        createdAt = game.createdAt,
        lastPlayedAt = game.lastPlayedAt,
        completedAt = game.completedAt,
        isTutorial = game.level.isTutorial,
        isEndless = game.level.isEndless,
        challengeType = game.level.challengeType?.name,
        challengeKey = game.level.challengeKey,
        levelJson = gson.toJson(
            LevelJson(
                initialTokens = game.level.initialTokens,
                useSeededDice = game.level.useSeededDice,
                mode = game.level.mode.name
            )
        ),
        coinsEarned = game.coinsEarned,
        xpEarned = game.xpEarned
    )

    fun fromEntity(entity: GameEntity): LudoGame {
        val levelJson = runCatching { gson.fromJson(entity.levelJson, LevelJson::class.java) }
            .getOrNull() ?: LevelJson()
        val state = runCatching { gson.fromJson(entity.tubeStateJson, GameStateJson::class.java) }
            .getOrNull()
        val tokens: List<List<Int>> = state?.tokens
            ?: runCatching { gson.fromJson<List<List<Int>>>(entity.tubeStateJson, intListType) }
                .getOrNull()
            ?: LudoEngine.defaultTokens()
        val level = LudoLevel(
            id = entity.id,
            seed = entity.seed,
            levelNumber = entity.levelNumber,
            difficulty = Difficulty.valueOf(entity.difficulty),
            initialTokens = levelJson.initialTokens,
            useSeededDice = levelJson.useSeededDice,
            mode = runCatching { LudoLevelMode.valueOf(levelJson.mode) }
                .getOrDefault(LudoLevelMode.STANDARD),
            isTutorial = entity.isTutorial,
            isEndless = entity.isEndless,
            challengeType = entity.challengeType?.let { ChallengeType.valueOf(it) },
            challengeKey = entity.challengeKey
        )
        return LudoGame(
            id = entity.id,
            level = level,
            status = GameStatus.valueOf(entity.status),
            tokens = tokens,
            currentPlayer = state?.currentPlayer?.let {
                runCatching { LudoPlayer.valueOf(it) }.getOrNull()
            } ?: LudoPlayer.RED,
            lastDiceRoll = state?.lastDiceRoll,
            diceRollCount = state?.diceRollCount ?: 0,
            extraRoll = state?.extraRoll ?: false,
            selectedTokenIndex = entity.selectedTubeId.takeIf { it >= 0 },
            winner = state?.winner?.let { runCatching { LudoPlayer.valueOf(it) }.getOrNull() },
            moves = entity.moves,
            hintsUsed = entity.hintsUsed,
            elapsedSeconds = entity.elapsedSeconds,
            createdAt = entity.createdAt,
            lastPlayedAt = entity.lastPlayedAt,
            completedAt = entity.completedAt,
            coinsEarned = entity.coinsEarned,
            xpEarned = entity.xpEarned
        )
    }

    fun toStatsEntity(stats: UserStats): StatsEntity = StatsEntity(
        gamesPlayed = stats.gamesPlayed,
        gamesWon = stats.gamesWon,
        gamesAbandoned = stats.gamesAbandoned,
        totalPlayTimeSeconds = stats.totalPlayTimeSeconds,
        fastestTimeBeginner = stats.fastestTimeBeginner,
        fastestTimeEasy = stats.fastestTimeEasy,
        fastestTimeMedium = stats.fastestTimeMedium,
        fastestTimeHard = stats.fastestTimeHard,
        fastestTimeExpert = stats.fastestTimeExpert,
        fastestTimeMaster = stats.fastestTimeMaster,
        currentStreak = stats.currentStreak,
        longestStreak = stats.longestStreak,
        lastPlayedDate = stats.lastPlayedDate,
        xpPoints = stats.xpPoints,
        level = stats.level,
        hintsUsedTotal = stats.hintsUsedTotal,
        perfectGames = stats.perfectGames,
        poursTotal = stats.poursTotal,
        endlessHighScore = stats.endlessHighScore
    )

    fun fromStatsEntity(entity: StatsEntity?): UserStats {
        if (entity == null) return UserStats()
        return UserStats(
            gamesPlayed = entity.gamesPlayed,
            gamesWon = entity.gamesWon,
            gamesAbandoned = entity.gamesAbandoned,
            totalPlayTimeSeconds = entity.totalPlayTimeSeconds,
            fastestTimeBeginner = entity.fastestTimeBeginner,
            fastestTimeEasy = entity.fastestTimeEasy,
            fastestTimeMedium = entity.fastestTimeMedium,
            fastestTimeHard = entity.fastestTimeHard,
            fastestTimeExpert = entity.fastestTimeExpert,
            fastestTimeMaster = entity.fastestTimeMaster,
            currentStreak = entity.currentStreak,
            longestStreak = entity.longestStreak,
            lastPlayedDate = entity.lastPlayedDate,
            xpPoints = entity.xpPoints,
            level = entity.level,
            hintsUsedTotal = entity.hintsUsedTotal,
            perfectGames = entity.perfectGames,
            poursTotal = entity.poursTotal,
            endlessHighScore = entity.endlessHighScore
        )
    }

    fun toChallengeEntity(record: ChallengeRecord): ChallengeEntity = ChallengeEntity(
        key = record.key,
        type = record.type.name,
        seed = record.seed,
        difficulty = record.difficulty.name,
        isCompleted = record.isCompleted,
        completionTime = record.completionTime,
        moves = record.moves,
        rewardCoins = record.rewardCoins,
        rewardXp = record.rewardXp,
        streakDay = record.streakDay
    )

    fun fromChallengeEntity(entity: ChallengeEntity): ChallengeRecord = ChallengeRecord(
        key = entity.key,
        type = ChallengeType.valueOf(entity.type),
        seed = entity.seed,
        difficulty = Difficulty.valueOf(entity.difficulty),
        isCompleted = entity.isCompleted,
        completionTime = entity.completionTime,
        moves = entity.moves,
        rewardCoins = entity.rewardCoins,
        rewardXp = entity.rewardXp,
        streakDay = entity.streakDay
    )

    fun toEconomyEntity(state: EconomyState): EconomyEntity = EconomyEntity(
        coins = state.coins,
        totalCoinsEarned = state.totalCoinsEarned,
        totalCoinsSpent = state.totalCoinsSpent,
        unlockedThemes = gson.toJson(state.unlockedThemeIds.toList())
    )

    fun fromEconomyEntity(entity: EconomyEntity?): EconomyState {
        if (entity == null) return EconomyState()
        val type = object : TypeToken<List<String>>() {}.type
        val unlocked: List<String> = gson.fromJson(entity.unlockedThemes, type) ?: emptyList()
        return EconomyState(
            coins = entity.coins,
            totalCoinsEarned = entity.totalCoinsEarned,
            totalCoinsSpent = entity.totalCoinsSpent,
            unlockedThemeIds = unlocked.toSet()
        )
    }

    fun mergeAchievement(def: Achievement, entity: AchievementEntity?): Achievement =
        def.copy(
            isUnlocked = entity?.isUnlocked ?: false,
            unlockedAt = entity?.unlockedAt,
            progress = entity?.progress ?: 0
        )

    fun toProfileEntity(profile: PuzzleProfile): ProfileEntity = ProfileEntity(
        gamesAnalyzed = profile.metrics.gamesAnalyzed,
        totalSolveTimeSeconds = profile.metrics.totalSolveTimeSeconds,
        totalMoves = profile.metrics.totalMoves,
        totalOptimalMoves = profile.metrics.totalOptimalMoves,
        totalHintsUsed = profile.metrics.totalHintsUsed,
        fastCompletions = profile.metrics.fastCompletions,
        slowCompletions = profile.metrics.slowCompletions,
        perfectCompletions = profile.metrics.perfectCompletions,
        complexChainWins = profile.metrics.complexChainWins,
        inefficientWins = profile.metrics.inefficientWins,
        hintHeavyWins = profile.metrics.hintHeavyWins,
        archetype = profile.archetype.name,
        strength = profile.strength.name,
        weakness = profile.weakness.name,
        adaptiveColorModifier = profile.adaptiveColorModifier
    )

    fun fromProfileEntity(entity: ProfileEntity?): PuzzleProfile {
        if (entity == null) return PuzzleProfile()
        val metrics = PuzzleProfileMetrics(
            gamesAnalyzed = entity.gamesAnalyzed,
            totalSolveTimeSeconds = entity.totalSolveTimeSeconds,
            totalMoves = entity.totalMoves,
            totalOptimalMoves = entity.totalOptimalMoves,
            totalHintsUsed = entity.totalHintsUsed,
            fastCompletions = entity.fastCompletions,
            slowCompletions = entity.slowCompletions,
            perfectCompletions = entity.perfectCompletions,
            complexChainWins = entity.complexChainWins,
            inefficientWins = entity.inefficientWins,
            hintHeavyWins = entity.hintHeavyWins
        )
        return PuzzleProfile(
            metrics = metrics,
            archetype = runCatching { PuzzleArchetype.valueOf(entity.archetype) }
                .getOrDefault(PuzzleArchetype.EXPLORER),
            strength = runCatching { SkillCategory.valueOf(entity.strength) }
                .getOrDefault(SkillCategory.PATTERN_RECOGNITION),
            weakness = runCatching { SkillCategory.valueOf(entity.weakness) }
                .getOrDefault(SkillCategory.TIME_PRESSURE),
            adaptiveColorModifier = entity.adaptiveColorModifier
        )
    }

    data class LevelJson(
        val initialTokens: List<List<Int>>? = null,
        val useSeededDice: Boolean = false,
        val mode: String = LudoLevelMode.STANDARD.name
    )

    data class GameStateJson(
        val tokens: List<List<Int>>,
        val currentPlayer: String,
        val lastDiceRoll: Int? = null,
        val diceRollCount: Int = 0,
        val extraRoll: Boolean = false,
        val winner: String? = null
    )
}
