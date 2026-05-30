
package com.chkaduuu.mcbank.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BankMenuHolder implements InventoryHolder
{
    private final MenuType menuType;
    private Inventory inventory;
    
    public BankMenuHolder(final MenuType menuType) {
        this.menuType = menuType;
    }
    
    public MenuType getMenuType() {
        return this.menuType;
    }
    
    public Inventory getInventory() {
        return this.inventory;
    }
    
    public void setInventory(final Inventory inventory) {
        this.inventory = inventory;
    }
    
    public enum MenuType
    {
        MAIN, 
        ATM;
    }
}
