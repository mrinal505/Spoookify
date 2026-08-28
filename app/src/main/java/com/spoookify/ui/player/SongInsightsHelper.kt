package com.spoookify.ui.player

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrackInsightsData(
    val genre: String,
    val energy: String,
    val bpm: String,
    val key: String,
    val playCountText: String,
    val skipRatioText: String,
    val firstListenedText: String
)

object SongInsightsHelper {

    fun detectGenre(artist: String, title: String): String {
        val text = "$artist $title".lowercase(Locale.ROOT)
        return when {
            text.contains("arijit") || text.contains("pritam") || text.contains("bollywood") ||
            text.contains("svf") || text.contains("shreya") || text.contains("badshah") ||
            text.contains("neha") || text.contains("t-series") || text.contains("zee music") ||
            text.contains("yrf") || text.contains("sonu") || text.contains("jubin") ||
            text.contains("kumar") || text.contains("sanit") || text.contains("tumi") ||
            text.contains("bhoomi") -> "Bollywood / Indian Pop"

            text.contains("ed sheeran") || text.contains("taylor swift") || text.contains("bieber") ||
            text.contains("sheeran") || text.contains("swift") || text.contains("dualipa") ||
            text.contains("dua lipa") || text.contains("charli") || text.contains("pop") ||
            text.contains("katy") || text.contains("gomez") -> "Pop / Acoustic"

            text.contains("drake") || text.contains("eminem") || text.contains("travis") ||
            text.contains("hip hop") || text.contains("rap") || text.contains("kendrick") ||
            text.contains("kanye") || text.contains("subu") || text.contains("21 savage") -> "Hip-Hop / Rap"

            text.contains("rock") || text.contains("coldplay") || text.contains("metal") ||
            text.contains("queen") || text.contains("linkin") || text.contains("nirvana") ||
            text.contains("imagine dragons") -> "Rock / Alternative"

            text.contains("garrix") || text.contains("edm") || text.contains("avicii") ||
            text.contains("dance") || text.contains("dj") || text.contains("skrillex") ||
            text.contains("marshmello") -> "EDM / Dance"

            text.contains("lofi") || text.contains("chill") || text.contains("piano") ||
            text.contains("relax") || text.contains("study") -> "Lo-Fi / Ambient"

            else -> "Pop / Contemporary"
        }
    }

    fun calculateEnergy(trackId: String, title: String): String {
        val hash = (trackId + title).hashCode().let { if (it < 0) -it else it }
        val energyPct = 68 + (hash % 26) // 68% to 93%
        val levelLabel = when {
            energyPct >= 85 -> "High"
            energyPct >= 75 -> "Moderate-High"
            else -> "Balanced"
        }
        return "$energyPct% ($levelLabel)"
    }

    fun calculateBpm(trackId: String, title: String): String {
        val hash = (trackId + title).hashCode().let { if (it < 0) -it else it }
        val bpm = 95 + (hash % 45) // 95 BPM to 139 BPM
        return "$bpm BPM"
    }

    fun calculateKey(trackId: String, title: String): String {
        val keys = listOf("C Major", "G Major", "D Major", "A Major", "E Major", "A Minor", "E Minor", "D Minor", "F Major", "B Minor", "F# Minor", "C# Minor")
        val hash = (trackId + title).hashCode().let { if (it < 0) -it else it }
        return keys[hash % keys.size]
    }

    fun formatFirstPlayedDate(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "Today (First session)"
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
