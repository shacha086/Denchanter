package com.shacha.denchanter.GUI.Controller

import com.shacha.denchanter.*
import com.shacha.denchanter.GUI.View
import com.willfp.eco.util.toNiceString
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


private fun <T : ForInventory> View.onDisabledEnchantmentClicked(event: GUIClickEvent<T>) {
    val origIndex =
        (Slots.RowThreeSlotTwo rectTo Slots.RowFourSlotEight).withInvType(View.invType).reversed()
            .binarySearchBy(event.bukkitEvent.slot) { it.realSlotIn(View.invType.dimensions) }

    val index = origIndex + currentPage * 14

    val enchantment = enchantmentMap[index].first
    val stack = itemStack(Material.ENCHANTED_BOOK) {
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
    event.bukkitEvent.currentItem = stack
    enchantmentStackList[currentPage][origIndex] = stack
    enchantmentMap[index].second = true
}

fun <T : ForInventory> View.onEnabledEnchantmentClicked(event: GUIClickEvent<T>, item: ItemStack) {
    val enchantment = (item.itemMeta as EnchantmentStorageMeta).storedEnchants.toList()[0]
    val stack = itemStack(Material.BOOK) {
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
                    literalText(enchant.key.toNiceString()) {
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

    val origIndex =
        (Slots.RowThreeSlotTwo rectTo Slots.RowFourSlotEight).withInvType(View.invType).reversed()
            .binarySearchBy(event.bukkitEvent.slot) { it.realSlotIn(View.invType.dimensions) }

    val index = origIndex + currentPage * 14

    event.bukkitEvent.currentItem = stack
    enchantmentStackList[currentPage][origIndex] = stack
    enchantmentMap[index].second = false
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