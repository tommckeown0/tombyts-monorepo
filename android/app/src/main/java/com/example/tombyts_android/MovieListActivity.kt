package com.example.tombyts_android

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MovieListActivity : FragmentActivity() {

    private lateinit var rowsAdapter: ArrayObjectAdapter
    private var token: String? = null

    companion object {
        // Static reference to shared ViewModel - this is how we pass it between activities
        private var sharedViewModel: MovieNavigationViewModel? = null

        fun setSharedViewModel(viewModel: MovieNavigationViewModel) {
            sharedViewModel = viewModel
        }

        fun getSharedViewModel(): MovieNavigationViewModel? = sharedViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get token from intent
        token = intent.getStringExtra("token")

        // Create the leanback browse fragment programmatically
        val fragment = CustomBrowseFragment()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()
        }
    }

    // Data class for folder navigation
    data class FolderCard(
        val parentFolder: String,
        val folderName: String,
        val files: List<Movie>
    )

    // Custom fragment that extends BrowseSupportFragment
    class CustomBrowseFragment : androidx.leanback.app.BrowseSupportFragment() {

        private var allMovies: List<Movie> = emptyList()
        private var viewModel: MovieNavigationViewModel? = null

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            // Get the shared ViewModel
            viewModel = getSharedViewModel()

            Log.d("MovieHighlight", "onActivityCreated")
            Log.d("MovieHighlight", "ViewModel available: ${viewModel != null}")
            Log.d("MovieHighlight", "isReturningFromPlayer: ${viewModel?.isReturningFromPlayer}")
            Log.d("MovieHighlight", "lastSelectedMovie: ${viewModel?.lastSelectedMovie}")
            Log.d("MovieHighlight", "currentFolderCard: ${viewModel?.currentFolderCard?.folderName}")

            setupUIElements()
            loadMoviesFromAPI()
            setupEventListeners()
        }

        override fun onResume() {
            super.onResume()

            viewModel?.let { vm ->
                Log.d("MovieHighlight", "onResume - checking for restoration")
                Log.d("MovieHighlight", "isReturningFromPlayer: ${vm.isReturningFromPlayer}")
                Log.d("MovieHighlight", "lastSelectedMovie: ${vm.lastSelectedMovie}")

                // If returning from player and we have a stored selection, restore it
                if (vm.isReturningFromPlayer && vm.lastSelectedMovie != null) {
                    Log.d("MovieHighlight", "Restoring last selected movie: ${vm.lastSelectedMovie}")
                    restoreLastSelection()
                    vm.setReturningFromPlayer(false) // Clear the flag
                }
            }
        }

        private fun setupUIElements() {
            title = "My Movies"
            headersState = HEADERS_ENABLED
            isHeadersTransitionOnBackEnabled = true
            brandColor = ContextCompat.getColor(requireActivity(), android.R.color.holo_blue_dark)
            searchAffordanceColor = ContextCompat.getColor(requireActivity(), android.R.color.white)
        }

        private fun loadMoviesFromAPI() {
            val token = activity?.intent?.getStringExtra("token")

            if (token == null) {
                Toast.makeText(activity, "No authentication token", Toast.LENGTH_LONG).show()
                return
            }

            Log.d("MovieHighlight", "Starting to load movies from API")

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    Log.d("MovieListActivity", "Fetching movies with token: ${token.take(20)}...")
                    val response = Classes.ApiProvider.apiService.getMovies("Bearer $token")
                    if (response.isSuccessful) {
                        val movies = response.body() ?: emptyList()
                        allMovies = movies
                        Log.d("MovieHighlight", "Movies loaded successfully, count: ${movies.size}")

                        viewModel?.let { vm ->
                            // If returning from player, restore the exact UI state
                            if (vm.isReturningFromPlayer && vm.currentFolderCard != null) {
                                Log.d("MovieHighlight", "Showing files in folder: ${vm.currentFolderCard?.folderName}")
                                showFilesInFolder(vm.currentFolderCard!!)
                            } else {
                                Log.d("MovieHighlight", "Creating rows from movies")
                                createRowsFromMovies(movies)
                            }

                            // Wait for UI to be fully rendered before attempting highlight
                            if (vm.isReturningFromPlayer && vm.lastSelectedMovie != null) {
                                Log.d("MovieHighlight", "Scheduling highlight attempt after UI load")
                                waitForUIAndHighlight()
                            }
                        } ?: run {
                            // No ViewModel, show normal view
                            createRowsFromMovies(movies)
                        }
                    } else {
                        val errorCode = response.code()
                        val errorMessage = response.message()
                        if (errorCode == 401) {
                            Log.e("Auth", "401 UNAUTHORIZED in MovieListActivity - Token expired or invalid. Token: ${token.take(20)}...")
                            Log.e("Auth", "Full error: $errorCode $errorMessage")
                            Toast.makeText(activity, "Authentication failed. Please login again.", Toast.LENGTH_LONG).show()
                        } else {
                            Log.e("MovieListActivity", "Failed to fetch movies: $errorCode $errorMessage")
                            Toast.makeText(activity, "Failed to load movies: $errorCode", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MovieListActivity", "Error fetching movies: ${e.message}", e)
                    Toast.makeText(activity, "Error loading movies", Toast.LENGTH_LONG).show()
                }
            }
        }

        private fun waitForUIAndHighlight() {
            Log.d("MovieHighlight", "waitForUIAndHighlight called")

            var attempts = 0
            val maxAttempts = 10

            fun tryHighlight() {
                attempts++
                Log.d("MovieHighlight", "Highlight attempt $attempts/$maxAttempts")

                if (adapter != null && (adapter as? ArrayObjectAdapter)?.size() ?: 0 > 0) {
                    Log.d("MovieHighlight", "Adapter is ready with ${(adapter as? ArrayObjectAdapter)?.size()} rows")
                    viewModel?.lastSelectedMovie?.let { movieTitle ->
                        highlightMovie(movieTitle)
                    }
                } else if (attempts < maxAttempts) {
                    Log.d("MovieHighlight", "Adapter not ready yet, retrying in 200ms")
                    view?.postDelayed({ tryHighlight() }, 200)
                } else {
                    Log.e("MovieHighlight", "Failed to highlight after $maxAttempts attempts")
                }
            }

            // Start the first attempt after a short delay
            view?.postDelayed({ tryHighlight() }, 100)
        }

        private fun restoreLastSelection() {
            // This method should no longer be called from onResume - remove the call
            Log.d("MovieHighlight", "restoreLastSelection called - but this should be handled by waitForUIAndHighlight")
        }

        private fun highlightMovie(movieTitle: String) {
            Log.d("MovieHighlight", "highlightMovie called for: $movieTitle")

            val currentAdapter = adapter as? ArrayObjectAdapter
            if (currentAdapter == null) {
                Log.e("MovieHighlight", "Current adapter is null")
                return
            }

            Log.d("MovieHighlight", "Searching through ${currentAdapter.size()} rows")

            for (rowIndex in 0 until currentAdapter.size()) {
                val row = currentAdapter.get(rowIndex) as? ListRow
                row?.let { listRow ->
                    Log.d("MovieHighlight", "Checking row $rowIndex: ${listRow.headerItem.name}")
                    val itemAdapter = listRow.adapter as? ArrayObjectAdapter
                    itemAdapter?.let { items ->
                        Log.d("MovieHighlight", "Row $rowIndex has ${items.size()} items")
                        for (itemIndex in 0 until items.size()) {
                            val item = items.get(itemIndex)
                            if (item is Movie) {
                                Log.d("MovieHighlight", "Found movie: ${item.title} at row $rowIndex, item $itemIndex")
                                if (item.title == movieTitle) {
                                    Log.d("MovieHighlight", "MATCH FOUND! Highlighting ${item.title} at row $rowIndex, item $itemIndex")

                                    // Set the selected position
                                    selectedPosition = rowIndex
                                    setSelectedPosition(rowIndex, true)

                                    Log.d("MovieHighlight", "Successfully set selection to row $rowIndex")
                                    return
                                }
                            } else if (item is FolderCard) {
                                Log.d("MovieHighlight", "Found folder: ${item.folderName} at row $rowIndex, item $itemIndex")
                            }
                        }
                    }
                }
            }

            Log.e("MovieHighlight", "Movie '$movieTitle' not found in current adapter")
        }

        private fun createRowsFromMovies(movies: List<Movie>) {
            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            val cardPresenter = SimpleCardPresenter()

            // Clear folder context when at top level
            viewModel?.clearFolder()

            // Group by the first folder in the path
            val groupedByFirstFolder = movies.groupBy { movie ->
                movie.path.split("/")[0]
            }

            // Sort folders alphabetically
            val sortedGroups = groupedByFirstFolder.toList().sortedBy { it.first }

            sortedGroups.forEach { (folderName, movieList) ->
                val listRowAdapter = ArrayObjectAdapter(cardPresenter)

                // Check if this folder has subfolders or just files
                val hasSubfolders = movieList.any { it.path.split("/").size > 1 }

                if (hasSubfolders) {
                    // Has subfolders: Group by next level folder
                    val subfolderGroups = movieList.groupBy { movie ->
                        val pathParts = movie.path.split("/")
                        if (pathParts.size > 1) pathParts[1] else "Files"
                    }.toSortedMap()

                    subfolderGroups.forEach { (subfolderName, files) ->
                        val folderCard = FolderCard(folderName, subfolderName, files)
                        listRowAdapter.add(folderCard)
                    }
                } else {
                    // No subfolders: Show files directly
                    val sortedMovies = movieList.sortedBy { it.title }
                    sortedMovies.forEach { movie ->
                        listRowAdapter.add(movie)
                    }
                }

                val header = HeaderItem(folderName.hashCode().toLong(), folderName)
                rowsAdapter.add(ListRow(header, listRowAdapter))
            }

            adapter = rowsAdapter
        }

        private fun setupEventListeners() {
            setOnSearchClickedListener {
                Toast.makeText(activity, "Search clicked", Toast.LENGTH_LONG).show()
            }

            onItemViewClickedListener = ItemViewClickedListener()
        }

        private inner class ItemViewClickedListener : OnItemViewClickedListener {
            override fun onItemClicked(
                itemViewHolder: Presenter.ViewHolder,
                item: Any,
                rowViewHolder: RowPresenter.ViewHolder,
                row: Row
            ) {
                when (item) {
                    is Movie -> {
                        Log.d("MovieListActivity", "Movie clicked: ${item.title}")

                        // Store the selection state in the ViewModel
                        viewModel?.setSelectedMovie(item.title)
                        Log.d("MovieHighlight", "Stored lastSelectedMovie in ViewModel: ${item.title}")

                        // Navigate to MoviePlayerScreen via MainActivity
                        val token = activity?.intent?.getStringExtra("token")
                        if (token != null) {
                            val intent = android.content.Intent(activity, MainActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("came_from_leanback", true)
                            }

                            val encodedTitle = android.net.Uri.encode(item.title)
                            intent.putExtra("navigate_to", "moviePlayer/${encodedTitle}/${token}")

                            startActivity(intent)
                        } else {
                            Toast.makeText(activity, "No token available", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is FolderCard -> {
                        Log.d("MovieListActivity", "Folder clicked: ${item.parentFolder}/${item.folderName}")

                        // Store the current folder context in the ViewModel
                        viewModel?.setCurrentFolder(item)

                        showFilesInFolder(item)
                    }
                }
            }
        }

        private fun showFilesInFolder(folderCard: FolderCard) {
            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            val cardPresenter = SimpleCardPresenter()
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)

            // Sort files by title
            val sortedFiles = folderCard.files.sortedBy { it.title }

            sortedFiles.forEach { file ->
                listRowAdapter.add(file)
            }

            val header = HeaderItem(0, "${folderCard.parentFolder}/${folderCard.folderName}")
            rowsAdapter.add(ListRow(header, listRowAdapter))

            adapter = rowsAdapter
        }

        override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            view.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                    viewModel?.let { vm ->
                        if (vm.isInSubfolder) {
                            // Clear the folder context when going back to top level
                            vm.clearFolder()
                            vm.clearSelection() // Also clear selection when navigating away
                            createRowsFromMovies(allMovies)
                            true
                        } else {
                            false
                        }
                    } ?: false
                } else {
                    false
                }
            }
        }

        private fun extractEpisodeNumber(path: String): Int {
            return when {
                path.contains("E\\d+".toRegex()) -> {
                    Regex("E(\\d+)").find(path)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
                path.contains("/ep") -> {
                    Regex("/ep(\\d+)").find(path)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
                else -> 0
            }
        }
    }

    // Simple card presenter for displaying movies
    class SimpleCardPresenter : Presenter() {

        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val cardView = androidx.leanback.widget.ImageCardView(parent.context).apply {
                isFocusable = true
                isFocusableInTouchMode = true
                setMainImageDimensions(313, 176)
            }
            return ViewHolder(cardView)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            val cardView = viewHolder.view as androidx.leanback.widget.ImageCardView

            when (item) {
                is Movie -> {
                    cardView.titleText = item.title
                    cardView.contentText = "Click to play"
                    cardView.mainImageView.setBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(cardView.context, android.R.color.darker_gray)
                    )
                }
                is FolderCard -> {
                    cardView.titleText = item.folderName
                    cardView.contentText = "${item.files.size} files"
                    cardView.mainImageView.setBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(cardView.context, android.R.color.holo_blue_dark)
                    )
                }
            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {
            val cardView = viewHolder.view as androidx.leanback.widget.ImageCardView
            cardView.badgeImage = null
            cardView.mainImage = null
        }
    }
}