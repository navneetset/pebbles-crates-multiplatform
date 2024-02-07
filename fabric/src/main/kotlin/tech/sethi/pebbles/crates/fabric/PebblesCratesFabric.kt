package tech.sethi.pebbles.crates.fabric

import tech.sethi.pebbles.crates.fabriclike.PebblesCratesFabricLike
import net.fabricmc.api.ModInitializer


object PebblesCratesFabric: ModInitializer {
    override fun onInitialize() {
        PebblesCratesFabricLike.init()
    }
}
