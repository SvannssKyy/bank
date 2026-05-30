
package com.chkaduuu.mcbank.hooks;

import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import com.chkaduuu.mcbank.McBank;

public class CMIHook
{
    private final McBank plugin;
    private boolean enabled;
    
    public CMIHook(final McBank plugin) {
        this.enabled = false;
        this.plugin = plugin;
        this.setup();
    }
    
    private void setup() {
        if (!this.plugin.getConfigManager().isCmiEnabled()) {
            return;
        }
        final Plugin cmi = Bukkit.getPluginManager().getPlugin("CMI");
        if (cmi != null && cmi.isEnabled()) {
            this.enabled = true;
        }
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }
}
