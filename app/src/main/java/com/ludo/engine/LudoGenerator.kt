package com.ludo.engine

import com.ludo.domain.model.Difficulty
import com.ludo.domain.model.GenerationProfile
import com.ludo.domain.model.LudoLevel
import com.ludo.domain.model.LudoLevelMode

object LudoGenerator {

    fun generate(
        seed: Long,
        levelNumber: Int,
        difficulty: Difficulty,
        generationProfile: GenerationProfile = GenerationProfile()
    ): LudoLevel {
        val useSeededDice = generationProfile.useSeededDice || shouldUseSeededDice(difficulty)
        return LudoLevel(
            seed = seed,
            levelNumber = levelNumber,
            difficulty = difficulty,
            useSeededDice = useSeededDice,
            mode = if (difficulty == Difficulty.BEGINNER) LudoLevelMode.VS_AI else LudoLevelMode.STANDARD,
            isEndless = difficulty == Difficulty.ENDLESS
        )
    }

    fun generateForChallenge(
        seed: Long,
        levelNumber: Int,
        difficulty: Difficulty
    ): LudoLevel = generate(seed, levelNumber, difficulty, GenerationProfile(useSeededDice = true))

    fun seedFromLevelNumber(levelNumber: Int, difficulty: Difficulty): Long {
        val difficultyOffset = difficulty.ordinal * 100_000L
        return levelNumber.toLong() * 9973L + difficultyOffset + 42L
    }

    fun formatShareText(seed: Long, levelNumber: Int, difficulty: Difficulty): String =
        "Ludo Level\nSeed: $seed\nLevel: $levelNumber\nDifficulty: ${difficulty.name}"

    private fun shouldUseSeededDice(difficulty: Difficulty): Boolean = when (difficulty) {
        Difficulty.BEGINNER, Difficulty.EASY -> false
        Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT, Difficulty.MASTER, Difficulty.ENDLESS -> true
    }
}
