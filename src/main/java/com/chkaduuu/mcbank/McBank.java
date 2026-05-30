
package com.chkaduuu.mcbank;

import java.io.File;
import com.chkaduuu.mcbank.hooks.PlaceholderAPIHook;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import com.chkaduuu.mcbank.listeners.PlayerJoinListener;
import com.chkaduuu.mcbank.listeners.ATMListener;
import com.chkaduuu.mcbank.listeners.GUIListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;
import com.chkaduuu.mcbank.commands.McBankCommand;
import com.chkaduuu.mcbank.listeners.BanknoteListener;
import com.chkaduuu.mcbank.listeners.ChatInputListener;
import com.chkaduuu.mcbank.hooks.CMIHook;
import com.chkaduuu.mcbank.hooks.EssentialsHook;
import com.chkaduuu.mcbank.hooks.VaultHook;
import com.chkaduuu.mcbank.atm.ATMManager;
import com.chkaduuu.mcbank.managers.GUIManager;
import com.chkaduuu.mcbank.managers.BankUpgradeManager;
import com.chkaduuu.mcbank.managers.AccountManager;
import com.chkaduuu.mcbank.managers.LanguageManager;
import com.chkaduuu.mcbank.managers.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public class McBank extends JavaPlugin
{
    private static McBank instance;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private AccountManager accountManager;
    private BankUpgradeManager bankUpgradeManager;
    private GUIManager guiManager;
    private ATMManager atmManager;
    private VaultHook vaultHook;
    private EssentialsHook essentialsHook;
    private CMIHook cmiHook;
    private ChatInputListener chatInputListener;
    private BanknoteListener banknoteListener;
    private McBankCommand mcBankCommand;
    
    public void onEnable() {
        (McBank.instance = this).saveDefaultConfigs();
        this.configManager = new ConfigManager(this);
        this.languageManager = new LanguageManager(this);
        this.accountManager = new AccountManager(this);
        this.bankUpgradeManager = new BankUpgradeManager(this);
        this.guiManager = new GUIManager(this);
        this.atmManager = new ATMManager(this);
        this.vaultHook = new VaultHook(this);
        this.essentialsHook = new EssentialsHook(this);
        this.cmiHook = new CMIHook(this);
        this.chatInputListener = new ChatInputListener(this);
        this.banknoteListener = new BanknoteListener(this);
        this.mcBankCommand = new McBankCommand(this);
        this.getServer().getPluginManager().registerEvents((Listener)this.chatInputListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.banknoteListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new GUIListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ATMListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new PlayerJoinListener(this), (Plugin)this);
        this.getCommand("mcbank").setExecutor((CommandExecutor)this.mcBankCommand);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && this.configManager.isPlaceholderApiEnabled()) {
            new PlaceholderAPIHook(this).register();
            this.getLogger().info("PlaceholderAPI hooked!");
        }
        this.scheduleBankIncome();
        this.getServer().getScheduler().runTaskTimerAsynchronously((Plugin)this, () -> this.getServer().getScheduler().runTask((Plugin)this, () -> this.accountManager.applyOverduePenalties()), 36000L, 36000L);
        this.getLogger().info("MBank v3.0 enabled!");
    }
    
    public void onDisable() {
        if (this.accountManager != null) {
            this.accountManager.saveData();
        }
        this.getLogger().info("MBank v3.0 disabled. Data saved.");
    }
    
    private void saveDefaultConfigs() {
        this.saveResourceIfNotExists("files/config.yml");
        this.saveResourceIfNotExists("files/data.yml");
        this.saveResourceIfNotExists("files/level.yml");
        this.saveResourceIfNotExists("files/upgrade.yml");
        this.saveResourceIfNotExists("files/atm.yml");
        this.saveResourceIfNotExists("files/gui_main.yml");
        this.saveResourceIfNotExists("files/gui_atm.yml");
        this.saveResourceIfNotExists("languages/en.yml");
        this.saveResourceIfNotExists("languages/ru.yml");
    }
    
    private void saveResourceIfNotExists(final String path) {
        final File file = new File(this.getDataFolder(), path);
        if (!file.exists()) {
            try {
                this.saveResource(path, false);
            }
            catch (final IllegalArgumentException e) {
                this.getLogger().warning("Could not save default resource: " + path);
            }
        }
    }
    
    private void scheduleBankIncome() {
        final String timeStr = this.configManager.getBankIncomeTime();
        long ticks = this.parseTicks(timeStr);
        if (ticks <= 0L) {
            ticks = 6000L;
        }
        final long finalTicks = ticks;
        this.getServer().getScheduler().runTaskTimerAsynchronously((Plugin)this, () -> this.getServer().getScheduler().runTask((Plugin)this, () -> this.bankUpgradeManager.distributeIncome()), finalTicks, finalTicks);
    }
    
    private long parseTicks(final String time) {
        if (time == null || time.isEmpty()) {
            return 6000L;
        }
        try {
            if (time.endsWith("s")) {
                return Long.parseLong(time.replace("s", "")) * 20L;
            }
            if (time.endsWith("m")) {
                return Long.parseLong(time.replace("m", "")) * 20L * 60L;
            }
            if (time.endsWith("h")) {
                return Long.parseLong(time.replace("h", "")) * 20L * 3600L;
            }
            return Long.parseLong(time) * 20L;
        }
        catch (final NumberFormatException e) {
            return 6000L;
        }
    }
    
    public void reloadPlugin() {
        this.configManager.reload();
        this.languageManager.reload();
        this.accountManager.reload();
        this.bankUpgradeManager.reload();
        this.guiManager.reload();
        this.atmManager.reload();
    }
    
    public static McBank getInstance() {
        return McBank.instance;
    }
    
    public ConfigManager getConfigManager() {
        return this.configManager;
    }
    
    public LanguageManager getLangManager() {
        return this.languageManager;
    }
    
    public AccountManager getAccountManager() {
        return this.accountManager;
    }
    
    public BankUpgradeManager getBankUpgradeManager() {
        return this.bankUpgradeManager;
    }
    
    public GUIManager getGuiManager() {
        return this.guiManager;
    }
    
    public ATMManager getATMManager() {
        return this.atmManager;
    }
    
    public VaultHook getVaultHook() {
        return this.vaultHook;
    }
    
    public EssentialsHook getEssentialsHook() {
        return this.essentialsHook;
    }
    
    public CMIHook getCmiHook() {
        return this.cmiHook;
    }
    
    public ChatInputListener getChatInputListener() {
        return this.chatInputListener;
    }
    
    public BanknoteListener getBanknoteListener() {
        return this.banknoteListener;
    }
    
    public McBankCommand getMcBankCommand() {
        return this.mcBankCommand;
    }
}
