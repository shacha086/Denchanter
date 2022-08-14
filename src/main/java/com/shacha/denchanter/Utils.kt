package com.shacha.denchanter

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import net.axay.kspigot.chat.LiteralTextBuilder
import net.axay.kspigot.gui.ForInventory
import net.axay.kspigot.gui.GUIType
import net.axay.kspigot.gui.InventorySlot
import net.axay.kspigot.gui.InventorySlotCompound
import net.kyori.adventure.text.Component
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
import java.lang.reflect.Field
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

fun Int.ensureIn(range: IntRange): Int {
    if (this in range) {
        return this
    }

    if (this < range.first) {
        return range.first
    }

    return range.last
}

@Suppress("UNCHECKED_CAST")
fun <T> Any?.cast() = this as T

inline fun <reified T> Any?.castOrNull(): T? {
    if (this is T) {
        return this
    }
    return null
}

fun <T> Field.get(obj: Any?) = this.get(obj).cast<T>()

inline fun <reified T> Field.getOrNull(obj: Any?) = this.get(obj).castOrNull<T>()