package com.example.navigo.data

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MapRepository {
    private val initialPlaces = listOf(
        Place(
            id = "p1",
            name = "Siam Paragon (สยามพารากอน)",
            address = "991 ถ. พระรามที่ ๑ แขวงปทุมวัน เขตปทุมวัน กรุงเทพฯ",
            latitude = 13.7460,
            longitude = 100.5348,
            category = PlaceCategory.SHOPPING,
            rating = 4.8,
            isFavorite = true
        ),
        Place(
            id = "p2",
            name = "ICONSIAM (ไอคอนสยาม)",
            address = "299 ถ. เจริญนคร แขวงคลองต้นไทร เขตคลองสาน กรุงเทพฯ",
            latitude = 13.7267,
            longitude = 100.5108,
            category = PlaceCategory.SHOPPING,
            rating = 4.9,
            isFavorite = true
        ),
        Place(
            id = "p3",
            name = "วัดพระศรีรัตนศาสดาราม (วัดพระแก้ว)",
            address = "ถ. หน้าพระลาน แขวงพระบรมมหาราชวัง เขตพระนคร กรุงเทพฯ",
            latitude = 13.7516,
            longitude = 100.4927,
            category = PlaceCategory.LANDMARK,
            rating = 4.9,
            isFavorite = false
        ),
        Place(
            id = "p4",
            name = "สวนลุมพินี (Lumphini Park)",
            address = "139/4 ถ. วิทยุ แขวงลุมพินี เขตปทุมวัน กรุงเทพฯ",
            latitude = 13.7314,
            longitude = 100.5417,
            category = PlaceCategory.LANDMARK,
            rating = 4.6,
            isFavorite = false
        ),
        Place(
            id = "p5",
            name = "PTT Station สาขาพระราม 4",
            address = "ถ. พระรามที่ ๔ คลองเตย กรุงเทพฯ",
            latitude = 13.7202,
            longitude = 100.5694,
            category = PlaceCategory.GAS,
            rating = 4.4,
            isFavorite = false
        ),
        Place(
            id = "p6",
            name = "The Commons Thonglor (เดอะ คอมมอนส์ ทองหล่อ)",
            address = "335 ซอยสุขุมวิท 55 แขวงคลองตันเหนือ เขตวัฒนา กรุงเทพฯ",
            latitude = 13.7339,
            longitude = 100.5828,
            category = PlaceCategory.CAFE,
            rating = 4.7,
            isFavorite = false
        ),
        Place(
            id = "p7",
            name = "ท่าอากาศยานสุวรรณภูมิ (BKK Airport)",
            address = "999 หมู่ 1 ต.หนองปรือ อ.บางพลี สมุทรปราการ",
            latitude = 13.6900,
            longitude = 100.7501,
            category = PlaceCategory.LANDMARK,
            rating = 4.5,
            isFavorite = false
        ),
        Place(
            id = "p8",
            name = "Bangkok Marriott Marquis Queen's Park",
            address = "199 ซอยสุขุมวิท 22 แขวงคลองตัน เขตคลองเตย กรุงเทพฯ",
            latitude = 13.7319,
            longitude = 100.5658,
            category = PlaceCategory.HOTEL,
            rating = 4.7,
            isFavorite = false
        )
    )

    private val _places = MutableStateFlow<List<Place>>(initialPlaces)
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _history = MutableStateFlow<List<TripHistory>>(
        listOf(
            TripHistory("h1", initialPlaces[0], 5.2, System.currentTimeMillis() - 86400000),
            TripHistory("h2", initialPlaces[1], 8.1, System.currentTimeMillis() - 172800000)
        )
    )
    val history: StateFlow<List<TripHistory>> = _history.asStateFlow()

    fun toggleFavorite(placeId: String) {
        _places.value = _places.value.map { place ->
            if (place.id == placeId) {
                place.copy(isFavorite = !place.isFavorite)
            } else {
                place
            }
        }
    }

    fun addCustomPin(latLng: LatLng, name: String = "หมุดที่กำหนดเอง"): Place {
        val newPlace = Place(
            id = UUID.randomUUID().toString(),
            name = name,
            address = "พิกัด: ${String.format("%.4f", latLng.latitude)}, ${String.format("%.4f", latLng.longitude)}",
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            category = PlaceCategory.LANDMARK,
            isFavorite = false
        )
        _places.value = _places.value + newPlace
        return newPlace
    }

    fun recordTrip(destination: Place, distanceKm: Double) {
        val newEntry = TripHistory(
            id = UUID.randomUUID().toString(),
            place = destination,
            distanceKm = distanceKm,
            timestamp = System.currentTimeMillis()
        )
        _history.value = listOf(newEntry) + _history.value
    }

    fun calculateRoute(origin: LatLng, destination: Place): RoutePlan {
        val distKm = calculateDistanceInKm(origin.latitude, origin.longitude, destination.latitude, destination.longitude)
        val estimatedMins = (distKm / 35.0 * 60).toInt().coerceAtLeast(5) // avg speed 35 km/h in city

        // Generate smooth intermediate polyline points
        val polyline = mutableListOf<LatLng>()
        val segments = 8
        polyline.add(origin)
        for (i in 1 until segments) {
            val fraction = i.toDouble() / segments
            val midLat = origin.latitude + (destination.latitude - origin.latitude) * fraction
            val midLng = origin.longitude + (destination.longitude - origin.longitude) * fraction
            // Add slight curve for realism
            val offset = sin(fraction * Math.PI) * 0.003
            polyline.add(LatLng(midLat + offset, midLng - offset))
        }
        polyline.add(destination.latLng)

        val steps = listOf(
            "เริ่มต้นเดินทางจากตำแหน่งปัจจุบัน",
            "ตรงไปตามถนนหลักอีก ${String.format("%.1f", distKm * 0.4)} กม.",
            "เลี้ยวขวาที่แยกถัดไปตามป้ายบอกทาง",
            "ตรงไปประมาณ ${String.format("%.1f", distKm * 0.5)} กม.",
            "ถึงจุดหมายปลายทาง: ${destination.name}"
        )

        return RoutePlan(
            origin = origin,
            destination = destination,
            distanceKm = distKm,
            estimatedMinutes = estimatedMins,
            polylinePoints = polyline,
            steps = steps
        )
    }

    private fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
