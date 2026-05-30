
package com.chkaduuu.mcbank.managers;

import org.bukkit.entity.Player;
import java.util.Iterator;
import com.chkaduuu.mcbank.models.BankAccount;
import com.chkaduuu.mcbank.models.PlayerData;
import com.chkaduuu.mcbank.hooks.VaultHook;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.HashMap;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.UUID;
import java.util.Map;
import com.chkaduuu.mcbank.McBank;

public class BankUpgradeManager
{
    private final McBank plugin;
    private final Map<UUID, Integer> bankLevels;
    private FileConfiguration upgradeConfig;
    
    public BankUpgradeManager(final McBank plugin) {
        this.bankLevels = new HashMap<UUID, Integer>();
        this.plugin = plugin;
        this.loadUpgradeConfig();
    }
    
    private void loadUpgradeConfig() {
        final File upgradeFile = new File(this.plugin.getDataFolder(), "files/upgrade.yml");
        if (!upgradeFile.exists()) {
            this.plugin.saveResource("files/upgrade.yml", false);
        }
        this.upgradeConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(upgradeFile);
    }
    
    public void reload() {
        this.loadUpgradeConfig();
    }
    
    public int getBankLevel(final UUID uuid) {
        return this.bankLevels.getOrDefault(uuid, 1);
    }
    
    public void setBankLevel(final UUID uuid, final int level) {
        this.bankLevels.put(uuid, level);
    }
    
    public double getBankLimit(final UUID uuid) {
        final int level = this.getBankLevel(uuid);
        return this.upgradeConfig.getDouble("bank-levels." + level + ".limit", 10000.0);
    }
    
    public double getIncomePercent(final UUID uuid) {
        final int level = this.getBankLevel(uuid);
        final String incomeStr = this.upgradeConfig.getString("bank-levels." + level + ".income", "1%");
        return this.parsePercent(incomeStr);
    }
    
    public double getOfflineIncomePercent(final UUID uuid) {
        final int level = this.getBankLevel(uuid);
        final String incomeStr = this.upgradeConfig.getString("bank-levels." + level + ".offline-income", "0%");
        return this.parsePercent(incomeStr);
    }
    
    public double getUpgradePrice(final int nextLevel) {
        return this.upgradeConfig.getDouble("bank-levels." + nextLevel + ".price", 0.0);
    }
    
    private double parsePercent(final String str) {
        if (str == null || str.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(str.replace("%", "").trim());
        }
        catch (final NumberFormatException e) {
            return 0.0;
        }
    }
    
    public boolean upgradeBank(final UUID uuid, final String playerName) {
        final int currentLevel = this.getBankLevel(uuid);
        final int maxLevel = this.plugin.getConfigManager().getMaxBankLevel();
        if (currentLevel >= maxLevel) {
            return false;
        }
        final int nextLevel = currentLevel + 1;
        final double price = this.getUpgradePrice(nextLevel);
        final VaultHook vault = this.plugin.getVaultHook();
        if (vault != null && vault.isEnabled()) {
            final double vaultBal = vault.getBalance(playerName);
            if (vaultBal < price) {
                return false;
            }
            vault.withdraw(playerName, price);
        }
        else {
            final PlayerData pd = this.plugin.getAccountManager().findPlayerDataByName(playerName);
            if (pd == null) {
                return false;
            }
            final BankAccount acc = pd.getFirstAccount();
            if (acc == null || acc.getBalance() < price) {
                return false;
            }
            acc.setBalance(acc.getBalance() - price);
        }
        this.bankLevels.put(uuid, nextLevel);
        return true;
    }
    
    public void distributeIncome() {
        for (final UUID uuid : this.bankLevels.keySet()) {
            if (!this.plugin.getAccountManager().hasAccount(uuid)) {
                continue;
            }
            final BankAccount acc = this.plugin.getAccountManager().getFirstAccount(uuid);
            if (acc == null) {
                continue;
            }
            if (acc.getBalance() <= 0.0) {
                continue;
            }
            final Player online = this.plugin.getServer().getPlayer(uuid);
            double percent;
            if (online != null && online.isOnline()) {
                percent = this.getIncomePercent(uuid);
            }
            else {
                percent = this.getOfflineIncomePercent(uuid);
            }
            if (percent <= 0.0) {
                continue;
            }
            final double income = acc.getBalance() * percent / 100.0;
            final double limit = this.getBankLimit(uuid);
            final double newBal = Math.min(acc.getBalance() + income, limit);
            final double actual = newBal - acc.getBalance();
            if (actual <= 0.0) {
                continue;
            }
            acc.setBalance(newBal);
            if (online == null || !online.isOnline()) {
                continue;
            }
            final String msg = this.plugin.getLangManager().get("bank_income_received", "%amount%", this.plugin.getConfigManager().formatAmount(actual));
            online.sendMessage(msg);
        }
        this.plugin.getAccountManager().saveData();
    }
    
    public void loadLevelsFromAccounts() {
    }
    
    public FileConfiguration getUpgradeConfig() {
        return this.upgradeConfig;
    }
    
    public int getMaxLevel() {
        return this.plugin.getConfigManager().getMaxBankLevel();
    }
}
