package com.shacha.denchanter.GUI.Controller

import com.shacha.denchanter.*
import com.shacha.denchanter.GUI.View
import com.shacha.denchanter.GUI.enchantmentMap
import com.shacha.denchanter.GUI.updateResult
import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchantments.EcoEnchant
import net.axay.kspigot.chat.KColors
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.gui.ForInventory
import net.axay.kspigot.gui.GUIClickEvent
import net.axay.kspigot.gui.Slots
import net.axay.kspigot.gui.rectTo
import net.axay.kspigot.items.itemStack
import net.axay.kspigot.items.meta
import net.axay.kspigot.items.name
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
//import top.iseason.deenchantment.manager.ConfigManager
//import top.iseason.deenchantment.manager.DeEnchantmentWrapper
//import top.iseason.deenchantment.utils.Tools


private fun <T : ForInventory> View.onDisabledEnchantmentClicked(event: GUIClickEvent<T>) {
    val index =
        (Slots.RowThreeSlotTwo rectTo Slots.RowFourSlotEight).withInvType(View.invType).reversed()
            .binarySearchBy(event.bukkitEvent.slot) { it.realSlotIn(View.invType.dimensions) }
    val enchantment = enchantmentMap[index].first
    event.bukkitEvent.currentItem = itemStack(Material.ENCHANTED_BOOK) {
        meta<EnchantmentStorageMeta> {
            name = literalText("附魔") { italic = false }
            addStoredEnchant(enchantment.first, enchantment.second, true)
            lore(
                listOf(
                    literalText(),
                    literalText("点击去除此附魔") { italic = true; color = KColors.GRAY })
            )
        }
    }
    enchantmentMap[index].second = true
}

fun <T : ForInventory> View.onEnabledEnchantmentClicked(event: GUIClickEvent<T>, item: ItemStack) {
    val enchantment = (item.itemMeta as EnchantmentStorageMeta).storedEnchants.toList()[0]
    event.bukkitEvent.currentItem = itemStack(Material.BOOK) {
        val enchant = enchantment.first
        val enchantmentName =
//            if (Compatibility.DEENCHANTMENT in compatibilityList && enchant is DeEnchantmentWrapper) {
//                literalText(
//                    "${ConfigManager.getEnchantmentName(enchant.name) ?: return} ${
//                        Tools.intToRome(
//                            enchantment.second
//                        )
//                    }"
//                ) { italic = false; color = KColors.GRAY }
//            } else
            if (Compatibility.ECOENCHANTS in compatibilityList) {
                if (enchant is EcoEnchant) {
                    literalText(enchant.displayName) {
                        displayName(enchantment)
                        italic = false; color = KColors.GRAY
                    }
                } else {
                    literalText(
                        EcoEnchantsPlugin.getInstance().langYml.getString("enchantments.${enchant.key.key}.name")
                    ) {
                        displayName(enchantment)
                        italic = false; color = KColors.GRAY

                    }
                }
            } else {
                try {
                    @Suppress("MoveLambdaOutsideParentheses")
                    enchant.displayName(enchantment.second)({ italic = false })
                } catch (e: AbstractMethodError) {
                    literalText(enchant.name.lowercase().replace("_", " ")
                        .replaceFirstChar { it.uppercaseChar() }) {
                        displayName(enchantment)
                        italic = false; color = KColors.GRAY
                    }
                }
            }
        meta {
            name = literalText("附魔") { italic = false; color = KColors.RED }

            lore(
                listOf(enchantmentName,
                    literalText(),
                    literalText("点击恢复此附魔") { italic = true; color = KColors.GRAY })
            )
        }
    }
    enchantmentMap.firstOrNull() { it.first == enchantment }?.second = false
}

fun <T : ForInventory> View.onEnchantmentClicked(event: GUIClickEvent<T>) {
    val item = event.bukkitEvent.currentItem ?: return
    val type = item.type

    if (type == Material.AIR) {
        return
    }

    when (type) {
        Material.BOOK -> {
            onDisabledEnchantmentClicked(event)
        }

        Material.ENCHANTED_BOOK -> {
            onEnabledEnchantmentClicked(event, item)
        }

        else -> return
    }
    updateResult(event.player)
}