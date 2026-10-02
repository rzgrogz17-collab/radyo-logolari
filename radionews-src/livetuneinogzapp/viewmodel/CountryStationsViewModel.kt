package com.globalradio.livetuneinogzapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.globalradio.livetuneinogzapp.model.RadioStation
import com.globalradio.livetuneinogzapp.repository.StationRepository
import com.globalradio.livetuneinogzapp.utils.CountryFlags
import kotlinx.coroutines.launch

class CountryStationsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StationRepository(application)

    private val _stations = MutableLiveData<List<RadioStation>>(emptyList())
    val stations: LiveData<List<RadioStation>> = _stations

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadStationsForCountry(country: String, countryCode: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val cached = repository.getCachedStations()
            if (!cached.isNullOrEmpty()) {
                _stations.value = filterCountry(cached, country, countryCode)
                _isLoading.value = false
                return@launch
            }

            repository.getStations()
                .onSuccess { all ->
                    _stations.value = filterCountry(all, country, countryCode)
                }
                .onFailure { e ->
                    _error.value = "Yüklenemedi: ${e.message}"
                }

            _isLoading.value = false
        }
    }

    private fun filterCountry(
        all: List<RadioStation>,
        country: String,
        countryCode: String?
    ): List<RadioStation> {
        val iso = CountryFlags.sanitizeIso(countryCode)
            ?: CountryFlags.resolveIso(country)
        return all.filter { st ->
            st.country.equals(country, ignoreCase = true) ||
                (iso != null && CountryFlags.resolveIso(st.country, st.countryCode) == iso)
        }
    }

    fun updateStations(stations: List<RadioStation>) {
        _stations.value = stations
    }

    fun toggleFavorite(station: RadioStation) {
        repository.toggleFavorite(station)
        val updated = _stations.value?.map { s ->
            if (s.id == station.id) s.copy(isFavorite = !s.isFavorite) else s
        } ?: emptyList()
        _stations.value = updated
    }
}
