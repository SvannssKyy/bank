
package com.chkaduuu.mcbank.hooks;

import org.bukkit.entity.Player;
import com.chkaduuu.mcbank.models.BankAccount;
import com.chkaduuu.mcbank.models.PlayerData;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import com.chkaduuu.mcbank.McBank;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderAPIHook extends PlaceholderExpansion
{
    private final McBank plugin;
    
    public PlaceholderAPIHook(final McBank plugin) {
        this.plugin = plugin;
    }
    
    @NotNull
    public String getIdentifier() {
        return "mcbank";
    }
    
    @NotNull
    public String getAuthor() {
        return "Chkaduuu";
    }
    
    @NotNull
    public String getVersion() {
        return "3.0";
    }
    
    public boolean persist() {
        return true;
    }
    
    public String onRequest(final OfflinePlayer player, @NotNull final String params) {
        if (player == null) {
            return "";
        }
        final UUID uuid = player.getUniqueId();
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        if (pd == null) {
            return "N/A";
        }
        final BankAccount acc = pd.getFirstAccount();
        final String lowerCase = params.toLowerCase();
        switch (lowerCase) {
            case "bank_balance": {
                return (acc != null) ? this.plugin.getConfigManager().formatAmount(acc.getBalance()) : "0";
            }
            case "bank_loan": {
                return (acc != null) ? this.plugin.getConfigManager().formatAmount(acc.getLoan()) : "0";
            }
            case "bank_number": {
                return (acc != null) ? acc.getAccountNumber() : "N/A";
            }
            case "bank_level": {
                return String.valueOf(this.plugin.getBankUpgradeManager().getBankLevel(uuid));
            }
            case "bank_limit": {
                return this.plugin.getConfigManager().formatAmount(this.plugin.getBankUpgradeManager().getBankLimit(uuid));
            }
            case "bank_loan_limit": {
                return this.plugin.getConfigManager().formatAmount(this.plugin.getAccountManager().getLoanLimit(uuid));
            }
            case "loan_percent": {
                return String.valueOf(this.plugin.getConfigManager().getLoanInterestPercent());
            }
            default: {
                return null;
            }
        }
    }
    
    public String onPlaceholderRequest(final Player player, @NotNull final String params) {
        return this.onRequest((OfflinePlayer)player, params);
    }
}
