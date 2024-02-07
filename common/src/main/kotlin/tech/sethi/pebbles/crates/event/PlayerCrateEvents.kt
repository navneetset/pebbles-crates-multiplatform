package tech.sethi.pebbles.crates.event

import dev.architectury.event.EventResult
import dev.architectury.event.events.common.InteractionEvent
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Hand
import net.minecraft.world.World
import tech.sethi.pebbles.crates.PebblesCrates
import tech.sethi.pebbles.crates.config.CrateLoader
import tech.sethi.pebbles.crates.screens.general.PrizeDisplayScreen
import tech.sethi.pebbles.crates.util.PM

object PlayerCrateEvents {
    val worldMap = mutableMapOf<String, World>()

    fun onRightClick() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register { player, hand, blockPos, _ ->
            if (hand == Hand.MAIN_HAND) {
                val crateLocation = CrateLoader.isCrateLocation(blockPos, player.world)
                if (crateLocation != null) {
                    val crate = CrateLoader.getCrateConfig(crateLocation.crateId) ?: return@register EventResult.pass()
                    if (isKey(player.inventory.mainHandStack).not() && isCrateCreator(player.inventory.mainHandStack).not()) {
                        PrizeDisplayScreen.open(player as ServerPlayerEntity, crate)
                    }

                    if (isKey(player.inventory.mainHandStack)) {
                        PebblesCrates.LOGGER.info("Key detected")
                    }

                    return@register EventResult.interruptFalse()
                }

                if (isCrateCreator(player.inventory.mainHandStack)) {
                    val crateId = player.inventory.mainHandStack.nbt!!.getString("CrateName")
                    val newLocation =
                        CrateLoader.CrateLocation(crateId, blockPos.asLong(), player.world.registryKey.value.toString())
                    CrateLoader.addCrateData(newLocation)

                    player.sendMessage(PM.returnStyledMMText("<gold>Created a new crate location for $crateId"), false)
                    return@register EventResult.interruptFalse()
                }
            }
            EventResult.pass()
        }
    }

    fun isKey(heldStack: ItemStack): Boolean {
        return heldStack.hasNbt() && heldStack.nbt!!.contains("CrateName") && heldStack.nbt!!.contains("CrateCreator")
            .not()
    }

    fun isCrateCreator(heldStack: ItemStack): Boolean {
        return heldStack.hasNbt() && heldStack.nbt!!.contains("CrateName") && heldStack.nbt!!.contains("CrateCreator")
    }
}