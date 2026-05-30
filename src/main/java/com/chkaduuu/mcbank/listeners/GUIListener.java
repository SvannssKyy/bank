// Fixed by Claude - Bug fixes: GUI buttons, /bank help, loan system

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

        // FIX #1: Use top inventory holder instead of clicked inventory holder.
        // The old code used event.getClickedInventory().getHolder() which returns null
        // when the player clicks their own inventory slots inside the GUI screen.
        final Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null) {
            return;
        }
        final InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof BankMenuHolder)) {
            return;
        }
        final BankMenuHolder bankHolder = (BankMenuHolder) holder;

        // Cancel ALL clicks while our GUI is open (including player's own inventory)
        event.setCancelled(true);

        // Only process clicks that happened in the TOP inventory (our GUI), not the player's inventory
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
        final String itemName = ChatColor.stripColor(meta.getDisplayName()).toLowerCase();
        final UUID uuid = player.getUniqueId();
        if (bankHolder.getMenuType() == BankMenuHolder.MenuType.MAIN) {
            this.handleMainMenuClick(player, uuid, itemName, event.isRightClick());
        }
        else if (bankHolder.getMenuType() == BankMenuHolder.MenuType.ATM) {
            this.handleATMMenuClick(player, uuid, itemName);
        }
    }
    
    private void handleMainMenuClick(final Player player, final UUID uuid, final String itemName, final boolean rightClick) {
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.closeInventory();
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        if (itemName.contains("account inf") || itemName.contains("\u0430\u043a\u043a\u0430\u0443\u043d\u0442")) {
            player.closeInventory();
            this.plugin.getGuiManager().openMainMenu(player);
        }
        else if (itemName.contains("deposit") || itemName.contains("\u0434\u0435\u043f\u043e\u0437\u0438\u0442") || itemName.contains("\u043f\u043e\u043f\u043e\u043b\u043d")) {
            player.closeInventory();
            this.startDeposit(player, uuid);
        }
        else if (itemName.contains("withdraw") || itemName.contains("\u0441\u043d\u044f\u0442")) {
            player.closeInventory();
            this.startWithdraw(player, uuid);
        }
        else if (itemName.contains("cashout") || itemName.contains("\u043e\u0431\u043d\u0430\u043b\u0438\u0447")) {
            player.closeInventory();
            this.startCashout(player, uuid);
        }
        else if (itemName.contains("transfer") || itemName.contains("\u043f\u0435\u0440\u0435\u0432\u043e\u0434")) {
            player.closeInventory();
            this.startTransfer(player, uuid);
        }
        else if (itemName.contains("loan") || itemName.contains("\u043a\u0440\u0435\u0434\u0438\u0442")) {
            player.closeInventory();
            if (rightClick) {
                this.startLoanRepay(player, uuid);
            }
            else {
                this.startLoanTake(player, uuid);
            }
        }
        else if (itemName.contains("upgrade") || itemName.contains("\u0443\u043b\u0443\u0447\u0448")) {
            player.closeInventory();
            this.handleUpgrade(player, uuid);
        }
    }
    
    private void handleATMMenuClick(final Player player, final UUID uuid, final String itemName) {
        if (itemName.contains("withdraw") || itemName.contains("\u0441\u043d\u044f\u0442")) {
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
        try {
            amount = Double.parseDouble(input);
        }
        catch (final NumberFormatException e) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_number"));
            return;
        }
        if (amount <= 0.0) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_amount"));
            return;
        }
        final double min = this.plugin.getConfigManager().getMinDeposit();
        if (amount < min) {
            player.sendMessage(this.plugin.getLangManager().get("deposit_min_amount", "%min%", this.plugin.getConfigManager().formatAmount(min)));
            return;
        }
        this.plugin.getVaultHook();
        final VaultHook vault = this.plugin.getVaultHook();
        if (vault != null && vault.isEnabled() && !vault.has(player.getName(), amount)) {
            player.sendMessage(this.plugin.getLangManager().get("vault_not_enough_money", "%balance%", this.plugin.getConfigManager().formatAmount(vault.getBalance(player.getName()))));
            return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        final double limit = this.plugin.getBankUpgradeManager().getBankLimit(uuid);
        if (acc.getBalance() >= limit) {
            player.sendMessage(this.plugin.getLangManager().get("deposit_limit_reached", "%limit%", this.plugin.getConfigManager().formatAmount(limit)));
            return;
        }
        final double actual = Math.min(amount, limit - acc.getBalance());
        if (vault != null && vault.isEnabled()) {
            vault.withdraw(player.getName(), actual);
        }
        acc.setBalance(acc.getBalance() + actual);
        this.plugin.getAccountManager().saveData();
        final String actualStr = this.plugin.getConfigManager().formatAmount(actual);
        if (actual < amount) {
            player.sendMessage(this.plugin.getLangManager().get("deposit_capped", "%amount%", actualStr, "%limit%", this.plugin.getConfigManager().formatAmount(limit)));
        }
        else {
            player.sendMessage(this.plugin.getLangManager().get("deposit_success", "%amount%", actualStr));
        }
        this.plugin.getAccountManager().addHistory(uuid, "Deposit: +" + actualStr);
    }
    
    private void startWithdraw(final Player player, final UUID uuid) {
        player.sendMessage(this.plugin.getLangManager().get("withdraw_prompt"));
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.WITHDRAW_AMOUNT, input -> this.processWithdraw(player, uuid, input), null);
    }
    
    private void processWithdraw(final Player player, final UUID uuid, final String input) {
        double amount;
        try {
            amount = Double.parseDouble(input);
        }
        catch (final NumberFormatException e) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_number"));
            return;
        }
        if (amount <= 0.0) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_amount"));
            return;
        }
        final double min = this.plugin.getConfigManager().getMinWithdraw();
        if (amount < min) {
            player.sendMessage(this.plugin.getLangManager().get("withdraw_min_amount", "%min%", this.plugin.getConfigManager().formatAmount(min)));
            return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        if (acc.getBalance() < amount) {
            player.sendMessage(this.plugin.getLangManager().get("withdraw_insufficient", "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance())));
            return;
        }
        acc.setBalance(acc.getBalance() - amount);
        final VaultHook vault = this.plugin.getVaultHook();
        if (vault != null && vault.isEnabled()) {
            vault.deposit(player.getName(), amount);
        }
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
        try {
            amount = Double.parseDouble(input);
        }
        catch (final NumberFormatException e) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_number"));
            return;
        }
        if (amount <= 0.0) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_amount"));
            return;
        }
        final double min = this.plugin.getConfigManager().getMinCashout();
        if (amount < min) {
            player.sendMessage(this.plugin.getLangManager().get("cashout_min_amount", "%min%", this.plugin.getConfigManager().formatAmount(min)));
            return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        if (acc.getBalance() < amount) {
            player.sendMessage(this.plugin.getLangManager().get("cashout_insufficient", "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance())));
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(this.plugin.getLangManager().get("cashout_inventory_full"));
            return;
        }
        acc.setBalance(acc.getBalance() - amount);
        this.plugin.getAccountManager().saveData();
        final ItemStack banknote = this.plugin.getBanknoteListener().createBanknote(amount, player.getName());
        player.getInventory().addItem(new ItemStack[] { banknote });
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
            if (target != null) {
                targetUUID = target.getUniqueId();
                resolvedName = target.getName();
            }
            else {
                targetUUID = this.plugin.getAccountManager().findUUIDByName(targetName);
                resolvedName = targetName;
            }
            if (targetUUID == null) {
                player.sendMessage(this.plugin.getLangManager().get("transfer_player_not_found", "%player%", targetName));
            }
            else if (targetUUID.equals(uuid)) {
                player.sendMessage(this.plugin.getLangManager().get("transfer_self"));
            }
            else if (!this.plugin.getAccountManager().hasAccount(targetUUID)) {
                player.sendMessage(this.plugin.getLangManager().get("transfer_no_account", "%player%", resolvedName));
            }
            else {
                final String finalResolvedName = resolvedName;
                player.sendMessage(this.plugin.getLangManager().get("transfer_prompt_amount"));
                this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.TRANSFER_AMOUNT, amountStr -> this.processTransfer(player, uuid, targetUUID, finalResolvedName, amountStr), null);
            }
        }, null);
    }
    
    private void processTransfer(final Player player, final UUID uuid, final UUID targetUUID, final String targetName, final String input) {
        double amount;
        try {
            amount = Double.parseDouble(input);
        }
        catch (final NumberFormatException e) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_number"));
            return;
        }
        if (amount <= 0.0) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_amount"));
            return;
        }
        final double min = this.plugin.getConfigManager().getMinTransfer();
        if (amount < min) {
            player.sendMessage(this.plugin.getLangManager().get("transfer_min_amount", "%min%", this.plugin.getConfigManager().formatAmount(min)));
            return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        if (acc.getBalance() < amount) {
            player.sendMessage(this.plugin.getLangManager().get("transfer_insufficient", "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance())));
            return;
        }
        final PlayerData targetPd = this.plugin.getAccountManager().getPlayerData(targetUUID);
        final BankAccount targetAcc = targetPd.getFirstAccount();
        acc.setBalance(acc.getBalance() - amount);
        targetAcc.setBalance(targetAcc.getBalance() + amount);
        this.plugin.getAccountManager().saveData();
        final String amountStr = this.plugin.getConfigManager().formatAmount(amount);
        player.sendMessage(this.plugin.getLangManager().get("transfer_success_sender", "%amount%", amountStr, "%player%", targetName));
        final String receiverMsg = this.plugin.getLangManager().get("transfer_success_receiver", "%amount%", amountStr, "%player%", player.getName());
        final Player targetOnline = this.plugin.getServer().getPlayer(targetUUID);
        if (targetOnline != null && targetOnline.isOnline()) {
            targetOnline.sendMessage(receiverMsg);
        }
        else {
            this.plugin.getAccountManager().addOfflineNotification(targetUUID, receiverMsg);
        }
        this.plugin.getAccountManager().addHistory(uuid, "Transfer to " + targetName + ": -" + amountStr);
        this.plugin.getAccountManager().addHistory(targetUUID, "Transfer from " + player.getName() + ": +" + amountStr);
    }
    
    private void startLoanTake(final Player player, final UUID uuid) {
        // FIX #3: Loan system - check if loans are enabled FIRST
        if (!this.plugin.getConfigManager().isLoanEnabled()) {
            player.sendMessage(this.plugin.getLangManager().get("loan_not_enabled"));
            return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        // FIX #3: Use pd.getLevel() correctly - level must be > 0 for loans
        if (pd.getLevel() <= 0) {
            player.sendMessage(this.plugin.getLangManager().get("loan_no_level"));
            return;
        }
        final BankAccount acc = pd.getFirstAccount();
        if (acc.getLoan() > 0.0) {
            player.sendMessage(this.plugin.getLangManager().get("loan_already_has", "%loan%", this.plugin.getConfigManager().formatAmount(acc.getLoan())));
            return;
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
        try {
            amount = Double.parseDouble(input);
        }
        catch (final NumberFormatException e) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_number"));
            return;
        }
        if (amount <= 0.0) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_amount"));
            return;
        }
        final double limit = this.plugin.getAccountManager().getLoanLimit(uuid);
        if (amount > limit) {
            player.sendMessage(this.plugin.getLangManager().get("loan_exceeds_limit", "%limit%", this.plugin.getConfigManager().formatAmount(limit)));
            return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        if (acc.getLoan() > 0.0) {
            player.sendMessage(this.plugin.getLangManager().get("loan_already_has", "%loan%", this.plugin.getConfigManager().formatAmount(acc.getLoan())));
            return;
        }
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
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        if (acc.getLoan() <= 0.0) {
            player.sendMessage(this.plugin.getLangManager().get("loan_no_active"));
            return;
        }
        player.sendMessage(this.plugin.getLangManager().get("loan_current", "%loan%", this.plugin.getConfigManager().formatAmount(acc.getLoan())));
        player.sendMessage(this.plugin.getLangManager().get("loan_repay_prompt"));
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.LOAN_REPAY_AMOUNT, input -> this.processLoanRepay(player, uuid, input), null);
    }
    
    private void processLoanRepay(final Player player, final UUID uuid, final String input) {
        double amount;
        try {
            amount = Double.parseDouble(input);
        }
        catch (final NumberFormatException e) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_number"));
            return;
        }
        if (amount <= 0.0) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_amount"));
            return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        if (amount > acc.getLoan()) {
            player.sendMessage(this.plugin.getLangManager().get("loan_repay_excess", "%loan%", this.plugin.getConfigManager().formatAmount(acc.getLoan())));
            return;
        }
        if (acc.getBalance() < amount) {
            player.sendMessage(this.plugin.getLangManager().get("loan_repay_insufficient_balance", "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance())));
            return;
        }
        acc.setBalance(acc.getBalance() - amount);
        acc.setLoan(acc.getLoan() - amount);
        if (acc.getLoan() <= 0.001) {
            acc.setLoan(0.0);
            acc.setLoanTakenDate(0L);
        }
        this.plugin.getAccountManager().saveData();
        final String amountStr = this.plugin.getConfigManager().formatAmount(amount);
        final String remainStr = this.plugin.getConfigManager().formatAmount(acc.getLoan());
        player.sendMessage(this.plugin.getLangManager().get("loan_repay_success", "%amount%", amountStr, "%remaining%", remainStr));
        this.plugin.getAccountManager().addHistory(uuid, "Loan repay: -" + amountStr);
    }
    
    private void handleUpgrade(final Player player, final UUID uuid) {
        final int currentLevel = this.plugin.getBankUpgradeManager().getBankLevel(uuid);
        final int maxLevel = this.plugin.getBankUpgradeManager().getMaxLevel();
        if (currentLevel >= maxLevel) {
            player.sendMessage(this.plugin.getLangManager().get("bank_upgrade_max"));
            return;
        }
        final int nextLevel = currentLevel + 1;
        final double price = this.plugin.getBankUpgradeManager().getUpgradePrice(nextLevel);
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        if (acc.getBalance() < price) {
            player.sendMessage(this.plugin.getLangManager().get("bank_upgrade_insufficient", "%price%", this.plugin.getConfigManager().formatAmount(price), "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance())));
            return;
        }
        acc.setBalance(acc.getBalance() - price);
        this.plugin.getBankUpgradeManager().setBankLevel(uuid, nextLevel);
        this.plugin.getAccountManager().saveData();
        player.sendMessage(this.plugin.getLangManager().get("bank_upgrade_success", "%level%", String.valueOf(nextLevel)));
    }
    
    private void startATMWithdraw(final Player player, final UUID uuid) {
        if (!this.plugin.getAccountManager().canDoAtmOperation(uuid)) {
            final int limit = this.plugin.getConfigManager().getAtmDailyLimit();
            player.sendMessage(this.plugin.getLangManager().get("atm_daily_limit_reached", "%limit%", String.valueOf(limit)));
            return;
        }
        player.sendMessage(this.plugin.getLangManager().get("withdraw_prompt"));
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.WITHDRAW_AMOUNT, input -> this.processATMWithdraw(player, uuid, input), null);
    }
    
    private void processATMWithdraw(final Player player, final UUID uuid, final String input) {
        double amount;
        try {
            amount = Double.parseDouble(input);
        }
        catch (final NumberFormatException e) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_number"));
            return;
        }
        if (amount <= 0.0) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_amount"));
            return;
        }
        final double min = this.plugin.getConfigManager().getMinWithdraw();
        if (amount < min) {
            player.sendMessage(this.plugin.getLangManager().get("withdraw_min_amount", "%min%", this.plugin.getConfigManager().formatAmount(min)));
            return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        if (acc.getBalance() < amount) {
            player.sendMessage(this.plugin.getLangManager().get("withdraw_insufficient", "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance())));
            return;
        }
        acc.setBalance(acc.getBalance() - amount);
        final VaultHook vault = this.plugin.getVaultHook();
        if (vault != null && vault.isEnabled()) {
            vault.deposit(player.getName(), amount);
        }
        this.plugin.getAccountManager().incrementAtmOps(uuid);
        this.plugin.getAccountManager().saveData();
        final String amountStr = this.plugin.getConfigManager().formatAmount(amount);
        player.sendMessage(this.plugin.getLangManager().get("withdraw_success", "%amount%", amountStr));
        this.plugin.getAccountManager().addHistory(uuid, "ATM Withdraw: -" + amountStr);
    }
}
