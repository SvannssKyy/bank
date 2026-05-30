
package com.chkaduuu.mcbank.managers;

import java.util.List;
import java.io.InputStream;
import org.bukkit.configuration.Configuration;
import java.io.Reader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import com.chkaduuu.mcbank.McBank;

public class ConfigManager
{
    private final McBank plugin;
    private FileConfiguration config;
    
    public ConfigManager(final McBank plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }
    
    private void loadConfig() {
        final File configFile = new File(this.plugin.getDataFolder(), "files/config.yml");
        if (!configFile.exists()) {
            this.plugin.saveResource("files/config.yml", false);
        }
        this.config = (FileConfiguration)YamlConfiguration.loadConfiguration(configFile);
        final InputStream defStream = this.plugin.getResource("files/config.yml");
        if (defStream != null) {
            final YamlConfiguration defConfig = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(defStream, StandardCharsets.UTF_8));
            this.config.setDefaults((Configuration)defConfig);
        }
    }
    
    public void reload() {
        this.loadConfig();
    }
    
    public FileConfiguration getConfig() {
        return this.config;
    }
    
    public String getLanguage() {
        return this.config.getString("language", "en");
    }
    
    public int getMaxBankLevel() {
        return this.config.getInt("max-bank-level", 3);
    }
    
    public String getBankIncomeTime() {
        return this.config.getString("bank-income-time", "5m");
    }
    
    public int getMaxAccountsPerPlayer() {
        return this.config.getInt("bank.max_accounts_per_player", 1);
    }
    
    public double getStartingBalance() {
        return this.config.getDouble("bank.starting_balance", 0.0);
    }
    
    public double getMinDeposit() {
        return this.config.getDouble("bank.min_deposit", 1.0);
    }
    
    public double getMinWithdraw() {
        return this.config.getDouble("bank.min_withdraw", 1.0);
    }
    
    public double getMinTransfer() {
        return this.config.getDouble("bank.min_transfer", 1.0);
    }
    
    public double getMinCashout() {
        return this.config.getDouble("bank.min_cashout", 1.0);
    }
    
    public String getCurrencySymbol() {
        return this.config.getString("bank.currency_symbol", "$");
    }
    
    public String getCurrencyName() {
        return this.config.getString("bank.currency_name", "Coin");
    }
    
    public String getCurrencyNamePlural() {
        return this.config.getString("bank.currency_name_plural", "Coins");
    }
    
    public boolean isLoanEnabled() {
        return this.config.getBoolean("loan.enabled", true);
    }
    
    public int getLoanInterestPercent() {
        return this.config.getInt("loan.interest_percent", 5);
    }
    
    public double getDefaultLoanLimit() {
        return this.config.getDouble("loan.default_loan_limit", 10000.0);
    }
    
    public int getLoanOverdueDays() {
        return this.config.getInt("loan.overdue_days", 7);
    }
    
    public int getLoanOverduePenaltyPercent() {
        return this.config.getInt("loan.overdue_penalty_percent", 10);
    }
    
    public List<String> getAccountFormats() {
        return this.config.getStringList("account_formats");
    }
    
    public boolean isVaultEnabled() {
        return this.config.getBoolean("vault.enabled", true);
    }
    
    public boolean isEssentialsEnabled() {
        return this.config.getBoolean("essentialsx.enabled", true);
    }
    
    public boolean isCmiEnabled() {
        return this.config.getBoolean("cmi.enabled", true);
    }
    
    public boolean isPlaceholderApiEnabled() {
        return this.config.getBoolean("placeholderapi.enabled", true);
    }
    
    public String getAtmHologramText() {
        return this.config.getString("atm.hologram_text", "&6&lBank ATM");
    }
    
    public double getAtmHologramHeight() {
        return this.config.getDouble("atm.hologram_height", 1.5);
    }
    
    public boolean isAtmPinRequired() {
        return this.config.getBoolean("atm.pin_required", true);
    }
    
    public int getAtmDailyLimit() {
        return this.config.getInt("atm.daily_limit", 10);
    }
    
    public int getPinMaxAttempts() {
        return this.config.getInt("pin.max_attempts", 3);
    }
    
    public boolean isDebug() {
        return this.config.getBoolean("debug", false);
    }
    
    public String formatAmount(final double amount) {
        if (amount == Math.floor(amount)) {
            return this.getCurrencySymbol() + String.format("%,.0f", amount);
        }
        return this.getCurrencySymbol() + String.format("%,.2f", amount);
    }
}
