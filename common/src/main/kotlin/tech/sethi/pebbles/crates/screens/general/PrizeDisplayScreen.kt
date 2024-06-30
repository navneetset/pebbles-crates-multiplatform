package tech.sethi.pebbles.crates.screens.general

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SimpleInventory
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import tech.sethi.pebbles.crates.config.ConfigHandler
import tech.sethi.pebbles.crates.config.CrateLoader
import tech.sethi.pebbles.crates.util.PM

class PrizeDisplayScreen(
    syncId: Int, player: ServerPlayerEntity, val crate: CrateLoader.CrateConfig
) : GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, syncId, player.inventory, SimpleInventory(9 * 6), 6) {

    companion object {
        fun open(player: ServerPlayerEntity, crate: CrateLoader.CrateConfig) {
            player.openHandledScreen(SimpleNamedScreenHandlerFactory({ syncId, _, p ->
                PrizeDisplayScreen(syncId, (p as ServerPlayerEntity), crate)
            }, PM.returnStyledText(crate.crateName)))
        }
    }

    var currentPage = 0
    val itemsPerPage = ConfigHandler.prizeScreenConfig.prizeSlots.size

    val backButton = ConfigHandler.prizeScreenConfig.backButton
    val nextButton = ConfigHandler.prizeScreenConfig.nextButton

    val fillSlots = ConfigHandler.prizeScreenConfig.fillSlots
    val prizeSlots = ConfigHandler.prizeScreenConfig.prizeSlots

    val totalWeight = crate.prize.sumOf { it.chance }

    init {
        fillPlaceholderItems()
        populateInventory()
        navigationItems()
    }

    fun fillPlaceholderItems() {
        for (slot in fillSlots) inventory.setStack(slot, ConfigHandler.prizeScreenConfig.fillItem.toItemStack())
    }

    fun populateInventory() {
        val items = crate.prize
        val startIndex = currentPage * itemsPerPage
        val endIndex = (currentPage + 1) * itemsPerPage

        for (i in startIndex until endIndex) {
            val slot = prizeSlots[i - startIndex]
            if (i < items.size) inventory.setStack(slot, items[i].toItemStack(totalWeight))
        }
    }

    fun navigationItems() {
        if (currentPage > 0) inventory.setStack(backButton.slot!!, backButton.toItemStack())
        if (currentPage < (crate.prize.size - 1) / itemsPerPage) inventory.setStack(
            nextButton.slot!!, nextButton.toItemStack()
        )
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType?, player: PlayerEntity?) {
        when (slotIndex) {
            backButton.slot -> {
                if (currentPage > 0) {
                    currentPage--
                    fillPlaceholderItems()
                    populateInventory()
                    navigationItems()
                }
            }
            nextButton.slot -> {
                if (currentPage < (crate.prize.size - 1) / itemsPerPage) {
                    currentPage++
                    fillPlaceholderItems()
                    populateInventory()
                    navigationItems()
                }
            }
        }

        return
    }
}