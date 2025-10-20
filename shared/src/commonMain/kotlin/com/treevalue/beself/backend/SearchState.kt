package com.treevalue.beself.backend

import androidx.compose.runtime.mutableStateOf
import com.treevalue.beself.net.SiteInfo

class SearchState {
    private val _searchText = mutableStateOf("")
    val searchText = _searchText

    private val _keepSearchContent = mutableStateOf(false)
    val keepSearchContent = _keepSearchContent

    private val _filteredSites = mutableStateOf<List<SiteInfo>>(emptyList())
    val filteredSites = _filteredSites

    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    fun clearSearchText() {
        _searchText.value = ""
    }

    fun toggleKeepSearchContent() {
        _keepSearchContent.value = !_keepSearchContent.value
    }

    fun filterSites(allSites: List<SiteInfo>) {
        _filteredSites.value = if (_searchText.value.isEmpty()) {
            allSites
        } else {
            allSites.filter { site ->
                site.label.contains(_searchText.value, ignoreCase = true) ||
                        site.host.contains(_searchText.value, ignoreCase = true)
            }
        }
    }

    fun resetForNewSearch() {
        if (!_keepSearchContent.value) {
            _searchText.value = ""
        }
        _filteredSites.value = emptyList()
    }
}
