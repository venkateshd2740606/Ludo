package com.ludo.engine

import com.ludo.domain.model.Difficulty
import com.ludo.domain.model.LudoLevel
import com.ludo.domain.model.LudoLevelMode
import com.ludo.domain.model.LudoPlayer

object TutorialLevels {

    val all: List<LudoLevel> = listOf(
        level1RollAndEnter(),
        level2MoveOnTrack(),
        level3Capture(),
        level4HomeStretch(),
        level5FullGame(),
        level6FourPlayer()
    )

    fun getTutorialLevel(index: Int): LudoLevel? = all.getOrNull(index)

    private fun homeTokens() = List(LudoEngine.TOKENS_PER_PLAYER) { LudoEngine.HOME }

    private fun inactiveTokens() = List(LudoEngine.TOKENS_PER_PLAYER) { LudoEngine.FINISHED }

    private fun level1RollAndEnter(): LudoLevel = LudoLevel(
        seed = 1,
        levelNumber = 1,
        difficulty = Difficulty.BEGINNER,
        isTutorial = true,
        useSeededDice = true,
        playerCount = 2,
        initialTokens = listOf(
            listOf(LudoEngine.startCell(LudoPlayer.RED), LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME),
            homeTokens()
        )
    )

    private fun level2MoveOnTrack(): LudoLevel = LudoLevel(
        seed = 2,
        levelNumber = 2,
        difficulty = Difficulty.BEGINNER,
        isTutorial = true,
        useSeededDice = true,
        playerCount = 2,
        initialTokens = listOf(
            listOf(3, LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME),
            homeTokens()
        )
    )

    private fun level3Capture(): LudoLevel = LudoLevel(
        seed = 3,
        levelNumber = 3,
        difficulty = Difficulty.EASY,
        isTutorial = true,
        useSeededDice = true,
        playerCount = 2,
        initialTokens = listOf(
            listOf(10, LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME),
            listOf(12, LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME)
        )
    )

    private fun level4HomeStretch(): LudoLevel = LudoLevel(
        seed = 4,
        levelNumber = 4,
        difficulty = Difficulty.EASY,
        isTutorial = true,
        useSeededDice = true,
        playerCount = 2,
        initialTokens = listOf(
            listOf(51, LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME),
            homeTokens()
        )
    )

    private fun level5FullGame(): LudoLevel = LudoLevel(
        seed = 5,
        levelNumber = 5,
        difficulty = Difficulty.MEDIUM,
        isTutorial = true,
        mode = LudoLevelMode.VS_AI,
        useSeededDice = false,
        playerCount = 2
    )

    private fun level6FourPlayer(): LudoLevel = LudoLevel(
        seed = 6,
        levelNumber = 6,
        difficulty = Difficulty.MEDIUM,
        isTutorial = true,
        mode = LudoLevelMode.VS_AI,
        useSeededDice = false,
        playerCount = 4,
        initialTokens = listOf(
            listOf(LudoEngine.startCell(LudoPlayer.RED), LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME),
            listOf(LudoEngine.startCell(LudoPlayer.GREEN), LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME),
            listOf(LudoEngine.startCell(LudoPlayer.YELLOW), LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME),
            listOf(LudoEngine.startCell(LudoPlayer.BLUE), LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME)
        )
    )
}
