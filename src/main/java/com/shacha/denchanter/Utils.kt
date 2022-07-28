package com.shacha.denchanter

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import net.axay.kspigot.chat.LiteralTextBuilder
import net.axay.kspigot.gui.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_19_R1.block.CraftSkull
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.logging.Logger

fun textureProfile(b64: String): GameProfile {
    val profile = GameProfile(UUID(0, 0), "Player")
    profile.properties.put("textures", Property("textures", b64))
    return profile
}

fun SkullMeta.texture(b64: String) {
    try {
        val profileField = this.javaClass.getDeclaredField("profile")
        profileField.isAccessible = true
        profileField.set(this, textureProfile(b64))
    } catch (exception: NoSuchFieldException) {
        exception.printStackTrace()
    } catch (exception: IllegalArgumentException) {
        exception.printStackTrace()
    } catch (exception: IllegalAccessException) {
        exception.printStackTrace()
    }
}

open class SingleInventorySlot<T : ForInventory>(
    inventorySlot: InventorySlot,
) : InventorySlotCompound<T> {
    constructor(row: Int, slotInRow: Int) : this(InventorySlot(row, slotInRow))

    private val slotAsList = listOf(inventorySlot)

    override fun withInvType(invType: GUIType<T>) = slotAsList
}

fun <T : ForInventory> InventorySlot.getCompound(): InventorySlotCompound<T> {
    return SingleInventorySlot(row, slotInRow)
}


fun JavaPlugin.newShapedRecipe(recipeName: String, itemStack: ItemStack): ShapedRecipe {
    return ShapedRecipe(
        NamespacedKey(this, recipeName), itemStack
    )
}

fun ItemStack.isDenchanter(): Boolean {
    if (this.type != Material.PLAYER_HEAD) {
        return false
    }

    if (!this.itemMeta.persistentDataContainer.has(NBT_DATA)) {
        return false
    }

    return true
}

fun Logger.d(obj: Any?) {
    this.info("$obj")
}

fun CraftSkull.isDenchanter(): Boolean {
    val profile = this.javaClass.getDeclaredField("profile").run {
        isAccessible = true
        get(this@isDenchanter) as GameProfile?
    } ?: return false
    return (profile.properties.get("textures").toList()[0] as Property).value == craftingTableB64
}

fun LinkedHashSet<InventorySlot>.reversed(): List<InventorySlot> {
    if (size <= 1) return toList()
    return sortedWith(Comparator { l, r -> return@Comparator -(l.row.compareTo(r.row)) })
}

operator fun Component.invoke(builder: LiteralTextBuilder.() -> Unit = { }) = LiteralTextBuilder(this).apply(builder).build()

inline operator fun ItemStack.invoke(builder: ItemStack.() -> Unit) = this.apply(builder)

data class MutablePair<T, U>(var first: T, var second: U)

val ItemStack?.isNullOrAir: Boolean
    get() = this?.let { it.type == Material.AIR } != false

fun LiteralTextBuilder.displayName(
    enchantment: Pair<Enchantment, Int>,
) {
    if (enchantment.second != 1 || enchantment.first.maxLevel != 1)
        component(
            Component.text(" ")
                .append(
                    Component.translatable("enchantment.level.${enchantment.second}")
                )
        )
}

fun Player.affordLevel(costLevel: Int) =
    this.level > costLevel || this.gameMode == GameMode.CREATIVE
