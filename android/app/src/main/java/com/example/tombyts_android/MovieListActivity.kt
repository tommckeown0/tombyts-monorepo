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

    // Custom fragment that extends BrowseSupportFragment
    class CustomBrowseFragment : androidx.leanback.app.BrowseSupportFragment() {

        private var parsedMedia: ParsedMedia? = null

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