package com.chkaduuu.mcbank.listeners;

import com.chkaduuu.mcbank.hooks.VaultHook;
import com.chkaduuu.mcbank.models.BankAccount;
import com.chkaduuu.mcbank.models.PlayerData;
import org.bukkit.event.EventHandler;
import java.util.UUID;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.ChatColor;
import com.chkaduuu.mcbank.gui.BankMenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import com.chkaduuu.mcbank.McBank;
import org.bukkit.event.Listener;

public class GUIListener implements Listener
{
    private final McBank plugin;

    public GUIListener(final McBank plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        final HumanEntity whoClicked = event.getWhoClicked();
        if (!(whoClicked instanceof Player)) {
            return;
        }
        final Player player = (Player) whoClicked;

        final Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null) {
            return;
        }
        final InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof BankMenuHolder)) {
            return;
        }
        final BankMenuHolder bankHolder = (BankMenuHolder) holder;

        // Cancel all clicks while GUI is open
        event.setCancelled(true);

        // Only process clicks in our GUI, not in player's own inventory
        final Inventory clicked = event.getClickedInventory();
        if (clicked == null || !clicked.equals(topInventory)) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }
        final ItemStack clickedItem = event.getCurrentItem();
        final ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        // FIX: strip color AND normalize unicode small-caps characters used in gui_main.yml names
        // The item names use unicode small-caps (e.g. ᴅᴇᴘᴏѕɪᴛ) so we normalize to ASCII for comparison
        final String rawName = ChatColor.stripColor(meta.getDisplayName());
        final String itemName = normalizeSmallCaps(rawName).toLowerCase().trim();

        final UUID uuid = player.getUniqueId();
        if (bankHolder.getMenuType() == BankMenuHolder.MenuType.MAIN) {
            this.handleMainMenuClick(player, uuid, itemName, event.isRightClick());
        } else if (bankHolder.getMenuType() == BankMenuHolder.MenuType.ATM) {
            this.handleATMMenuClick(player, uuid, itemName);
        }
    }

    /**
     * Converts unicode small-caps letters used in item names to plain ASCII.
     * gui_main.yml uses chars like ᴅᴇᴘᴏѕɪᴛ instead of deposit.
     */
    private String normalizeSmallCaps(final String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            sb.append(convertChar(c));
        }
        return sb.toString();
    }

    private char convertChar(char c) {
        switch (c) {
            case 'ᴀ': return 'a'; case 'ʙ': return 'b'; case 'ᴄ': return 'c';
            case 'ᴅ': return 'd'; case 'ᴇ': return 'e'; case 'ꜰ': return 'f';
            case 'ɢ': return 'g'; case 'ʜ': return 'h'; case 'ɪ': return 'i';
            case 'ᴊ': return 'j'; case 'ᴋ': return 'k'; case 'ʟ': return 'l';
            case 'ᴍ': return 'm'; case 'ɴ': return 'n'; case 'ᴏ': return 'o';
            case 'ᴘ': return 'p'; case 'ǫ': return 'q'; case 'ʀ': return 'r';
            case 'ѕ': return 's'; case 'ᴛ': return 't'; case 'ᴜ': return 'u';
            case 'ᴠ': return 'v'; case 'ᴡ': return 'w'; case 'х': return 'x';
            case 'ʏ': return 'y'; case 'ᴢ': return 'z';
            // Cyrillic look-alikes
            case 'с': return 'c'; case 'е': return 'e'; case 'о': return 'o';
            case 'р': return 'r'; case 'а': return 'a';
            default: return c;
        }
    }

    private void handleMainMenuClick(final Player player, final UUID uuid, final String itemName, final boolean rightClick) {
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.closeInventory();
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        if (itemName.contains("account") || itemName.contains("info") || itemName.contains("аккаунт")) {
            player.closeInventory();
            this.plugin.getGuiManager().openMainMenu(player);
        } else if (itemName.contains("deposit") || itemName.contains("депозит") || itemName.contains("пополн")) {
            player.closeInventory();
            this.startDeposit(player, uuid);
        } else if (itemName.contains("withdraw") || itemName.contains("снят")) {
            player.closeInventory();
            this.startWithdraw(player, uuid);
        } else if (itemName.contains("cashout") || itemName.contains("обналич")) {
            player.closeInventory();
            this.startCashout(player, uuid);
        } else if (itemName.contains("transfer") || itemName.contains("перевод")) {
            player.closeInventory();
            this.startTransfer(player, uuid);
        } else if (itemName.contains("loan") || itemName.contains("credit") || itemName.contains("кредит")) {
            player.closeInventory();
            if (rightClick) {
                this.startLoanRepay(player, uuid);
            } else {
                this.startLoanTake(player, uuid);
            }
        } else if (itemName.contains("upgrade") || itemName.contains("улучш")) {
            player.closeInventory();
            this.handleUpgrade(player, uuid);
        }
    }

    private void handleATMMenuClick(final Player player, final UUID uuid, final String itemName) {
        if (itemName.contains("withdraw") || itemName.contains("снят")) {
            player.closeInventory();
            this.startATMWithdraw(player, uuid);
        }
    }

    private void startDeposit(final Player player, final UUID uuid) {
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        final double limit = this.plugin.getBankUpgradeManager().getBankLimit(uuid);
        if (acc.getBalance() >= limit) {
            player.sendMessage(this.plugin.getLangManager().get("deposit_limit_reached", "%limit%", this.plugin.getConfigManager().formatAmount(limit)));
            return;
        }
        player.sendMessage(this.plugin.getLangManager().get("deposit_prompt"));
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.DEPOSIT_AMOUNT, input -> this.processDeposit(player, uuid, input), null);
    }

    private void processDeposit(final Player player, final UUID uuid, final String input) {
        double amount;
        try { amount = Double.parseDouble(input); }
        catch (NumberFormatException e) { player.sendMessage(this.plugin.getLangManager().get("invalid_number")); return; }
        if (amount <= 0.0) { player.sendMessage(this.plugin.getLangManager().get("invalid_amount")); return; }
        final double min = this.plugin.getConfigManager().getMinDeposit();
        if (amount < min) { player.sendMessage(this.plugin.getLangManager().get("deposit_min_amount", "%min%", this.plugin.getConfigManager().formatAmount(min))); return; }
        final VaultHook vault = this.plugin.getVaultHook();
        if (vault != null && vault.isEnabled() && !vault.has(player.getName(), amount)) {
            player.sendMessage(this.plugin.getLangManager().get("vault_not_enough_money", "%balance%", this.plugin.getConfigManager().formatAmount(vault.getBalance(player.getName())))); return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        final double limit = this.plugin.getBankUpgradeManager().getBankLimit(uuid);
        if (acc.getBalance() >= limit) { player.sendMessage(this.plugin.getLangManager().get("deposit_limit_reached", "%limit%", this.plugin.getConfigManager().formatAmount(limit))); return; }
        final double actual = Math.min(amount, limit - acc.getBalance());
        if (vault != null && vault.isEnabled()) vault.withdraw(player.getName(), actual);
        acc.setBalance(acc.getBalance() + actual);
        this.plugin.getAccountManager().saveData();
        final String actualStr = this.plugin.getConfigManager().formatAmount(actual);
        if (actual < amount) player.sendMessage(this.plugin.getLangManager().get("deposit_capped", "%amount%", actualStr, "%limit%", this.plugin.getConfigManager().formatAmount(limit)));
        else player.sendMessage(this.plugin.getLangManager().get("deposit_success", "%amount%", actualStr));
        this.plugin.getAccountManager().addHistory(uuid, "Deposit: +" + actualStr);
    }

    private void startWithdraw(final Player player, final UUID uuid) {
        player.sendMessage(this.plugin.getLangManager().get("withdraw_prompt"));
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.WITHDRAW_AMOUNT, input -> this.processWithdraw(player, uuid, input), null);
    }

    private void processWithdraw(final Player player, final UUID uuid, final String input) {
        double amount;
        try { amount = Double.parseDouble(input); }
        catch (NumberFormatException e) { player.sendMessage(this.plugin.getLangManager().get("invalid_number")); return; }
        if (amount <= 0.0) { player.sendMessage(this.plugin.getLangManager().get("invalid_amount")); return; }
        final double min = this.plugin.getConfigManager().getMinWithdraw();
        if (amount < min) { player.sendMessage(this.plugin.getLangManager().get("withdraw_min_amount", "%min%", this.plugin.getConfigManager().formatAmount(min))); return; }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        if (acc.getBalance() < amount) { player.sendMessage(this.plugin.getLangManager().get("withdraw_insufficient", "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance()))); return; }
        acc.setBalance(acc.getBalance() - amount);
        final VaultHook vault = this.plugin.getVaultHook();
        if (vault != null && vault.isEnabled()) vault.deposit(player.getName(), amount);
        this.plugin.getAccountManager().saveData();
        final String amountStr = this.plugin.getConfigManager().formatAmount(amount);
        player.sendMessage(this.plugin.getLangManager().get("withdraw_success", "%amount%", amountStr));
        this.plugin.getAccountManager().addHistory(uuid, "Withdraw: -" + amountStr);
    }

    private void startCashout(final Player player, final UUID uuid) {
        player.sendMessage(this.plugin.getLangManager().get("cashout_prompt"));
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.CASHOUT_AMOUNT, input -> this.processCashout(player, uuid, input), null);
    }

    private void processCashout(final Player player, final UUID uuid, final String input) {
        double amount;
        try { amount = Double.parseDouble(input); }
        catch (NumberFormatException e) { player.sendMessage(this.plugin.getLangManager().get("invalid_number")); return; }
        if (amount <= 0.0) { player.sendMessage(this.plugin.getLangManager().get("invalid_amount")); return; }
        final double min = this.plugin.getConfigManager().getMinCashout();
        if (amount < min) { player.sendMessage(this.plugin.getLangManager().get("cashout_min_amount", "%min%", this.plugin.getConfigManager().formatAmount(min))); return; }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        if (acc.getBalance() < amount) { player.sendMessage(this.plugin.getLangManager().get("cashout_insufficient", "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance()))); return; }
        if (player.getInventory().firstEmpty() == -1) { player.sendMessage(this.plugin.getLangManager().get("cashout_inventory_full")); return; }
        acc.setBalance(acc.getBalance() - amount);
        this.plugin.getAccountManager().saveData();
        final ItemStack banknote = this.plugin.getBanknoteListener().createBanknote(amount, player.getName());
        player.getInventory().addItem(banknote);
        final String amountStr = this.plugin.getConfigManager().formatAmount(amount);
        player.sendMessage(this.plugin.getLangManager().get("cashout_success", "%amount%", amountStr));
        this.plugin.getAccountManager().addHistory(uuid, "Cashout: -" + amountStr);
    }

    private void startTransfer(final Player player, final UUID uuid) {
        player.sendMessage(this.plugin.getLangManager().get("transfer_prompt_player"));
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.TRANSFER_PLAYER, targetName -> {
            final Player target = this.plugin.getServer().getPlayerExact(targetName);
            UUID targetUUID;
            String resolvedName;
            if (target != null) { targetUUID = target.getUniqueId(); resolvedName = target.getName(); }
            else { targetUUID = this.plugin.getAccountManager().findUUIDByName(targetName); resolvedName = targetName; }
            if (targetUUID == null) { player.sendMessage(this.plugin.getLangManager().get("transfer_player_not_found", "%player%", targetName)); }
            else if (targetUUID.equals(uuid)) { player.sendMessage(this.plugin.getLangManager().get("transfer_self")); }
            else if (!this.plugin.getAccountManager().hasAccount(targetUUID)) { player.sendMessage(this.plugin.getLangManager().get("transfer_no_account", "%player%", resolvedName)); }
            else {
                final String finalName = resolvedName;
                player.sendMessage(this.plugin.getLangManager().get("transfer_prompt_amount"));
                this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.TRANSFER_AMOUNT, amountStr -> this.processTransfer(player, uuid, targetUUID, finalName, amountStr), null);
            }
        }, null);
    }

    private void processTransfer(final Player player, final UUID uuid, final UUID targetUUID, final String targetName, final String input) {
        double amount;
        try { amount = Double.parseDouble(input); }
        catch (NumberFormatException e) { player.sendMessage(this.plugin.getLangManager().get("invalid_number")); return; }
        if (amount <= 0.0) { player.sendMessage(this.plugin.getLangManager().get("invalid_amount")); return; }
        final double min = this.plugin.getConfigManager().getMinTransfer();
        if (amount < min) { player.sendMessage(this.plugin.getLangManager().get("transfer_min_amount", "%min%", this.plugin.getConfigManager().formatAmount(min))); return; }
        final BankAccount acc = this.plugin.getAccountManager().getPlayerData(uuid).getFirstAccount();
        if (acc.getBalance() < amount) { player.sendMessage(this.plugin.getLangManager().get("transfer_insufficient", "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance()))); return; }
        final BankAccount targetAcc = this.plugin.getAccountManager().getPlayerData(targetUUID).getFirstAccount();
        acc.setBalance(acc.getBalance() - amount);
        targetAcc.setBalance(targetAcc.getBalance() + amount);
        this.plugin.getAccountManager().saveData();
        final String amountStr = this.plugin.getConfigManager().formatAmount(amount);
        player.sendMessage(this.plugin.getLangManager().get("transfer_success_sender", "%amount%", amountStr, "%player%", targetName));
        final String receiverMsg = this.plugin.getLangManager().get("transfer_success_receiver", "%amount%", amountStr, "%player%", player.getName());
        final Player targetOnline = this.plugin.getServer().getPlayer(targetUUID);
        if (targetOnline != null && targetOnline.isOnline()) targetOnline.sendMessage(receiverMsg);
        else this.plugin.getAccountManager().addOfflineNotification(targetUUID, receiverMsg);
        this.plugin.getAccountManager().addHistory(uuid, "Transfer to " + targetName + ": -" + amountStr);
        this.plugin.getAccountManager().addHistory(targetUUID, "Transfer from " + player.getName() + ": +" + amountStr);
    }

    private void startLoanTake(final Player player, final UUID uuid) {
        if (!this.plugin.getConfigManager().isLoanEnabled()) {
            player.sendMessage(this.plugin.getLangManager().get("loan_not_enabled")); return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        if (pd.getLevel() <= 0) {
            player.sendMessage(this.plugin.getLangManager().get("loan_no_level")); return;
        }
        final BankAccount acc = pd.getFirstAccount();
        if (acc.getLoan() > 0.0) {
            player.sendMessage(this.plugin.getLangManager().get("loan_already_has", "%loan%", this.plugin.getConfigManager().formatAmount(acc.getLoan()))); return;
        }
        final double loanLimit = this.plugin.getAccountManager().getLoanLimit(uuid);
        player.sendMessage(this.plugin.getLangManager().get("loan_current", "%loan%", this.plugin.getConfigManager().formatAmount(acc.getLoan())));
        player.sendMessage(this.plugin.getLangManager().get("loan_limit", "%limit%", this.plugin.getConfigManager().formatAmount(loanLimit)));
        player.sendMessage(this.plugin.getLangManager().get("loan_percent", "%percent%", String.valueOf(this.plugin.getConfigManager().getLoanInterestPercent())));
        player.sendMessage(this.plugin.getLangManager().get("loan_prompt"));
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.LOAN_AMOUNT, input -> this.processLoanTake(player, uuid, input), null);
    }

    private void processLoanTake(final Player player, final UUID uuid, final String input) {
        double amount;
        try { amount = Double.parseDouble(input); }
        catch (NumberFormatException e) { player.sendMessage(this.plugin.getLangManager().get("invalid_number")); return; }
        if (amount <= 0.0) { player.sendMessage(this.plugin.getLangManager().get("invalid_amount")); return; }
        final double limit = this.plugin.getAccountManager().getLoanLimit(uuid);
        if (amount > limit) { player.sendMessage(this.plugin.getLangManager().get("loan_exceeds_limit", "%limit%", this.plugin.getConfigManager().formatAmount(limit))); return; }
        final BankAccount acc = this.plugin.getAccountManager().getPlayerData(uuid).getFirstAccount();
        if (acc.getLoan() > 0.0) { player.sendMessage(this.plugin.getLangManager().get("loan_already_has", "%loan%", this.plugin.getConfigManager().formatAmount(acc.getLoan()))); return; }
        final double withInterest = amount * (1.0 + this.plugin.getConfigManager().getLoanInterestPercent() / 100.0);
        acc.setLoan(withInterest);
        acc.setBalance(acc.getBalance() + amount);
        acc.setLoanTakenDate(System.currentTimeMillis());
        this.plugin.getAccountManager().saveData();
        final String amountStr = this.plugin.getConfigManager().formatAmount(amount);
        player.sendMessage(this.plugin.getLangManager().get("loan_success", "%amount%", amountStr));
        this.plugin.getAccountManager().addHistory(uuid, "Loan taken: +" + amountStr);
    }

    private void startLoanRepay(final Player player, final UUID uuid) {
        final BankAccount acc = this.plugin.getAccountManager().getPlayerData(uuid).getFirstAccount();
        if (acc.getLoan() <= 0.0) { player.sendMessage(this.plugin.getLangManager().get("loan_no_active")); return; }
        player.sendMessage(this.plugin.getLangManager().get("loan_current", "%loan%", this.plugin.getConfigManager().formatAmount(acc.getLoan())));
        player.sendMessage(this.plugin.getLangManager().get("loan_repay_prompt"));
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.LOAN_REPAY_AMOUNT, input -> this.processLoanRepay(player, uuid, input), null);
    }

    private void processLoanRepay(final Player player, final UUID uuid, final String input) {
        double amount;
        try { amount = Double.parseDouble(input); }
        catch (NumberFormatException e) { player.sendMessage(this.plugin.getLangManager().get("invalid_number")); return; }
        if (amount <= 0.0) { player.sendMessage(this.plugin.getLangManager().get("invalid_amount")); return; }
        final BankAccount acc = this.plugin.getAccountManager().getPlayerData(uuid).getFirstAccount();
        if (amount > acc.getLoan()) { player.sendMessage(this.plugin.getLangManager().get("loan_repay_excess", "%loan%", this.plugin.getConfigManager().formatAmount(acc.getLoan()))); return; }
        if (acc.getBalance() < amount) { player.sendMessage(this.plugin.getLangManager().get("loan_repay_insufficient_balance", "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance()))); return; }
        acc.setBalance(acc.getBalance() - amount);
        acc.setLoan(acc.getLoan() - amount);
        if (acc.getLoan() <= 0.001) { acc.setLoan(0.0); acc.setLoanTakenDate(0L); }
        this.plugin.getAccountManager().saveData();
        final String amountStr = this.plugin.getConfigManager().formatAmount(amount);
        player.sendMessage(this.plugin.getLangManager().get("loan_repay_success", "%amount%", amountStr, "%remaining%", this.plugin.getConfigManager().formatAmount(acc.getLoan())));
        this.plugin.getAccountManager().addHistory(uuid, "Loan repay: -" + amountStr);
    }

    private void handleUpgrade(final Player player, final UUID uuid) {
        final int currentLevel = this.plugin.getBankUpgradeManager().getBankLevel(uuid);
        final int maxLevel = this.plugin.getBankUpgradeManager().getMaxLevel();
        if (currentLevel >= maxLevel) { player.sendMessage(this.plugin.getLangManager().get("bank_upgrade_max")); return; }
        final int nextLevel = currentLevel + 1;
        final double price = this.plugin.getBankUpgradeManager().getUpgradePrice(nextLevel);
        final BankAccount acc = this.plugin.getAccountManager().getPlayerData(uuid).getFirstAccount();
        if (acc.getBalance() < price) { player.sendMessage(this.plugin.getLangManager().get("bank_upgrade_insufficient", "%price%", this.plugin.getConfigManager().formatAmount(price), "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance()))); return; }
        acc.setBalance(acc.getBalance() - price);
        this.plugin.getBankUpgradeManager().setBankLevel(uuid, nextLevel);
        this.plugin.getAccountManager().saveData();
        player.sendMessage(this.plugin.getLangManager().get("bank_upgrade_success", "%level%", String.valueOf(nextLevel)));
    }

    private void startATMWithdraw(final Player player, final UUID uuid) {
        if (!this.plugin.getAccountManager().canDoAtmOperation(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("atm_daily_limit_reached", "%limit%", String.valueOf(this.plugin.getConfigManager().getAtmDailyLimit()))); return;
        }
        player.sendMessage(this.plugin.getLangManager().get("withdraw_prompt"));
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.WITHDRAW_AMOUNT, input -> this.processATMWithdraw(player, uuid, input), null);
    }

    private void processATMWithdraw(final Player player, final UUID uuid, final String input) {
        double amount;
        try { amount = Double.parseDouble(input); }
        catch (NumberFormatException e) { player.sendMessage(this.plugin.getLangManager().get("invalid_number")); return; }
        if (amount <= 0.0) { player.sendMessage(this.plugin.getLangManager().get("invalid_amount")); return; }
        final double min = this.plugin.getConfigManager().getMinWithdraw();
        if (amount < min) { player.sendMessage(this.plugin.getLangManager().get("withdraw_min_amount", "%min%", this.plugin.getConfigManager().formatAmount(min))); return; }
        final BankAccount acc = this.plugin.getAccountManager().getPlayerData(uuid).getFirstAccount();
        if (acc.getBalance() < amount) { player.sendMessage(this.plugin.getLangManager().get("withdraw_insufficient", "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance()))); return; }
        acc.setBalance(acc.getBalance() - amount);
        final VaultHook vault = this.plugin.getVaultHook();
        if (vault != null && vault.isEnabled()) vault.deposit(player.getName(), amount);
        this.plugin.getAccountManager().incrementAtmOps(uuid);
        this.plugin.getAccountManager().saveData();
        final String amountStr = this.plugin.getConfigManager().formatAmount(amount);
        player.sendMessage(this.plugin.getLangManager().get("withdraw_success", "%amount%", amountStr));
        this.plugin.getAccountManager().addHistory(uuid, "ATM Withdraw: -" + amountStr);
    }
}
