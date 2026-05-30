package com.chkaduuu.mcbank.listeners;

import org.bukkit.inventory.ItemFlag;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import com.chkaduuu.mcbank.models.BankAccount;
import com.chkaduuu.mcbank.models.PlayerData;
import java.util.UUID;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.NamespacedKey;
import com.chkaduuu.mcbank.McBank;
import org.bukkit.event.Listener;

public class BanknoteListener implements Listener
{
    private final McBank plugin;
    public static NamespacedKey BANKNOTE_KEY;

    public BanknoteListener(final McBank plugin) {
        this.plugin = plugin;
        BanknoteListener.BANKNOTE_KEY = new NamespacedKey((Plugin)plugin, "banknote_amount");
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        final Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Player player = event.getPlayer();
        final ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.PAPER) {
            return;
        }
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(BanknoteListener.BANKNOTE_KEY, PersistentDataType.DOUBLE)) {
            return;
        }
        event.setCancelled(true);
        // FIX: explicit cast to Double instead of Object to satisfy generic bounds
        final double amount = pdc.get(BanknoteListener.BANKNOTE_KEY, PersistentDataType.DOUBLE);
        final UUID uuid = player.getUniqueId();
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        if (acc == null) {
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        final double limit = this.plugin.getBankUpgradeManager().getBankLimit(uuid);
        if (acc.getBalance() >= limit) {
            player.sendMessage(this.plugin.getLangManager().get("deposit_limit_reached", "%limit%", this.plugin.getConfigManager().formatAmount(limit)));
            return;
        }
        final double actual = Math.min(amount, limit - acc.getBalance());
        acc.setBalance(acc.getBalance() + actual);
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        }
        else {
            player.getInventory().setItemInMainHand(null);
        }
        this.plugin.getAccountManager().saveData();
        final String amountStr = this.plugin.getConfigManager().formatAmount(actual);
        player.sendMessage(this.plugin.getLangManager().get("cashout_deposit_success", "%amount%", amountStr));
        this.plugin.getAccountManager().addHistory(uuid, this.plugin.getLangManager().get("cashout_deposit_success", "%amount%", amountStr).replaceAll("§.", ""));
    }

    public ItemStack createBanknote(final double amount, final String playerName) {
        final ItemStack paper = new ItemStack(Material.PAPER);
        final ItemMeta meta = paper.getItemMeta();
        if (meta == null) {
            return paper;
        }
        final String amountStr = this.plugin.getConfigManager().formatAmount(amount);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&lBanknote &e" + amountStr));
        final List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Amount: &a" + amountStr));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Owner: &e" + playerName));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eRight-Click to deposit back to bank"));
        meta.setLore(lore);
        // FIX: explicit Double type instead of (Object) cast
        meta.getPersistentDataContainer().set(BanknoteListener.BANKNOTE_KEY, PersistentDataType.DOUBLE, amount);
        meta.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES });
        paper.setItemMeta(meta);
        return paper;
    }
}