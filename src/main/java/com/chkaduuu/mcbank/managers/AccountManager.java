
package com.chkaduuu.mcbank.managers;

import org.bukkit.entity.Player;
import java.util.Collection;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.Random;
import java.util.Collections;
import java.io.IOException;
import java.util.logging.Level;
import java.util.List;
import java.util.Iterator;
import com.chkaduuu.mcbank.models.BankAccount;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.HashMap;
import com.chkaduuu.mcbank.models.PlayerData;
import java.util.UUID;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import com.chkaduuu.mcbank.McBank;

public class AccountManager
{
    private final McBank plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, PlayerData> playerDataMap;
    
    public AccountManager(final McBank plugin) {
        this.playerDataMap = new HashMap<UUID, PlayerData>();
        this.plugin = plugin;
        this.loadData();
    }
    
    private void loadData() {
        this.dataFile = new File(this.plugin.getDataFolder(), "files/data.yml");
        if (!this.dataFile.exists()) {
            this.plugin.saveResource("files/data.yml", false);
        }
        this.dataConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(this.dataFile);
        this.playerDataMap.clear();
        if (this.dataConfig.isConfigurationSection("players")) {
            for (String uuidStr : this.dataConfig.getConfigurationSection("players").getKeys(false)) {
                final String base = "players." + uuidStr;
                final String name = this.dataConfig.getString(base + ".name", "Unknown");
                final PlayerData pd = new PlayerData(uuidStr, name);
                pd.setLevel(this.dataConfig.getInt(base + ".level", 0));
                pd.setPin(this.dataConfig.getString(base + ".pin", (String)null));
                pd.setPinLocked(this.dataConfig.getBoolean(base + ".pin_locked", false));
                pd.setPinAttempts(this.dataConfig.getInt(base + ".pin_attempts", 0));
                pd.setAtmOpsDate(this.dataConfig.getString(base + ".atm_ops_date", ""));
                pd.setAtmOpsCount(this.dataConfig.getInt(base + ".atm_ops_count", 0));
                final List<String> notifs = this.dataConfig.getStringList(base + ".pending_notifications");
                pd.setPendingNotifications(notifs);
                if (this.dataConfig.isConfigurationSection(base + ".accounts")) {
                    for (String accNum : this.dataConfig.getConfigurationSection(base + ".accounts").getKeys(false)) {
                        final String accBase = base + ".accounts." + accNum;
                        final double balance = this.dataConfig.getDouble(accBase + ".balance", 0.0);
                        final double loan = this.dataConfig.getDouble(accBase + ".loan", 0.0);
                        final long loanTakenDate = this.dataConfig.getLong(accBase + ".loan_taken_date", 0L);
                        final boolean active = this.dataConfig.getBoolean(accBase + ".active", true);
                        final long created = this.dataConfig.getLong(accBase + ".created", System.currentTimeMillis());
                        final List<String> history = this.dataConfig.getStringList(accBase + ".history");
                        final BankAccount acc = new BankAccount(accNum, balance, loan, loanTakenDate, active, created, history);
                        pd.getAccounts().put(accNum, acc);
                    }
                }
                try {
                    this.playerDataMap.put(UUID.fromString(uuidStr), pd);
                }
                catch (final IllegalArgumentException ex) {}
            }
        }
    }
    
    public void saveData() {
        if (this.dataFile == null) {
            return;
        }
        this.dataConfig.set("players", (Object)null);
        for (Map.Entry<UUID, PlayerData> entry : this.playerDataMap.entrySet()) {
            final String uuidStr = entry.getKey().toString();
            final PlayerData pd = entry.getValue();
            final String base = "players." + uuidStr;
            this.dataConfig.set(base + ".name", (Object)pd.getName());
            this.dataConfig.set(base + ".level", (Object)pd.getLevel());
            this.dataConfig.set(base + ".pin", (Object)pd.getPin());
            this.dataConfig.set(base + ".pin_locked", (Object)pd.isPinLocked());
            this.dataConfig.set(base + ".pin_attempts", (Object)pd.getPinAttempts());
            this.dataConfig.set(base + ".atm_ops_date", (Object)pd.getAtmOpsDate());
            this.dataConfig.set(base + ".atm_ops_count", (Object)pd.getAtmOpsCount());
            this.dataConfig.set(base + ".pending_notifications", (Object)pd.getPendingNotifications());
            for (Map.Entry<String, BankAccount> accEntry : pd.getAccounts().entrySet()) {
                final String accNum = accEntry.getKey();
                final BankAccount acc = accEntry.getValue();
                final String accBase = base + ".accounts." + accNum;
                this.dataConfig.set(accBase + ".balance", (Object)acc.getBalance());
                this.dataConfig.set(accBase + ".loan", (Object)acc.getLoan());
                this.dataConfig.set(accBase + ".loan_taken_date", (Object)acc.getLoanTakenDate());
                this.dataConfig.set(accBase + ".active", (Object)acc.isActive());
                this.dataConfig.set(accBase + ".created", (Object)acc.getCreated());
                this.dataConfig.set(accBase + ".history", (Object)acc.getHistory());
            }
        }
        try {
            this.dataConfig.save(this.dataFile);
        }
        catch (final IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }
    
    public void reload() {
        this.loadData();
    }
    
    public PlayerData getPlayerData(final UUID uuid) {
        return this.playerDataMap.get(uuid);
    }
    
    public PlayerData getOrCreatePlayerData(final UUID uuid, final String name) {
        return this.playerDataMap.computeIfAbsent(uuid, k -> new PlayerData(uuid.toString(), name));
    }
    
    public Map<UUID, PlayerData> getAllPlayerData() {
        return Collections.unmodifiableMap((Map<? extends UUID, ? extends PlayerData>)this.playerDataMap);
    }
    
    public String generateAccountNumber() {
        final List<String> formats = this.plugin.getConfigManager().getAccountFormats();
        final String format = formats.isEmpty() ? "BNK0000000" : formats.get(new Random().nextInt(formats.size()));
        final String digits = String.format("%09d", new Random().nextInt(1000000000));
        final String base = format.replaceAll("0+$", "") + digits.substring(0, format.replaceAll("[^0]", "").length());
        return base;
    }
    
    public boolean hasAccount(final UUID uuid) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        return pd != null && !pd.getAccounts().isEmpty();
    }
    
    public BankAccount getFirstAccount(final UUID uuid) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return null;
        }
        return pd.getFirstAccount();
    }
    
    public void createAccount(final UUID uuid, final String name) {
        final PlayerData pd = this.getOrCreatePlayerData(uuid, name);
        final String accNum = this.generateAccountNumber();
        final BankAccount acc = new BankAccount(accNum, this.plugin.getConfigManager().getStartingBalance(), true);
        pd.getAccounts().put(accNum, acc);
        this.saveData();
    }
    
    public void resetAccount(final UUID uuid) {
        this.playerDataMap.remove(uuid);
        this.saveData();
    }
    
    public void resetAll() {
        this.playerDataMap.clear();
        this.saveData();
    }
    
    public double getLoanLimit(final UUID uuid) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return 0.0;
        }
        final int level = pd.getLevel();
        if (level == 0) {
            return 0.0;
        }
        final File levelFile = new File(this.plugin.getDataFolder(), "files/level.yml");
        if (!levelFile.exists()) {
            this.plugin.saveResource("files/level.yml", false);
        }
        final FileConfiguration levelConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(levelFile);
        return levelConfig.getDouble("loan-and-level." + level + ".loan", this.plugin.getConfigManager().getDefaultLoanLimit());
    }
    
    public double getBankLimit(final UUID uuid) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return 10000.0;
        }
        final int bankLevel = this.getBankLevel(uuid);
        final File upgradeFile = new File(this.plugin.getDataFolder(), "files/upgrade.yml");
        if (!upgradeFile.exists()) {
            this.plugin.saveResource("files/upgrade.yml", false);
        }
        final FileConfiguration upgradeConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(upgradeFile);
        return upgradeConfig.getDouble("bank-levels." + bankLevel + ".limit", 10000.0);
    }
    
    public int getBankLevel(final UUID uuid) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return 1;
        }
        final BankAccount acc = pd.getFirstAccount();
        if (acc == null) {
            return 1;
        }
        return this.plugin.getBankUpgradeManager().getBankLevel(uuid);
    }
    
    public boolean canDoAtmOperation(final UUID uuid) {
        final int limit = this.plugin.getConfigManager().getAtmDailyLimit();
        if (limit <= 0) {
            return true;
        }
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return true;
        }
        final String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (!today.equals(pd.getAtmOpsDate())) {
            pd.setAtmOpsDate(today);
            pd.setAtmOpsCount(0);
        }
        return pd.getAtmOpsCount() < limit;
    }
    
    public void incrementAtmOps(final UUID uuid) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return;
        }
        final String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (!today.equals(pd.getAtmOpsDate())) {
            pd.setAtmOpsDate(today);
            pd.setAtmOpsCount(0);
        }
        pd.setAtmOpsCount(pd.getAtmOpsCount() + 1);
        this.saveData();
    }
    
    public double getTotalMoneyInCirculation() {
        double total = 0.0;
        for (final PlayerData pd : this.playerDataMap.values()) {
            for (final BankAccount acc : pd.getAccounts().values()) {
                total += acc.getBalance();
            }
        }
        return total;
    }
    
    public double getTotalLoans() {
        double total = 0.0;
        for (final PlayerData pd : this.playerDataMap.values()) {
            for (final BankAccount acc : pd.getAccounts().values()) {
                total += acc.getLoan();
            }
        }
        return total;
    }
    
    public int getTotalAccounts() {
        int count = 0;
        for (final PlayerData pd : this.playerDataMap.values()) {
            count += pd.getAccounts().size();
        }
        return count;
    }
    
    public List<Map.Entry<String, Double>> getTopPlayers(final int limit) {
        final Map<String, Double> totals = new HashMap<String, Double>();
        for (final PlayerData pd : this.playerDataMap.values()) {
            double total = 0.0;
            for (final BankAccount acc : pd.getAccounts().values()) {
                total += acc.getBalance();
            }
            if (total > 0.0) {
                totals.put(pd.getName(), total);
            }
        }
        final List<Map.Entry<String, Double>> sorted = new ArrayList<Map.Entry<String, Double>>(totals.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }
    
    public void applyOverduePenalties() {
        final int overdueDays = this.plugin.getConfigManager().getLoanOverdueDays();
        final int penaltyPercent = this.plugin.getConfigManager().getLoanOverduePenaltyPercent();
        final long now = System.currentTimeMillis();
        final long overdueMills = overdueDays * 24L * 60L * 60L * 1000L;
        for (final Map.Entry<UUID, PlayerData> entry : this.playerDataMap.entrySet()) {
            final UUID uuid = entry.getKey();
            final PlayerData pd = entry.getValue();
            for (final BankAccount acc : pd.getAccounts().values()) {
                if (acc.getLoan() > 0.0 && acc.getLoanTakenDate() > 0L) {
                    final long elapsed = now - acc.getLoanTakenDate();
                    if (elapsed < overdueMills) {
                        continue;
                    }
                    final double penalty = acc.getLoan() * penaltyPercent / 100.0;
                    final double newLoan = acc.getLoan() + penalty;
                    acc.setLoan(newLoan);
                    acc.setLoanTakenDate(now);
                    final Player online = this.plugin.getServer().getPlayer(uuid);
                    final String penaltyStr = this.plugin.getConfigManager().formatAmount(penalty);
                    final String loanStr = this.plugin.getConfigManager().formatAmount(newLoan);
                    final String msg = this.plugin.getLangManager().get("loan_overdue_penalty", "%penalty%", penaltyStr, "%loan%", loanStr);
                    if (online != null && online.isOnline()) {
                        online.sendMessage(msg);
                    }
                    else {
                        pd.addPendingNotification(msg);
                    }
                }
            }
        }
        this.saveData();
    }
    
    public void addHistory(final UUID uuid, final String entry) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return;
        }
        final BankAccount acc = pd.getFirstAccount();
        if (acc == null) {
            return;
        }
        acc.addHistory(entry);
        this.saveData();
    }
    
    public void addOfflineNotification(final UUID uuid, final String msg) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return;
        }
        pd.addPendingNotification(msg);
        this.saveData();
    }
    
    public boolean isPinLocked(final UUID uuid) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        return pd != null && pd.isPinLocked();
    }
    
    public int incrementPinAttempts(final UUID uuid) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return 0;
        }
        final int attempts = pd.getPinAttempts() + 1;
        pd.setPinAttempts(attempts);
        final int max = this.plugin.getConfigManager().getPinMaxAttempts();
        if (attempts >= max) {
            pd.setPinLocked(true);
        }
        this.saveData();
        return attempts;
    }
    
    public int getRemainingPinAttempts(final UUID uuid) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return this.plugin.getConfigManager().getPinMaxAttempts();
        }
        return Math.max(0, this.plugin.getConfigManager().getPinMaxAttempts() - pd.getPinAttempts());
    }
    
    public void resetPinAttempts(final UUID uuid) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return;
        }
        pd.setPinAttempts(0);
        pd.setPinLocked(false);
        this.saveData();
    }
    
    public void unlockPin(final UUID uuid) {
        final PlayerData pd = this.playerDataMap.get(uuid);
        if (pd == null) {
            return;
        }
        pd.setPinLocked(false);
        pd.setPinAttempts(0);
        this.saveData();
    }
    
    public UUID findUUIDByName(final String name) {
        for (final Map.Entry<UUID, PlayerData> entry : this.playerDataMap.entrySet()) {
            if (entry.getValue().getName().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    public PlayerData findPlayerDataByName(final String name) {
        for (final PlayerData pd : this.playerDataMap.values()) {
            if (pd.getName().equalsIgnoreCase(name)) {
                return pd;
            }
        }
        return null;
    }
}
