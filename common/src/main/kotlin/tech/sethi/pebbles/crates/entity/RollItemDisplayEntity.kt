package tech.sethi.pebbles.crates.entity

import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import tech.sethi.pebbles.crates.config.CrateLoader

class RollItemDisplayEntity(val player: ServerPlayerEntity, pos: BlockPos, val prizes: List<CrateLoader.Prize>) :
    ItemDisplayEntity(EntityType.ITEM_DISPLAY, player.serverWorld) {

    private val ticksPerRoll = 10
    private var ticks = 0
    private var currentPrize = 0

    init {
        this.updatePosition(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
    }

    override fun tick() {
        super.tick()

        if (ticks >= ticksPerRoll) {
            ticks = 0
            if (currentPrize >= prizes.size) {
                currentPrize = 0
            }
            setStack(prizes[currentPrize].toItemStack())
            currentPrize++
        }

        ticks++
    }

    private fun setStack(stack: ItemStack) {
        try {
            val method = this.javaClass.getMethod("pebbles_crates\$publicSetStack", ItemStack::class.java)
            method.invoke(this, stack)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}