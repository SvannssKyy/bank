
package com.chkaduuu.mcbank.managers;

import org.bukkit.ChatColor;
import com.chkaduuu.mcbank.models.BankAccount;
import com.chkaduuu.mcbank.models.PlayerData;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.util.UUID;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Material;
import java.util.ArrayList;
import java.util.Iterator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import java.util.List;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.Bukkit;
import com.chkaduuu.mcbank.gui.BankMenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import com.chkaduuu.mcbank.McBank;

public class GUIManager
{
    private final McBank plugin;
    private FileConfiguration mainGuiConfig;
    private FileConfiguration atmGuiConfig;
    
    public GUIManager(final McBank plugin) {
        this.plugin = plugin;
        this.loadConfigs();
    }
    
    private void loadConfigs() {
        final File mainFile = new File(this.plugin.getDataFolder(), "files/gui_main.yml");
        if (!mainFile.exists()) {
            this.plugin.saveResource("files/gui_main.yml", false);
        }
        this.mainGuiConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(mainFile);
        final File atmFile = new File(this.plugin.getDataFolder(), "files/gui_atm.yml");
        if (!atmFile.exists()) {
            this.plugin.saveResource("files/gui_atm.yml", false);
        }
        this.atmGuiConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(atmFile);
    }
    
    public void reload() {
        this.loadConfigs();
    }
    
    public String getMainMenuTitle() {
        return this.colorize(this.mainGuiConfig.getString("main_menu.title", "&0Bank"));
    }
    
    public String getAtmMenuTitle() {
        return this.colorize(this.atmGuiConfig.getString("atm_menu.title", "&0Bank ATM"));
    }
    
    public void openMainMenu(final Player player) {
        final int rows = this.mainGuiConfig.getInt("main_menu.rows", 6);
        String title = this.colorize(this.mainGuiConfig.getString("main_menu.title", "&0Bank"));
        title = this.applyPlaceholders(title, player);
        final BankMenuHolder holder = new BankMenuHolder(BankMenuHolder.MenuType.MAIN);
        final Inventory inv = Bukkit.createInventory((InventoryHolder)holder, rows * 9, title);
        holder.setInventory(inv);
        final ConfigurationSection border = this.mainGuiConfig.getConfigurationSection("main_menu.border");
        if (border != null && border.getBoolean("enabled", true)) {
            final ItemStack borderItem = this.makeItem(border.getString("item", "BLACK_STAINED_GLASS_PANE"), border.getString("name", " "), null, false, player);
            final List<?> slots = border.getList("slots");
            if (slots != null) {
                for (final Object slot : slots) {
                    inv.setItem((int)slot, borderItem);
                }
            }
        }
        final ConfigurationSection filler = this.mainGuiConfig.getConfigurationSection("main_menu.filler");
        if (filler != null && filler.getBoolean("enabled", true)) {
            final ItemStack fillerItem = this.makeItem(filler.getString("item", "GRAY_STAINED_GLASS_PANE"), filler.getString("name", " "), null, false, player);
            for (int i = 0; i < rows * 9; ++i) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, fillerItem);
                }
            }
        }
        this.placeButton(inv, this.mainGuiConfig.getConfigurationSection("main_menu.account_info"), player);
        this.placeButton(inv, this.mainGuiConfig.getConfigurationSection("main_menu.deposit"), player);
        this.placeButton(inv, this.mainGuiConfig.getConfigurationSection("main_menu.withdraw"), player);
        this.placeButton(inv, this.mainGuiConfig.getConfigurationSection("main_menu.cashout"), player);
        this.placeButton(inv, this.mainGuiConfig.getConfigurationSection("main_menu.transfer"), player);
        this.placeButton(inv, this.mainGuiConfig.getConfigurationSection("main_menu.loan"), player);
        this.placeButton(inv, this.mainGuiConfig.getConfigurationSection("main_menu.upgrade"), player);
        player.openInventory(inv);
    }
    
    public void openATMMenu(final Player player) {
        final int rows = this.atmGuiConfig.getInt("atm_menu.rows", 3);
        final String title = this.colorize(this.atmGuiConfig.getString("atm_menu.title", "&0Bank ATM"));
        final BankMenuHolder holder = new BankMenuHolder(BankMenuHolder.MenuType.ATM);
        final Inventory inv = Bukkit.createInventory((InventoryHolder)holder, rows * 9, title);
        holder.setInventory(inv);
        final ConfigurationSection border = this.atmGuiConfig.getConfigurationSection("atm_menu.border");
        if (border != null && border.getBoolean("enabled", true)) {
            final ItemStack borderItem = this.makeItem(border.getString("item", "BLACK_STAINED_GLASS_PANE"), border.getString("name", " "), null, false, player);
            final List<?> slots = border.getList("slots");
            if (slots != null) {
                for (final Object slot : slots) {
                    inv.setItem((int)slot, borderItem);
                }
            }
        }
        final ConfigurationSection filler = this.atmGuiConfig.getConfigurationSection("atm_menu.filler");
        if (filler != null && filler.getBoolean("enabled", true)) {
            final ItemStack fillerItem = this.makeItem(filler.getString("item", "GRAY_STAINED_GLASS_PANE"), filler.getString("name", " "), null, false, player);
            for (int i = 0; i < rows * 9; ++i) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, fillerItem);
                }
            }
        }
        this.placeButton(inv, this.atmGuiConfig.getConfigurationSection("atm_menu.withdraw"), player);
        player.openInventory(inv);
    }
    
    private void placeButton(final Inventory inv, final ConfigurationSection section, final Player player) {
        if (section == null) {
            return;
        }
        final int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= inv.getSize()) {
            return;
        }
        final String itemStr = section.getString("item", "STONE");
        final String name = this.colorize(this.applyPlaceholders(section.getString("name", ""), player));
        final List<String> rawLore = section.getStringList("lore");
        final List<String> lore = new ArrayList<String>();
        for (final String l : rawLore) {
            lore.add(this.colorize(this.applyPlaceholders(l, player)));
        }
        final boolean glow = section.getBoolean("glow", false);
        inv.setItem(slot, this.makeItem(itemStr, name, lore, glow, player));
    }
    
    private ItemStack makeItem(final String itemStr, final String name, final List<String> lore, final boolean glow, final Player player) {
        ItemStack item;
        ItemMeta meta;
        if (itemStr.startsWith("basehead-")) {
            final String texture = itemStr.substring(9);
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta)item.getItemMeta();
            if (texture.equals("%player_name%")) {
                if (player != null) {
                    skullMeta.setOwningPlayer((OfflinePlayer)player);
                }
            }
            else {
                try {
                    skullMeta = this.setSkullTexture(skullMeta, texture);
                }
                catch (final Exception ex) {}
            }
            meta = (ItemMeta)skullMeta;
        }
        else {
            Material mat;
            try {
                mat = Material.valueOf(itemStr.toUpperCase());
            }
            catch (final IllegalArgumentException e) {
                mat = Material.STONE;
            }
            item = new ItemStack(mat);
            meta = item.getItemMeta();
        }
        if (meta == null) {
            return item;
        }
        if (name != null && !name.isEmpty()) {
            meta.setDisplayName(name);
        }
        if (lore != null && !lore.isEmpty()) {
            meta.setLore((List)lore);
        }
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ENCHANTS });
        }
        meta.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES });
        item.setItemMeta(meta);
        return item;
    }
    
    private SkullMeta setSkullTexture(final SkullMeta meta, final String base64) {
        try {
            final PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "MBankSkull");
            profile.setProperty(new ProfileProperty("textures", base64));
            meta.setPlayerProfile(profile);
        }
        catch (final Exception ex) {}
        return meta;
    }
    
    private String applyPlaceholders(String text, final Player player) {
        if (text == null || player == null) {
            return text;
        }
        final UUID uuid = player.getUniqueId();
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = (pd != null) ? pd.getFirstAccount() : null;
        text = text.replace("%player_name%", player.getName());
        text = text.replace("%mcbank_bank_balance%", (acc != null) ? this.plugin.getConfigManager().formatAmount(acc.getBalance()) : "0");
        text = text.replace("%mcbank_bank_loan%", (acc != null) ? this.plugin.getConfigManager().formatAmount(acc.getLoan()) : "0");
        text = text.replace("%mcbank_bank_number%", (acc != null) ? acc.getAccountNumber() : "N/A");
        text = text.replace("%mcbank_bank_level%", String.valueOf(this.plugin.getBankUpgradeManager().getBankLevel(uuid)));
        text = text.replace("%mcbank_bank_limit%", this.plugin.getConfigManager().formatAmount(this.plugin.getBankUpgradeManager().getBankLimit(uuid)));
        text = text.replace("%mcbank_bank_loan_limit%", this.plugin.getConfigManager().formatAmount(this.plugin.getAccountManager().getLoanLimit(uuid)));
        text = text.replace("%mcbank_loan_percent%", String.valueOf(this.plugin.getConfigManager().getLoanInterestPercent()));
        text = text.replace("%min_deposit%", this.plugin.getConfigManager().formatAmount(this.plugin.getConfigManager().getMinDeposit()));
        text = text.replace("%min_withdraw%", this.plugin.getConfigManager().formatAmount(this.plugin.getConfigManager().getMinWithdraw()));
        text = text.replace("%min_transfer%", this.plugin.getConfigManager().formatAmount(this.plugin.getConfigManager().getMinTransfer()));
        final int next = this.plugin.getBankUpgradeManager().getBankLevel(uuid) + 1;
        final double price = this.plugin.getBankUpgradeManager().getUpgradePrice(next);
        final String priceStr = (price > 0.0) ? this.plugin.getConfigManager().formatAmount(price) : "MAX";
        text = text.replace("%bank_upgrade_price%", priceStr);
        return text;
    }
    
    private String colorize(final String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
