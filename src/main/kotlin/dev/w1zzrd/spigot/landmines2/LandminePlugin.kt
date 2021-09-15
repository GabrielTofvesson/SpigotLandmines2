package dev.w1zzrd.spigot.landmines2

import kr.entree.spigradle.annotations.SpigotPlugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentWrapper
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

private val field_acceptingNew = Enchantment::class.java.getDeclaredField("acceptingNew")

private var isAcceptingNewEnchantments: Boolean
    get() {
        field_acceptingNew.isAccessible = true
        return field_acceptingNew.get(null) as Boolean
    }
    set(value) {
        field_acceptingNew.isAccessible = true
        field_acceptingNew.set(null, value)
    }

@SpigotPlugin
class LandminePlugin: JavaPlugin() {
    private val recipeKey = NamespacedKey(this, "landmineRecipe")
    private val enchantmentKey = NamespacedKey(this, "landmineEnchantment")
    private lateinit var enchantment: Enchantment
    private var landmineManager: LandmineManager? = null

    override fun onEnable() {
        super.onEnable()

        val ench = Enchantment.getByKey(enchantmentKey)
        if (ench == null) {
            enchantment = LandmineEnchantment(enchantmentKey)
            isAcceptingNewEnchantments = true
            Enchantment.registerEnchantment(enchantment)
            isAcceptingNewEnchantments = false
        } else {
            enchantment = ench
        }

        val stack = ItemStack(Material.STONE_PRESSURE_PLATE)
        val meta = stack.itemMeta!!
        meta.addEnchant(enchantment, 1, true)
        meta.itemFlags.add(ItemFlag.HIDE_ENCHANTS)
        meta.setDisplayName("Landmine")
        stack.itemMeta = meta

        saveDefaultConfig()
        landmineManager = LandmineManager(
            this,
            enchantment,
            YamlFile(File(dataFolder, "data.yml"))
        )
        landmineManager!!.onEnable()

        Bukkit.addRecipe(ShapelessRecipe(recipeKey, stack).addIngredient(Material.STONE_PRESSURE_PLATE).addIngredient(Material.TNT))
    }

    override fun reloadConfig() {
        super.reloadConfig()
        saveDefaultConfig()
        landmineManager?.reload()
    }

    override fun onDisable() {
        Bukkit.removeRecipe(recipeKey)

        landmineManager!!.onDisable()
        saveConfig()

        super.onDisable()
    }
}