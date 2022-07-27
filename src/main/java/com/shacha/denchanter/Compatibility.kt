package com.shacha.denchanter

enum class Compatibility(private val string: String) {
    DEENCHANTMENT("DeEnchantment"),
    ECOENCHANTS("EcoEnchants");

    override fun toString(): String = this.string
}