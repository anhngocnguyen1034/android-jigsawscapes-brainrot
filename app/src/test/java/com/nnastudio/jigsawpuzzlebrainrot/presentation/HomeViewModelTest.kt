package com.nnastudio.jigsawpuzzlebrainrot.presentation

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GetPuzzleListUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveProgressUseCase
import com.nnastudio.jigsawpuzzlebrainrot.fake.FakeProgressRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val puzzleRepository = FakePuzzleRepository()
    private val progressRepository = FakeProgressRepository()

    private fun viewModel() = HomeViewModel(
        getPuzzleList = GetPuzzleListUseCase(puzzleRepository),
        observeProgress = ObserveProgressUseCase(progressRepository)
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
    fun `should expose best time after a puzzle is completed`() = runTest(dispatcher) {
        val viewModel = viewModel()
        progressRepository.saveResult(
            puzzleId = "capybara",
            difficulty = com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty.EASY,
            timeSeconds = 90,
            moves = 12
        )
        advanceUntilIdle()

        assertEquals(90, viewModel.uiState.value.progress["capybara"]?.bestTimeSeconds)
    }
}
