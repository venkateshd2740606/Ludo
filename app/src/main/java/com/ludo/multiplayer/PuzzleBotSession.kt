package com.ludo.multiplayer

import com.ludo.domain.model.Difficulty
import com.ludo.domain.model.LudoGame
import com.ludo.domain.model.LudoPlayer
import com.ludo.domain.model.MultiplayerMode
import com.ludo.domain.model.MultiplayerSession
import com.ludo.engine.LudoEngine
import com.ludo.engine.LudoGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PuzzleBotSession @Inject constructor() {
    private val _session = MutableStateFlow<MultiplayerSession?>(null)
    val session: StateFlow<MultiplayerSession?> = _session.asStateFlow()

    private var sharedGame: LudoGame? = null
    private var playerName = "You"
    private val botName = "AI Bot"
    private val humanPlayer = LudoPlayer.RED
    private val botPlayer = LudoPlayer.BLUE

    fun start(player: String, difficulty: Difficulty, seed: Long = System.currentTimeMillis()) {
        playerName = player
        val level = LudoGenerator.generate(seed, 1, difficulty)
        val game = LudoEngine.createInitialGame(level)
        sharedGame = game
        _session.value = MultiplayerSession(
            mode = MultiplayerMode.SAME_DEVICE,
            localPlayerName = playerName,
            remotePlayerName = botName,
            activePlayerName = playerName,
            isActive = true,
            seed = seed,
            difficulty = difficulty
        )
    }

    fun getPlayerGame(): LudoGame? = sharedGame

    fun applyPlayerRoll(): LudoGame? {
        val game = sharedGame ?: return null
        if (game.currentPlayer != humanPlayer || !game.needsRoll) return null
        val updated = LudoEngine.rollDice(game)
        sharedGame = updated
        return updated
    }

    fun applyPlayerMove(tokenIndex: Int): LudoGame? {
        val game = sharedGame ?: return null
        if (game.currentPlayer != humanPlayer || !game.canMove) return null
        val updated = LudoEngine.moveToken(game, tokenIndex) ?: return null
        sharedGame = updated
        return updated
    }

    fun applyBotTurn(): LudoGame? {
        val game = sharedGame ?: return null
        if (game.isCompleted || game.currentPlayer != botPlayer) return null
        var current = game
        if (current.needsRoll) {
            current = LudoEngine.rollDice(current)
        }
        val move = LudoEngine.getBestAiMove(current) ?: return current
        if (move.first < 0) {
            sharedGame = current
            return current
        }
        val updated = LudoEngine.moveToken(current, move.first) ?: return current
        sharedGame = updated
        val session = _session.value
        if (session != null) {
            _session.value = session.copy(
                activePlayerName = if (updated.currentPlayer == humanPlayer) playerName else botName
            )
        }
        return updated
    }

    fun onPlayerWon() {
        val session = _session.value ?: return
        _session.value = session.copy(
            localScore = session.localScore + 1,
            activePlayerName = playerName
        )
        startNewRound(session)
    }

    fun onBotWon() {
        val session = _session.value ?: return
        _session.value = session.copy(
            remoteScore = session.remoteScore + 1,
            activePlayerName = playerName
        )
        startNewRound(session)
    }

    private fun startNewRound(session: MultiplayerSession) {
        val newSeed = session.seed + session.localScore + session.remoteScore
        val level = LudoGenerator.generate(
            newSeed,
            session.localScore + session.remoteScore + 1,
            session.difficulty
        )
        sharedGame = LudoEngine.createInitialGame(level)
    }

    fun end() {
        _session.value = null
        sharedGame = null
    }
}
