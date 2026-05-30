
package com.chkaduuu.mcbank.commands;

import java.util.Iterator;
import org.bukkit.block.Block;
import java.util.Map;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import com.chkaduuu.mcbank.hooks.VaultHook;
import com.chkaduuu.mcbank.listeners.ChatInputListener;
import com.chkaduuu.mcbank.models.BankAccount;
import com.chkaduuu.mcbank.models.PlayerData;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import com.chkaduuu.mcbank.McBank;
import org.bukkit.command.CommandExecutor;

public class McBankCommand implements CommandExecutor
{
    private final McBank plugin;
    
    public McBankCommand(final McBank plugin) {
        this.plugin = plugin;
    }
    
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length != 0) {
            final String lowerCase;
            final String sub = lowerCase = args[0].toLowerCase();
            switch (lowerCase) {
                case "create": {
                    this.handleCreate(sender, args);
                    break;
                }
                case "deposit": {
                    this.handleDeposit(sender, args);
                    break;
                }
                case "withdraw": {
                    this.handleWithdraw(sender, args);
                    break;
                }
                case "cashout": {
                    this.handleCashout(sender, args);
                    break;
                }
                case "transfer": {
                    this.handleTransfer(sender, args);
                    break;
                }
                case "info": {
                    this.handleInfo(sender, args);
                    break;
                }
                case "loan": {
                    this.handleLoan(sender, args);
                    break;
                }
                case "history": {
                    this.handleHistory(sender, args);
                    break;
                }
                case "top": {
                    this.handleTop(sender, args);
                    break;
                }
                case "pin": {
                    this.handlePin(sender, args);
                    break;
                }
                case "help": {
                    this.handleHelp(sender);
                    break;
                }
                case "open": {
                    // FIX #2: Added "open" subcommand so players can open the GUI via /bank open
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(this.plugin.getLangManager().get("player_only"));
                    } else {
                        final Player p = (Player) sender;
                        if (!this.plugin.getAccountManager().hasAccount(p.getUniqueId())) {
                            p.sendMessage(this.plugin.getLangManager().get("no_account"));
                        } else {
                            this.deliverPendingNotifications(p);
                            this.plugin.getGuiManager().openMainMenu(p);
                        }
                    }
                    break;
                }
                case "reload": {
                    this.handleReload(sender);
                    break;
                }
                case "reset": {
                    this.handleReset(sender, args);
                    break;
                }
                case "level": {
                    this.handleLevel(sender, args);
                    break;
                }
                case "atm": {
                    this.handleAtm(sender, args);
                    break;
                }
                case "stats": {
                    this.handleStats(sender, args);
                    break;
                }
                case "unpin": {
                    this.handleUnpin(sender, args);
                    break;
                }
                default: {
                    sender.sendMessage(this.plugin.getLangManager().get("invalid_command"));
                    break;
                }
            }
            return true;
        }
        // FIX #2: Always show help when no args, open GUI only if they have account and we add a "open" subcommand
// FIX #2: /bank with no args now always shows help first.
        // Previously, players with an account never saw help - the GUI opened silently.
        // Now /bank shows help, and /bank open (or just clicking the GUI button) opens the menu.
        this.handleHelp(sender);
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            this.deliverPendingNotifications(player);
        }
        return true;
    }
    
    private void handleCreate(final CommandSender sender, final String[] args) {
        // FIX #2: Always show help when no args, open GUI only if they have account and we add a "open" subcommand
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.getLangManager().get("player_only"));
            return;
        }
        final Player player = (Player)sender;
        final UUID uuid = player.getUniqueId();
        final int maxAccounts = this.plugin.getConfigManager().getMaxAccountsPerPlayer();
        final PlayerData pd = this.plugin.getAccountManager().getOrCreatePlayerData(uuid, player.getName());
        if (pd.getAccounts().size() >= maxAccounts) {
            player.sendMessage(this.plugin.getLangManager().get("account_limit_reached", "%max%", String.valueOf(maxAccounts)));
            return;
        }
        if (this.plugin.getAccountManager().hasAccount(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("account_already_exists"));
            return;
        }
        this.plugin.getAccountManager().createAccount(uuid, player.getName());
        player.sendMessage(this.plugin.getLangManager().get("account_created"));
        final BankAccount acc = this.plugin.getAccountManager().getFirstAccount(uuid);
        if (acc != null) {
            player.sendMessage(this.plugin.getLangManager().get("account_number", "%account_number%", acc.getAccountNumber()));
        }
    }
    
    private void handleDeposit(final CommandSender sender, final String[] args) {
        // FIX #2: Always show help when no args, open GUI only if they have account and we add a "open" subcommand
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.getLangManager().get("player_only"));
            return;
        }
        final Player player = (Player)sender;
        final UUID uuid = player.getUniqueId();
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(this.plugin.getLangManager().get("deposit_prompt"));
            this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.DEPOSIT_AMOUNT, input -> this.doDeposit(player, uuid, input), null);
        }
        else {
            this.doDeposit(player, uuid, args[1]);
        }
    }
    
    private void doDeposit(final Player player, final UUID uuid, final String input) {
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
    
    private void handleWithdraw(final CommandSender sender, final String[] args) {
        // FIX #2: Always show help when no args, open GUI only if they have account and we add a "open" subcommand
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.getLangManager().get("player_only"));
            return;
        }
        final Player player = (Player)sender;
        final UUID uuid = player.getUniqueId();
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(this.plugin.getLangManager().get("withdraw_prompt"));
            this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.WITHDRAW_AMOUNT, input -> this.doWithdraw(player, uuid, input), null);
        }
        else {
            this.doWithdraw(player, uuid, args[1]);
        }
    }
    
    private void doWithdraw(final Player player, final UUID uuid, final String input) {
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
    
    private void handleCashout(final CommandSender sender, final String[] args) {
        // FIX #2: Always show help when no args, open GUI only if they have account and we add a "open" subcommand
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.getLangManager().get("player_only"));
            return;
        }
        final Player player = (Player)sender;
        final UUID uuid = player.getUniqueId();
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(this.plugin.getLangManager().get("cashout_prompt"));
            this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.CASHOUT_AMOUNT, input -> this.doCashout(player, uuid, input), null);
        }
        else {
            this.doCashout(player, uuid, args[1]);
        }
    }
    
    private void doCashout(final Player player, final UUID uuid, final String input) {
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
    
    private void handleTransfer(final CommandSender sender, final String[] args) {
        // FIX #2: Always show help when no args, open GUI only if they have account and we add a "open" subcommand
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.getLangManager().get("player_only"));
            return;
        }
        final Player player = (Player)sender;
        final UUID uuid = player.getUniqueId();
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        if (args.length >= 3) {
            this.resolveAndTransfer(player, uuid, args[1], args[2]);
        }
        else if (args.length == 2) {
            this.resolveTransferTarget(player, uuid, args[1]);
        }
        else {
            player.sendMessage(this.plugin.getLangManager().get("transfer_prompt_player"));
            this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.TRANSFER_PLAYER, targetName -> this.resolveTransferTarget(player, uuid, targetName), null);
        }
    }
    
    private void resolveTransferTarget(final Player player, final UUID uuid, final String targetName) {
        final UUID targetUUID = this.resolveTarget(targetName);
        String resolvedName = targetName;
        if (targetUUID == null) {
            player.sendMessage(this.plugin.getLangManager().get("transfer_player_not_found", "%player%", targetName));
            return;
        }
        final Player tOnline = this.plugin.getServer().getPlayer(targetUUID);
        if (tOnline != null) {
            resolvedName = tOnline.getName();
        }
        if (targetUUID.equals(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("transfer_self"));
            return;
        }
        if (!this.plugin.getAccountManager().hasAccount(targetUUID)) {
            player.sendMessage(this.plugin.getLangManager().get("transfer_no_account", "%player%", resolvedName));
            return;
        }
        final String finalResolvedName = resolvedName;
        final UUID finalTargetUUID = targetUUID;
        player.sendMessage(this.plugin.getLangManager().get("transfer_prompt_amount"));
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.TRANSFER_AMOUNT, amountStr -> this.doTransfer(player, uuid, finalTargetUUID, finalResolvedName, amountStr), null);
    }
    
    private void resolveAndTransfer(final Player player, final UUID uuid, final String targetName, final String amountStr) {
        final UUID targetUUID = this.resolveTarget(targetName);
        String resolvedName = targetName;
        if (targetUUID == null) {
            player.sendMessage(this.plugin.getLangManager().get("transfer_player_not_found", "%player%", targetName));
            return;
        }
        final Player tOnline = this.plugin.getServer().getPlayer(targetUUID);
        if (tOnline != null) {
            resolvedName = tOnline.getName();
        }
        if (targetUUID.equals(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("transfer_self"));
            return;
        }
        if (!this.plugin.getAccountManager().hasAccount(targetUUID)) {
            player.sendMessage(this.plugin.getLangManager().get("transfer_no_account", "%player%", resolvedName));
            return;
        }
        this.doTransfer(player, uuid, targetUUID, resolvedName, amountStr);
    }
    
    private void doTransfer(final Player player, final UUID uuid, final UUID targetUUID, final String targetName, final String input) {
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
    
    private UUID resolveTarget(final String name) {
        final Player online = this.plugin.getServer().getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        return this.plugin.getAccountManager().findUUIDByName(name);
    }
    
    private void handleInfo(final CommandSender sender, final String[] args) {
        // FIX #2: Always show help when no args, open GUI only if they have account and we add a "open" subcommand
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.getLangManager().get("player_only"));
            return;
        }
        final Player player = (Player)sender;
        final UUID uuid = player.getUniqueId();
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        final BankAccount acc = pd.getFirstAccount();
        final double loanLimit = this.plugin.getAccountManager().getLoanLimit(uuid);
        final String status = acc.isActive() ? this.plugin.getLangManager().get("info_status_active") : this.plugin.getLangManager().get("info_status_inactive");
        player.sendMessage(this.plugin.getLangManager().get("info_header"));
        player.sendMessage(this.plugin.getLangManager().get("info_account_number", "%account_number%", acc.getAccountNumber()));
        player.sendMessage(this.plugin.getLangManager().get("info_balance", "%balance%", this.plugin.getConfigManager().formatAmount(acc.getBalance())));
        player.sendMessage(this.plugin.getLangManager().get("info_loan", "%loan%", this.plugin.getConfigManager().formatAmount(acc.getLoan())));
        player.sendMessage(this.plugin.getLangManager().get("info_loan_limit", "%loan_limit%", this.plugin.getConfigManager().formatAmount(loanLimit)));
        player.sendMessage(this.plugin.getLangManager().get("info_loan_percent", "%percent%", String.valueOf(this.plugin.getConfigManager().getLoanInterestPercent())));
        player.sendMessage(this.plugin.getLangManager().get("info_level", "%level%", String.valueOf(pd.getLevel())));
        player.sendMessage(this.plugin.getLangManager().get("info_status", "%status%", status));
        player.sendMessage(this.plugin.getLangManager().get("info_footer"));
    }
    
    private void handleLoan(final CommandSender sender, final String[] args) {
        // FIX #2: Always show help when no args, open GUI only if they have account and we add a "open" subcommand
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.getLangManager().get("player_only"));
            return;
        }
        final Player player = (Player)sender;
        if (!this.plugin.getConfigManager().isLoanEnabled()) {
            player.sendMessage(this.plugin.getLangManager().get("loan_not_enabled"));
            return;
        }
        final UUID uuid = player.getUniqueId();
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        if (pd.getLevel() == 0) {
            player.sendMessage(this.plugin.getLangManager().get("loan_no_level"));
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("repay")) {
            this.startLoanRepay(player, uuid);
        }
        else {
            this.startLoanTake(player, uuid, pd);
        }
    }
    
    private void startLoanTake(final Player player, final UUID uuid, final PlayerData pd) {
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
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.LOAN_AMOUNT, input -> this.doLoanTake(player, uuid, input), null);
    }
    
    private void doLoanTake(final Player player, final UUID uuid, final String input) {
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
        this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.LOAN_REPAY_AMOUNT, input -> this.doLoanRepay(player, uuid, input), null);
    }
    
    private void doLoanRepay(final Player player, final UUID uuid, final String input) {
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
    
    private void handleHistory(final CommandSender sender, final String[] args) {
        // FIX #2: Always show help when no args, open GUI only if they have account and we add a "open" subcommand
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.getLangManager().get("player_only"));
            return;
        }
        final Player player = (Player)sender;
        final UUID uuid = player.getUniqueId();
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        final BankAccount acc = this.plugin.getAccountManager().getFirstAccount(uuid);
        player.sendMessage(this.plugin.getLangManager().get("history_header"));
        if (acc == null || acc.getHistory().isEmpty()) {
            player.sendMessage(this.plugin.getLangManager().get("history_empty"));
        }
        else {
            final List<String> hist = acc.getHistory();
            for (int i = 0; i < hist.size(); ++i) {
                player.sendMessage(this.plugin.getLangManager().get("history_entry", "%num%", String.valueOf(i + 1), "%entry%", hist.get(i)));
            }
        }
        player.sendMessage(this.plugin.getLangManager().get("history_footer"));
    }
    
    private void handleTop(final CommandSender sender, final String[] args) {
        sender.sendMessage(this.plugin.getLangManager().get("top_header"));
        final List<Map.Entry<String, Double>> top = this.plugin.getAccountManager().getTopPlayers(10);
        if (top.isEmpty()) {
            sender.sendMessage(this.plugin.getLangManager().get("top_empty"));
        }
        else {
            for (int i = 0; i < top.size(); ++i) {
                sender.sendMessage(this.plugin.getLangManager().get("top_entry", "%pos%", String.valueOf(i + 1), "%player%", (String)top.get(i).getKey(), "%balance%", this.plugin.getConfigManager().formatAmount((double)top.get(i).getValue())));
            }
        }
        sender.sendMessage(this.plugin.getLangManager().get("top_footer"));
    }
    
    private void handleStats(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("mcbank.admin")) {
            sender.sendMessage(this.plugin.getLangManager().get("no_permission"));
            return;
        }
        final double totalMoney = this.plugin.getAccountManager().getTotalMoneyInCirculation();
        final double totalLoans = this.plugin.getAccountManager().getTotalLoans();
        final int totalAccounts = this.plugin.getAccountManager().getTotalAccounts();
        sender.sendMessage(this.plugin.getLangManager().get("stats_header"));
        sender.sendMessage(this.plugin.getLangManager().get("stats_total_money", "%amount%", this.plugin.getConfigManager().formatAmount(totalMoney)));
        sender.sendMessage(this.plugin.getLangManager().get("stats_total_loans", "%amount%", this.plugin.getConfigManager().formatAmount(totalLoans)));
        sender.sendMessage(this.plugin.getLangManager().get("stats_total_accounts", "%count%", String.valueOf(totalAccounts)));
        sender.sendMessage(this.plugin.getLangManager().get("stats_footer"));
    }
    
    private void handlePin(final CommandSender sender, final String[] args) {
        // FIX #2: Always show help when no args, open GUI only if they have account and we add a "open" subcommand
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.getLangManager().get("player_only"));
            return;
        }
        final Player player = (Player)sender;
        final UUID uuid = player.getUniqueId();
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(this.plugin.getLangManager().get("invalid_command"));
            return;
        }
        final String lowerCase = args[1].toLowerCase();
        switch (lowerCase) {
            case "create": {
                if (args.length < 3) {
                    player.sendMessage("Usage: /bank pin create <4-digit-PIN>");
                    return;
                }
                final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
                if (pd.getPin() != null) {
                    player.sendMessage(this.plugin.getLangManager().get("pin_already_exists"));
                    return;
                }
                final String pin = args[2];
                if (!pin.matches("\\d{4}")) {
                    player.sendMessage(this.plugin.getLangManager().get("pin_invalid_format"));
                    return;
                }
                pd.setPin(pin);
                this.plugin.getAccountManager().saveData();
                player.sendMessage(this.plugin.getLangManager().get("pin_created"));
                break;
            }
            case "edit": {
                if (args.length < 4) {
                    player.sendMessage("Usage: /bank pin edit <old-PIN> <new-PIN>");
                    return;
                }
                final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
                if (pd.getPin() == null) {
                    player.sendMessage(this.plugin.getLangManager().get("pin_no_pin"));
                    return;
                }
                if (!pd.getPin().equals(args[2])) {
                    player.sendMessage(this.plugin.getLangManager().get("pin_wrong_old"));
                    return;
                }
                final String newPin = args[3];
                if (!newPin.matches("\\d{4}")) {
                    player.sendMessage(this.plugin.getLangManager().get("pin_invalid_format"));
                    return;
                }
                pd.setPin(newPin);
                this.plugin.getAccountManager().saveData();
                player.sendMessage(this.plugin.getLangManager().get("pin_changed"));
                break;
            }
            case "info": {
                if (!sender.hasPermission("mcbank.admin")) {
                    sender.sendMessage(this.plugin.getLangManager().get("no_permission"));
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage("Usage: /bank pin info <player>");
                    return;
                }
                final PlayerData target = this.plugin.getAccountManager().findPlayerDataByName(args[2]);
                if (target == null) {
                    player.sendMessage(this.plugin.getLangManager().get("level_player_not_found", "%player%", args[2]));
                    return;
                }
                if (target.getPin() != null) {
                    player.sendMessage(this.plugin.getLangManager().get("pin_info_has", "%player%", target.getName(), "%pin%", target.getPin()));
                }
                else {
                    player.sendMessage(this.plugin.getLangManager().get("pin_info_no", "%player%", target.getName()));
                }
                break;
            }
            default: {
                player.sendMessage(this.plugin.getLangManager().get("invalid_command"));
                break;
            }
        }
    }
    
    private void handleUnpin(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("mcbank.admin")) {
            sender.sendMessage(this.plugin.getLangManager().get("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /bank unpin <player>");
            return;
        }
        final UUID targetUUID = this.plugin.getAccountManager().findUUIDByName(args[1]);
        if (targetUUID == null) {
            sender.sendMessage(this.plugin.getLangManager().get("level_player_not_found", "%player%", args[1]));
            return;
        }
        this.plugin.getAccountManager().unlockPin(targetUUID);
        sender.sendMessage(this.plugin.getLangManager().get("pin_unlocked", "%player%", args[1]));
    }
    
    private void handleReload(final CommandSender sender) {
        if (!sender.hasPermission("mcbank.admin")) {
            sender.sendMessage(this.plugin.getLangManager().get("no_permission"));
            return;
        }
        this.plugin.reloadPlugin();
        sender.sendMessage(this.plugin.getLangManager().get("plugin_reloaded"));
    }
    
    private void handleReset(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("mcbank.admin")) {
            sender.sendMessage(this.plugin.getLangManager().get("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /bank reset <player|all>");
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            this.plugin.getAccountManager().resetAll();
            sender.sendMessage(this.plugin.getLangManager().get("reset_all_success"));
        }
        else {
            final UUID targetUUID = this.resolveTarget(args[1]);
            if (targetUUID == null) {
                sender.sendMessage(this.plugin.getLangManager().get("level_player_not_found", "%player%", args[1]));
                return;
            }
            this.plugin.getAccountManager().resetAccount(targetUUID);
            sender.sendMessage(this.plugin.getLangManager().get("reset_success", "%player%", args[1]));
        }
    }
    
    private void handleLevel(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("mcbank.admin")) {
            sender.sendMessage(this.plugin.getLangManager().get("no_permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /bank level <give|take> <player>");
            return;
        }
        final String action = args[1].toLowerCase();
        UUID targetUUID = this.resolveTarget(args[2]);
        if (targetUUID == null) {
            final PlayerData pd = this.plugin.getAccountManager().findPlayerDataByName(args[2]);
            if (pd == null) {
                sender.sendMessage(this.plugin.getLangManager().get("level_player_not_found", "%player%", args[2]));
                return;
            }
            targetUUID = UUID.fromString(pd.getUuid());
        }
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(targetUUID);
        if (pd == null) {
            sender.sendMessage(this.plugin.getLangManager().get("level_player_not_found", "%player%", args[2]));
            return;
        }
        final int maxLevel = 50;
        if (action.equals("give")) {
            if (pd.getLevel() >= maxLevel) {
                sender.sendMessage(this.plugin.getLangManager().get("level_max", "%player%", pd.getName(), "%level%", String.valueOf(maxLevel)));
                return;
            }
            pd.setLevel(pd.getLevel() + 1);
            this.plugin.getAccountManager().saveData();
            sender.sendMessage(this.plugin.getLangManager().get("level_give", "%player%", pd.getName(), "%level%", String.valueOf(pd.getLevel())));
        }
        else if (action.equals("take")) {
            if (pd.getLevel() <= 0) {
                sender.sendMessage(this.plugin.getLangManager().get("level_min", "%player%", pd.getName()));
                return;
            }
            pd.setLevel(pd.getLevel() - 1);
            this.plugin.getAccountManager().saveData();
            sender.sendMessage(this.plugin.getLangManager().get("level_take", "%player%", pd.getName(), "%level%", String.valueOf(pd.getLevel())));
        }
    }
    
    private void handleAtm(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("mcbank.atm.set")) {
            sender.sendMessage(this.plugin.getLangManager().get("no_permission"));
            return;
        }
        // FIX #2: Always show help when no args, open GUI only if they have account and we add a "open" subcommand
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.getLangManager().get("player_only"));
            return;
        }
        final Player player = (Player)sender;
        if (args.length < 2) {
            sender.sendMessage("Usage: /bank atm <set|remove>");
            return;
        }
        final Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(this.plugin.getLangManager().get("atm_set_look"));
            return;
        }
        if (args[1].equalsIgnoreCase("set")) {
            final boolean success = this.plugin.getATMManager().addATM(target.getLocation(), player.getName());
            if (!success) {
                player.sendMessage(this.plugin.getLangManager().get("atm_set_already"));
                return;
            }
            player.sendMessage(this.plugin.getLangManager().get("atm_set_success"));
        }
        else if (args[1].equalsIgnoreCase("remove")) {
            final boolean success = this.plugin.getATMManager().removeATM(target.getLocation());
            if (!success) {
                player.sendMessage(this.plugin.getLangManager().get("atm_remove_not_found"));
                return;
            }
            player.sendMessage(this.plugin.getLangManager().get("atm_remove_success"));
        }
    }
    
    private void handleHelp(final CommandSender sender) {
        sender.sendMessage(this.plugin.getLangManager().get("help_header"));
        sender.sendMessage(this.plugin.getLangManager().get("help_open"));
        sender.sendMessage(this.plugin.getLangManager().get("help_create"));
        sender.sendMessage(this.plugin.getLangManager().get("help_deposit"));
        sender.sendMessage(this.plugin.getLangManager().get("help_withdraw"));
        sender.sendMessage(this.plugin.getLangManager().get("help_cashout"));
        sender.sendMessage(this.plugin.getLangManager().get("help_info"));
        sender.sendMessage(this.plugin.getLangManager().get("help_loan"));
        sender.sendMessage(this.plugin.getLangManager().get("help_history"));
        sender.sendMessage(this.plugin.getLangManager().get("help_top"));
        sender.sendMessage(this.plugin.getLangManager().get("help_pin_create"));
        sender.sendMessage(this.plugin.getLangManager().get("help_pin_edit"));
        sender.sendMessage(this.plugin.getLangManager().get("help_help"));
        if (sender.hasPermission("mcbank.admin")) {
            sender.sendMessage(this.plugin.getLangManager().get("help_admin_header"));
            sender.sendMessage(this.plugin.getLangManager().get("help_reset"));
            sender.sendMessage(this.plugin.getLangManager().get("help_level_give"));
            sender.sendMessage(this.plugin.getLangManager().get("help_level_take"));
            sender.sendMessage(this.plugin.getLangManager().get("help_atm_set"));
            sender.sendMessage(this.plugin.getLangManager().get("help_atm_remove"));
            sender.sendMessage(this.plugin.getLangManager().get("help_pin_info"));
            sender.sendMessage(this.plugin.getLangManager().get("help_stats"));
            sender.sendMessage(this.plugin.getLangManager().get("help_unpin"));
            sender.sendMessage(this.plugin.getLangManager().get("help_reload"));
        }
        sender.sendMessage(this.plugin.getLangManager().get("help_author"));
        sender.sendMessage(this.plugin.getLangManager().get("help_footer"));
    }
    
    public void deliverPendingNotifications(final Player player) {
        final UUID uuid = player.getUniqueId();
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        if (pd == null || pd.getPendingNotifications().isEmpty()) {
            return;
        }
        for (final String msg : pd.getPendingNotifications()) {
            player.sendMessage(msg);
        }
        pd.clearPendingNotifications();
        this.plugin.getAccountManager().saveData();
    }
}
