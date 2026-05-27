package me.quantiom.advancedvanish;

import org.bukkit.plugin.java.JavaPlugin;

public class AdvancedVanishPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        AdvancedVanish.INSTANCE.onEnable(this);
    }

    @Override
    public void onDisable() {
        AdvancedVanish.INSTANCE.onDisable();
    }
}
