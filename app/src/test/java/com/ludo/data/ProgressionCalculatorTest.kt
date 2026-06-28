package com.ludo.data

import com.ludo.domain.model.Difficulty
import com.ludo.domain.model.GameStatus
import com.ludo.engine.LudoEngine
import com.ludo.engine.LudoGenerator
import com.ludo.util.ProgressionCalculator
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionCalculatorTest {

    @Test
    fun xpForCompletedGame_isPositive() {
        val level = LudoGenerator.generate(1L, 1, Difficulty.EASY)
        val game = LudoEngine.createInitialGame(level).copy(status = GameStatus.COMPLETED)
        assertTrue(ProgressionCalculator.xpForGame(game) > 0)
    }

    @Test
    fun xpForGame_withHints_isLowerThanWithoutHints() {
        val level = LudoGenerator.generate(1L, 1, Difficulty.EASY)
        val withHints = LudoEngine.createInitialGame(level).copy(hintsUsed = 2, status = GameStatus.COMPLETED)
        val noHints = LudoEngine.createInitialGame(level).copy(hintsUsed = 0, status = GameStatus.COMPLETED)
        assertTrue(ProgressionCalculator.xpForGame(noHints) >= ProgressionCalculator.xpForGame(withHints))
    }
}
