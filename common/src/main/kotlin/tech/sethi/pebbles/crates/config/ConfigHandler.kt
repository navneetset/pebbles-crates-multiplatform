package tech.sethi.pebbles.crates.config

import com.google.gson.GsonBuilder
import net.minecraft.item.ItemStack
import tech.sethi.pebbles.crates.PebblesCrates
import tech.sethi.pebbles.crates.util.ConfigFileHandler
import tech.sethi.pebbles.crates.util.PM
import java.io.File

object ConfigHandler {

    val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    val configFile = File(PebblesCrates.configDir, "config.json")
    val configFileHandler = ConfigFileHandler(Config::class.java, configFile, gson)

    val prizeScreenConfigFile = File(PebblesCrates.configDir, "prize-screen.json")
    val prizeScreenConfigFileHandler = ConfigFileHandler(PrizeScreenConfig::class.java, prizeScreenConfigFile, gson)

    var config = Config()
    var prizeScreenConfig = PrizeScreenConfig()

    init {
        reload()
    }

    fun reload() {
        PebblesCrates.LOGGER.info("Pebble's Crates: Reloading config")
        configFileHandler.reload()
        config = configFileHandler.config

        prizeScreenConfigFileHandler.reload()
        prizeScreenConfig = prizeScreenConfigFileHandler.config

        PebblesCrates.LOGGER.info("Config reloaded")

        CrateLoader.reload()
    }

    enum class TextType {
        LEGACY, MINIMESSAGE
    }

    data class Config(
        val enableParticleEffects: Boolean = true,
        val textType: TextType = TextType.LEGACY,
        val shuffleSound: SoundConfig = SoundConfig(true, "minecraft:block.note_block.bell", 0.5f, 1.0f),
        val rewardSound: SoundConfig = SoundConfig(true, "minecraft:block.ender_chest.open", 1.0f, 1.5f),
    )

    data class SoundConfig(
        val enable: Boolean, val sound: String, val volume: Float, val pitch: Float
    )

    data class PrizeScreenConfig(
        val backButton: DisplayItem = DisplayItem("Back", "minecraft:arrow", null, emptyList(), 45),
        val nextButton: DisplayItem = DisplayItem("Next", "minecraft:arrow", null, emptyList(), 53),
        val prizeSlots: List<Int> = (0..44).toList(),
        val fillSlots: List<Int> = (0..44).toList(),
        val fillItem: DisplayItem = DisplayItem(" ", "minecraft:gray_stained_glass_pane", null, emptyList(), null),
    )

    data class DisplayItem(
        val name: String, val material: String, val nbt: String?, val lore: List<String>, val slot: Int?
    ) {
        fun toItemStack(): ItemStack {
            return PM.createItemStack(PM.getItem(material), 1, name, lore, nbt)
        }
    }
}