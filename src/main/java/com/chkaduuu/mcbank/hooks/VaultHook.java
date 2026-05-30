
package com.chkaduuu.mcbank.hooks;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.Bukkit;
import net.milkbowl.vault.economy.Economy;
import com.chkaduuu.mcbank.McBank;

public class VaultHook
{
    private final McBank plugin;
    private Economy economy;
    private boolean enabled;
    
    public VaultHook(final McBank plugin) {
        this.enabled = false;
        this.plugin = plugin;
        this.setup();
    }
    
    private void setup() {
        if (!this.plugin.getConfigManager().isVaultEnabled()) {
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        final RegisteredServiceProvider<Economy> rsp = (RegisteredServiceProvider<Economy>)Bukkit.getServicesManager().getRegistration((Class)Economy.class);
        if (rsp == null) {
            return;
        }
        this.economy = (Economy)rsp.getProvider();
        this.enabled = (this.economy != null);
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }
    
    public double getBalance(final String playerName) {
        if (!this.enabled) {
            return 0.0;
        }
        return this.economy.getBalance(playerName);
    }
    
    public boolean has(final String playerName, final double amount) {
        return this.enabled && this.economy.has(playerName, amount);
    }
    
    public boolean withdraw(final String playerName, final double amount) {
        return this.enabled && this.economy.withdrawPlayer(playerName, amount).transactionSuccess();
    }
    
    public boolean deposit(final String playerName, final double amount) {
        return this.enabled && this.economy.depositPlayer(playerName, amount).transactionSuccess();
    }
}
