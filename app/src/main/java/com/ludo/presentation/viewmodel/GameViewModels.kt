package com.ludo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludo.analytics.AnalyticsManager
import com.ludo.domain.model.ChallengeType
import com.ludo.domain.model.Difficulty
import com.ludo.domain.model.MultiplayerSession
import com.ludo.domain.model.PuzzleProfile
import com.ludo.domain.model.LudoGame
import com.ludo.domain.model.LudoLevelMode
import com.ludo.domain.model.LudoPlayer
import com.ludo.domain.model.UserPreferences
import com.ludo.domain.model.UserStats
import com.ludo.domain.repository.ChallengeRepository
import com.ludo.domain.repository.GameRepository
import com.ludo.domain.repository.PreferencesRepository
import com.ludo.domain.repository.ProgressionRepository
import com.ludo.domain.model.P2PRole
import com.ludo.engine.LudoEngine
import com.ludo.engine.LudoGenerator
import com.ludo.multiplayer.NetworkPuzzleSession
import com.ludo.multiplayer.PuzzleBotSession
import com.ludo.multiplayer.SameDeviceSession
import com.ludo.network.P2PSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val inProgressGame: LudoGame? = null,
    val stats: UserStats = UserStats(),
    val puzzleProfile: PuzzleProfile = PuzzleProfile(),
    val preferences: UserPreferences = UserPreferences(),
    val coins: Int = 0,
    val isLoading: Boolean = true
)

enum class GameLoadError {
    GAME_NOT_FOUND,
    CHALLENGE_ALREADY_COMPLETED,
    TUTORIAL_NOT_FOUND
}

data class WinDialogState(
    val challengeRewardCoins: Int = 0,
    val challengeRewardXp: Int = 0,
    val showNextLevel: Boolean = false,
    val nextDifficulty: Difficulty? = null,
    val nextLevelNumber: Int = 1,
    val sameDeviceLocalScore: Int = 0,
    val sameDeviceRemoteScore: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val progressionRepository: ProgressionRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            preferencesRepository.getUserPreferences().collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
        viewModelScope.launch {
            progressionRepository.observePuzzleProfile().collect { profile ->
                _uiState.update { it.copy(puzzleProfile = profile) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val game = gameRepository.getInProgressGame()
            val stats = progressionRepository.getStats()
            val economy = progressionRepository.getEconomy()
            _uiState.update {
                it.copy(
                    inProgressGame = game,
                    stats = stats,
                    coins = economy.coins,
                    isLoading = false
                )
            }
        }
    }
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val progressionRepository: ProgressionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val challengeRepository: ChallengeRepository,
    private val analyticsManager: AnalyticsManager,
    private val sameDeviceSession: SameDeviceSession,
    private val networkPuzzleSession: NetworkPuzzleSession,
    private val puzzleBotSession: PuzzleBotSession,
    private val p2pSessionManager: P2PSessionManager
) : ViewModel() {

    companion object {
        const val HINTS_PER_GAME = 3
    }

    private val _game = MutableStateFlow<LudoGame?>(null)
    val game: StateFlow<LudoGame?> = _game.asStateFlow()

    private val _showWinDialog = MutableStateFlow(false)
    val showWinDialog: StateFlow<Boolean> = _showWinDialog.asStateFlow()

    private val _winDialogState = MutableStateFlow(WinDialogState())
    val winDialogState: StateFlow<WinDialogState> = _winDialogState.asStateFlow()

    private val _shareText = MutableStateFlow<String?>(null)
    val shareText: StateFlow<String?> = _shareText.asStateFlow()

    private val _coins = MutableStateFlow(0)
    val coins: StateFlow<Int> = _coins.asStateFlow()

    private val _hintsRemaining = MutableStateFlow(HINTS_PER_GAME)
    val hintsRemaining: StateFlow<Int> = _hintsRemaining.asStateFlow()

    private val _showNoHintsDialog = MutableStateFlow(false)
    val showNoHintsDialog: StateFlow<Boolean> = _showNoHintsDialog.asStateFlow()

    private val _loadError = MutableStateFlow<GameLoadError?>(null)
    val loadError: StateFlow<GameLoadError?> = _loadError.asStateFlow()

    val sameDeviceSessionState: StateFlow<MultiplayerSession?> = sameDeviceSession.session
    val networkSessionState: StateFlow<MultiplayerSession?> = networkPuzzleSession.session
    val botSessionState: StateFlow<MultiplayerSession?> = puzzleBotSession.session

    private var sameDeviceMode = false
    private var networkP2PMode = false
    private var botMode = false
    private var p2pListenerJob: Job? = null
    private var campaignMode = false
    private var activeChallengeType: ChallengeType? = null
    private var endlessWave = 1

    init {
        refreshCoins()
    }

    private fun refreshCoins() {
        viewModelScope.launch {
            _coins.value = progressionRepository.getEconomy().coins
        }
    }

    private fun resetHints() {
        _hintsRemaining.value = HINTS_PER_GAME
    }

    private fun resetSessionFlags() {
        sameDeviceMode = false
        networkP2PMode = false
        botMode = false
        p2pListenerJob?.cancel()
        p2pListenerJob = null
        campaignMode = false
        campaignVsAi = false
        activeChallengeType = null
        activeSessionKey = null
        resetHints()
        _loadError.value = null
    }

    fun clearLoadError() {
        _loadError.value = null
    }

    private var activeSessionKey: String? = null

    fun initializeSession(sessionKey: String, initializer: () -> Unit) {
        if (activeSessionKey == sessionKey && _game.value != null) return
        activeSessionKey = sessionKey
        initializer()
    }

    fun loadGame(gameId: Long?) {
        viewModelScope.launch {
            resetSessionFlags()
            sameDeviceSession.end()
            val loaded = if (gameId != null) gameRepository.getGame(gameId) else null
            if (loaded == null) {
                _loadError.value = GameLoadError.GAME_NOT_FOUND
            } else {
                _game.value = loaded
                analyticsManager.logGameStarted(loaded.level.difficulty, loaded.level.levelNumber)
            }
        }
    }

    fun startNewGame(difficulty: Difficulty) {
        viewModelScope.launch {
            resetSessionFlags()
            sameDeviceSession.end()
            campaignMode = true
            val levelNumber = preferencesRepository.getCampaignLevel(difficulty)
            val newGame = gameRepository.createNewGame(difficulty, levelNumber)
            campaignVsAi = newGame.level.mode == LudoLevelMode.VS_AI
            val id = gameRepository.saveGame(newGame)
            _game.value = newGame.copy(id = id)
            analyticsManager.logGameStarted(difficulty, levelNumber)
        }
    }

    fun startFromSeed(seed: Long, levelNumber: Int, difficulty: Difficulty) {
        viewModelScope.launch {
            resetSessionFlags()
            sameDeviceSession.end()
            val seededGame = gameRepository.createGameFromSeed(seed, levelNumber, difficulty)
            val id = gameRepository.saveGame(seededGame)
            _game.value = seededGame.copy(id = id)
            analyticsManager.logGameStarted(difficulty, levelNumber)
        }
    }

    fun startSameDevice(playerOne: String, playerTwo: String, difficulty: Difficulty) {
        viewModelScope.launch {
            resetSessionFlags()
            sameDeviceSession.end()
            networkPuzzleSession.end()
            puzzleBotSession.end()
            sameDeviceMode = true
            sameDeviceSession.start(playerOne, playerTwo, difficulty)
            val activeGame = sameDeviceSession.getActiveGame() ?: return@launch
            val id = gameRepository.saveGame(activeGame)
            _game.value = activeGame.copy(id = id)
            analyticsManager.logMultiplayerStarted("same_device")
            progressionRepository.checkAndUnlockAchievements(activeGame, sameDevicePlayed = true)
        }
    }

    fun startNetworkP2P(difficulty: Difficulty, localName: String = "You", remoteName: String = "Friend") {
        viewModelScope.launch {
            resetSessionFlags()
            sameDeviceSession.end()
            puzzleBotSession.end()
            networkP2PMode = true
            p2pListenerJob = viewModelScope.launch {
                p2pSessionManager.incoming.collect { message ->
                    val updated = networkPuzzleSession.onRemoteMessage(message) ?: return@collect
                    _game.value = updated
                    gameRepository.saveGame(updated)
                    if (updated.isCompleted) {
                        val role = p2pSessionManager.role.value
                        val localWon = when (role) {
                            P2PRole.HOST -> updated.winner == LudoPlayer.RED
                            P2PRole.CLIENT -> updated.winner == LudoPlayer.BLUE
                            null -> false
                        }
                        handleNetworkRoundComplete(localWon = localWon)
                    }
                }
            }
            when (p2pSessionManager.role.value) {
                P2PRole.HOST -> {
                    networkPuzzleSession.startAsHost(localName, remoteName, difficulty)
                    val activeGame = networkPuzzleSession.getGame() ?: return@launch
                    val id = gameRepository.saveGame(activeGame)
                    _game.value = activeGame.copy(id = id)
                }
                P2PRole.CLIENT -> Unit
                null -> return@launch
            }
            analyticsManager.logMultiplayerStarted("network_p2p")
        }
    }

    fun startVsAi(difficulty: Difficulty, playerName: String = "You") {
        viewModelScope.launch {
            resetSessionFlags()
            sameDeviceSession.end()
            networkPuzzleSession.end()
            botMode = true
            puzzleBotSession.start(playerName, difficulty)
            val activeGame = puzzleBotSession.getPlayerGame() ?: return@launch
            val id = gameRepository.saveGame(activeGame)
            _game.value = activeGame.copy(id = id)
            analyticsManager.logMultiplayerStarted("vs_ai")
        }
    }

    private fun runBotTurn() {
        viewModelScope.launch {
            delay(700)
            val updated = puzzleBotSession.applyBotTurn() ?: return@launch
            _game.value = updated
            gameRepository.saveGame(updated)
            if (updated.isCompleted) {
                handleBotRoundComplete(botWon = updated.winner == LudoPlayer.BLUE)
            } else if (updated.currentPlayer == LudoPlayer.BLUE) {
                runBotTurn()
            }
        }
    }

    private fun handleNetworkRoundComplete(localWon: Boolean) {
        networkPuzzleSession.onRoundWon(localWon)
        val session = networkPuzzleSession.session.value
        networkPuzzleSession.getGame()?.let { nextGame ->
            _game.value = nextGame
            viewModelScope.launch { gameRepository.saveGame(nextGame) }
        }
        _winDialogState.value = WinDialogState(
            sameDeviceLocalScore = session?.localScore ?: 0,
            sameDeviceRemoteScore = session?.remoteScore ?: 0
        )
        _showWinDialog.value = true
    }

    private fun handleBotRoundComplete(botWon: Boolean) {
        if (botWon) puzzleBotSession.onBotWon() else puzzleBotSession.onPlayerWon()
        val session = puzzleBotSession.session.value
        puzzleBotSession.getPlayerGame()?.let { nextGame ->
            _game.value = nextGame
            viewModelScope.launch { gameRepository.saveGame(nextGame) }
        }
        _winDialogState.value = WinDialogState(
            sameDeviceLocalScore = session?.localScore ?: 0,
            sameDeviceRemoteScore = session?.remoteScore ?: 0
        )
        _showWinDialog.value = true
    }

    fun startTutorial(index: Int) {
        viewModelScope.launch {
            resetSessionFlags()
            sameDeviceSession.end()
            val tutorial = gameRepository.createTutorialGame(index)
            if (tutorial == null) {
                _loadError.value = GameLoadError.TUTORIAL_NOT_FOUND
                return@launch
            }
            val id = gameRepository.saveGame(tutorial)
            _game.value = tutorial.copy(id = id)
            analyticsManager.logGameStarted(Difficulty.BEGINNER, index + 1)
        }
    }

    fun startEndless(wave: Int) {
        viewModelScope.launch {
            resetSessionFlags()
            sameDeviceSession.end()
            endlessWave = wave
            val endless = gameRepository.createEndlessGame(wave)
            val id = gameRepository.saveGame(endless)
            _game.value = endless.copy(id = id)
            analyticsManager.logGameStarted(Difficulty.ENDLESS, wave)
        }
    }

    fun startChallenge(type: ChallengeType) {
        viewModelScope.launch {
            resetSessionFlags()
            sameDeviceSession.end()
            val record = challengeRepository.resolveActiveChallenge(type)
            if (record.isCompleted) {
                _loadError.value = GameLoadError.CHALLENGE_ALREADY_COMPLETED
                return@launch
            }
            activeChallengeType = type
            val challengeGame = challengeRepository.getChallengeGame(record)
            val id = gameRepository.saveGame(challengeGame)
            _game.value = challengeGame.copy(id = id)
            analyticsManager.logGameStarted(record.difficulty, challengeGame.level.levelNumber)
        }
    }

    private var campaignVsAi = false

    fun onRollDice() {
        val current = _game.value ?: return
        when {
            networkP2PMode -> {
                if (!networkPuzzleSession.isLocalTurn) return
                viewModelScope.launch {
                    val result = networkPuzzleSession.applyLocalRoll() ?: return@launch
                    _game.value = result
                    gameRepository.saveGame(result)
                }
                return
            }
            botMode -> {
                val updated = puzzleBotSession.applyPlayerRoll() ?: return
                _game.value = updated
                viewModelScope.launch { gameRepository.saveGame(updated) }
                if (updated.isCompleted) {
                    handleBotRoundComplete(botWon = updated.winner == LudoPlayer.BLUE)
                } else if (updated.currentPlayer == LudoPlayer.BLUE) {
                    runBotTurn()
                }
                return
            }
            sameDeviceMode -> {
                val updated = sameDeviceSession.applyRoll() ?: return
                _game.value = updated
                viewModelScope.launch { gameRepository.saveGame(updated) }
                if (updated.isCompleted) handleCompletion(updated)
                return
            }
            campaignVsAi && current.currentPlayer == LudoPlayer.BLUE -> return
            else -> {
                val updated = LudoEngine.rollDice(current)
                _game.value = updated
                viewModelScope.launch { gameRepository.saveGame(updated) }
                if (updated.isCompleted) {
                    handleCompletion(updated)
                } else if (campaignVsAi && updated.currentPlayer == LudoPlayer.BLUE) {
                    runCampaignBotTurn()
                }
            }
        }
    }

    fun onTokenClick(tokenIndex: Int) {
        val current = _game.value ?: return
        when {
            networkP2PMode -> {
                if (!networkPuzzleSession.isLocalTurn) return
                viewModelScope.launch {
                    val result = networkPuzzleSession.applyLocalMove(tokenIndex) ?: return@launch
                    _game.value = result
                    gameRepository.saveGame(result)
                    if (result.isCompleted) {
                        val role = p2pSessionManager.role.value
                        val localWon = when (role) {
                            P2PRole.HOST -> result.winner == LudoPlayer.RED
                            P2PRole.CLIENT -> result.winner == LudoPlayer.BLUE
                            null -> false
                        }
                        handleNetworkRoundComplete(localWon = localWon)
                    }
                }
                return
            }
            botMode -> {
                val updated = puzzleBotSession.applyPlayerMove(tokenIndex) ?: return
                _game.value = updated
                viewModelScope.launch { gameRepository.saveGame(updated) }
                when {
                    updated.isCompleted -> handleBotRoundComplete(botWon = updated.winner == LudoPlayer.BLUE)
                    updated.currentPlayer == LudoPlayer.BLUE -> runBotTurn()
                }
                return
            }
            sameDeviceMode -> {
                val updated = sameDeviceSession.applyTokenMove(tokenIndex) ?: return
                _game.value = updated
                viewModelScope.launch { gameRepository.saveGame(updated) }
                if (updated.isCompleted) handleCompletion(updated)
                return
            }
            campaignVsAi && current.currentPlayer == LudoPlayer.BLUE -> return
            else -> {
                val updated = LudoEngine.moveToken(current, tokenIndex) ?: return
                _game.value = updated
                viewModelScope.launch { gameRepository.saveGame(updated) }
                when {
                    updated.isCompleted -> handleCompletion(updated)
                    campaignVsAi && updated.currentPlayer == LudoPlayer.BLUE -> runCampaignBotTurn()
                }
            }
        }
    }

    private fun runCampaignBotTurn() {
        viewModelScope.launch {
            delay(600)
            val current = _game.value ?: return@launch
            if (current.isCompleted || current.currentPlayer != LudoPlayer.BLUE) return@launch
            var state = current
            if (state.needsRoll) {
                state = LudoEngine.rollDice(state)
            }
            val move = LudoEngine.getBestAiMove(state) ?: return@launch
            if (move.first < 0) {
                _game.value = state
                gameRepository.saveGame(state)
                if (state.currentPlayer == LudoPlayer.RED) return@launch
                runCampaignBotTurn()
                return@launch
            }
            val updated = LudoEngine.moveToken(state, move.first) ?: return@launch
            _game.value = updated
            gameRepository.saveGame(updated)
            if (updated.isCompleted) {
                handleCompletion(updated)
            } else if (updated.currentPlayer == LudoPlayer.BLUE) {
                runCampaignBotTurn()
            }
        }
    }

    private fun handleCompletion(updated: LudoGame) {
        if (sameDeviceMode) {
            val session = sameDeviceSession.session.value
            sameDeviceSession.getActiveGame()?.let { nextGame ->
                _game.value = nextGame
                viewModelScope.launch { gameRepository.saveGame(nextGame) }
            }
            _shareText.value = null
            _winDialogState.value = WinDialogState(
                sameDeviceLocalScore = session?.localScore ?: 0,
                sameDeviceRemoteScore = session?.remoteScore ?: 0
            )
            _showWinDialog.value = true
            return
        }
        if (networkP2PMode || botMode) return

        viewModelScope.launch {
            val completed = gameRepository.completeGame(updated)
            progressionRepository.updateStatsAfterGame(completed)
            val unlocked = progressionRepository.checkAndUnlockAchievements(completed)
            unlocked.forEach { analyticsManager.logAchievementUnlocked(it.id) }

            analyticsManager.logGameCompleted(
                completed.level.difficulty,
                completed.elapsedSeconds,
                completed.moves,
                completed.hintsUsed
            )
            analyticsManager.setUserLevel(progressionRepository.getStats().level)

            var rewardCoins = 0
            var rewardXp = 0
            var showNextLevel = false
            var nextLevel = completed.level.levelNumber
            val nextDifficulty = completed.level.difficulty

            if (activeChallengeType != null) {
                val record = challengeRepository.resolveActiveChallenge(activeChallengeType!!)
                if (!record.isCompleted) {
                    val done = challengeRepository.completeChallenge(
                        record,
                        completed.elapsedSeconds,
                        completed.moves
                    )
                    progressionRepository.grantChallengeRewards(done.rewardCoins, done.rewardXp)
                    rewardCoins = done.rewardCoins
                    rewardXp = done.rewardXp
                    val streak = challengeRepository.getCurrentStreak(activeChallengeType!!)
                    analyticsManager.logChallengeCompleted(activeChallengeType!!.name, streak)
                }
            } else if (campaignMode && !completed.level.isTutorial && !completed.level.isEndless) {
                nextLevel = preferencesRepository.advanceCampaignLevel(completed.level.difficulty)
                showNextLevel = true
            } else if (completed.level.isEndless) {
                endlessWave += 1
                showNextLevel = true
                nextLevel = endlessWave
            }

            refreshCoins()
            _shareText.value = LudoGenerator.formatShareText(
                completed.level.seed,
                completed.level.levelNumber,
                completed.level.difficulty
            )
            _winDialogState.value = WinDialogState(
                challengeRewardCoins = rewardCoins,
                challengeRewardXp = rewardXp,
                showNextLevel = showNextLevel,
                nextDifficulty = if (completed.level.isEndless) Difficulty.ENDLESS else nextDifficulty,
                nextLevelNumber = nextLevel
            )
            _showWinDialog.value = true
        }
    }

    fun requestHint() {
        val current = _game.value ?: return
        if (LudoEngine.getHintMove(current) == null) return
        if (_hintsRemaining.value <= 0) {
            _showNoHintsDialog.value = true
            return
        }
        applyHintInternal()
    }

    fun dismissNoHintsDialog() {
        _showNoHintsDialog.value = false
    }

    fun showNoHintsDialog() {
        _showNoHintsDialog.value = true
    }

    fun onHintAdRewarded(@Suppress("UNUSED_PARAMETER") hintsGranted: Int = HINTS_PER_GAME) {
        _showNoHintsDialog.value = false
        _hintsRemaining.value = HINTS_PER_GAME
        applyHintInternal()
    }

    private fun applyHintInternal() {
        val current = _game.value ?: return
        if (_hintsRemaining.value <= 0) return
        _hintsRemaining.value -= 1
        val updated = LudoEngine.applyHint(current)
        _game.value = updated
        viewModelScope.launch { gameRepository.saveGame(updated) }
        if (updated.isCompleted) handleCompletion(updated)
    }

    fun startNextLevel() {
        val state = _winDialogState.value
        val difficulty = state.nextDifficulty ?: return
        dismissWinDialog()
        if (difficulty == Difficulty.ENDLESS) {
            startEndless(state.nextLevelNumber)
        } else {
            viewModelScope.launch {
                resetSessionFlags()
                campaignMode = true
                val newGame = gameRepository.createNewGame(difficulty, state.nextLevelNumber)
                val id = gameRepository.saveGame(newGame)
                _game.value = newGame.copy(id = id)
                analyticsManager.logGameStarted(difficulty, state.nextLevelNumber)
            }
        }
    }

    fun tickElapsed() {
        _game.update { game ->
            game?.takeIf { !it.isCompleted }?.copy(elapsedSeconds = game.elapsedSeconds + 1)
        }
    }

    fun updateElapsed(seconds: Long) {
        _game.update { it?.copy(elapsedSeconds = seconds) }
    }

    fun dismissWinDialog() {
        _showWinDialog.value = false
        _shareText.value = null
        _winDialogState.value = WinDialogState()
    }

    fun endMultiplayerSession() {
        sameDeviceMode = false
        networkP2PMode = false
        botMode = false
        p2pListenerJob?.cancel()
        sameDeviceSession.end()
        networkPuzzleSession.end()
        puzzleBotSession.end()
        p2pSessionManager.disconnect()
    }

    fun endSameDeviceSession() = endMultiplayerSession()

    override fun onCleared() {
        if (networkP2PMode) {
            p2pSessionManager.disconnect()
        }
        super.onCleared()
    }
}
