package me.quantiom.advancedvanish.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import me.quantiom.advancedvanish.AdvancedVanish
import me.quantiom.advancedvanish.config.Config
import me.quantiom.advancedvanish.event.PlayerUnVanishEvent
import me.quantiom.advancedvanish.event.PlayerVanishEvent
import me.quantiom.advancedvanish.event.PrePlayerUnVanishEvent
import me.quantiom.advancedvanish.event.PrePlayerVanishEvent
import me.quantiom.advancedvanish.permission.PermissionsManager
import me.quantiom.advancedvanish.state.VanishStateManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

fun Player.isVanished() = AdvancedVanishAPI.isPlayerVanished(this)

object AdvancedVanishAPI {
    val vanishedPlayers: MutableList<UUID> = CopyOnWriteArrayList()
    private val storedPotionEffects: MutableMap<UUID, List<PotionEffect>> = ConcurrentHashMap()

    fun vanishPlayer(player: Player, onJoin: Boolean = false) {
        val prePlayerVanishEvent = PrePlayerVanishEvent(player, onJoin)
        Bukkit.getPluginManager().callEvent(prePlayerVanishEvent)

        if (prePlayerVanishEvent.isCancelled) return

        this.vanishedPlayers.add(player.uniqueId)
        
        player.setMetadata("vanished", FixedMetadataValue(AdvancedVanish.instance!!, true))

        val previousEffects: MutableList<PotionEffect> = mutableListOf()
        
        Config.getValueOrDefault("when-vanished.give-potion-effects", mutableListOf<String>())
            .map { it.split(":") }
            .filter { it.size > 1 }
            .forEach { potionInfo ->
                PotionEffectType.values().find { e -> e?.name == potionInfo[0] }?.run {
                    val currentPotionEffect = player.activePotionEffects.find { e -> e.type == this }

                    if (currentPotionEffect != null) {
                        previousEffects.add(currentPotionEffect)
                    } else {
                        previousEffects.add(this.createEffect(0, 0))
                    }
                    
                    val duration = if (Bukkit.getVersion().contains("1.19.4") || Bukkit.getVersion().contains(" 1.2")) {
                        -1
                    } else Integer.MAX_VALUE

                    if (onJoin) {
                        player.getScheduler().runDelayed(AdvancedVanish.instance!!, {
                            player.addPotionEffect(this.createEffect(duration, potionInfo[1].toInt() - 1))
                        }, null, 10L)
                    } else {
                        player.addPotionEffect(this.createEffect(duration, potionInfo[1].toInt() - 1))
                    }
                }
            }

        if (previousEffects.isNotEmpty()) {
            this.storedPotionEffects[player.uniqueId] = previousEffects
        }

        val usePriority = Config.usingPriorities && PermissionsManager.handler != null
        val playerPriority = PermissionsManager.handler?.getVanishPriority(player)

        Bukkit.getOnlinePlayers()
            .filter { it.uniqueId != player.uniqueId }
            .forEach { observer ->
                observer.getScheduler().run(AdvancedVanish.instance!!, {
                    if (usePriority && observer.hasPermission(Config.getValueOrDefault(
                            "permissions.vanish",
                            "advancedvanish.vanish"
                        ))) {
                        val pPriority = PermissionsManager.handler!!.getVanishPriority(observer)

                        if (pPriority < playerPriority!!) {
                            observer.hidePlayer(AdvancedVanish.instance!!, player)
                        }
                    } else {
                        observer.hidePlayer(AdvancedVanish.instance!!, player)
                    }
                }, null)
            }

        if (!onJoin && Config.getValueOrDefault("join-leave-messages.fake-leave-message-on-vanish.enable", false)) {
            val message = Config.getValueOrDefault(
                "join-leave-messages.fake-leave-message-on-vanish.message",
                "<yellow>%player-name% has left the game."
            ).applyPlaceholders(
                "%player-name%" to player.name
            ).color()

            Bukkit.getOnlinePlayers().forEach { it.sendMessage(message) }
        }

        if (Config.getValueOrDefault("when-vanished.fly.enable", true)) {
            player.allowFlight = true
        }

        Bukkit.getPluginManager().callEvent(PlayerVanishEvent(player, onJoin))
    }

    fun unVanishPlayer(player: Player, onLeave: Boolean = false) {
        val prePlayerUnVanishEvent = PrePlayerUnVanishEvent(player, onLeave)
        Bukkit.getPluginManager().callEvent(prePlayerUnVanishEvent)

        if (prePlayerUnVanishEvent.isCancelled) return

        this.vanishedPlayers.remove(player.uniqueId)

        player.removeMetadata("vanished", AdvancedVanish.instance!!)

        VanishStateManager.interactEnabled.remove(player.uniqueId)

        this.storedPotionEffects[player.uniqueId]?.let {
            for (potionEffect in it) {
                player.removePotionEffect(potionEffect.type)

                if (potionEffect.duration != 0) {
                    player.addPotionEffect(potionEffect)
                }
            }

            this.storedPotionEffects.remove(player.uniqueId)
        }

        Bukkit.getOnlinePlayers()
            .forEach { observer ->
                observer.getScheduler().run(AdvancedVanish.instance!!, {
                    observer.showPlayer(AdvancedVanish.instance!!, player)
                }, null)
            }

        if (player.gameMode != GameMode.SPECTATOR && !player.hasPermission(Config.getValueOrDefault("permissions.keep-fly-on-unvanish", "advancedvanish.keep-fly"))
            && !Config.getValueOrDefault("advancedvanish.fly.keep-on-unvanish", false)) {
            player.isFlying = false
            player.allowFlight = false
        }

        if (!onLeave && Config.getValueOrDefault("join-leave-messages.fake-join-message-on-unvanish.enable", false)) {
            val message = Config.getValueOrDefault(
                "join-leave-messages.fake-join-message-on-unvanish.message",
                "<yellow>%player-name% has joined the game."
            ).applyPlaceholders(
                "%player-name%" to player.name
            ).color()

            Bukkit.getOnlinePlayers().forEach { it.sendMessage(message) }
        }

        Bukkit.getPluginManager().callEvent(PlayerUnVanishEvent(player, onLeave))
    }

    fun refreshVanished(player: Player) {
        this.vanishedPlayers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let {
                if (!this.canSee(player, it)) {
                    player.hidePlayer(AdvancedVanish.instance!!, it)
                }
            }
        }
    }

    fun isPlayerVanished(player: Player): Boolean = this.vanishedPlayers.contains(player.uniqueId)

    fun canSee(player: Player, target: Player): Boolean {
        if (!target.isVanished()) return false

        if (!player.hasPermission(Config.getValueOrDefault(
                "permissions.vanish",
                "advancedvanish.vanish"
            ))) return false

        if (!Config.usingPriorities || PermissionsManager.handler == null) return true

        return PermissionsManager.handler!!.getVanishPriority(player) >= PermissionsManager.handler!!.getVanishPriority(target)
    }
}
