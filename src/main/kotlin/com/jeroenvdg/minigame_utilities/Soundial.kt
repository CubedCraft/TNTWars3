package com.jeroenvdg.minigame_utilities

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.random.Random

@Deprecated("Deprecated since 1.21.11, use SoundHelper")
enum class Soundial {
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

    constructor(sound: Soundial) {
        this.id = sound.id
    }

    companion object {
        fun playAll(sound: Soundial, volume: Float = 1f, pitch: Float = 1f) {
            Audience.audience(Bukkit.getOnlinePlayers()).playSound(Sound.sound(Key.key(sound.id), Sound.Source.MASTER, volume, Random.nextFloat() * 0.01f + pitch - 0.005f))
        }

        fun playAll(sound: String, volume: Float = 1f, pitch: Float = 1f) {
            Audience.audience(Bukkit.getOnlinePlayers()).playSound(Sound.sound(Key.key(sound), Sound.Source.MASTER, volume, Random.nextFloat() * 0.01f + pitch - 0.005f))
        }

        fun playAll(sound: org.bukkit.Sound, volume: Float = 1f, pitch: Float = 1f) {
            playAll(transform(sound), volume, pitch)
        }

        fun play(player: Player, sound: Soundial, volume: Float = 1f, pitch: Float = 1f) {
            player.playSound(Sound.sound(Key.key(sound.id), Sound.Source.MASTER, volume, Random.nextFloat() * 0.01f + pitch - 0.005f))
        }

        fun play(player: Player, sound: String, volume: Float = 1f, pitch: Float = 1f) {
            player.playSound(Sound.sound(Key.key(sound), Sound.Source.MASTER, volume, Random.nextFloat() * 0.01f + pitch - 0.005f))
        }

        fun play(player: Player, sound: org.bukkit.Sound, volume: Float = 1f, pitch: Float = 1f) {
            play(player, transform(sound), volume, pitch)
        }

        private fun transform(sound: org.bukkit.Sound) = sound.toString().lowercase().replace('_', '.')
    }
}