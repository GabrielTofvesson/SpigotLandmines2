package dev.w1zzrd.spigot.landmines2

import kr.entree.spigradle.annotations.SpigotPlugin
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

@SpigotPlugin
class LandminePlugin: JavaPlugin() {
    private var landmineManager: LandmineManager? = null

    override fun onEnable() {
        super.onEnable()

        saveDefaultConfig()
        landmineManager = LandmineManager(this, YamlFile(File(dataFolder, "data.yml")))
        landmineManager!!.onEnable()
    }

    override fun reloadConfig() {
        super.reloadConfig()
        saveDefaultConfig()
        landmineManager?.reload()
    }

    override fun onDisable() {
        landmineManager!!.onDisable()
        saveConfig()

        super.onDisable()
    }
}