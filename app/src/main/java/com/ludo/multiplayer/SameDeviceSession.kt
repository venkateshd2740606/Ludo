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
class SameDeviceSession @Inject constructor() {
    private val _session = MutableStateFlow<MultiplayerSession?>(null)
    val session: StateFlow<MultiplayerSession?> = _session.asStateFlow()

    private var sharedGame: LudoGame? = null
    private var activePlayer = LudoPlayer.RED
    private var playerCount = 2
    private val playerNames = mutableMapOf<LudoPlayer, String>()

    fun start(
        playerOne: String,
        playerTwo: String,
        difficulty: Difficulty,
        seed: Long = System.currentTimeMillis(),
        playerThree: String = "Player 3",
        playerFour: String = "Player 4",
        players: Int = 4
    ) {
        playerCount = players.coerceIn(2, 4)
        playerNames[LudoPlayer.RED] = playerOne
        playerNames[LudoPlayer.GREEN] = playerThree
        playerNames[LudoPlayer.YELLOW] = playerFour
        playerNames[LudoPlayer.BLUE] = playerTwo
        val level = LudoGenerator.generate(seed, 1, difficulty).copy(playerCount = playerCount)
        val game = LudoEngine.createInitialGame(level)
        sharedGame = game
        activePlayer = LudoPlayer.RED
        publishSession(difficulty, seed, isActive = true)
    }

    fun getActiveGame(): LudoGame? = sharedGame

    fun applyRoll(): LudoGame? {
        val game = sharedGame ?: return null
        if (game.currentPlayer != activePlayer || !game.needsRoll) return null
        val updated = LudoEngine.rollDice(game)
        sharedGame = updated
        activePlayer = updated.currentPlayer
        publishSession(
            difficulty = _session.value?.difficulty ?: Difficulty.MEDIUM,
            seed = _session.value?.seed ?: System.currentTimeMillis(),
            isActive = true
        )
        return updated
    }

    fun applyTokenMove(tokenIndex: Int): LudoGame? {
        val game = sharedGame ?: return null
        if (game.currentPlayer != activePlayer || !game.canMove) return null
        val updated = LudoEngine.moveToken(game, tokenIndex) ?: return null
        sharedGame = updated
        if (updated.isCompleted) {
            handleRoundComplete(updated.winner == LudoPlayer.RED)
            return updated
        }
        activePlayer = updated.currentPlayer
        publishSession(
            difficulty = _session.value?.difficulty ?: Difficulty.MEDIUM,
            seed = _session.value?.seed ?: System.currentTimeMillis(),
            isActive = true
        )
        return updated
    }

    private fun handleRoundComplete(redWon: Boolean) {
        val session = _session.value ?: return
        val newLocal = session.localScore + if (redWon) 1 else 0
        val newRemote = session.remoteScore + if (redWon) 0 else 1
        val newLevel = LudoGenerator.generate(
            session.seed + newLocal + newRemote,
            newLocal + newRemote + 1,
            session.difficulty
        ).copy(playerCount = playerCount)
        sharedGame = LudoEngine.createInitialGame(newLevel)
        val activeOrder = LudoPlayer.activePlayers(playerCount)
        activePlayer = if (redWon) {
            activeOrder.last()
        } else {
            activeOrder.first()
        }
        _session.value = session.copy(
            localScore = newLocal,
            remoteScore = newRemote,
            activePlayerName = playerName(activePlayer)
        )
    }

    fun end() {
        _session.value = null
        sharedGame = null
        activePlayer = LudoPlayer.RED
        playerCount = 2
        playerNames.clear()
    }

    private fun playerName(player: LudoPlayer): String =
        playerNames[player] ?: player.name

    private fun publishSession(difficulty: Difficulty, seed: Long, isActive: Boolean) {
        _session.value = MultiplayerSession(
            mode = MultiplayerMode.SAME_DEVICE,
            localPlayerName = playerNames[LudoPlayer.RED] ?: "Player 1",
            remotePlayerName = when (playerCount) {
                2 -> playerNames[LudoPlayer.BLUE]
                else -> listOfNotNull(
                    playerNames[LudoPlayer.GREEN],
                    playerNames[LudoPlayer.YELLOW],
                    playerNames[LudoPlayer.BLUE]
                ).joinToString(", ")
            },
            activePlayerName = playerName(activePlayer),
            localScore = _session.value?.localScore ?: 0,
            remoteScore = _session.value?.remoteScore ?: 0,
            isActive = isActive,
            seed = seed,
            difficulty = difficulty
        )
    }
}
