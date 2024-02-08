package tech.sethi.pebbles.crates

import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.LifecycleEvent
import net.minecraft.server.MinecraftServer
import org.apache.logging.log4j.LogManager
import tech.sethi.pebbles.crates.commands.PebblesCratesCommands
import tech.sethi.pebbles.crates.config.ConfigHandler
import tech.sethi.pebbles.crates.config.CrateLoader
import tech.sethi.pebbles.crates.event.PlayerCrateEvents
import java.io.File

object PebblesCrates {
    const val MOD_ID = "pebbles_crates"
    val LOGGER = LogManager.getLogger()
    var server: MinecraftServer? = null
    val configDir = File("config/pebbles-crate/")

    fun init() {
        LOGGER.info("Pebble's Crates Initialized!")

        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            PebblesCratesCommands.register(dispatcher)
        }

        LifecycleEvent.SERVER_STARTING.register { server ->
            this.server = server
        }

        PlayerCrateEvents.onRightClick()
        PlayerCrateEvents.onBreakCrate()

        LifecycleEvent.SERVER_STARTED.register {

            it.worlds.forEach { world ->
                PlayerCrateEvents.worldMap[world.registryKey.value.toString()] = world
            }

            ConfigHandler
            CrateLoader
        }
    }
}
