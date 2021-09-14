package dev.w1zzrd.spigot.landmines2

import org.bukkit.configuration.Configuration
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class YamlFile(
    private val file: File,
    private val conf: YamlConfiguration = YamlConfiguration.loadConfiguration(file)
): ConfigurationSection, Configuration by conf {
    private var firstLoad = false

    fun save() = conf.save(file)
    fun reload() {
        if (firstLoad) firstLoad = false
        else if (file.isFile) conf.load(file)
    }
}