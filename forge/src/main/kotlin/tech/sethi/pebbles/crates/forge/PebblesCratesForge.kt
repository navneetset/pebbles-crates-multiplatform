package tech.sethi.pebbles.crates.forge

import dev.architectury.platform.forge.EventBuses
import tech.sethi.pebbles.crates.PebblesCrates
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(PebblesCrates.MOD_ID)
object PebblesCratesForge {
    init {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(PebblesCrates.MOD_ID, MOD_BUS)
        PebblesCrates.init()
    }
}