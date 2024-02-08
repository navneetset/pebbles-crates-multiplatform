package tech.sethi.pebbles.crates.event

import dev.architectury.event.EventResult
import dev.architectury.event.events.common.BlockEvent
import dev.architectury.event.events.common.InteractionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import tech.sethi.pebbles.crates.PebblesCrates
import tech.sethi.pebbles.crates.config.ConfigHandler
import tech.sethi.pebbles.crates.config.CrateLoader
import tech.sethi.pebbles.crates.entity.RollItemDisplayEntity
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

                    if (isKey(player.inventory.mainHandStack) && crateLocation.isRolling.not()) {
                        crateLocation.isRolling = true
                        val keyCrateName = player.inventory.mainHandStack.nbt!!.getString("CrateName")
                        if (keyCrateName != crate.crateName) {
                            player.sendMessage(PM.returnStyledMMText("<red>Wrong key for this crate!"), false)
                            return@register EventResult.interruptFalse()
                        } else {
                            val prizes = rollCrate(keyCrateName)
                            player.inventory.mainHandStack.decrement(1)

                            RollItemDisplayEntity(
                                player as ServerPlayerEntity, blockPos, prizes, crate, crateLocation
                            )
                            return@register EventResult.interruptFalse()
                        }
                    } else if (isKey(player.inventory.mainHandStack) && crateLocation.isRolling) {
                        player.sendMessage(
                            PM.returnStyledMMText("<red>This crate is already rolling! Please wait."), false
                        )
                        return@register EventResult.interruptFalse()
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

    fun onBreakCrate() {
        BlockEvent.BREAK.register { world, pos, state, player, _ ->
            val crateLocation = CrateLoader.isCrateLocation(pos, world)
            if (crateLocation != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(500)
                    // if block is now air, remove crate data
                    PebblesCrates.server!!.execute {
                        if (world.getBlockState(pos).isAir) {
                            CrateLoader.removeCrateData(crateLocation)
                            player.sendMessage(PM.returnStyledMMText("<aqua>Removed ${crateLocation.crateId}"), false)
                        }
                    }
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

    fun rollCrate(crateId: String): List<CrateLoader.Prize> {
        val crate = CrateLoader.getCrateConfig(crateId) ?: return emptyList()
        val sumWeight = crate.prize.map { it.chance }.sum()
        val roll = (0..sumWeight).random()
        var currentWeight = 0
        val outputAmount = ConfigHandler.config.shuffleCount
        val output = mutableListOf<CrateLoader.Prize>()

        for (prize in crate.prize) {
            currentWeight += prize.chance
            if (roll <= currentWeight) {
                output.add(prize)
                if (output.size == outputAmount) {
                    break
                }
            }
        }

        return output
    }
}