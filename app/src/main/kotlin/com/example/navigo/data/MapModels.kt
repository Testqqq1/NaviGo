package com.example.navigo.data

import org.osmdroid.util.GeoPoint

enum class PlaceCategory(val displayName: String) {
    ALL("ทั้งหมด"),
    LANDMARK("สถานที่สำคัญ"),
    SHOPPING("ช้อปปิ้ง"),
    CAFE("คาเฟ่ & อาหาร"),
    GAS("ปั๊มน้ำมัน"),
    HOTEL("ที่พัก/โรงแรม")
}

data class Place(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory,
    val rating: Double = 4.5,
    val isFavorite: Boolean = false
) {
    val geoPoint: GeoPoint get() = GeoPoint(latitude, longitude)
}

data class RoutePlan(
    val origin: GeoPoint,
    val destination: Place,
    val distanceKm: Double,
    val estimatedMinutes: Int,
    val polylinePoints: List<GeoPoint>,
    val steps: List<String>
)

data class TripHistory(
    val id: String,
    val place: Place,
    val distanceKm: Double,
    val timestamp: Long = System.currentTimeMillis()
)
