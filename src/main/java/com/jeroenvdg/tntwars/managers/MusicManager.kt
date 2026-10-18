package com.jeroenvdg.tntwars.managers

import com.jeroenvdg.tntwars.TNTWars
import com.xxmicloxx.NoteBlockAPI.model.Playlist
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode
import com.xxmicloxx.NoteBlockAPI.model.Song
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder
import net.kyori.adventure.audience.Audience
import org.bukkit.entity.Player
import java.io.File
import java.nio.file.Files

class MusicManager(private val plugin: TNTWars) {
    private var radio: RadioSongPlayer? = null
    val audience: Audience
        get() {
            return Audience.audience(radio?.playerUUIDs?.mapNotNull(plugin.server::getPlayer) ?: listOf<Player>())
    }

    init {
        loadMusicFiles()
    }

    private fun loadMusicFiles() {
        val folder = File(plugin.dataFolder, "music")
        Files.createDirectories(folder.toPath())

        val songs = mutableListOf<Song>()

        // Files.walk returnerar en Stream som stängs automatiskt med .use { }
        Files.walk(folder.toPath()).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".nbs", ignoreCase = true) } // Filtrera på önskat format
                .forEach { path ->
                    val file = path.toFile()
                    val song = NBSDecoder.parse(file)

                    if (song == null) {
                        plugin.logger.warning("Could not load music file: ${file.name}")
                        return@forEach
                    }

                    songs.add(song)
                    // Hantera filen här
                    plugin.logger.info("Found music file: ${file.name}")
                }
        }

        if (songs.isEmpty()) {
            plugin.logger.info("No music files found in ${folder.path}; music playback is unavailable.")
            return
        }

        val player = RadioSongPlayer(Playlist(*songs.toTypedArray()))
        radio = player
        player.setPlaying(true, true)
        player.volume = 30
        radio?.repeatMode = RepeatMode.ALL
    }
    fun enableMusic(player: Player) {
        val radio = radio ?: run {
            player.sendMessage("No music is currently available.")
            return
        }

        if(radio.playerUUIDs.contains(player.uniqueId)) return
        radio.addPlayer(player)
    }

    fun disableMusic(player: Player) {
        val radio = radio ?: return
        if(!radio.playerUUIDs.contains(player.uniqueId)) return
        radio.removePlayer(player)
    }

    fun unload() {
        val radio = radio ?: return
        radio.playerUUIDs.forEach { radio.removePlayer(it) }
        radio.destroy()
        this.radio = null
    }

}