package me.quantiom.advancedvanish.hook.impl

import dev.esophose.playerparticles.api.PlayerParticlesAPI
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import me.quantiom.advancedvanish.AdvancedVanish
import me.quantiom.advancedvanish.hook.IHook
import me.quantiom.advancedvanish.util.AdvancedVanishAPI
import org.bukkit.Bukkit

class PlayerParticlesHook : IHook {
    override fun getID() = "PlayerParticles"

    private var updateTask: ScheduledTask? = null

    override fun onEnable() {
        this.updateTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(AdvancedVanish.instance!!, { _ ->
            AdvancedVanishAPI.vanishedPlayers.forEach { uuid ->
                Bukkit.getPlayer(uuid)?.let { player ->
                    player.getScheduler().run(AdvancedVanish.instance!!, { _ ->
                        PlayerParticlesAPI.getInstance().getPPlayer(player.uniqueId)?.activeParticles?.clear()
                    }, null)
                }
            }
        }, 1L, 20L)
    }

    override fun onDisable() {
        this.updateTask?.cancel()
        this.updateTask = null
    }
}
