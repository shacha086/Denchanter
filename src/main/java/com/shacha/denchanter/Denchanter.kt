package com.shacha.denchanter

import com.shacha.denchanter.GUI.View
import com.shacha.denchanter.GUI.freeSlot
import com.shacha.denchanter.GUI.onGUIClose
import com.shacha.denchanter.GUI.update
import io.papermc.paper.network.ConnectionEvent
import net.axay.kspigot.chat.KColors
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.event.listen
import net.axay.kspigot.gui.GUI
import net.axay.kspigot.gui.GUIInstance
import net.axay.kspigot.gui.openGUI
import net.axay.kspigot.items.itemStack
import net.axay.kspigot.items.meta
import net.axay.kspigot.items.name
import net.axay.kspigot.main.KSpigot
import org.bukkit.Bukkit
import org.bukkit.GameMode
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
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import java.util.logging.Logger


const val craftingTableB64 =
    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTZmZDNkMDNkYWFkNjlmNjYzZjE4ODhiNDg4YzBjYWYzZWE3ZGE3YWUwNTgwZGFlYTVmMzliZDk0MGQ0YjMyYiJ9fX0="
lateinit var NBT_DATA: NamespacedKey
lateinit var denchanter_block: ItemStack
lateinit var log: Logger
val compatibilityList = mutableListOf<Compatibility>()
val guiInstanceList = mutableListOf<GUI<*>>()

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

        if (server.pluginManager.getPlugin("EcoEnchants") != null) {
            log.d("EcoEnchants detected. Using Eco compatibility. ")
            compatibilityList.add(Compatibility.ECOENCHANTS)
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

            if (it.player.gameMode != GameMode.CREATIVE) {
                it.isDropItems = false
                it.player.world.dropItemNaturally(it.block.location, denchanter_block)
            }
        }

        listen<PlayerInteractEvent> {
            if (it.clickedBlock?.type?.let {
                    it != Material.PLAYER_HEAD &&
                            it != Material.PLAYER_WALL_HEAD
            } == true) {
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

            val guiInstance = View()
            val gui = it.player.openGUI(guiInstance.denchanterGUI) ?: return@listen
            guiInstanceList.add(guiInstance.denchanterGUI)
            guiInstance.inventoryView = gui
            listen<InventoryClickEvent> click@{ e ->
                if (e.view != gui) {
                    return@click
                }

                if (e.clickedInventory == gui.bottomInventory) {
                    e.isCancelled = true
                    if (guiInstance.freeSlot?.let { it.type == Material.AIR } == false) {
                        return@click
                    }
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

//            if (it.block.type == Material.PLAYER_WALL_HEAD) {
//                it.block.setType(Material.PLAYER_HEAD, true)
//                val state = it.block.state
//                state.javaClass.getDeclaredField("profile").run {
//                    isAccessible = true
//                    set(state, textureProfile(craftingTableB64))
//                }
//                state.update(true, true)
//                val blockUpdatePacket = PacketContainer(PacketType.Play.Server.BLOCK_CHANGE)
//                blockUpdatePacket.modifier.writeDefaults()
//                blockUpdatePacket.blockPositionModifier.write(0, it.block.run { return@run BlockPosition(x, y, z) } )
//                blockUpdatePacket.blockData.write(0, WrappedBlockData.createData(it.block.state.blockData))
//                protocolManager.sendServerPacket(it.player, blockUpdatePacket)
//            }
            if (it.block.type == Material.PLAYER_HEAD) {
                it.block.run {
                    blockData = blockData
                        .merge(
                            Bukkit.createBlockData(
                                "minecraft:player_head[rotation=0]"
                            )
                        )
                }
            }
        }

        listen<PlayerQuitEvent> { e ->
            guiInstanceList.firstOrNull {
                it.getAllInstances().any { gui ->
                    gui.javaClass.getField("bukkitInventory")
                        .get<Inventory>(gui).viewers
                        .any { human -> human == e.player }
                }
            }?.closeGUI()
        }
    }

    override fun shutdown() {
        Bukkit.resetRecipes()
        guiInstanceList.forEach {
            it.closeGUI()
            logger.d("${it.hashCode()} closed.")
        }
    }
}