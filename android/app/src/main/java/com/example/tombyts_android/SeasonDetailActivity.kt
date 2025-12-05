package com.example.tombyts_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SeasonDetailActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val showName = intent.getStringExtra("show_name")
        val seasonNumber = intent.getIntExtra("season_number", 0)
        val token = intent.getStringExtra("token")

        if (showName == null || token == null) {
            Toast.makeText(this, "Missing episode information", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val fragment = SeasonDetailFragment.newInstance(showName, seasonNumber, token)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()
        }
    }

    class SeasonDetailFragment : VerticalGridSupportFragment() {

        private lateinit var showName: String
        private var seasonNumber: Int = 0
        private lateinit var token: String
        private var season: Season? = null

        companion object {
            private const val ARG_SHOW_NAME = "show_name"
            private const val ARG_SEASON_NUMBER = "season_number"
            private const val ARG_TOKEN = "token"

            fun newInstance(showName: String, seasonNumber: Int, token: String): SeasonDetailFragment {
                return SeasonDetailFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_SHOW_NAME, showName)
                        putInt(ARG_SEASON_NUMBER, seasonNumber)
                        putString(ARG_TOKEN, token)
                    }
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            showName = arguments?.getString(ARG_SHOW_NAME) ?: ""
            seasonNumber = arguments?.getInt(ARG_SEASON_NUMBER) ?: 0
            token = arguments?.getString(ARG_TOKEN) ?: ""

            setupUI()
            loadSeasonData()
        }

        private fun setupUI() {
            val gridPresenter = VerticalGridPresenter()
            gridPresenter.numberOfColumns = 4
            setGridPresenter(gridPresenter)

            title = "$showName - Season $seasonNumber"

            onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
                if (item is Episode) {
                    Log.d("SeasonDetailActivity", "Episode clicked: ${item.title}")

                    // Navigate to movie player
                    val intent = Intent(activity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("came_from_leanback", true)
                    }

                    val encodedTitle = Uri.encode(item.title)
                    intent.putExtra("navigate_to", "moviePlayer/${encodedTitle}/${token}")

                    startActivity(intent)
                }
            }
        }

        private fun loadSeasonData() {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    Log.d("SeasonDetailActivity", "Fetching all media to find season: $showName S$seasonNumber")
                    val response = Classes.ApiProvider.apiService.getMovies("Bearer $token")

                    if (response.isSuccessful) {
                        val allMedia = response.body() ?: emptyList()
                        val parsedMedia = MediaParser.parseMedia(allMedia)

                        // Find the TV show and season
                        val tvShow = parsedMedia.tvShows.find { it.name == showName }
                        season = tvShow?.seasons?.find { it.seasonNumber == seasonNumber }

                        if (season != null) {
                            displayEpisodes(season!!.episodes)
                        } else {
                            Toast.makeText(activity, "Season not found", Toast.LENGTH_SHORT).show()
                            activity?.finish()
                        }
                    } else {
                        Toast.makeText(activity, "Failed to load season data", Toast.LENGTH_SHORT).show()
                        activity?.finish()
                    }
                } catch (e: Exception) {
                    Log.e("SeasonDetailActivity", "Error loading season: ${e.message}", e)
                    Toast.makeText(activity, "Error loading season", Toast.LENGTH_SHORT).show()
                    activity?.finish()
                }
            }
        }

        private fun displayEpisodes(episodes: List<Episode>) {
            val adapter = ArrayObjectAdapter(EpisodeCardPresenter())
            episodes.forEach { episode ->
                adapter.add(episode)
            }
            setAdapter(adapter)
        }
    }

    class EpisodeCardPresenter : Presenter() {

        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val cardView = ImageCardView(parent.context).apply {
                isFocusable = true
                isFocusableInTouchMode = true
                setMainImageDimensions(313, 176)
            }
            return ViewHolder(cardView)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            if (item is Episode) {
                val cardView = viewHolder.view as ImageCardView
                cardView.titleText = item.title
                cardView.contentText = "Episode ${item.episodeNumber}"
                cardView.mainImageView.setBackgroundColor(
                    ContextCompat.getColor(cardView.context, android.R.color.darker_gray)
                )
            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {
            val cardView = viewHolder.view as ImageCardView
            cardView.badgeImage = null
            cardView.mainImage = null
        }
    }
}
