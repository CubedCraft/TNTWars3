package com.jeroenvdg.minigame_utilities

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import kotlin.random.Random

object SoundHelper {
    enum class Sounds {
        Banjo("block.note_block.banjo"),
        BaseDrum("block.note_block.basedrum"),
        Bass("block.note_block.bass"),
        Bell("block.note_block.bell"),
        Bit("block.note_block.bit"),
        CowBell("block.note_block.cow_bell"),
        Didgeridoo("block.note_block.didgeridoo"),
        Flute("block.note_block.flute"),
        Guitar("block.note_block.guitar"),
        Harp("block.note_block.harp"),
        Hat("block.note_block.hat"),
        Pling("block.note_block.pling"),
        Exp("entity.experience_orb.pickup"),
        DragonGrowl("entity.ender_dragon.growl"),
        Horn1("item.goat_horn.sound.1"),
        VillagerNo("entity.villager.no"),

        Success(Pling),
        Fail(VillagerNo),
        Countdown(Hat),
        MatchStarted(Horn1),
        MatchEnded(DragonGrowl),
        UIOpen(Harp),
        UIClick(Hat),
        UIFail(VillagerNo);

        val id: String

        constructor(sound: String) {
            this.id = sound
        }

        constructor(sound: Sounds) {
            this.id = sound.id
        }
    }
    fun playAll(sound: Sounds, volume: Float = 1f, pitch: Float = 1f) {
        playAll(Sound.sound().type(NamespacedKey.minecraft(sound.id)).source(Sound.Source.MASTER).volume(volume).pitch(Random.nextFloat() * 0.01f + pitch - 0.005f).build())
    }

    fun playAll(sound: Sound) {
        Audience.audience(Bukkit.getOnlinePlayers()).playSound(sound)
    }

    fun playAll(sound: String, volume: Float = 1f, pitch: Float = 1f) {
        playAll(Sound.sound().type(NamespacedKey.minecraft(sound)).source(Sound.Source.MASTER).volume(volume).pitch(Random.nextFloat() * 0.01f + pitch - 0.005f).build())
    }

    fun play(player: Player, sound: Sounds, volume: Float = 1f, pitch: Float = 1f) {
        play(player, Sound.sound().type(NamespacedKey.minecraft(sound.id)).source(Sound.Source.MASTER).volume(volume).pitch(Random.nextFloat() * 0.01f + pitch - 0.005f).build())
    }

    fun play(player: Player, sound: String, volume: Float = 1f, pitch: Float = 1f) {
        play(player, Sound.sound().type(NamespacedKey.minecraft(sound)).source(Sound.Source.MASTER).volume(volume).pitch(Random.nextFloat() * 0.01f + pitch - 0.005f).build())
    }

    fun play(player: Player, sound: Sound) {
        player.playSound(sound)
    }

}