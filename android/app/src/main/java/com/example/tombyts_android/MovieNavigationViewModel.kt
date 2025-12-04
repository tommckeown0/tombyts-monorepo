package com.example.tombyts_android

import androidx.lifecycle.ViewModel

class MovieNavigationViewModel : ViewModel() {
    // State for the currently selected movie
    var lastSelectedMovie: String? = null
        private set

    // State for the current folder context (if in a subfolder)
    var currentFolderCard: MovieListActivity.FolderCard? = null
        private set

    // State for whether we're currently in a subfolder
    var isInSubfolder: Boolean = false
        private set

    // State for whether we're returning from the player
    var isReturningFromPlayer: Boolean = false
        private set

    // Methods to update state
    fun setSelectedMovie(movieTitle: String) {
        lastSelectedMovie = movieTitle
    }

    fun setCurrentFolder(folderCard: MovieListActivity.FolderCard?) {
        currentFolderCard = folderCard
        isInSubfolder = folderCard != null
    }

    fun setReturningFromPlayer(returning: Boolean) {
        isReturningFromPlayer = returning
    }

    fun clearSelection() {
        lastSelectedMovie = null
    }

    fun clearFolder() {
        currentFolderCard = null
        isInSubfolder = false
    }

    fun clearAll() {
        clearSelection()
        clearFolder()
        isReturningFromPlayer = false
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources if needed
    }
}