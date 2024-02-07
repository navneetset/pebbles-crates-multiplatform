package tech.sethi.pebbles.crates.impl

import net.minecraft.item.ItemStack


interface ItemDisplayEntityImpl {
    fun `pebbles_items$publicSetStack`(stack: ItemStack?)

    fun `pebbles_affineRotation$publicSetRotation`(rotation: Float)
}