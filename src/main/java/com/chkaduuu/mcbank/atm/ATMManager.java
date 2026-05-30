
package com.chkaduuu.mcbank.atm;

import java.io.IOException;
import org.bukkit.Location;
import java.util.Collection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import com.chkaduuu.mcbank.McBank;

public class ATMManager
{
    private final McBank plugin;
    private File atmFile;
    private FileConfiguration atmConfig;
    private final Set<String> atmLocations;
    
    public ATMManager(final McBank plugin) {
        this.atmLocations = new HashSet<String>();
        this.plugin = plugin;
        this.loadATMs();
    }
    
    private void loadATMs() {
        this.atmFile = new File(this.plugin.getDataFolder(), "files/atm.yml");
        if (!this.atmFile.exists()) {
            this.plugin.saveResource("files/atm.yml", false);
        }
        this.atmConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(this.atmFile);
        this.atmLocations.clear();
        if (this.atmConfig.isConfigurationSection("atms")) {
            this.atmLocations.addAll(this.atmConfig.getConfigurationSection("atms").getKeys(false));
        }
    }
    
    public void reload() {
        this.loadATMs();
    }
    
    private String locationKey(final Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
    
    public boolean isATM(final Location loc) {
        return this.atmLocations.contains(this.locationKey(loc));
    }
    
    public boolean addATM(final Location loc, final String placedBy) {
        final String key = this.locationKey(loc);
        if (this.atmLocations.contains(key)) {
            return false;
        }
        this.atmLocations.add(key);
        final String base = "atms." + key;
        this.atmConfig.set(base + ".world", (Object)loc.getWorld().getName());
        this.atmConfig.set(base + ".x", (Object)loc.getBlockX());
        this.atmConfig.set(base + ".y", (Object)loc.getBlockY());
        this.atmConfig.set(base + ".z", (Object)loc.getBlockZ());
        this.atmConfig.set(base + ".placed_by", (Object)placedBy);
        this.atmConfig.set(base + ".placed_at", (Object)System.currentTimeMillis());
        this.save();
        return true;
    }
    
    public boolean removeATM(final Location loc) {
        final String key = this.locationKey(loc);
        if (!this.atmLocations.contains(key)) {
            return false;
        }
        this.atmLocations.remove(key);
        this.atmConfig.set("atms." + key, (Object)null);
        this.save();
        return true;
    }
    
    private void save() {
        try {
            this.atmConfig.save(this.atmFile);
        }
        catch (final IOException e) {
            this.plugin.getLogger().severe("Could not save atm.yml: " + e.getMessage());
        }
    }
    
    public int getATMCount() {
        return this.atmLocations.size();
    }
}
