package com.nnastudio.jigsawpuzzlebrainrot.presentation

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceOffset
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PiecePlacement
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.puzzleOfDay
import com.nnastudio.jigsawpuzzlebrainrot.utils.currentEpochDay
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GetPuzzleListUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveFavoritesUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveProgressUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveSavedProgressUseCase
import com.nnastudio.jigsawpuzzlebrainrot.fake.FakeFavoriteRepository
import com.nnastudio.jigsawpuzzlebrainrot.fake.FakeProgressRepository
import com.nnastudio.jigsawpuzzlebrainrot.fake.FakeSavedGameRepository
import com.nnastudio.jigsawpuzzlebrainrot.fake.FakePuzzleRepository
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val puzzleRepository = FakePuzzleRepository()
    private val progressRepository = FakeProgressRepository()
    private val savedGameRepository = FakeSavedGameRepository()
    private val favoriteRepository = FakeFavoriteRepository()

    private fun viewModel() = HomeViewModel(
        getPuzzleList = GetPuzzleListUseCase(puzzleRepository),
        observeProgress = ObserveProgressUseCase(progressRepository),
        observeSavedProgress = ObserveSavedProgressUseCase(savedGameRepository),
        observeFavorites = ObserveFavoritesUseCase(favoriteRepository)
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `should go from loading to success with all puzzles`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(true, viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(FakePuzzleRepository.defaultPuzzles.size, state.puzzles.size)
    }

    @Test
    fun `should filter puzzles when a category is selected`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCategorySelected(PuzzleCategory.ANIMAL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.puzzles.size)
        assertEquals(PuzzleCategory.ANIMAL, state.puzzles.first().category)
    }

    @Test
    fun `should expose the percent of an unfinished game and nothing for untouched puzzles`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            savedGameRepository.save(savedGame(puzzleId = "capybara", placed = 1, total = 4))
            advanceUntilIdle()

            val percent = viewModel.uiState.value.progressPercent
            assertEquals(25, percent["capybara"])
            assertNull(percent["tralalero"])
        }

    @Test
    fun `should show a completed puzzle as one hundred percent`() = runTest(dispatcher) {
        val viewModel = viewModel()
        progressRepository.saveResult(
            puzzleId = "capybara",
            difficulty = Difficulty.EASY,
            timeSeconds = 90,
            moves = 12,
            score = 420
        )
        advanceUntilIdle()

        assertEquals(100, viewModel.uiState.value.progressPercent["capybara"])
        assertEquals(420, viewModel.uiState.value.bestScore["capybara"])
    }

    /** Van dang do dang: [placed] tren [total] manh da vao dung o. */
    private fun savedGame(puzzleId: String, placed: Int, total: Int) = SavedGame(
        puzzleId = puzzleId,
        difficulty = Difficulty.EASY,
        seed = 1L,
        placements = (0 until total).map { id ->
            PiecePlacement(pieceId = id, position = PieceOffset.Zero, isPlaced = id < placed)
        },
        groups = (0 until total).associateWith { it },
        trayOrder = emptyList(),
        moves = 0,
        elapsedSeconds = 0,
        hintsLeft = 0,
        score = 0
    )

    @Test
    fun `should list unfinished puzzles and pick one puzzle of the day`() = runTest(dispatcher) {
        val viewModel = viewModel()
        savedGameRepository.save(savedGame(puzzleId = "capybara", placed = 2, total = 4))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("capybara"), state.inProgress.map { it.id })
        assertEquals(FakePuzzleRepository.defaultPuzzles.size, state.allPuzzles.size)
        assertEquals(
            FakePuzzleRepository.defaultPuzzles.puzzleOfDay(currentEpochDay())?.id,
            state.dailyPuzzle?.id
        )
    }

    @Test
    fun `should keep showing the list when the same category is picked twice`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onCategorySelected(null)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `should collect the puzzles marked as favorite`() = runTest(dispatcher) {
        val viewModel = viewModel()
        favoriteRepository.toggle("capybara")
        advanceUntilIdle()

        assertEquals(listOf("capybara"), viewModel.uiState.value.favorites.map { it.id })

        favoriteRepository.toggle("capybara")
        advanceUntilIdle()

        assertEquals(emptyList<String>(), viewModel.uiState.value.favorites.map { it.id })
    }

    @Test
    fun `should expose best time after a puzzle is completed`() = runTest(dispatcher) {
        val viewModel = viewModel()
        progressRepository.saveResult(
            puzzleId = "capybara",
            difficulty = Difficulty.EASY,
            timeSeconds = 90,
            moves = 12,
            score = 420
        )
        advanceUntilIdle()

        assertEquals(90, viewModel.uiState.value.progress["capybara"]?.bestTimeSeconds)
    }
}
