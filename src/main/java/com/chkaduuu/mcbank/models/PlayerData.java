
package com.chkaduuu.mcbank.models;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class PlayerData
{
    private final String uuid;
    private String name;
    private int level;
    private String pin;
    private boolean pinLocked;
    private int pinAttempts;
    private String atmOpsDate;
    private int atmOpsCount;
    private List<String> pendingNotifications;
    private Map<String, BankAccount> accounts;
    
    public PlayerData(final String uuid, final String name) {
        this.uuid = uuid;
        this.name = name;
        this.level = 0;
        this.pin = null;
        this.pinLocked = false;
        this.pinAttempts = 0;
        this.atmOpsDate = "";
        this.atmOpsCount = 0;
        this.pendingNotifications = new ArrayList<String>();
        this.accounts = new HashMap<String, BankAccount>();
    }
    
    public String getUuid() {
        return this.uuid;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    public int getLevel() {
        return this.level;
    }
    
    public void setLevel(final int level) {
        this.level = level;
    }
    
    public String getPin() {
        return this.pin;
    }
    
    public void setPin(final String pin) {
        this.pin = pin;
    }
    
    public boolean isPinLocked() {
        return this.pinLocked;
    }
    
    public void setPinLocked(final boolean pinLocked) {
        this.pinLocked = pinLocked;
    }
    
    public int getPinAttempts() {
        return this.pinAttempts;
    }
    
    public void setPinAttempts(final int pinAttempts) {
        this.pinAttempts = pinAttempts;
    }
    
    public String getAtmOpsDate() {
        return this.atmOpsDate;
    }
    
    public void setAtmOpsDate(final String atmOpsDate) {
        this.atmOpsDate = atmOpsDate;
    }
    
    public int getAtmOpsCount() {
        return this.atmOpsCount;
    }
    
    public void setAtmOpsCount(final int atmOpsCount) {
        this.atmOpsCount = atmOpsCount;
    }
    
    public List<String> getPendingNotifications() {
        return this.pendingNotifications;
    }
    
    public void setPendingNotifications(final List<String> pendingNotifications) {
        this.pendingNotifications = ((pendingNotifications != null) ? pendingNotifications : new ArrayList<String>());
    }
    
    public void addPendingNotification(final String msg) {
        this.pendingNotifications.add(msg);
    }
    
    public void clearPendingNotifications() {
        this.pendingNotifications.clear();
    }
    
    public Map<String, BankAccount> getAccounts() {
        return this.accounts;
    }
    
    public BankAccount getFirstAccount() {
        if (this.accounts.isEmpty()) {
            return null;
        }
        return this.accounts.values().iterator().next();
    }
}
