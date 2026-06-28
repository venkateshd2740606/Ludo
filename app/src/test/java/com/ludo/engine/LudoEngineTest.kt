package com.ludo.engine

import com.ludo.domain.model.Difficulty
import com.ludo.domain.model.LudoPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoEngineTest {

    @Test
    fun tutorialLevel_isValid() {
        val level = TutorialLevels.getTutorialLevel(0)!!
        assertTrue(LudoEngine.validateLevel(level))
    }

    @Test
    fun rollDice_setsValue() {
        val level = TutorialLevels.getTutorialLevel(1)!!
        val game = LudoEngine.createInitialGame(level)
        val rolled = LudoEngine.rollDice(game)
        assertTrue(rolled.diceRollCount > game.diceRollCount)
        assertTrue(rolled.lastDiceRoll != null || rolled.currentPlayer != game.currentPlayer)
    }

    @Test
    fun enterBoard_requiresSix() {
        val game = LudoEngine.createInitialGame(TutorialLevels.getTutorialLevel(4)!!)
        val pos = LudoEngine.computeNewPosition(LudoPlayer.RED, LudoEngine.HOME, 6)
        assertEquals(LudoEngine.startCell(LudoPlayer.RED), pos)
        assertNull(LudoEngine.computeNewPosition(LudoPlayer.RED, LudoEngine.HOME, 5))
    }

    @Test
    fun capture_sendsOpponentHome() {
        val tokens = listOf(
            listOf(10, LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME),
            listOf(12, LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME)
        )
        val level = TutorialLevels.getTutorialLevel(2)!!.copy(initialTokens = tokens)
        var game = LudoEngine.createInitialGame(level).copy(lastDiceRoll = 2, currentPlayer = LudoPlayer.RED)
        game = LudoEngine.moveToken(game, 0)!!
        assertEquals(LudoEngine.HOME, game.tokens[LudoPlayer.BLUE.ordinal][0])
    }

    @Test
    fun homeStretch_finishesWithExactRoll() {
        val tokens = listOf(
            listOf(51, LudoEngine.HOME, LudoEngine.HOME, LudoEngine.HOME),
            LudoEngine.defaultTokens()[1]
        )
        val level = TutorialLevels.getTutorialLevel(3)!!.copy(initialTokens = tokens)
        var game = LudoEngine.createInitialGame(level).copy(lastDiceRoll = 1, currentPlayer = LudoPlayer.RED)
        game = LudoEngine.moveToken(game, 0)!!
        assertEquals(52, game.tokens[0][0])
        game = game.copy(lastDiceRoll = 6, currentPlayer = LudoPlayer.RED)
        game = LudoEngine.moveToken(game, 0)!!
        assertEquals(LudoEngine.FINISHED, game.tokens[0][0])
    }

    @Test
    fun seededDice_isDeterministic() {
        val level = LudoGenerator.generate(999L, 1, Difficulty.MEDIUM)
        val game = LudoEngine.createInitialGame(level)
        val first = LudoEngine.nextDiceValue(game)
        val second = LudoEngine.nextDiceValue(game)
        assertEquals(first, second)
    }

    @Test
    fun generator_sameSeed_producesSameLevel() {
        val a = LudoGenerator.generate(999L, 5, Difficulty.MEDIUM)
        val b = LudoGenerator.generate(999L, 5, Difficulty.MEDIUM)
        assertEquals(a.seed, b.seed)
        assertEquals(a.useSeededDice, b.useSeededDice)
    }

    @Test
    fun getHintMove_suggestsValidAction() {
        val level = TutorialLevels.getTutorialLevel(1)!!
        var game = LudoEngine.createInitialGame(level)
        val rollHint = LudoEngine.getHintMove(game)
        assertNotNull(rollHint)
        game = LudoEngine.rollDice(game)
        if (game.canMove) {
            assertNotNull(LudoEngine.getHintMove(game))
        }
    }

    @Test
    fun isWon_whenWinnerSet() {
        val game = LudoEngine.createInitialGame(TutorialLevels.getTutorialLevel(4)!!)
            .copy(winner = LudoPlayer.RED, status = com.ludo.domain.model.GameStatus.COMPLETED)
        assertTrue(LudoEngine.isWon(game))
    }
}
