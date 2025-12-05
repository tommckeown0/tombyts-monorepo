package com.example.tombyts_android

import android.content.Intent
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

class ShowDetailActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val showName = intent.getStringExtra("show_name")
        val token = intent.getStringExtra("token")

        if (showName == null || token == null) {
            Toast.makeText(this, "Missing show information", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val fragment = ShowDetailFragment.newInstance(showName, token)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()
        }
    }

    class ShowDetailFragment : VerticalGridSupportFragment() {

        private lateinit var showName: String
        private lateinit var token: String
        private var tvShow: TVShow? = null

        companion object {
            private const val ARG_SHOW_NAME = "show_name"
            private const val ARG_TOKEN = "token"

            fun newInstance(showName: String, token: String): ShowDetailFragment {
                return ShowDetailFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_SHOW_NAME, showName)
                        putString(ARG_TOKEN, token)
                    }
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            showName = arguments?.getString(ARG_SHOW_NAME) ?: ""
            token = arguments?.getString(ARG_TOKEN) ?: ""

            setupUI()
            loadShowData()
        }

        private fun setupUI() {
            val gridPresenter = VerticalGridPresenter()
            gridPresenter.numberOfColumns = 4
            setGridPresenter(gridPresenter)

            title = showName

            onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
                if (item is Season) {
                    Log.d("ShowDetailActivity", "Season clicked: ${item.seasonNumber}")

                    val intent = Intent(activity, SeasonDetailActivity::class.java).apply {
                        putExtra("show_name", showName)
                        putExtra("season_number", item.seasonNumber)
                        putExtra("token", token)
                    }
                    startActivity(intent)
                }
            }
        }

        private fun loadShowData() {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    Log.d("ShowDetailActivity", "Fetching all media to find show: $showName")
                    val response = Classes.ApiProvider.apiService.getMovies("Bearer $token")

                    if (response.isSuccessful) {
                        val allMedia = response.body() ?: emptyList()
                        val parsedMedia = MediaParser.parseMedia(allMedia)

                        // Find the TV show
                        tvShow = parsedMedia.tvShows.find { it.name == showName }

                        if (tvShow != null) {
                            displaySeasons(tvShow!!.seasons)
                        } else {
                            Toast.makeText(activity, "Show not found", Toast.LENGTH_SHORT).show()
                            activity?.finish()
                        }
                    } else {
                        Toast.makeText(activity, "Failed to load show data", Toast.LENGTH_SHORT).show()
                        activity?.finish()
                    }
                } catch (e: Exception) {
                    Log.e("ShowDetailActivity", "Error loading show: ${e.message}", e)
                    Toast.makeText(activity, "Error loading show", Toast.LENGTH_SHORT).show()
                    activity?.finish()
                }
            }
        }

        private fun displaySeasons(seasons: List<Season>) {
            val adapter = ArrayObjectAdapter(SeasonCardPresenter())
            seasons.forEach { season ->
                adapter.add(season)
            }
            setAdapter(adapter)
        }
    }

    class SeasonCardPresenter : Presenter() {

        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val cardView = ImageCardView(parent.context).apply {
                isFocusable = true
                isFocusableInTouchMode = true
                setMainImageDimensions(313, 176)
            }
            return ViewHolder(cardView)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            if (item is Season) {
                val cardView = viewHolder.view as ImageCardView
                cardView.titleText = "Season ${item.seasonNumber}"
                cardView.contentText = "${item.episodeCount} episode${if (item.episodeCount != 1) "s" else ""}"
                cardView.mainImageView.setBackgroundColor(
                    ContextCompat.getColor(cardView.context, android.R.color.holo_green_dark)
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
