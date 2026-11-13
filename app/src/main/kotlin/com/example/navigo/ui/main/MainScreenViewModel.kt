package com.example.navigo.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.navigo.data.MapRepository
import com.example.navigo.data.Place
import com.example.navigo.data.PlaceCategory
import com.example.navigo.data.RoutePlan
import com.example.navigo.data.TripHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.osmdroid.util.GeoPoint

enum class ActiveTab {
    MAP,
    FAVORITES,
    HISTORY
}

enum class MapLayerType(val displayName: String) {
    STANDARD("แผนที่มาตรฐาน"),
    SATELLITE("ดาวเทียม (Satellite)"),
    DARK("โหมดกลางคืน (Dark)")
}

data class MapUiState(
    val searchQuery: String = "",
    val selectedCategory: PlaceCategory = PlaceCategory.ALL,
    val places: List<Place> = emptyList(),
    val selectedPlace: Place? = null,
    val activeRoute: RoutePlan? = null,
    val userLocation: GeoPoint = GeoPoint(13.7563, 100.5018), // Bangkok City Center
    val isNavigating: Boolean = false,
    val activeTab: ActiveTab = ActiveTab.MAP,
    val mapLayer: MapLayerType = MapLayerType.STANDARD,
    val history: List<TripHistory> = emptyList(),
    val favorites: List<Place> = emptyList()
)

class MainScreenViewModel(
    private val repository: MapRepository = MapRepository()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(PlaceCategory.ALL)
    private val _selectedPlace = MutableStateFlow<Place?>(null)
    private val _activeRoute = MutableStateFlow<RoutePlan?>(null)
    private val _userLocation = MutableStateFlow(GeoPoint(13.7563, 100.5018))
    private val _isNavigating = MutableStateFlow(false)
    private val _activeTab = MutableStateFlow(ActiveTab.MAP)
    private val _mapLayer = MutableStateFlow(MapLayerType.STANDARD)

    val uiState: StateFlow<MapUiState> = combine(
        repository.places,
        repository.history,
        _searchQuery,
        _selectedCategory,
        _selectedPlace,
        _activeRoute,
        _userLocation,
        _isNavigating,
        _activeTab,
        _mapLayer
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val places = args[0] as List<Place>
        @Suppress("UNCHECKED_CAST")
        val history = args[1] as List<TripHistory>
        val query = args[2] as String
        val category = args[3] as PlaceCategory
        val selectedPlace = args[4] as Place?
        val route = args[5] as RoutePlan?
        val userLoc = args[6] as GeoPoint
        val navigating = args[7] as Boolean
        val tab = args[8] as ActiveTab
        val mapLayer = args[9] as MapLayerType

        val filteredPlaces = places.filter { p ->
            val matchesCategory = category == PlaceCategory.ALL || p.category == category
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.address.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        MapUiState(
            searchQuery = query,
            selectedCategory = category,
            places = filteredPlaces,
            selectedPlace = selectedPlace,
            activeRoute = route,
            userLocation = userLoc,
            isNavigating = navigating,
            activeTab = tab,
            mapLayer = mapLayer,
            history = history,
            favorites = places.filter { it.isFavorite }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MapUiState()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelect(category: PlaceCategory) {
        _selectedCategory.value = category
    }

    fun onSelectPlace(place: Place?) {
        _selectedPlace.value = place
        if (place != null) {
            _activeRoute.value = repository.calculateRoute(_userLocation.value, place)
        } else {
            _activeRoute.value = null
            _isNavigating.value = false
        }
    }

    fun onMapClick(geoPoint: GeoPoint) {
        val customPlace = repository.addCustomPin(geoPoint)
        onSelectPlace(customPlace)
    }

    fun onToggleFavorite(placeId: String) {
        repository.toggleFavorite(placeId)
    }

    fun onStartNavigation() {
        val place = _selectedPlace.value ?: return
        val route = _activeRoute.value ?: return
        _isNavigating.value = true
        _activeTab.value = ActiveTab.MAP
        repository.recordTrip(place, route.distanceKm)
    }

    fun onStopNavigation() {
        _isNavigating.value = false
    }

    fun onTabSelect(tab: ActiveTab) {
        _activeTab.value = tab
    }

    fun onToggleMapType() {
        _mapLayer.value = when (_mapLayer.value) {
            MapLayerType.STANDARD -> MapLayerType.SATELLITE
            MapLayerType.SATELLITE -> MapLayerType.DARK
            MapLayerType.DARK -> MapLayerType.STANDARD
        }
    }

    fun onUpdateUserLocation(geoPoint: GeoPoint) {
        _userLocation.value = geoPoint
        _selectedPlace.value?.let { place ->
            if (_activeRoute.value != null) {
                _activeRoute.value = repository.calculateRoute(geoPoint, place)
            }
        }
    }
}
