package com.ludo.multiplayer

import com.ludo.domain.model.Difficulty
import com.ludo.domain.model.LudoGame
import com.ludo.domain.model.LudoPlayer
import com.ludo.domain.model.MultiplayerMode
import com.ludo.domain.model.MultiplayerSession
import com.ludo.domain.model.P2PRole
import com.ludo.engine.LudoEngine
import com.ludo.engine.LudoGenerator
import com.ludo.network.P2PMessage
import com.ludo.network.P2PSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkPuzzleSession @Inject constructor(
    private val p2pSessionManager: P2PSessionManager
) {
    private val _session = MutableStateFlow<MultiplayerSession?>(null)
    val session: StateFlow<MultiplayerSession?> = _session.asStateFlow()

    private var sharedGame: LudoGame? = null
    private var localName = "Player 1"
    private var remoteName = "Player 2"
    private var isMyTurn = false
    private var localPlayer = LudoPlayer.RED

    val isLocalTurn: Boolean get() = isMyTurn

    suspend fun startAsHost(localPlayerName: String, remotePlayer: String, difficulty: Difficulty) {
        localName = localPlayerName
        remoteName = remotePlayer
        localPlayer = LudoPlayer.RED
        val seed = System.currentTimeMillis()
        val level = LudoGenerator.generate(seed, 1, difficulty)
        val game = LudoEngine.createInitialGame(level)
        sharedGame = game
        isMyTurn = true
        publishSession(difficulty, seed, isActive = true)
        p2pSessionManager.send(
            P2PMessage.gameStart(
                levelSeed = seed,
                levelNumber = 1,
                hostName = localName,
                difficulty = difficulty.name
            )
        )
    }

    fun getGame(): LudoGame? = sharedGame

    suspend fun applyLocalRoll(): LudoGame? {
        if (!isMyTurn) return null
        val game = sharedGame ?: return null
        if (!game.needsRoll || game.currentPlayer != localPlayer) return null
        val updated = LudoEngine.rollDice(game)
        sharedGame = updated
        publishSession(updated.level.difficulty, updated.level.seed, isActive = true)
        p2pSessionManager.send(P2PMessage.move("roll"))
        return updated
    }

    suspend fun applyLocalMove(tokenIndex: Int): LudoGame? {
        if (!isMyTurn) return null
        val game = sharedGame ?: return null
        if (!game.canMove || game.currentPlayer != localPlayer) return null
        val dice = game.lastDiceRoll ?: return null
        val updated = LudoEngine.moveToken(game, tokenIndex) ?: return null
        sharedGame = updated
        if (!updated.isCompleted) {
            isMyTurn = updated.currentPlayer == localPlayer
            publishSession(updated.level.difficulty, updated.level.seed, isActive = true)
        }
        p2pSessionManager.send(P2PMessage.move("move:$tokenIndex:$dice"))
        return updated
    }

    suspend fun onRemoteMessage(message: P2PMessage): LudoGame? {
        return when (message.type) {
            P2PMessage.TYPE_GAME_START -> {
                val seed = message.levelSeed ?: return null
                val levelNumber = message.levelNumber ?: 1
                val difficulty = message.difficulty?.let {
                    runCatching { Difficulty.valueOf(it) }.getOrNull()
                } ?: Difficulty.MEDIUM
                val level = LudoGenerator.generate(seed, levelNumber, difficulty)
                val game = LudoEngine.createInitialGame(level)
                sharedGame = game
                localPlayer = LudoPlayer.BLUE
                isMyTurn = false
                remoteName = message.playerName ?: remoteName
                publishSession(difficulty, seed, isActive = true)
                game
            }
            P2PMessage.TYPE_MOVE -> {
                val payload = message.movePayload ?: return sharedGame
                val game = sharedGame ?: return null
                if (payload == "roll") {
                    if (isMyTurn) return sharedGame
                    val updated = LudoEngine.rollDice(game)
                    sharedGame = updated
                    isMyTurn = updated.currentPlayer == localPlayer
                    publishSession(updated.level.difficulty, updated.level.seed, isActive = true)
                    return updated
                }
                if (payload.startsWith("move:")) {
                    if (isMyTurn) return sharedGame
                    val parts = payload.removePrefix("move:").split(":")
                    if (parts.size != 2) return null
                    val tokenIndex = parts[0].toIntOrNull() ?: return null
                    val updated = LudoEngine.moveToken(game, tokenIndex) ?: return null
                    sharedGame = updated
                    if (!updated.isCompleted) {
                        isMyTurn = updated.currentPlayer == localPlayer
                        publishSession(updated.level.difficulty, updated.level.seed, isActive = true)
                    }
                    return updated
                }
                sharedGame
            }
            P2PMessage.TYPE_RESIGN -> {
                isMyTurn = false
                sharedGame
            }
            else -> sharedGame
        }
    }

    suspend fun resign() {
        p2pSessionManager.send(P2PMessage.resign())
    }

    fun onRoundWon(localWon: Boolean) {
        val session = _session.value ?: return
        val newLocal = session.localScore + if (localWon) 1 else 0
        val newRemote = session.remoteScore + if (localWon) 0 else 1
        val newLevel = LudoGenerator.generate(
            session.seed + newLocal + newRemote,
            newLocal + newRemote + 1,
            session.difficulty
        )
        val newGame = LudoEngine.createInitialGame(newLevel)
        sharedGame = newGame
        isMyTurn = if (localWon) {
            p2pSessionManager.role.value != P2PRole.HOST
        } else {
            p2pSessionManager.role.value == P2PRole.HOST
        }
        _session.value = session.copy(
            localScore = newLocal,
            remoteScore = newRemote,
            activePlayerName = if (isMyTurn) localName else remoteName
        )
    }

    fun end() {
        _session.value = null
        sharedGame = null
        isMyTurn = false
    }

    private fun publishSession(difficulty: Difficulty, seed: Long, isActive: Boolean) {
        _session.value = MultiplayerSession(
            mode = MultiplayerMode.LOCAL_P2P,
            localPlayerName = localName,
            remotePlayerName = remoteName,
            activePlayerName = if (isMyTurn) localName else remoteName,
            localScore = _session.value?.localScore ?: 0,
            remoteScore = _session.value?.remoteScore ?: 0,
            isActive = isActive,
            seed = seed,
            difficulty = difficulty
        )
    }
}
