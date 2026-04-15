package com.translator.universal.service

import android.content.Context
import android.location.Location
import android.location.Geocoder
import android.location.LocationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val geocoder = Geocoder(context, Locale.getDefault())

    data class LocationInfo(
        val latitude: Double,
        val longitude: Double,
        val country: String?,
        val city: String?,
        val language: String?
    )

    fun getCurrentLocation(): Location? {
        return try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLocationInfo(): LocationInfo? = withContext(Dispatchers.IO) {
        val location = getCurrentLocation() ?: return@withContext null

        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val address = addresses?.firstOrNull()

            val language = when (address?.countryCode) {
                "TR" -> "tr"
                "US", "GB" -> "en"
                "DE" -> "de"
                "FR" -> "fr"
                "ES" -> "es"
                "IT" -> "it"
                "RU" -> "ru"
                "CN", "TW" -> "zh"
                "JP" -> "ja"
                "KR" -> "ko"
                "AR" -> "ar"
                else -> "en"
            }

            LocationInfo(
                latitude = location.latitude,
                longitude = location.longitude,
                country = address?.countryName,
                city = address?.locality,
                language = language
            )
        } catch (e: Exception) {
            null
        }
    }

    // Convert city names between languages
    suspend fun translateCityName(cityName: String, targetLang: String): String {
        // Simplified - would use translation service
        return cityName
    }

    // Get timezone based on location
    fun getTimeZone(): String {
        val location = getCurrentLocation()
        return location?.let {
            java.util.TimeZone.getDefault().id
        } ?: "UTC"
    }
}

@Singleton
class CurrencyService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Offline exchange rates (would be updated periodically)
    private val exchangeRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "TRY" to 32.50,
        "JPY" to 149.50,
        "CNY" to 7.24,
        "RUB" to 89.50,
        "AED" to 3.67,
        "SAR" to 3.75,
        "CHF" to 0.88,
        "CAD" to 1.36,
        "AUD" to 1.53
    )

    data class Currency(
        val code: String,
        val name: String,
        val symbol: String,
        val flag: String
    )

    fun getSupportedCurrencies(): List<Currency> = listOf(
        Currency("USD", "Dolar", "$", "🇺🇸"),
        Currency("EUR", "Euro", "€", "🇪🇺"),
        Currency("GBP", "Sterlin", "£", "🇬🇧"),
        Currency("TRY", "Türk Lirası", "₺", "🇹🇷"),
        Currency("JPY", "Yen", "¥", "🇯🇵"),
        Currency("CNY", "Yuan", "¥", "🇨🇳"),
        Currency("RUB", "Ruble", "₽", "🇷🇺"),
        Currency("AED", "Dirhem", "د.إ", "🇦🇪"),
        Currency("SAR", "Riyal", "﷼", "🇸🇦"),
        Currency("CHF", "Frank", "Fr", "🇨🇭"),
        Currency("CAD", "Kanada Doları", "$", "🇨🇦"),
        Currency("AUD", "Avustralya Doları", "$", "🇦🇺")
    )

    fun convert(amount: Double, from: String, to: String): Double {
        val fromRate = exchangeRates[from] ?: 1.0
        val toRate = exchangeRates[to] ?: 1.0
        return (amount / fromRate) * toRate
    }

    fun formatCurrency(amount: Double, currencyCode: String): String {
        val currency = getSupportedCurrencies().find { it.code == currencyCode }
        return "${currency?.symbol ?: ""}%.2f".format(amount)
    }
}

@Singleton
class UnitConverterService @Inject constructor() {
    data class UnitCategory(
        val id: String,
        val name: String,
        val icon: String
    )

    data class Unit(
        val id: String,
        val name: String,
        val symbol: String,
        val toBase: Double // conversion factor to base unit
    )

    fun getCategories(): List<UnitCategory> = listOf(
        UnitCategory("length", "Uzunluk", "📏"),
        UnitCategory("weight", "Ağırlık", "⚖️"),
        UnitCategory("temperature", "Sıcaklık", "🌡️"),
        UnitCategory("volume", "Hacim", "🧪"),
        UnitCategory("area", "Alan", "📐"),
        UnitCategory("speed", "Hız", "🚀"),
        UnitCategory("time", "Zaman", "⏰"),
        UnitCategory("digital", "Dijital", "💾")
    )

    fun getUnitsForCategory(category: String): List<Unit> {
        return when (category) {
            "length" -> listOf(
                Unit("m", "Metre", "m", 1.0),
                Unit("km", "Kilometre", "km", 1000.0),
                Unit("cm", "Santimetre", "cm", 0.01),
                Unit("mm", "Milimetre", "mm", 0.001),
                Unit("ft", "Fit", "ft", 0.3048),
                Unit("in", "İnç", "in", 0.0254),
                Unit("mi", "Mil", "mi", 1609.34),
                Unit("yd", "Yard", "yd", 0.9144)
            )
            "weight" -> listOf(
                Unit("kg", "Kilogram", "kg", 1.0),
                Unit("g", "Gram", "g", 0.001),
                Unit("mg", "Miligram", "mg", 0.000001),
                Unit("lb", "Pound", "lb", 0.453592),
                Unit("oz", "Ounce", "oz", 0.0283495),
                Unit("ton", "Ton", "t", 1000.0)
            )
            "temperature" -> listOf(
                Unit("c", "Celsius", "°C", 1.0),
                Unit("f", "Fahrenheit", "°F", 1.0),
                Unit("k", "Kelvin", "K", 1.0)
            )
            else -> emptyList()
        }
    }

    fun convert(value: Double, fromUnit: String, toUnit: String, category: String): Double {
        if (category == "temperature") {
            return convertTemperature(value, fromUnit, toUnit)
        }

        val units = getUnitsForCategory(category)
        val from = units.find { it.id == fromUnit } ?: return 0.0
        val to = units.find { it.id == toUnit } ?: return 0.0

        return value * from.toBase / to.toBase
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        // Convert to Celsius first
        val celsius = when (from) {
            "c" -> value
            "f" -> (value - 32) * 5 / 9
            "k" -> value - 273.15
            else -> value
        }

        // Convert from Celsius to target
        return when (to) {
            "c" -> celsius
            "f" -> celsius * 9 / 5 + 32
            "k" -> celsius + 273.15
            else -> celsius
        }
    }
}

@Singleton
class WeatherService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationService: LocationService
) {
    // Simulated weather - in production would use API
    data class WeatherInfo(
        val temperature: Int,
        val condition: String,
        val humidity: Int,
        val windSpeed: Int,
        val location: String
    )

    suspend fun getCurrentWeather(): WeatherInfo? {
        val location = locationService.getCurrentLocation() ?: return null
        return WeatherInfo(
            temperature = (15..30).random(),
            condition = listOf("Güneşli", "Bulutlu", "Parçalı", "Yağışlı").random(),
            humidity = (40..90).random(),
            windSpeed = (5..25).random(),
            location = "Konum"
        )
    }

    // Weather translations
    fun getWeatherTranslation(condition: String, targetLang: String): String {
        val translations = mapOf(
            "tr" to mapOf(
                "Güneşli" to "Sunny",
                "Bulutlu" to "Cloudy",
                "Parçalı" to "Partly Cloudy",
                "Yağışlı" to "Rainy",
                "Karlı" to "Snowy",
                "Fırtınalı" to "Stormy"
            ),
            "en" to mapOf(
                "Sunny" to "Güneşli",
                "Cloudy" to "Bulutlu",
                "Partly Cloudy" to "Parçalı",
                "Rainy" to "Yağışlı"
            )
        )
        return translations[targetLang]?.get(condition) ?: condition
    }
}