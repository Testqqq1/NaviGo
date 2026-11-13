package com.example.navigo.ui.main

import com.example.navigo.data.MapRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initialState_hasPlacesAndDefaults() = runTest(testDispatcher) {
        val repository = MapRepository()
        val viewModel = MainScreenViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.places.isNotEmpty())
        assertEquals(ActiveTab.MAP, state.activeTab)
    }

    @Test
    fun uiState_filteringBySearch_returnsMatchingPlace() = runTest(testDispatcher) {
        val repository = MapRepository()
        val viewModel = MainScreenViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("Paragon")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.places.any { it.name.contains("Paragon", ignoreCase = true) })
    }

    @Test
    fun uiState_selectingPlace_createsActiveRoute() = runTest(testDispatcher) {
        val repository = MapRepository()
        val viewModel = MainScreenViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        val targetPlace = repository.places.value.first()
        viewModel.onSelectPlace(targetPlace)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(targetPlace, state.selectedPlace)
        assertNotNull(state.activeRoute)
        assertTrue(state.activeRoute!!.distanceKm > 0)
    }

    @Test
    fun uiState_toggleFavorite_updatesFavoriteList() = runTest(testDispatcher) {
        val repository = MapRepository()
        val viewModel = MainScreenViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        val targetPlace = repository.places.value.first { !it.isFavorite }
        viewModel.onToggleFavorite(targetPlace.id)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.favorites.any { it.id == targetPlace.id })
    }
}
