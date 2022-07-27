package com.shacha.denchanter

import net.axay.kspigot.chat.KColors
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.event.listen
import net.axay.kspigot.gui.openGUI
import net.axay.kspigot.items.itemStack
import net.axay.kspigot.items.meta
import net.axay.kspigot.items.name
import net.axay.kspigot.main.KSpigot
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_19_R1.block.CraftSkull
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.ClickType.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType.CRAFTING
import org.bukkit.event.inventory.InventoryType.SlotType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import java.util.logging.Logger


const val craftingTableB64 =
    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzJiN2U2ZDJlY2ZlMWQ1OGU2NjgyMzhiYmE2ZTZlZjMwODYwMTllNzY3MmIwY2JlZTgyYzIzYTg2Zjg1ZmU3ZSJ9fX0="
lateinit var NBT_DATA: NamespacedKey
lateinit var denchanter_block: ItemStack
lateinit var log: Logger

@Suppress("unused")
class Denchanter : KSpigot() {
    companion object {
        lateinit var INSTANCE: Denchanter; private set
    }


    override fun startup() {
        INSTANCE = this
        log = logger
        NBT_DATA = NamespacedKey(this, "nbt_data")
        denchanter_block = itemStack(Material.PLAYER_HEAD) {
            amount = 1
            meta {
                name = literalText("驱魔台") {
                    color = KColors.WHITE
                    italic = false
                }
                (this as SkullMeta).texture(craftingTableB64)
                persistentDataContainer.set(NBT_DATA, PersistentDataType.STRING, "denchanter_block")
            }
        }
        listen<InventoryDragEvent> {
            if (it.inventory.type != CRAFTING) {
                return@listen
            }

            if (it.inventory.size != 5) {
                return@listen
            }

            if (39 !in it.inventorySlots) {  // Armor Head
                return@listen
            }

            if (it.oldCursor.isDenchanter()) {
                object : BukkitRunnable() {
                    override fun run() {
                        it.whoClicked.inventory.helmet = ItemStack(Material.AIR)
                    }
                }.runTaskLater(this, 0)

                if (it.cursor == null) {
                    it.cursor = denchanter_block
                    return@listen
                }

                if (it.cursor?.isDenchanter() == true) {
                    it.cursor?.add()
                }
            }
        }

//        listen<InventoryCreativeEvent> {
//            object : BukkitRunnable() {
//                override fun run() {
//                    if (it.whoClicked.inventory.helmet?.isDenchanter() == true) {
//                        it.whoClicked.inventory.run { addItem(helmet!!) }
//                        it.whoClicked.inventory.helmet = ItemStack(Material.AIR)
//                    }
//                }
//            }.runTaskLater(this, 0)
//        }

        listen<InventoryClickEvent> {
            if (it.inventory.type != CRAFTING) {
                return@listen
            }

            when (it.click) {
                LEFT, RIGHT -> {
                    if (it.slotType != SlotType.ARMOR) {
                        return@listen
                    }

                    if (it.cursor?.isDenchanter() ?: return@listen) {
                        it.isCancelled = true
                    }
                }
                SHIFT_LEFT, SHIFT_RIGHT -> {
                    if (it.slotType != SlotType.QUICKBAR && it.slotType != SlotType.CONTAINER) {
                        return@listen
                    }

                    if (it.currentItem?.isDenchanter() ?: return@listen) {
                        it.isCancelled = true
                    }
                }
                else -> return@listen
            }
        }
        listen<BlockBreakEvent> {
            if (it.block.type != Material.PLAYER_HEAD) {
                return@listen
            }

            if (!(it.block.state as CraftSkull).isDenchanter()) {
                return@listen
            }

            it.isDropItems = false

            it.player.world.dropItemNaturally(it.block.location, denchanter_block)
        }

        listen<PlayerInteractEvent> {
            if (it.clickedBlock?.type != Material.PLAYER_HEAD) {
                return@listen
            }

            if (it.action != Action.RIGHT_CLICK_BLOCK) {
                return@listen
            }

            if (!(it.clickedBlock!!.state as CraftSkull).isDenchanter()) {
                return@listen
            }

            if (it.player.isSneaking) {
                return@listen
            }

            val guiInstance = DenchanterGUI()
            val gui = it.player.openGUI(guiInstance.denchanterGUI) ?: return@listen
            guiInstance.inventoryView = gui
            listen<InventoryClickEvent> click@{ e ->
                if (e.view != gui) {
                    return@click
                }

                if (e.clickedInventory == gui.bottomInventory) {
                    e.isCancelled = true
                    val item = e.currentItem ?: return@click
                    val enchantments = item.enchantments
                    if (enchantments.isEmpty()) {
                        e.whoClicked.sendMessage(literalText("不支持此工具！") { color = KColors.RED })
                        return@click
                    }
                    guiInstance.freeSlot = item
                    e.currentItem = ItemStack(Material.AIR)
                    guiInstance.update(guiInstance.denchanterGUI.getInstance(e.whoClicked as Player), e.whoClicked as Player)
                }
            }
        }
        listen<BlockPlaceEvent> {
            if (!it.itemInHand.isDenchanter()) {
                return@listen
            }


            if (it.block.type == Material.PLAYER_WALL_HEAD) {
                it.block.type = Material.PLAYER_HEAD
                val state = it.block.state
                state.javaClass.getDeclaredField("profile").run {
                    isAccessible = true
                    set(state, textureProfile(craftingTableB64))
                }
                state.update(true)
            }

            it.block.run {
                blockData = blockData
                    .merge(
                        Bukkit.createBlockData(
                            "minecraft:player_head[rotation=0]"
                        )
                    )
            }
        }
        val denchanterRecipe = newShapedRecipe("denchanter_block_recipe", denchanter_block)
        denchanterRecipe.shape(
            "dod",
            "oro",
            "dod"
        ).setIngredient('d', Material.DIAMOND)
            .setIngredient('o', Material.OBSIDIAN)
            .setIngredient('r', Material.REDSTONE)
        Bukkit.addRecipe(denchanterRecipe)
    }

    override fun shutdown() {
        Bukkit.resetRecipes()
    }
}