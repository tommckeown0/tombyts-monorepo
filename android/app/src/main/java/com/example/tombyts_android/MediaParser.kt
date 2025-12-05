package com.example.tombyts_android

object MediaParser {

    /**
     * Separates movies from TV episodes
     */
    fun parseMedia(allMedia: List<Movie>): ParsedMedia {
        android.util.Log.d("MediaParser", "parseMedia called with ${allMedia.size} items")

        val movies = allMedia.filter { it.path.startsWith("Movies/") }
        val tvEpisodes = allMedia.filter { it.path.startsWith("TV Shows/") }

        android.util.Log.d("MediaParser", "Filtered: ${movies.size} movies, ${tvEpisodes.size} TV episodes")

        val tvShows = groupIntoShows(tvEpisodes)

        android.util.Log.d("MediaParser", "Grouped into ${tvShows.size} TV shows")

        return ParsedMedia(movies, tvShows)
    }

    /**
     * Groups TV episodes into shows with seasons
     * Path format: "TV Shows/[Show Name]/Season XX/[Show Name] - SXXEXX.mkv"
     */
    private fun groupIntoShows(episodes: List<Movie>): List<TVShow> {
        // Group by show name (second path component)
        val episodesByShow = episodes.groupBy { episode ->
            val pathParts = episode.path.split("/")
            if (pathParts.size >= 2) pathParts[1] else "Unknown"
        }

        return episodesByShow.map { (showName, showEpisodes) ->
            val seasons = groupIntoSeasons(showName, showEpisodes)
            TVShow(
                id = showName,
                name = showName,
                seasonCount = seasons.size,
                path = "TV Shows/$showName",
                seasons = seasons
            )
        }.sortedBy { it.name }
    }

    /**
     * Groups episodes into seasons for a TV show
     * Path format: "TV Shows/[Show Name]/Season XX/[Show Name] - SXXEXX.mkv"
     */
    private fun groupIntoSeasons(showName: String, episodes: List<Movie>): List<Season> {
        // Group by season number (extracted from path or title)
        val episodesBySeason = episodes.groupBy { episode ->
            extractSeasonNumber(episode.path, episode.title)
        }

        return episodesBySeason.map { (seasonNum, seasonEpisodes) ->
            val episodeList = seasonEpisodes.map { movie ->
                Episode(
                    id = movie.id,
                    title = movie.title,
                    showName = showName,
                    seasonNumber = seasonNum,
                    episodeNumber = extractEpisodeNumber(movie.path, movie.title),
                    path = movie.path
                )
            }.sortedBy { it.episodeNumber }

            Season(
                seasonNumber = seasonNum,
                showName = showName,
                episodeCount = episodeList.size,
                path = "TV Shows/$showName/Season ${seasonNum.toString().padStart(2, '0')}",
                episodes = episodeList
            )
        }.sortedBy { it.seasonNumber }
    }

    /**
     * Extracts season number from path or title
     * Examples: "Season 01", "S01E02", etc.
     */
    private fun extractSeasonNumber(path: String, title: String): Int {
        // Try path first: "TV Shows/Show/Season 01/..."
        val seasonRegex = """Season (\d+)""".toRegex()
        seasonRegex.find(path)?.let {
            return it.groupValues[1].toInt()
        }

        // Try title: "Show - S01E02"
        val titleRegex = """S(\d+)E\d+""".toRegex()
        titleRegex.find(title)?.let {
            return it.groupValues[1].toInt()
        }

        return 0
    }

    /**
     * Extracts episode number from path or title
     * Examples: "S01E02", "E02", etc.
     */
    private fun extractEpisodeNumber(path: String, title: String): Int {
        // Try title: "Show - S01E02"
        val episodeRegex = """E(\d+)""".toRegex()
        episodeRegex.find(title)?.let {
            return it.groupValues[1].toInt()
        }

        // Try path if title doesn't work
        episodeRegex.find(path)?.let {
            return it.groupValues[1].toInt()
        }

        return 0
    }
}

/**
 * Container for parsed media
 */
data class ParsedMedia(
    val movies: List<Movie>,
    val tvShows: List<TVShow>
)
