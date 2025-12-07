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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MovieListActivity", "🏗️ onCreate called, savedInstanceState=${if (savedInstanceState == null) "null" else "exists"}")

        // Get token from intent
        token = intent.getStringExtra("token")
        val restoreTitle = intent.getStringExtra("restore_selection")
        Log.d("MovieListActivity", "onCreate: token present=${token != null}, restore_selection='$restoreTitle'")

        // Create the leanback browse fragment programmatically
        val fragment = CustomBrowseFragment()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()
        }
    }

    override fun onNewIntent(newIntent: android.content.Intent) {
        super.onNewIntent(newIntent)
        Log.d("MovieListActivity", "⚡ onNewIntent called")
        // Update the intent so fragment can read the new extras
        setIntent(newIntent)

        // Get the restore_selection from the new intent
        val restoreTitle = newIntent.getStringExtra("restore_selection")
        Log.d("MovieListActivity", "onNewIntent: restore_selection = '$restoreTitle'")
        if (restoreTitle != null) {
            Log.d("MovieListActivity", "onNewIntent: Restoring selection to $restoreTitle")
            // Find the fragment and trigger restoration
            val fragment = supportFragmentManager.findFragmentById(android.R.id.content) as? CustomBrowseFragment
            if (fragment != null) {
                Log.d("MovieListActivity", "onNewIntent: Fragment found, calling restoreSelectionIfNeeded")
                fragment.restoreSelectionIfNeeded(restoreTitle)
            } else {
                Log.w("MovieListActivity", "onNewIntent: Fragment not found!")
            }
        }
    }

    // Custom fragment that extends BrowseSupportFragment
    class CustomBrowseFragment : androidx.leanback.app.BrowseSupportFragment() {

        private var parsedMedia: ParsedMedia? = null
        private var currentRowsAdapter: ArrayObjectAdapter? = null

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            setupUIElements()
            loadMoviesFromAPI()
            setupEventListeners()
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

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    Log.d("MovieListActivity", "Fetching movies with token: ${token.take(20)}...")
                    val response = Classes.ApiProvider.apiService.getMovies("Bearer $token")
                    if (response.isSuccessful) {
                        val allMedia = response.body() ?: emptyList()
                        Log.d("MovieListActivity", "Media loaded successfully, count: ${allMedia.size}")

                        // Log first few items to see their paths
                        allMedia.take(5).forEach { movie ->
                            Log.d("MovieListActivity", "Sample item - Title: ${movie.title}, Path: ${movie.path}")
                        }

                        // Parse media into movies and TV shows
                        parsedMedia = MediaParser.parseMedia(allMedia)
                        Log.d("MovieListActivity", "Parsed: ${parsedMedia?.movies?.size} movies, ${parsedMedia?.tvShows?.size} TV shows")

                        createSimpleBrowseRows()
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

        /**
         * Creates simple 2-row layout: Movies and TV Shows
         */
        private fun createSimpleBrowseRows() {
            val media = parsedMedia
            if (media == null) {
                Log.e("MovieListActivity", "parsedMedia is null!")
                return
            }

            Log.d("MovieListActivity", "createSimpleBrowseRows - Movies: ${media.movies.size}, TV Shows: ${media.tvShows.size}")

            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            val cardPresenter = SimpleCardPresenter()

            // Row 1: Movies
            if (media.movies.isNotEmpty()) {
                Log.d("MovieListActivity", "Adding Movies row with ${media.movies.size} items")
                val moviesAdapter = ArrayObjectAdapter(cardPresenter)
                media.movies.sortedBy { it.title }.forEach { movie ->
                    moviesAdapter.add(movie)
                }
                val moviesHeader = HeaderItem(0, "Movies")
                rowsAdapter.add(ListRow(moviesHeader, moviesAdapter))
            } else {
                Log.w("MovieListActivity", "No movies to display")
            }

            // Row 2: TV Shows
            if (media.tvShows.isNotEmpty()) {
                Log.d("MovieListActivity", "Adding TV Shows row with ${media.tvShows.size} items")
                val tvShowsAdapter = ArrayObjectAdapter(cardPresenter)
                media.tvShows.forEach { tvShow ->
                    tvShowsAdapter.add(tvShow)
                }
                val tvShowsHeader = HeaderItem(1, "TV Shows")
                rowsAdapter.add(ListRow(tvShowsHeader, tvShowsAdapter))
            } else {
                Log.w("MovieListActivity", "No TV shows to display")
            }

            if (rowsAdapter.size() == 0) {
                Log.e("MovieListActivity", "No rows to display! Both movies and TV shows are empty")
            } else {
                Log.d("MovieListActivity", "Setting adapter with ${rowsAdapter.size()} rows")
            }

            adapter = rowsAdapter
            currentRowsAdapter = rowsAdapter

            // Restore selection if coming back from player (for fresh onCreate)
            val restoreTitle = activity?.intent?.getStringExtra("restore_selection")
            if (restoreTitle != null) {
                Log.d("MovieListActivity", "onCreate: Attempting to restore selection to: $restoreTitle")
                restoreSelection(rowsAdapter, restoreTitle)
            }
        }

        // Public method that can be called from the activity when onNewIntent is triggered
        fun restoreSelectionIfNeeded(titleToFind: String) {
            val adapter = currentRowsAdapter
            if (adapter != null) {
                Log.d("MovieListActivity", "restoreSelectionIfNeeded called for: $titleToFind")
                restoreSelection(adapter, titleToFind)
            } else {
                Log.w("MovieListActivity", "restoreSelectionIfNeeded: adapter not ready yet")
            }
        }

        private fun restoreSelection(rowsAdapter: ArrayObjectAdapter, titleToFind: String) {
            Log.d("MovieListActivity", "restoreSelection: Searching for '$titleToFind', isResumed=$isResumed, view=$view")

            // Search through all rows and items to find the matching title
            for (rowIndex in 0 until rowsAdapter.size()) {
                val row = rowsAdapter.get(rowIndex)
                if (row is ListRow) {
                    val itemsAdapter = row.adapter
                    for (itemIndex in 0 until itemsAdapter.size()) {
                        val item = itemsAdapter.get(itemIndex)
                        val itemTitle = when (item) {
                            is Movie -> item.title
                            is TVShow -> item.name
                            else -> null
                        }

                        if (itemTitle == titleToFind) {
                            Log.d("MovieListActivity", "Found item at row=$rowIndex, position=$itemIndex")

                            // Function to perform the actual selection
                            val performSelection: () -> Unit = {
                                Log.d("MovieListActivity", "Performing selection for row=$rowIndex, item=$itemIndex")
                                setSelectedPosition(rowIndex, true)
                                // Additional delay to ensure row header is selected before selecting item
                                view?.postDelayed({
                                    setSelectedPosition(rowIndex, true, object : ListRowPresenter.SelectItemViewHolderTask(itemIndex) {
                                        override fun run(holder: Presenter.ViewHolder?) {
                                            super.run(holder)
                                            Log.d("MovieListActivity", "✓ Selection restored to: $titleToFind")
                                        }
                                    })
                                }, 200)
                            }

                            // If fragment is resumed and view is ready, select immediately
                            if (isResumed && view != null) {
                                Log.d("MovieListActivity", "Fragment is resumed, selecting immediately")
                                view?.post(performSelection)
                            } else {
                                // Otherwise wait for fragment to be ready
                                Log.d("MovieListActivity", "Fragment not ready, waiting for onResume")
                                view?.postDelayed({
                                    if (isResumed) {
                                        performSelection()
                                    } else {
                                        Log.w("MovieListActivity", "Fragment still not resumed after delay")
                                    }
                                }, 500)
                            }
                            return
                        }
                    }
                }
            }
            Log.w("MovieListActivity", "Could not find item with title: $titleToFind")
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
                    is TVShow -> {
                        Log.d("MovieListActivity", "TV Show clicked: ${item.name}")

                        // Navigate to show detail activity to display seasons
                        val token = activity?.intent?.getStringExtra("token")
                        if (token != null) {
                            val intent = android.content.Intent(activity, ShowDetailActivity::class.java).apply {
                                putExtra("show_name", item.name)
                                putExtra("token", token)
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(activity, "No token available", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
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
                is TVShow -> {
                    cardView.titleText = item.name
                    cardView.contentText = "${item.seasonCount} season${if (item.seasonCount != 1) "s" else ""}"
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