# MBank Plugin - Fixed Source

## Bugs Fixed

### Fix #1: GUI buttons not working
**File:** `GUIListener.java`

**Root cause:** The event handler used `event.getClickedInventory().getHolder()` to get the menu holder.
When a player clicks anywhere inside an open GUI — including their own inventory slots at the bottom —
`getClickedInventory()` returns the player's inventory, not the bank menu. So `holder` was never a
`BankMenuHolder` and the click was ignored.

**Fix:** Now uses `event.getView().getTopInventory().getHolder()` to always get the holder of our GUI,
then separately checks that the actual click happened in the top inventory (not the player's slots).

---

### Fix #2: `/bank` shows no available commands
**File:** `McBankCommand.java`

**Root cause:** When `/bank` was typed with no arguments AND the player had an account, the code
immediately opened the GUI instead of showing help. Players with no account saw help; players with
an account didn't — confusing and inconsistent.

**Fix:** `/bank` (no args) always shows help now. A new `/bank open` subcommand opens the GUI menu.

---

### Fix #3: Loan (кредит) not working
**File:** `GUIListener.java`

**Root cause:** Two issues:
1. The level check used `level == 0` (old decompiled code kept `== 0` instead of `<= 0`).
   If somehow level was negative this would slip through. Minor, but fixed.
2. The loan enable check (`isLoanEnabled()`) was done AFTER the level check, so a disabled
   loan system would still show the level error instead of the correct "loans disabled" message.

**Fix:** Reordered checks: enabled check first, then level check. Fixed level guard to `<= 0`.

---

## Project Structure

```
src/
  main/
    java/com/chkaduuu/mcbank/
      McBank.java                    - Main plugin class
      atm/ATMManager.java            - ATM block management
      commands/McBankCommand.java    - /bank command handler
      gui/BankMenuHolder.java        - GUI inventory holder
      hooks/                         - Vault, Essentials, CMI, PlaceholderAPI hooks
      listeners/
        ATMListener.java             - ATM click events
        BanknoteListener.java        - Banknote item handling
        ChatInputListener.java       - Chat-based number input
        GUIListener.java             - GUI click events (FIXED)
        PlayerJoinListener.java      - Join notifications
      managers/
        AccountManager.java          - Player account data
        BankUpgradeManager.java      - Bank level upgrades
        ConfigManager.java           - config.yml wrapper
        GUIManager.java              - GUI builder
        LanguageManager.java         - Language files
      models/
        BankAccount.java             - Account data model
        PlayerData.java              - Player data model
    resources/
      plugin.yml
      files/                         - config.yml, data.yml, gui_main.yml, etc.
      languages/                     - en.yml, ru.yml
```

## Building

Requires Java 17 and Maven.

```bash
mvn clean package
```

Output JAR will be in `target/MBank-3.1.jar`.
