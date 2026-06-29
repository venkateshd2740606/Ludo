package com.ludo.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludo.R
import com.ludo.domain.model.LudoGame
import com.ludo.domain.model.LudoPlayer
import com.ludo.engine.LudoEngine
import com.ludo.engine.LudoPalette

@Composable
fun LudoBoard(
    game: LudoGame,
    reducedMotion: Boolean,
    onRollDice: () -> Unit,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val boardDescription = stringResource(R.string.color_sort)
    val validMoves = if (game.canMove) LudoEngine.getValidMoves(game) else emptyList()
    val hintToken = LudoEngine.getHintMove(game)?.first?.takeIf { it >= 0 }
    val activePlayers = LudoPlayer.activePlayers(game.level.playerCount)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .semantics { contentDescription = boardDescription },
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (LudoPlayer.GREEN in activePlayers) {
                PlayerBanner(
                    player = LudoPlayer.GREEN,
                    isActive = game.currentPlayer == LudoPlayer.GREEN && !game.isCompleted,
                    label = stringResource(R.string.player_green),
                    modifier = Modifier.weight(1f)
                )
            }
            if (LudoPlayer.YELLOW in activePlayers) {
                PlayerBanner(
                    player = LudoPlayer.YELLOW,
                    isActive = game.currentPlayer == LudoPlayer.YELLOW && !game.isCompleted,
                    label = stringResource(R.string.player_yellow),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .aspectRatio(1f)
        ) {
            val cellSize = maxWidth / LudoEngine.BOARD_GRID
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LudoPalette.boardGreen)
                    .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            ) {
                LudoGridBackground(cellSize, activePlayers)
                LudoTokens(
                    game = game,
                    cellSize = cellSize,
                    validMoves = validMoves,
                    hintToken = hintToken,
                    activePlayers = activePlayers,
                    onTokenClick = onTokenClick
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerBanner(
                player = LudoPlayer.RED,
                isActive = game.currentPlayer == LudoPlayer.RED && !game.isCompleted,
                label = stringResource(R.string.player_red),
                modifier = Modifier.weight(1f)
            )
            if (LudoPlayer.BLUE in activePlayers) {
                PlayerBanner(
                    player = LudoPlayer.BLUE,
                    isActive = game.currentPlayer == LudoPlayer.BLUE && !game.isCompleted,
                    label = stringResource(R.string.player_blue),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        DicePanel(
            dice = game.lastDiceRoll,
            canRoll = game.needsRoll && !game.isCompleted,
            onRollDice = onRollDice
        )
    }
}

@Composable
private fun PlayerBanner(
    player: LudoPlayer,
    isActive: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                LudoPalette.colorForPlayer(player).copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = if (isActive) "▶ $label" else label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = LudoPalette.colorForPlayer(player)
        )
    }
}

@Composable
private fun DicePanel(dice: Int?, canRoll: Boolean, onRollDice: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dice != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = "🎲 $dice",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Button(
            onClick = onRollDice,
            enabled = canRoll,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(stringResource(R.string.roll_dice))
        }
    }
}

@Composable
private fun LudoGridBackground(cellSize: Dp, activePlayers: List<LudoPlayer>) {
    for (row in 0 until LudoEngine.BOARD_GRID) {
        for (col in 0 until LudoEngine.BOARD_GRID) {
            val color = cellColor(row, col, activePlayers)
            if (color != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .offset(x = cellSize * col, y = cellSize * row)
                        .size(cellSize)
                        .background(color)
                )
            }
        }
    }
    LudoEngine.TRACK_COORDS.forEachIndexed { index, (row, col) ->
        val isSafe = LudoEngine.isSafeCell(index)
        Box(
            modifier = Modifier
                .offset(x = cellSize * col, y = cellSize * row)
                .size(cellSize)
                .background(
                    if (isSafe) LudoPalette.safeGold.copy(alpha = 0.55f) else LudoPalette.pathCream
                )
                .border(0.5.dp, Color.Black.copy(alpha = 0.08f))
        )
    }
    if (LudoPlayer.RED in activePlayers) {
        drawHomeStretch(cellSize, LudoEngine.RED_HOME_COORDS, LudoPalette.red)
    }
    if (LudoPlayer.GREEN in activePlayers) {
        drawHomeStretch(cellSize, LudoEngine.GREEN_HOME_COORDS, LudoPalette.green)
    }
    if (LudoPlayer.YELLOW in activePlayers) {
        drawHomeStretch(cellSize, LudoEngine.YELLOW_HOME_COORDS, LudoPalette.yellow)
    }
    if (LudoPlayer.BLUE in activePlayers) {
        drawHomeStretch(cellSize, LudoEngine.BLUE_HOME_COORDS, LudoPalette.blue)
    }
    Box(
        modifier = Modifier
            .offset(x = cellSize * 6, y = cellSize * 6)
            .size(cellSize * 3)
            .background(LudoPalette.centerWhite.copy(alpha = 0.9f))
    )
}

@Composable
private fun drawHomeStretch(cellSize: Dp, coords: List<Pair<Int, Int>>, color: Color) {
    coords.forEach { (row, col) ->
        Box(
            modifier = Modifier
                .offset(x = cellSize * col, y = cellSize * row)
                .size(cellSize)
                .background(color.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun LudoTokens(
    game: LudoGame,
    cellSize: Dp,
    validMoves: List<Int>,
    hintToken: Int?,
    activePlayers: List<LudoPlayer>,
    onTokenClick: (Int) -> Unit
) {
    val tokenSize = cellSize * 0.72f
    val offset = cellSize * 0.14f

    activePlayers.forEach { player ->
        val isCurrent = game.currentPlayer == player
        game.tokens[player.ordinal].forEachIndexed { tokenIndex, position ->
            if (position == LudoEngine.FINISHED) return@forEachIndexed
            val (row, col) = LudoEngine.getTokenCoord(player, tokenIndex, position)
            val canClick = isCurrent && game.canMove && tokenIndex in validMoves
            val isHinted = isCurrent && hintToken == tokenIndex
            Box(
                modifier = Modifier
                    .offset(x = cellSize * col + offset, y = cellSize * row + offset)
                    .size(tokenSize)
                    .clip(CircleShape)
                    .background(LudoPalette.colorForPlayer(player))
                    .then(
                        if (isHinted) {
                            Modifier.border(3.dp, LudoPalette.safeGold, CircleShape)
                        } else if (canClick) {
                            Modifier
                                .border(2.dp, Color.White, CircleShape)
                                .clickable { onTokenClick(tokenIndex) }
                        } else {
                            Modifier.border(1.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${tokenIndex + 1}",
                    color = Color.White,
                    fontSize = (cellSize.value * 0.28f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun cellColor(row: Int, col: Int, activePlayers: List<LudoPlayer>): Color {
    val inRedYard = row in 0..5 && col in 0..5
    val inGreenYard = row in 0..5 && col in 9..14
    val inYellowYard = row in 9..14 && col in 0..5
    val inBlueYard = row in 9..14 && col in 9..14
    return when {
        inRedYard && LudoPlayer.RED in activePlayers -> LudoPalette.redLight
        inGreenYard && LudoPlayer.GREEN in activePlayers -> LudoPalette.greenLight
        inYellowYard && LudoPlayer.YELLOW in activePlayers -> LudoPalette.yellowLight
        inBlueYard && LudoPlayer.BLUE in activePlayers -> LudoPalette.blueLight
        else -> Color.Transparent
    }
}

@Composable
fun GameStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
    )
}
