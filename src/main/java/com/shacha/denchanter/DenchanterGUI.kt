package com.shacha.denchanter

import net.axay.kspigot.chat.KColors
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.extensions.bukkit.give
import net.axay.kspigot.gui.*
import net.axay.kspigot.items.addLore
import net.axay.kspigot.items.itemStack
import net.axay.kspigot.items.meta
import net.axay.kspigot.items.name
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.Repairable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

class DenchanterGUI {
    lateinit var inventoryView: InventoryView

    fun <T : ForInventory> update(guiInstance: GUIInstance<T>, player: Player) {
        enchantmentMap.clear()

        if (freeSlot?.let { it.type == Material.AIR } != false) {
            for (x in (Slots.RowThreeSlotTwo rectTo Slots.RowFourSlotEight).withInvType(invType)) {
                guiInstance[x.getCompound()] = ItemStack(Material.AIR)
            }
            updateResult(player)
            return
        }

        val enchantments = freeSlot?.enchantments ?: return
        if (enchantments.isEmpty()) return
        val iter = enchantments.iterator()
        for (x in (Slots.RowThreeSlotTwo rectTo Slots.RowFourSlotEight).withInvType(invType).reversed()) {
            if (!iter.hasNext()) {
                break
            }
            guiInstance[x.getCompound()] = itemStack(Material.ENCHANTED_BOOK) {
                iter.next().let { entry ->
                    enchantmentMap.add(entry.toPair() of true)
                    meta<EnchantmentStorageMeta> {
                        name = literalText("附魔") { italic = false }
                        addStoredEnchant(entry.key, entry.value, true)
                        lore(listOf(
                            literalText(),
                            literalText("点击去除此附魔") { italic = true; color = KColors.GRAY }
                        ))
                    }
                }
            }
        }
        updateResult(player)
    }

    var freeSlot: ItemStack?
        get() = inventoryView.topInventory.getItem(freeSlotNum)
        set(value) = inventoryView.topInventory.setItem(freeSlotNum, value)

    private var resultSlot: ItemStack?
        get() = inventoryView.topInventory.getItem(resultSlotNum)
        set(value) = inventoryView.topInventory.setItem(resultSlotNum, value)

    private var resultItem: ItemStack? = null

    private var costLevel = 0

    private var enchantmentMap: MutableList<MutablePair<Pair<Enchantment, Int>, Boolean>> = mutableListOf()

    companion object {
        private val invType = GUIType.SIX_BY_NINE
        val freeSlotNum =
            Slots.RowSixSlotFive.inventorySlot.realSlotIn(invType.dimensions)
                ?: throw NullPointerException()

        val resultSlotNum =
            Slots.RowOneSlotFive.inventorySlot.realSlotIn(invType.dimensions)
                ?: throw NullPointerException()
    }

    val denchanterGUI = kSpigotGUI(invType) {
        title = literalText("驱魔台")

        page(1) {
            onClose {
                freeSlot?.let { it1 -> it.player.give(it1) }
                it.guiInstance
            }

            placeholder(
                Slots.RowOneSlotOne rectTo Slots.RowSixSlotNine,
                itemStack(Material.BLACK_STAINED_GLASS_PANE) {
                    meta {
                        name = literalText()
                    }
                })

            button(
                Slots.RowThreeSlotTwo rectTo Slots.RowFourSlotEight,
                ItemStack(Material.AIR)
            ) { e ->
                val item = e.bukkitEvent.currentItem ?: return@button
                val type = item.type

                if (type == Material.AIR) {
                    return@button
                }

                when (type) {
                    Material.BOOK -> {
                        val index =
                            (Slots.RowThreeSlotTwo rectTo Slots.RowFourSlotEight).withInvType(invType).reversed()
                                .binarySearchBy(e.bukkitEvent.slot) { it.realSlotIn(invType.dimensions) }
                        val enchantment = enchantmentMap[index].first
                        e.bukkitEvent.currentItem = itemStack(Material.ENCHANTED_BOOK) {
                            meta<EnchantmentStorageMeta> {
                                name = literalText("附魔") { italic = false }
                                addStoredEnchant(enchantment.first, enchantment.second, true)
                                lore(listOf(
                                    literalText(),
                                    literalText("点击去除此附魔") { italic = true; color = KColors.GRAY }
                                ))
                            }
                        }
                        enchantmentMap[index].second = true
                    }

                    Material.ENCHANTED_BOOK -> {
                        val enchantment = (item.itemMeta as EnchantmentStorageMeta).storedEnchants.toList()[0]
                        e.bukkitEvent.currentItem = itemStack(Material.BOOK) {
                            meta {
                                name = literalText("附魔") { italic = false; color = KColors.RED }
                                lore(listOf(
                                    @Suppress("MoveLambdaOutsideParentheses")
                                    enchantment.first.displayName(enchantment.second)({ italic = false }),
                                    literalText(),
                                    literalText("点击恢复此附魔") { italic = true; color = KColors.GRAY }
                                ))
                            }
                        }
                        enchantmentMap.firstOrNull() { it.first == enchantment }
                            ?.second = false
                    }

                    else -> return@button
                }
                updateResult(e.player)
            }

//            button(
//                Slots.RowOneSlotTwo,
//                itemStack(Material.ARROW) { meta { name = literalText("上一页") { italic = false } } }
//            ) {
//
//            }
//            button(
//                Slots.RowOneSlotEight,
//                itemStack(Material.ARROW) { meta { name = literalText("下一页") { italic = false } } }
//            ) {
//
//            }
            button(
                Slots.RowOneSlotFour,
                itemStack(Material.BARRIER) {
                    meta {
                        name = literalText("取消") { color = KColors.RED; italic = false }
                    }
                }
            ) {
                it.player.closeInventory()
            }
            button(Slots.RowOneSlotFive, ItemStack(Material.AIR)) {
                if (it.player.level > costLevel || it.player.gameMode == GameMode.CREATIVE) {
                    it.player.give(resultItem ?: return@button)
                    if (it.player.gameMode != GameMode.CREATIVE) {
                        it.player.giveExpLevels(-costLevel)
                    }
                    this@DenchanterGUI.clear(it.guiInstance, it.player)
                } else {
                    it.player.sendMessage { literalText("你没有足够的经验！") { bold = true; color = KColors.RED } }
                }
            }
            button(Slots.RowSixSlotFive, ItemStack(Material.AIR)) {
                it.bukkitEvent.currentItem?.let { it1 -> it.player.give(it1) }
                it.guiInstance[Slots.RowSixSlotFive] = ItemStack(Material.AIR)
            }
        }
    }

    private fun updateResult(player: Player) {
        val result = freeSlot?.clone()
        if (result?.let { it.type == Material.AIR } != false) {
            resultItem = null
            resultSlot = ItemStack(Material.AIR)
            return
        }
        val enchantments = enchantmentMap.asSequence().filter { it.second }.map { it.first }
        var disabledPercent: Double
        enchantmentMap.filter { !it.second }
            .run {
                disabledPercent = size / enchantmentMap.size.toDouble()
                this
            }
            .forEach {
                result.removeEnchantment(it.first.first)
            }

        costLevel = if (disabledPercent == 1.0 || disabledPercent == 0.0) {
            0
        } else {
            ((enchantments.fold(0) { int, it ->
                int + (it.first.rarity.weight * 0.8).toInt()
            }
                    + (result.itemMeta as Repairable).repairCost)
                    * 0.5 * (0.5 * cos(PI * disabledPercent) + 0.5)
                    ).roundToInt()
        }

        resultItem = result.clone()

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

    private fun <T : ForInventory> clear(guiInstance: GUIInstance<T>, player: Player) {
        this.freeSlot = ItemStack(Material.AIR)
        update(guiInstance, player)
    }
}

private infix fun <F, S> F.of(that: S): MutablePair<F, S> = MutablePair(this, that)