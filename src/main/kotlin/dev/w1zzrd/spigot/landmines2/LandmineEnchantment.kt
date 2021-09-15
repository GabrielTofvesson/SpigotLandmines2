package dev.w1zzrd.spigot.landmines2

import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.inventory.ItemStack

class LandmineEnchantment(key: NamespacedKey): Enchantment(key) {
    override fun getName() = "Landmine"
    override fun getMaxLevel() = 1
    override fun getStartLevel() = 1
    override fun getItemTarget() = EnchantmentTarget.TOOL
    override fun isTreasure() = false
    override fun isCursed() = true
    override fun conflictsWith(other: Enchantment) = true
    override fun canEnchantItem(item: ItemStack) = false
}