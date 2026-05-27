package me.quantiom.advancedvanish.hook.impl

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import me.quantiom.advancedvanish.AdvancedVanish
import me.quantiom.advancedvanish.config.Config
import me.quantiom.advancedvanish.event.PlayerUnVanishEvent
import me.quantiom.advancedvanish.event.PlayerVanishEvent
import me.quantiom.advancedvanish.hook.IHook
import me.quantiom.advancedvanish.util.AdvancedVanishAPI
import me.quantiom.advancedvanish.util.color
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler

class ActionBarHook : IHook {
    private var updateTask: ScheduledTask? = null

    override fun getID() = "ActionBar"

    override fun onEnable() {
        this.updateTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(AdvancedVanish.instance!!, { _ ->
            AdvancedVanishAPI.vanishedPlayers.forEach { uuid ->
                Bukkit.getPlayer(uuid)?.let { player ->
                    player.getScheduler().run(AdvancedVanish.instance!!, { _ ->
                        sendActionBar(player)
                    }, null)
                }
            }
        }, 1L, 30L)
    }

    override fun onDisable() {
        this.updateTask?.cancel()
        this.updateTask = null
    }

    private fun sendActionBar(player: Player) {
        this.sendActionBarStr(player, Config.getValueOrDefault("messages.action-bar", "<red>You are in vanish."))
    }

    private fun sendActionBarStr(player: Player, str: String) {
        player.sendActionBar(str.color())
    }

    @EventHandler
    private fun onVanish(event: PlayerVanishEvent) {
        this.sendActionBar(event.player)
    }

    @EventHandler
    private fun onUnVanish(event: PlayerUnVanishEvent) {
        this.sendActionBarStr(event.player, "")
    }
}
