package com.example.navigo.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.navigo.data.Place
import com.example.navigo.data.PlaceCategory
import com.example.navigo.data.TripHistory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(state.userLocation, 14f)
    }

    // Effect to animate camera when a place is selected
    LaunchedEffect(state.selectedPlace) {
        state.selectedPlace?.let { place ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(place.latLng, 15f),
                durationMs = 800
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!state.isNavigating) {
                NavigationBar {
                    NavigationBarItem(
                        selected = state.activeTab == ActiveTab.MAP,
                        onClick = { viewModel.onTabSelect(ActiveTab.MAP) },
                        icon = { Icon(Icons.Default.Map, contentDescription = "แผนที่") },
                        label = { Text("แผนที่") }
                    )
                    NavigationBarItem(
                        selected = state.activeTab == ActiveTab.FAVORITES,
                        onClick = { viewModel.onTabSelect(ActiveTab.FAVORITES) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "รายการโปรด") },
                        label = { Text("รายการโปรด") }
                    )
                    NavigationBarItem(
                        selected = state.activeTab == ActiveTab.HISTORY,
                        onClick = { viewModel.onTabSelect(ActiveTab.HISTORY) },
                        icon = { Icon(Icons.Default.History, contentDescription = "ประวัติ") },
                        label = { Text("ประวัติ") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.activeTab) {
                ActiveTab.MAP -> {
                    MapTabContent(
                        state = state,
                        cameraPositionState = cameraPositionState,
                        onMapClick = { viewModel.onMapClick(it) },
                        onPlaceClick = { viewModel.onSelectPlace(it) },
                        onRecenter = {
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(state.userLocation, 15f)
                                )
                            }
                        },
                        onToggleMapType = { viewModel.onToggleMapType() },
                        onToggleFavorite = { viewModel.onToggleFavorite(it) },
                        onStartNavigation = { viewModel.onStartNavigation() },
                        onStopNavigation = { viewModel.onStopNavigation() },
                        onClosePlaceCard = { viewModel.onSelectPlace(null) },
                        onSearchChange = { viewModel.onSearchQueryChange(it) },
                        onCategorySelect = { viewModel.onCategorySelect(it) }
                    )
                }
                ActiveTab.FAVORITES -> {
                    FavoritesTabContent(
                        favorites = state.favorites,
                        onSelectPlace = { place ->
                            viewModel.onSelectPlace(place)
                            viewModel.onTabSelect(ActiveTab.MAP)
                        },
                        onToggleFavorite = { viewModel.onToggleFavorite(it) }
                    )
                }
                ActiveTab.HISTORY -> {
                    HistoryTabContent(
                        history = state.history,
                        onSelectPlace = { place ->
                            viewModel.onSelectPlace(place)
                            viewModel.onTabSelect(ActiveTab.MAP)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MapTabContent(
    state: MapUiState,
    cameraPositionState: CameraPositionState,
    onMapClick: (LatLng) -> Unit,
    onPlaceClick: (Place) -> Unit,
    onRecenter: () -> Unit,
    onToggleMapType: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onClosePlaceCard: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (PlaceCategory) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Google Map View
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = state.mapType,
                isMyLocationEnabled = false // using custom pin for simulator safety
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                myLocationButtonEnabled = false
            ),
            onMapClick = onMapClick
        ) {
            // User Location Marker
            Marker(
                state = MarkerState(position = state.userLocation),
                title = "ตำแหน่งของคุณ",
                snippet = "กรุงเทพมหานคร"
            )

            // Places Markers
            state.places.forEach { place ->
                Marker(
                    state = MarkerState(position = place.latLng),
                    title = place.name,
                    snippet = "${place.category.displayName} • ★ ${place.rating}",
                    onClick = {
                        onPlaceClick(place)
                        true
                    }
                )
            }

            // Route Polyline
            state.activeRoute?.let { route ->
                Polyline(
                    points = route.polylinePoints,
                    color = if (state.isNavigating) Color(0xFF00C853) else Color(0xFF1E88E5),
                    width = 14f
                )
            }
        }

        // Top Search & Category Filter (Hidden while actively navigating)
        if (!state.isNavigating) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Search Input Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "ค้นหา",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("ค้นหาสถานที่, ปั๊มน้ำมัน, ร้านอาหาร...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "ล้างการค้นหา")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlaceCategory.values().forEach { category ->
                        FilterChip(
                            selected = state.selectedCategory == category,
                            onClick = { onCategorySelect(category) },
                            label = { Text(category.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // Active Navigation HUD Overlay (Top banner during navigation)
        AnimatedVisibility(
            visible = state.isNavigating,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1B5E20), // Dark green navigation HUD
                shadowElevation = 10.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "นำทาง",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ใน 200 ม. เลี้ยวขวาเข้าสู่ทางแยก",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            state.selectedPlace?.let {
                                Text(
                                    text = "มุ่งหน้าสู่: ${it.name}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Button(
                            onClick = onStopNavigation,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("สิ้นสุด", color = Color.White)
                        }
                    }
                }
            }
        }

        // Map Control Floating Buttons (Right side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingActionButton(
                onClick = onToggleMapType,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Layers, contentDescription = "เปลี่ยนประเภทแผนที่")
            }
            FloatingActionButton(
                onClick = onRecenter,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "ตำแหน่งปัจจุบัน")
            }
        }

        // Place Detail & Route Card (Bottom)
        AnimatedVisibility(
            visible = state.selectedPlace != null && !state.isNavigating,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            state.selectedPlace?.let { place ->
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = place.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = place.address,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { onToggleFavorite(place.id) }) {
                                Icon(
                                    imageVector = if (place.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "ถูกใจ",
                                    tint = if (place.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onClosePlaceCard) {
                                Icon(Icons.Default.Close, contentDescription = "ปิด")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Route Info Preview
                        state.activeRoute?.let { route ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${route.estimatedMinutes} นาที",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${String.format("%.1f", route.distanceKm)} กม.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onStartNavigation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("เริ่มนำทาง", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTabContent(
    favorites: List<Place>,
    onSelectPlace: (Place) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "สถานที่โปรดของคุณ",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ยังไม่มีสถานที่โปรด\nกดไอคอนหัวใจในแผนที่เพื่อบันทึกสถานที่",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favorites) { place ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlace(place) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(place.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(place.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            IconButton(onClick = { onToggleFavorite(place.id) }) {
                                Icon(Icons.Default.Favorite, contentDescription = "ลบจากรายการโปรด", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTabContent(
    history: List<TripHistory>,
    onSelectPlace: (Place) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("th-TH")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ประวัติการเดินทาง",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ยังไม่มีประวัติการเดินทาง",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { trip ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlace(trip.place) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(trip.place.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = "ระยะทาง ${String.format("%.1f", trip.distanceKm)} กม. • ${dateFormat.format(Date(trip.timestamp))}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
