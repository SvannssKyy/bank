
package com.chkaduuu.mcbank.hooks;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import com.chkaduuu.mcbank.McBank;

public class EssentialsHook
{
    private final McBank plugin;
    private boolean enabled;
    private Object essentials;
    
    public EssentialsHook(final McBank plugin) {
        this.enabled = false;
        this.plugin = plugin;
        this.setup();
    }
    
    private void setup() {
        if (!this.plugin.getConfigManager().isEssentialsEnabled()) {
            return;
        }
        final Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
        if (ess != null && ess.isEnabled()) {
            this.essentials = ess;
            this.enabled = true;
        }
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }
    
    public boolean isVanished(final Player player) {
        if (!this.enabled || this.essentials == null) {
            return false;
        }
        try {
            final Class<?> essClass = this.essentials.getClass().getClassLoader().loadClass("com.earth2me.essentials.Essentials");
            final Object user = essClass.getMethod("getUser", Player.class).invoke(this.essentials, player);
            return user != null && (boolean)user.getClass().getMethod("isVanished", (Class<?>[])new Class[0]).invoke(user, new Object[0]);
        }
        catch (final Exception e) {
            return false;
        }
    }
}
