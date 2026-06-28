package com.ludo.engine

import androidx.compose.ui.graphics.Color
import com.ludo.domain.model.GameStatus
import com.ludo.domain.model.LudoGame
import com.ludo.domain.model.LudoLevel
import com.ludo.domain.model.LudoPlayer
import kotlin.random.Random

object LudoPalette {
    val red = Color(0xFFE53935)
    val redLight = Color(0xFFFFCDD2)
    val blue = Color(0xFF1E88E5)
    val blueLight = Color(0xFFBBDEFB)
    val boardGreen = Color(0xFF43A047)
    val pathCream = Color(0xFFFFF8E1)
    val safeGold = Color(0xFFFFD54F)
    val centerWhite = Color(0xFFFAFAFA)

    fun colorForPlayer(player: LudoPlayer): Color = when (player) {
        LudoPlayer.RED -> red
        LudoPlayer.BLUE -> blue
    }

    fun yardColorForPlayer(player: LudoPlayer): Color = when (player) {
        LudoPlayer.RED -> redLight
        LudoPlayer.BLUE -> blueLight
    }
}

object LudoEngine {

    const val TOKENS_PER_PLAYER = 4
    const val PLAYER_COUNT = 2
    const val TRACK_SIZE = 52
    const val HOME_STRETCH_LEN = 6
    const val HOME = -1
    const val FINISHED = 64
    const val BOARD_GRID = 15

    private val SAFE_CELLS = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    /** Main track cell coordinates on a 15x15 grid, clockwise from red entry. */
    val TRACK_COORDS: List<Pair<Int, Int>> = buildTrackCoords()

    /** Home stretch coordinates per player (6 cells each). */
    val RED_HOME_COORDS: List<Pair<Int, Int>> = listOf(
        7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5, 7 to 6
    )
    val BLUE_HOME_COORDS: List<Pair<Int, Int>> = listOf(
        7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9, 7 to 8
    )

    /** Yard slot positions for each player's 4 tokens. */
    val RED_YARD_COORDS: List<Pair<Int, Int>> = listOf(2 to 2, 2 to 4, 4 to 2, 4 to 4)
    val BLUE_YARD_COORDS: List<Pair<Int, Int>> = listOf(10 to 10, 10 to 12, 12 to 10, 12 to 12)

    fun startCell(player: LudoPlayer): Int = when (player) {
        LudoPlayer.RED -> 0
        LudoPlayer.BLUE -> 26
    }

    fun homeStretchStart(player: LudoPlayer): Int = when (player) {
        LudoPlayer.RED -> 52
        LudoPlayer.BLUE -> 58
    }

    fun defaultTokens(): List<List<Int>> =
        List(PLAYER_COUNT) { List(TOKENS_PER_PLAYER) { HOME } }

    fun createInitialGame(level: LudoLevel): LudoGame {
        val tokens = level.initialTokens?.map { it.toList() } ?: defaultTokens()
        return LudoGame(
            level = level,
            tokens = tokens,
            currentPlayer = LudoPlayer.RED
        )
    }

    fun rollDice(game: LudoGame, random: Random = Random.Default): LudoGame {
        if (!game.needsRoll || game.isCompleted) return game
        val dice = nextDiceValue(game, random)
        val valid = getValidMoves(game, dice)
        val grantsExtra = dice == 6 && valid.isNotEmpty()
        return if (valid.isEmpty()) {
            endTurn(game.copy(
                lastDiceRoll = dice,
                diceRollCount = game.diceRollCount + 1,
                lastPlayedAt = System.currentTimeMillis()
            ))
        } else {
            game.copy(
                lastDiceRoll = dice,
                diceRollCount = game.diceRollCount + 1,
                extraRoll = grantsExtra,
                selectedTokenIndex = null,
                lastPlayedAt = System.currentTimeMillis()
            )
        }
    }

    fun nextDiceValue(game: LudoGame, random: Random = Random.Default): Int {
        return if (game.level.useSeededDice) {
            Random(game.level.seed + game.diceRollCount).nextInt(1, 7)
        } else {
            random.nextInt(1, 7)
        }
    }

    fun getValidMoves(game: LudoGame, dice: Int? = game.lastDiceRoll): List<Int> {
        val roll = dice ?: return emptyList()
        if (game.isCompleted) return emptyList()
        val player = game.currentPlayer
        return (0 until TOKENS_PER_PLAYER).filter { tokenIndex ->
            computeNewPosition(player, game.tokens[player.ordinal][tokenIndex], roll) != null
        }
    }

    fun canMoveToken(game: LudoGame, tokenIndex: Int): Boolean =
        tokenIndex in getValidMoves(game)

    fun moveToken(game: LudoGame, tokenIndex: Int): LudoGame? {
        val dice = game.lastDiceRoll ?: return null
        if (tokenIndex !in getValidMoves(game, dice)) return null

        val player = game.currentPlayer
        val tokens = game.tokens.map { it.toMutableList() }.toMutableList()
        val currentPos = tokens[player.ordinal][tokenIndex]
        val newPos = computeNewPosition(player, currentPos, dice) ?: return null

        tokens[player.ordinal][tokenIndex] = newPos
        applyCapture(tokens, player, newPos)

        val winner = findWinner(tokens)
        val gameOver = winner != null
        val grantsExtra = game.extraRoll && dice == 6

        val afterMove = game.copy(
            tokens = tokens.map { it.toList() },
            moves = game.moves + 1,
            selectedTokenIndex = null,
            winner = winner,
            status = if (gameOver) GameStatus.COMPLETED else game.status,
            completedAt = if (gameOver) System.currentTimeMillis() else game.completedAt,
            lastPlayedAt = System.currentTimeMillis()
        )

        return if (gameOver) {
            afterMove.copy(lastDiceRoll = null, extraRoll = false)
        } else if (grantsExtra) {
            afterMove.copy(lastDiceRoll = null, extraRoll = false)
        } else {
            endTurn(afterMove)
        }
    }

    fun onTokenSelected(game: LudoGame, tokenIndex: Int): LudoGame {
        if (!game.canMove || tokenIndex !in 0 until TOKENS_PER_PLAYER) return game
        if (!canMoveToken(game, tokenIndex)) return game
        return moveToken(game, tokenIndex) ?: game
    }

    fun skipTurnIfNoMoves(game: LudoGame): LudoGame {
        val dice = game.lastDiceRoll ?: return game
        return if (getValidMoves(game, dice).isEmpty()) endTurn(game) else game
    }

    private fun endTurn(game: LudoGame): LudoGame = game.copy(
        currentPlayer = game.currentPlayer.opponent(),
        lastDiceRoll = null,
        extraRoll = false,
        selectedTokenIndex = null
    )

    fun computeNewPosition(player: LudoPlayer, currentPos: Int, steps: Int): Int? {
        when {
            currentPos == FINISHED -> return null
            currentPos == HOME -> return if (steps == 6) startCell(player) else null
            isInHomeStretch(currentPos, player) -> {
                val stretchEnd = homeStretchStart(player) + HOME_STRETCH_LEN - 1
                val newPos = currentPos + steps
                return when {
                    newPos > stretchEnd + 1 -> null
                    newPos == stretchEnd + 1 -> FINISHED
                    else -> newPos
                }
            }
            isOnTrack(currentPos) -> {
                val dist = distanceFromStart(player, currentPos)
                val total = dist + steps
                return when {
                    total < TRACK_SIZE -> (currentPos + steps) % TRACK_SIZE
                    total > TRACK_SIZE + HOME_STRETCH_LEN -> null
                    total == TRACK_SIZE + HOME_STRETCH_LEN -> FINISHED
                    else -> homeStretchStart(player) + (total - TRACK_SIZE)
                }
            }
            else -> return null
        }
    }

    fun isOnTrack(pos: Int): Boolean = pos in 0 until TRACK_SIZE

    fun isInHomeStretch(pos: Int, player: LudoPlayer): Boolean =
        pos in homeStretchStart(player) until homeStretchStart(player) + HOME_STRETCH_LEN

    fun distanceFromStart(player: LudoPlayer, trackPos: Int): Int =
        (trackPos - startCell(player) + TRACK_SIZE) % TRACK_SIZE

    fun isSafeCell(pos: Int): Boolean = pos in SAFE_CELLS

    private fun applyCapture(tokens: MutableList<MutableList<Int>>, player: LudoPlayer, landingPos: Int) {
        if (!isOnTrack(landingPos) || landingPos in SAFE_CELLS) return
        val opponent = player.opponent()
        val opponentTokens = tokens[opponent.ordinal]
        val onCell = opponentTokens.indices.filter { opponentTokens[it] == landingPos }
        if (onCell.size == 1) {
            opponentTokens[onCell.single()] = HOME
        }
    }

    private fun findWinner(tokens: List<List<Int>>): LudoPlayer? {
        LudoPlayer.entries.forEach { player ->
            if (tokens[player.ordinal].all { it == FINISHED }) return player
        }
        return null
    }

    fun isWon(game: LudoGame): Boolean = game.winner != null

    fun getHintMove(game: LudoGame): Pair<Int, Int>? {
        if (game.needsRoll) {
            return 0 to nextDiceValue(game)
        }
        val dice = game.lastDiceRoll ?: return null
        val player = game.currentPlayer
        var best: Pair<Int, Int>? = null
        var bestScore = Int.MIN_VALUE
        for (tokenIndex in getValidMoves(game, dice)) {
            val pos = game.tokens[player.ordinal][tokenIndex]
            val newPos = computeNewPosition(player, pos, dice) ?: continue
            val score = scoreMove(game, player, pos, newPos, dice)
            if (score > bestScore) {
                bestScore = score
                best = tokenIndex to dice
            }
        }
        return best
    }

    fun applyHint(game: LudoGame): LudoGame {
        val hint = getHintMove(game) ?: return game
        val rolled = if (game.needsRoll) rollDice(game, Random(game.level.seed + game.diceRollCount)) else game
        val moved = moveToken(rolled, hint.first) ?: rolled
        return moved.copy(hintsUsed = game.hintsUsed + 1)
    }

    private fun scoreMove(game: LudoGame, player: LudoPlayer, from: Int, to: Int, dice: Int): Int {
        var score = dice
        if (to == FINISHED) score += 100
        if (from == HOME && to == startCell(player)) score += 40
        if (isOnTrack(to) && to !in SAFE_CELLS) {
            val opponent = player.opponent()
            val onCell = game.tokens[opponent.ordinal].count { it == to }
            if (onCell == 1) score += 80
        }
        if (isInHomeStretch(to, player)) score += 30 + (to - homeStretchStart(player))
        if (isOnTrack(to)) score += distanceFromStart(player, to) / 4
        return score
    }

    fun getTokenCoord(player: LudoPlayer, tokenIndex: Int, position: Int): Pair<Int, Int> {
        if (position == HOME) {
            return yardCoord(player, tokenIndex)
        }
        if (position == FINISHED) {
            return homeStretchCoord(player, HOME_STRETCH_LEN - 1)
        }
        if (isOnTrack(position)) {
            return TRACK_COORDS[position]
        }
        if (isInHomeStretch(position, player)) {
            val index = position - homeStretchStart(player)
            return homeStretchCoord(player, index)
        }
        return BOARD_GRID / 2 to BOARD_GRID / 2
    }

    fun yardCoord(player: LudoPlayer, tokenIndex: Int): Pair<Int, Int> {
        val coords = if (player == LudoPlayer.RED) RED_YARD_COORDS else BLUE_YARD_COORDS
        return coords[tokenIndex % coords.size]
    }

    fun homeStretchCoord(player: LudoPlayer, index: Int): Pair<Int, Int> {
        val coords = if (player == LudoPlayer.RED) RED_HOME_COORDS else BLUE_HOME_COORDS
        return coords[index.coerceIn(0, coords.lastIndex)]
    }

    fun validateLevel(level: LudoLevel): Boolean = true

    fun solve(game: LudoGame): List<Pair<Int, Int>>? {
        if (isWon(game)) return emptyList()
        return getHintMove(game)?.let { listOf(it) }
    }

    fun optimalMoveCount(game: LudoGame): Int = 1

    fun getBestMove(game: LudoGame): Pair<Int, Int>? = getHintMove(game)

    fun getBestAiMove(game: LudoGame): Pair<Int, Int>? {
        if (game.needsRoll) return -1 to nextDiceValue(game)
        return getHintMove(game)
    }

    private fun buildTrackCoords(): List<Pair<Int, Int>> {
        val coords = mutableListOf<Pair<Int, Int>>()
        for (c in 1..5) coords.add(6 to c)
        for (r in 5 downTo 0) coords.add(r to 6)
        for (c in 7..8) coords.add(0 to c)
        for (r in 1..5) coords.add(r to 8)
        for (c in 9..14) coords.add(6 to c)
        coords.add(7 to 14)
        for (c in 13 downTo 9) coords.add(8 to c)
        for (r in 9..13) coords.add(r to 8)
        coords.add(14 to 7)
        coords.add(14 to 6)
        for (r in 13 downTo 9) coords.add(r to 6)
        for (c in 5 downTo 1) coords.add(8 to c)
        coords.add(7 to 0)
        for (r in 6 downTo 1) coords.add(r to 0)
        coords.add(0 to 1)
        for (c in 2..5) coords.add(0 to c)
        return coords.take(TRACK_SIZE)
    }
}
