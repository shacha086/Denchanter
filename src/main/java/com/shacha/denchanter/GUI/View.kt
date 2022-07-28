package com.shacha.denchanter.GUI

import com.shacha.denchanter.GUI.Controller.onEnchantmentClicked
import net.axay.kspigot.chat.KColors
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.gui.*
import net.axay.kspigot.items.itemStack
import net.axay.kspigot.items.meta
import net.axay.kspigot.items.name
import org.bukkit.Material
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack

class View {
    lateinit var inventoryView: InventoryView
    companion object {
        internal val invType = GUIType.SIX_BY_NINE
        val freeSlotNum =
            Slots.RowSixSlotFive.inventorySlot.realSlotIn(invType.dimensions)!!

        val resultSlotNum =
            Slots.RowOneSlotFive.inventorySlot.realSlotIn(invType.dimensions)!!

        val nextPageSlotNum =
            Slots.RowOneSlotEight.inventorySlot.realSlotIn(invType.dimensions)!!

        val prevPageSlotNum =
            Slots.RowOneSlotTwo.inventorySlot.realSlotIn(invType.dimensions)!!
    }

    val denchanterGUI = kSpigotGUI(invType) {
        title = literalText("驱魔台")

        page(1) {
            onClose(::onGUIClose)

            placeholder(
                Slots.RowOneSlotOne rectTo Slots.RowSixSlotNine,
                itemStack(Material.BLACK_STAINED_GLASS_PANE) {
                    meta {
                        name = literalText()
                    }
                }
            )

            button(
                Slots.RowThreeSlotTwo rectTo Slots.RowFourSlotEight,
                ItemStack(Material.AIR),
                ::onEnchantmentClicked
            )

            button(
                Slots.RowOneSlotTwo,
                itemStack(Material.BLACK_STAINED_GLASS_PANE) {
                    meta {
                        name = literalText()
                    }
                },
                ::onPrevPageClicked
            )

            button(
                Slots.RowOneSlotEight,
                itemStack(Material.BLACK_STAINED_GLASS_PANE) {
                    meta {
                        name = literalText()
                    }
                },
                ::onNextPageClicked
            )

            button(Slots.RowOneSlotFour, itemStack(Material.BARRIER) {
                meta {
                    name = literalText("取消") { color = KColors.RED; italic = false }
                }
            }) {
                it.player.closeInventory()
            }

            button(
                Slots.RowOneSlotFive,
                ItemStack(Material.AIR),
                ::onConfirmButtonClicked
            )

            button(
                Slots.RowSixSlotFive,
                ItemStack(Material.AIR),
                ::onTakeBackItem
            )
        }
    }
}