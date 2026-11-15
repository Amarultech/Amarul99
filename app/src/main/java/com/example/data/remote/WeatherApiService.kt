package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String? = null,
    val admin1: String? = null
)

@JsonClass(generateAdapter = true)
data class WeatherForecastResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String? = null,
    val current: CurrentWeatherDto? = null,
    val daily: DailyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "temperature_2m") val temperature2m: Double = 0.0,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int = 0,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
    @Json(name = "weather_code") val weatherCode: Int = 0,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    val time: List<String> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "temperature_2m_max") val temperatureMax: List<Double> = emptyList(),
    @Json(name = "temperature_2m_min") val temperatureMin: List<Double> = emptyList()
)

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 5
    ): WeatherForecastResponse
}

data class DailyForecastItem(
    val dateLabel: String,
    val condition: String,
    val highCelsius: Double,
    val lowCelsius: Double,
    val weatherCode: Int
)

data class LiveWeatherSnapshot(
    val locationLabel: String,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val conditionLabel: String,
    val weatherCode: Int,
    val forecast: List<DailyForecastItem>
)

object WeatherService {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val geocodingApi: OpenMeteoGeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoGeocodingApi::class.java)
    }

    private val forecastApi: OpenMeteoForecastApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoForecastApi::class.java)
    }

    suspend fun fetchWeatherForCity(cityQuery: String): LiveWeatherSnapshot {
        val cleanQuery = cityQuery.trim().ifBlank { "San Francisco" }
        val geo = geocodingApi.searchCity(cleanQuery)
        val match = geo.results?.firstOrNull()
            ?: throw IllegalStateException("Location '$cleanQuery' not found.")

        val forecastResp = forecastApi.getForecast(
            latitude = match.latitude,
            longitude = match.longitude
        )
        val current = forecastResp.current
            ?: throw IllegalStateException("Weather telemetry unavailable for ${match.name}.")

        val daily = forecastResp.daily
        val dailyList = if (daily != null) {
            daily.time.indices.map { idx ->
                val code = daily.weatherCode.getOrElse(idx) { 0 }
                DailyForecastItem(
                    dateLabel = daily.time[idx],
                    condition = weatherCodeToLabel(code),
                    highCelsius = daily.temperatureMax.getOrElse(idx) { 0.0 },
                    lowCelsius = daily.temperatureMin.getOrElse(idx) { 0.0 },
                    weatherCode = code
                )
            }
        } else {
            emptyList()
        }

        val labelParts = listOfNotNull(match.name, match.admin1, match.country).distinct()
        return LiveWeatherSnapshot(
            locationLabel = labelParts.joinToString(", "),
            temperatureCelsius = current.temperature2m,
            feelsLikeCelsius = current.apparentTemperature,
            humidityPercent = current.relativeHumidity2m,
            windSpeedKmh = current.windSpeed10m,
            conditionLabel = weatherCodeToLabel(current.weatherCode),
            weatherCode = current.weatherCode,
            forecast = dailyList
        )
    }

    fun weatherCodeToLabel(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1, 2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snowfall"
        80, 81, 82 -> "Rain Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Atmospheric"
    }
}
