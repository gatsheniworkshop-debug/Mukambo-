package com.example.data.model

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class ZimSuburb(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class ZimCity(
    val name: String,
    val province: String,
    val latitude: Double,
    val longitude: Double,
    val suburbs: List<ZimSuburb>
)

object ZimbabweLocations {
    val provinces: List<String> = listOf(
        "Harare Metropolitan",
        "Bulawayo Metropolitan",
        "Manicaland",
        "Mashonaland Central",
        "Mashonaland East",
        "Mashonaland West",
        "Masvingo",
        "Matabeleland North",
        "Matabeleland South",
        "Midlands"
    )

    val cities: List<ZimCity> = listOf(
        // 1. Harare Metropolitan
        ZimCity(
            name = "Harare",
            province = "Harare Metropolitan",
            latitude = -17.8252,
            longitude = 31.0335,
            suburbs = listOf(
                ZimSuburb("CBD", -17.8292, 31.0522),
                ZimSuburb("Borrowdale", -17.7564, 31.0882),
                ZimSuburb("Avondale", -17.7985, 31.0365),
                ZimSuburb("Mount Pleasant", -17.7755, 31.0442),
                ZimSuburb("Hatfield", -17.8825, 31.0725),
                ZimSuburb("Warren Park", -17.8350, 30.9850),
                ZimSuburb("Msasa", -17.8380, 31.1120)
            )
        ),
        ZimCity(
            name = "Chitungwiza",
            province = "Harare Metropolitan",
            latitude = -18.0127,
            longitude = 31.0756,
            suburbs = listOf(
                ZimSuburb("Zengeza", -17.9970, 31.0690),
                ZimSuburb("Seke", -18.0190, 31.0840),
                ZimSuburb("St Marys", -17.9880, 31.0570)
            )
        ),
        ZimCity(
            name = "Epworth",
            province = "Harare Metropolitan",
            latitude = -17.8900,
            longitude = 31.1475,
            suburbs = listOf(
                ZimSuburb("Overspill", -17.8920, 31.1500),
                ZimSuburb("Domboramwari", -17.8960, 31.1410)
            )
        ),
        // 2. Bulawayo Metropolitan
        ZimCity(
            name = "Bulawayo",
            province = "Bulawayo Metropolitan",
            latitude = -20.1325,
            longitude = 28.6265,
            suburbs = listOf(
                ZimSuburb("CBD", -20.1560, 28.5830),
                ZimSuburb("Suburbs", -20.1605, 28.5980),
                ZimSuburb("Hillside", -20.1945, 28.6105),
                ZimSuburb("Nkulumane", -20.1885, 28.5130),
                ZimSuburb("Bradfield", -20.1740, 28.5910)
            )
        ),
        // 3. Manicaland
        ZimCity(
            name = "Mutare",
            province = "Manicaland",
            latitude = -18.9707,
            longitude = 32.6709,
            suburbs = listOf(
                ZimSuburb("CBD", -18.9728, 32.6700),
                ZimSuburb("Murambi", -18.9600, 32.6850),
                ZimSuburb("Dangamvura", -19.0160, 32.6330),
                ZimSuburb("Chikanga", -18.9830, 32.6310)
            )
        ),
        ZimCity(
            name = "Rusape",
            province = "Manicaland",
            latitude = -18.5278,
            longitude = 32.1284,
            suburbs = listOf(
                ZimSuburb("Town Centre", -18.5280, 32.1280),
                ZimSuburb("Vengere", -18.5410, 32.1190)
            )
        ),
        ZimCity(
            name = "Chipinge",
            province = "Manicaland",
            latitude = -20.1930,
            longitude = 32.6237,
            suburbs = listOf(
                ZimSuburb("Town Centre", -20.1930, 32.6237),
                ZimSuburb("Gaza", -20.1990, 32.6160)
            )
        ),
        // 4. Mashonaland Central
        ZimCity(
            name = "Bindura",
            province = "Mashonaland Central",
            latitude = -17.3019,
            longitude = 31.3306,
            suburbs = listOf(
                ZimSuburb("CBD", -17.3020, 31.3310),
                ZimSuburb("Chipadze", -17.3130, 31.3200),
                ZimSuburb("Aerodrome", -17.2950, 31.3410)
            )
        ),
        ZimCity(
            name = "Shamva",
            province = "Mashonaland Central",
            latitude = -17.3116,
            longitude = 31.5756,
            suburbs = listOf(
                ZimSuburb("Town Centre", -17.3116, 31.5756),
                ZimSuburb("Wadzanai", -17.3190, 31.5690)
            )
        ),
        // 5. Mashonaland East
        ZimCity(
            name = "Marondera",
            province = "Mashonaland East",
            latitude = -18.1853,
            longitude = 31.5519,
            suburbs = listOf(
                ZimSuburb("CBD", -18.1850, 31.5520),
                ZimSuburb("Dombotombo", -18.1940, 31.5420),
                ZimSuburb("Cherutombo", -18.1990, 31.5560)
            )
        ),
        ZimCity(
            name = "Ruwa",
            province = "Mashonaland East",
            latitude = -17.8897,
            longitude = 31.2447,
            suburbs = listOf(
                ZimSuburb("Town Centre", -17.8900, 31.2450),
                ZimSuburb("Zimre Park", -17.8780, 31.2190)
            )
        ),
        // 6. Mashonaland West
        ZimCity(
            name = "Chinhoyi",
            province = "Mashonaland West",
            latitude = -17.3667,
            longitude = 30.2000,
            suburbs = listOf(
                ZimSuburb("CBD", -17.3667, 30.2000),
                ZimSuburb("Chikonohono", -17.3750, 30.1890),
                ZimSuburb("Orange Grove", -17.3580, 30.2090)
            )
        ),
        ZimCity(
            name = "Kadoma",
            province = "Mashonaland West",
            latitude = -18.3333,
            longitude = 29.9153,
            suburbs = listOf(
                ZimSuburb("CBD", -18.3333, 29.9153),
                ZimSuburb("Rimuka", -18.3480, 29.9030)
            )
        ),
        ZimCity(
            name = "Kariba",
            province = "Mashonaland West",
            latitude = -16.5167,
            longitude = 28.8000,
            suburbs = listOf(
                ZimSuburb("Heights", -16.5200, 28.7850),
                ZimSuburb("Nyamhunga", -16.5310, 28.8220)
            )
        ),
        // 7. Masvingo
        ZimCity(
            name = "Masvingo",
            province = "Masvingo",
            latitude = -20.0637,
            longitude = 30.8277,
            suburbs = listOf(
                ZimSuburb("CBD", -20.0740, 30.8320),
                ZimSuburb("Rhodene", -20.0620, 30.8440),
                ZimSuburb("Mucheke", -20.0880, 30.8180)
            )
        ),
        ZimCity(
            name = "Chiredzi",
            province = "Masvingo",
            latitude = -21.0500,
            longitude = 31.6667,
            suburbs = listOf(
                ZimSuburb("Town Centre", -21.0500, 31.6667),
                ZimSuburb("Tshovani", -21.0570, 31.6580)
            )
        ),
        // 8. Matabeleland North
        ZimCity(
            name = "Victoria Falls",
            province = "Matabeleland North",
            latitude = -17.9329,
            longitude = 25.8307,
            suburbs = listOf(
                ZimSuburb("Town Centre", -17.9320, 25.8300),
                ZimSuburb("Chinotimba", -17.9390, 25.8190),
                ZimSuburb("Mkhosana", -17.9510, 25.8140)
            )
        ),
        ZimCity(
            name = "Hwange",
            province = "Matabeleland North",
            latitude = -18.3645,
            longitude = 26.5000,
            suburbs = listOf(
                ZimSuburb("Colliery", -18.3645, 26.5000),
                ZimSuburb("Empumalanga", -18.3740, 26.4890)
            )
        ),
        // 9. Matabeleland South
        ZimCity(
            name = "Gwanda",
            province = "Matabeleland South",
            latitude = -20.9333,
            longitude = 29.0000,
            suburbs = listOf(
                ZimSuburb("Town Centre", -20.9333, 29.0000),
                ZimSuburb("Jahunda", -20.9410, 28.9930)
            )
        ),
        ZimCity(
            name = "Beitbridge",
            province = "Matabeleland South",
            latitude = -22.2167,
            longitude = 30.0000,
            suburbs = listOf(
                ZimSuburb("Town Centre", -22.2167, 30.0000),
                ZimSuburb("Dulibadzimu", -22.2080, 29.9920)
            )
        ),
        // 10. Midlands
        ZimCity(
            name = "Gweru",
            province = "Midlands",
            latitude = -19.4500,
            longitude = 29.8167,
            suburbs = listOf(
                ZimSuburb("CBD", -19.4550, 29.8160),
                ZimSuburb("Mkoba", -19.4670, 29.7660),
                ZimSuburb("Kopje", -19.4590, 29.8250)
            )
        ),
        ZimCity(
            name = "Kwekwe",
            province = "Midlands",
            latitude = -18.9281,
            longitude = 29.8149,
            suburbs = listOf(
                ZimSuburb("CBD", -18.9280, 29.8150),
                ZimSuburb("Mbizo", -18.9210, 29.7820)
            )
        ),
        ZimCity(
            name = "Zvishavane",
            province = "Midlands",
            latitude = -20.3333,
            longitude = 30.0500,
            suburbs = listOf(
                ZimSuburb("Town Centre", -20.3333, 30.0500),
                ZimSuburb("Mandava", -20.3260, 30.0430)
            )
        )
    )

    val cityNames: List<String> = cities.map { it.name }

    fun citiesInProvince(province: String): List<ZimCity> =
        if (province == "All Provinces") cities else cities.filter { it.province.equals(province, ignoreCase = true) }

    fun findCity(name: String): ZimCity =
        cities.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: cities.first()

    fun findNearestCity(latitude: Double, longitude: Double): ZimCity =
        cities.minByOrNull { distanceKm(latitude, longitude, it.latitude, it.longitude) } ?: cities.first()

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    fun formatDistance(km: Double): String = when {
        km < 1.0 -> "< 1 km away"
        km < 10.0 -> String.format(Locale.US, "%.1f km away", km)
        else -> "${km.toInt()} km away"
    }
}
