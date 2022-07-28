package com.shacha.denchanter.GUI

import com.shacha.denchanter.*
import net.axay.kspigot.chat.KColors
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.extensions.bukkit.give
import net.axay.kspigot.gui.*
import net.axay.kspigot.items.addLore
import net.axay.kspigot.items.itemStack
import net.axay.kspigot.items.meta
import net.axay.kspigot.items.name
import net.axay.kspigot.sound.sound
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.Repairable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

var View.freeSlot: ItemStack?
    get() = inventoryView.topInventory.getItem(View.freeSlotNum)
    set(value) = inventoryView.topInventory.setItem(View.freeSlotNum, value)

internal var View.resultSlot: ItemStack?
    get() = inventoryView.topInventory.getItem(View.resultSlotNum)
    set(value) = inventoryView.topInventory.setItem(View.resultSlotNum, value)

internal var View.nextPageSlot: ItemStack?
    get() = inventoryView.topInventory.getItem(View.nextPageSlotNum)
    set(value) = inventoryView.topInventory.setItem(View.nextPageSlotNum, value)

internal var View.prevPageSlot: ItemStack?
    get() = inventoryView.topInventory.getItem(View.prevPageSlotNum)
    set(value) = inventoryView.topInventory.setItem(View.prevPageSlotNum, value)

internal var resultItem: ItemStack? = null

internal var costLevel = 0

internal var enchantmentMap: MutableList<MutablePair<Pair<Enchantment, Int>, Boolean>> = mutableListOf()

val nextPageStack = itemStack(Material.ARROW) { meta { name = literalText("下一页") { italic = false } } }

val prevPageStack = itemStack(Material.ARROW) { meta { name = literalText("上一页") { italic = false } } }

fun <T : ForInventory> View.onTakeBackItem(event: GUIClickEvent<T>) {
    if (event.bukkitEvent.currentItem.isNullOrAir) return
    event.player.give(event.bukkitEvent.currentItem!!)
    event.bukkitEvent.currentItem = ItemStack(Material.AIR)
    clear(event.guiInstance, event.player)
}

fun <T : ForInventory> View.onConfirmButtonClicked(event: GUIClickEvent<T>) {
    if (event.player.affordLevel(costLevel)) {
        event.player.give(resultItem ?: return)
        if (event.player.gameMode != GameMode.CREATIVE) {
            event.player.giveExpLevels(-costLevel)
        }
        event.player.sound(Sound.BLOCK_ANVIL_USE)
        clear(event.guiInstance, event.player)
    } else {
        event.player.sendMessage {
            literalText("你没有足够的经验！") { bold = true; color = KColors.RED }
        }
    }
}

fun <T : ForInventory> onPrevPageClicked(event: GUIClickEvent<T>) {

}

fun <T : ForInventory> onNextPageClicked(event: GUIClickEvent<T>) {
    if (event.bukkitEvent.currentItem.isNullOrAir) {
        return
    }

}

fun <T : ForInventory> View.onGUIClose(event: GUICloseEvent<T>) {
    freeSlot?.let { it -> event.player.give(it) }
    event.guiInstance
}

fun <T : ForInventory> View.update(guiInstance: GUIInstance<T>, player: Player) {
    enchantmentMap.clear()

    if (freeSlot.isNullOrAir) {
        for (x in (Slots.RowThreeSlotTwo rectTo Slots.RowFourSlotEight).withInvType(View.invType)) {
            guiInstance[x.getCompound()] = ItemStack(Material.AIR)
        }
        updateResult(player)
        return
    }

    val enchantments = freeSlot?.enchantments ?: return
    if (enchantments.isEmpty()) return
    val iter = enchantments.iterator()
    for (x in (Slots.RowThreeSlotTwo rectTo Slots.RowFourSlotEight).withInvType(View.invType).reversed()) {
        if (!iter.hasNext()) {
            break
        }
        guiInstance[x.getCompound()] = itemStack(Material.ENCHANTED_BOOK) {
            iter.next().let { entry ->
                enchantmentMap.add(entry.toPair() of true)
                meta<EnchantmentStorageMeta> {
                    name = literalText("附魔") { italic = false }
                    addStoredEnchant(entry.key, entry.value, true)
                    lore(listOf(literalText(), literalText("点击去除此附魔") { italic = true; color = KColors.GRAY }))
                }
            }
        }
    }
    if (!iter.hasNext()) {
        nextPageSlot = ItemStack(Material.AIR)
    }
    updateResult(player)
}


internal fun View.updateResult(player: Player) {
    val result = freeSlot?.clone()
    if (result.isNullOrAir) {
        resultItem = null
        resultSlot = ItemStack(Material.AIR)
        return
    }
    val enchantments = enchantmentMap.asSequence().filter { it.second }.map { it.first }
    var disabledPercent: Double
    enchantmentMap.filter { !it.second }.run {
        disabledPercent = size / enchantmentMap.size.toDouble()
        this
    }.forEach {
        result!!.removeEnchantment(it.first.first)
    }

    costLevel = if (disabledPercent == 1.0 || disabledPercent == 0.0) {
        0
    } else {
        ((enchantments.fold(0) { int, _ -> int + 8 } + (result!!.itemMeta as Repairable).repairCost) * 0.5 * (0.5 * cos(
            PI * disabledPercent
        ) + 0.5)).roundToInt()
    }

    resultItem = result!!.clone()

    result {
        meta {
            addLore {
                if (player.level < costLevel && player.gameMode != GameMode.CREATIVE) {
                    +literalText()
                    +literalText("经验不足！(${player.level}级/${costLevel}级)") {
                        color = KColors.RED
                        bold = true
                    }
                } else {
                    if (costLevel != 0) {
                        +literalText()
                        +literalText("耗费${costLevel}级") {
                            color = KColors.YELLOW
                            bold = true
                        }
                    }
                }
            }
        }
    }
    resultSlot = result
}


private fun <T : ForInventory> View.clear(guiInstance: GUIInstance<T>, player: Player) {
    freeSlot = ItemStack(Material.AIR)
    update(guiInstance, player)
}
