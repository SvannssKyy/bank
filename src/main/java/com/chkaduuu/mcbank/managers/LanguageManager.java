
package com.chkaduuu.mcbank.managers;

import org.bukkit.ChatColor;
import java.io.InputStream;
import org.bukkit.configuration.Configuration;
import java.io.Reader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import com.chkaduuu.mcbank.McBank;

public class LanguageManager
{
    private final McBank plugin;
    private YamlConfiguration lang;
    private String currentLang;
    
    public LanguageManager(final McBank plugin) {
        this.plugin = plugin;
        this.loadLanguage();
    }
    
    public void loadLanguage() {
        this.currentLang = this.plugin.getConfigManager().getLanguage();
        String fileName = "languages/" + this.currentLang + ".yml";
        File langFile = new File(this.plugin.getDataFolder(), fileName);
        if (!langFile.exists()) {
            final InputStream res = this.plugin.getResource(fileName);
            if (res != null) {
                this.plugin.saveResource(fileName, false);
            }
            else {
                this.currentLang = "en";
                fileName = "languages/en.yml";
                langFile = new File(this.plugin.getDataFolder(), fileName);
                if (!langFile.exists()) {
                    this.plugin.saveResource(fileName, false);
                }
            }
        }
        this.lang = YamlConfiguration.loadConfiguration(langFile);
        final InputStream defStream = this.plugin.getResource("languages/" + this.currentLang + ".yml");
        if (defStream != null) {
            final YamlConfiguration def = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(defStream, StandardCharsets.UTF_8));
            this.lang.setDefaults((Configuration)def);
        }
    }
    
    public void reload() {
        this.loadLanguage();
    }
    
    public String get(final String key) {
        final String raw = this.lang.getString("messages." + key, "&c[MBank] Missing key: " + key);
        return this.colorize(raw);
    }
    
    public String get(final String key, final String... placeholders) {
        String msg = this.get(key);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return msg;
    }
    
    private String colorize(String text) {
        if (text == null) {
            return "";
        }
        text = text.replaceAll("&#([A-Fa-f0-9]{6})", "§x§$1").replaceAll("(?<=§x§)([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])", "§$1§$2§$3§$4§$5§$6");
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
