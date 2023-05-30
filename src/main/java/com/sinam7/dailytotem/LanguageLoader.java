package com.sinam7.dailytotem;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.logging.Level;

public class LanguageLoader {

    HashMap<String, String> translationMap = new HashMap<>();

    public LanguageLoader(Main plugin) {
        File languageDirectory = new File(plugin.getDataFolder(), "languages/");
        File defaultLanguageFile = new File(plugin.getDataFolder(), "languages/en_US.yml");
        File koLanguageFile = new File(plugin.getDataFolder(), "languages/ko_KR.yml");
        if (!languageDirectory.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            languageDirectory.mkdir();
            try {
                InputStream stream = plugin.getResource("languages/en_US.yml");
                //noinspection DataFlowIssue
                Files.copy(stream, defaultLanguageFile.toPath());
                InputStream streamKR = plugin.getResource("languages/ko_KR.yml");
                //noinspection DataFlowIssue
                Files.copy(streamKR, koLanguageFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String locale = plugin.getConfig().getString("locale");
        plugin.getLogger().log(Level.INFO, "Your current locale: %s".formatted(locale));
        //noinspection DataFlowIssue
        if (locale != null && locale.equals("en_US") || locale.equals("ko_KR")) {
            FileConfiguration translations = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "languages/" + locale + ".yml"));
            for (String translation : translations.getKeys(false)) {
                translationMap.put(translation, translations.getString(translation));
            }
        } else {
            plugin.getLogger().log(Level.WARNING, "Locale %s is invalid! locale default set to en_US".formatted(locale));
            FileConfiguration translations = YamlConfiguration.loadConfiguration(defaultLanguageFile);
            for (String translation : translations.getKeys(false)) {
                translationMap.put(translation, translations.getString(translation));
            }
        }
    }

    public String get(String path){
        return translationMap.get(path);
    }

}