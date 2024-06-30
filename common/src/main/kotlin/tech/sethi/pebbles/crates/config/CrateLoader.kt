package tech.sethi.pebbles.crates.config

import com.google.gson.GsonBuilder
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import tech.sethi.pebbles.crates.PebblesCrates
import tech.sethi.pebbles.crates.event.PlayerCrateEvents
import tech.sethi.pebbles.crates.particles.HeartParticle
import tech.sethi.pebbles.crates.particles.SparkleParticle
import tech.sethi.pebbles.crates.particles.SpiralParticle
import tech.sethi.pebbles.crates.util.ConfigFileHandler
import tech.sethi.pebbles.crates.util.PM
import java.io.File

object CrateLoader {
    val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val cratesDirectory = File(PebblesCrates.configDir, "crates")

    private val crateDataFile = File(PebblesCrates.configDir, "crate-data.json")
    private val crateDataFileHandler = ConfigFileHandler(CrateData::class.java, crateDataFile, gson)
    val crateLocations = mutableMapOf<BlockPos, CrateLocation>()

    val crateConfigs = mutableMapOf<String, CrateConfig>()

    fun reload() {
        createCratesFolder()
        loadCrateConfigs()
        loadCrateData()
    }

    fun createCratesFolder() {
        if (!cratesDirectory.exists()) {
            cratesDirectory.mkdirs()
        }
    }

    fun getCrateConfig(crateName: String): CrateConfig? {
        loadCrateConfigs()
        return crateConfigs[crateName]
    }

    fun loadCrateConfigs(): MutableList<CrateConfig> {
        if (!cratesDirectory.exists()) {
            cratesDirectory.mkdirs()
        }

        val loadedConfigs = mutableListOf<CrateConfig>()

        cratesDirectory.listFiles { _, name -> name.endsWith(".json") }?.forEach { file ->
            val json = file.readText()
            if (json.isNotEmpty()) {
                val crateConfig = gson.fromJson(json, CrateConfig::class.java)
                val crateName = crateConfig.id ?: crateConfig.crateName
                crateConfigs[crateName] = crateConfig
                loadedConfigs.add(crateConfig)
            }
        }

        return loadedConfigs
    }

    fun loadCrateData() {
        if (!crateDataFile.exists()) {
            println("Crate data file does not exist")
            return
        }

        crateDataFileHandler.reload()
        crateLocations.clear()

        val data = crateDataFileHandler.config

        HeartParticle.clearAll()
        SpiralParticle.clearAll()
        SparkleParticle.clearAll()

        data.crates.forEach { crateLocation ->
            val pos = BlockPos.fromLong(crateLocation.blockPos)
            crateLocations[pos] = crateLocation

            val world = PlayerCrateEvents.worldMap[crateLocation.world]!!
            when (getCrateConfig(crateLocation.crateId)?.particle) {
                "spiral" -> SpiralParticle.queueAdd(pos, world)
                "sparkle" -> SparkleParticle.queueAdd(pos, world)
                "heart" -> HeartParticle.queueAdd(pos, world)
                "none" -> {}
                else -> SparkleParticle.queueAdd(pos, world)
            }
        }

        println("Pebble's Crates: ${crateLocations.size} crate locations loaded")
    }

    fun isCrateLocation(blockPos: BlockPos, world: World): CrateLocation? {
        val crateLocation = crateLocations[blockPos]
        return if (crateLocation != null && crateLocation.world == world.registryKey.value.toString()) {
            crateLocation
        } else {
            null
        }
    }

    fun addCrateData(crateLocation: CrateLocation) {
        val data = crateDataFileHandler.config
        data.crates.add(crateLocation)
        crateDataFileHandler.save()

        crateLocations[BlockPos.fromLong(crateLocation.blockPos)] = crateLocation

        val world = PlayerCrateEvents.worldMap[crateLocation.world]!!
        when (getCrateConfig(crateLocation.crateId)?.particle) {
            "spiral" -> SpiralParticle.queueAdd(BlockPos.fromLong(crateLocation.blockPos), world)
            "sparkle" -> SparkleParticle.queueAdd(BlockPos.fromLong(crateLocation.blockPos), world)
            "heart" -> HeartParticle.queueAdd(BlockPos.fromLong(crateLocation.blockPos), world)
            "none" -> {}
            else -> SparkleParticle.queueAdd(BlockPos.fromLong(crateLocation.blockPos), world)
        }
    }

    fun removeCrateData(crateLocation: CrateLocation) {
        val data = crateDataFileHandler.config
        data.crates.remove(crateLocation)

        SpiralParticle.queueRemove(BlockPos.fromLong(crateLocation.blockPos))
        SparkleParticle.queueRemove(BlockPos.fromLong(crateLocation.blockPos))
        HeartParticle.queueRemove(BlockPos.fromLong(crateLocation.blockPos))

        crateDataFileHandler.save()

        crateLocations.remove(BlockPos.fromLong(crateLocation.blockPos))
    }


    data class CrateConfig(
        val id: String? = null,
        val crateName: String,
        val particle: String? = "spiral",
        val crateKey: CrateKey,
        var prize: List<Prize>,
    )

    data class CrateKey(
        val material: String, val name: String, val nbt: String?, val lore: List<String>
    ) {
        fun toItemStack(): ItemStack {
            return PM.createItemStack(PM.getItem(material), 1, name, lore, nbt)
        }
    }

    data class Prize(
        val name: String,
        val material: String,
        val amount: Int,
        val nbt: String? = null,
        val commands: List<String>,
        val broadcast: String? = null,
        val messageToOpener: String? = null,
        val lore: List<String>?,
        val chance: Int
    ) {
        fun toItemStack(totalWeight: Int? = 1): ItemStack {
            val calculatedChance: Double = (chance.toDouble() / totalWeight!!) * 100
            val twoDecimalChance = String.format("%.2f", calculatedChance)
            val parsedLore = lore?.map {
                it.replace("{chance}", twoDecimalChance).replace("{prize_name}", name).replace("{prize_amount}", "$amount")
                    .replace("{crate_name}", name)
            }
            val stack = PM.createItemStack(PM.getItem(material), amount, name, parsedLore, nbt)

            if (lore.isNullOrEmpty()) {
                val chanceLore = listOf("Chance: $twoDecimalChance%")
                PM.setLore(stack, chanceLore)
            }

            return stack
        }

        fun onReward(player: ServerPlayerEntity, crate: CrateConfig) {
            commands.forEach { command ->
                val parsedCommand = command.replace("{player_name}", player.name.string)
                PM.runCommand(parsedCommand)

                if (broadcast != null) {
                    val parsedMessage =
                        broadcast.replace("{player_name}", player.name.string).replace("{prize_name}", name)
                            .replace("{prize_amount}", "$amount").replace("{crate_name}", crate.crateName)
                            .replace("{chance}", "$chance")

                    PebblesCrates.server!!.playerManager.broadcast(PM.returnStyledText(parsedMessage), false)
                }

                if (messageToOpener != null) {
                    val parsedMessage =
                        messageToOpener.replace("{player_name}", player.name.string).replace("{prize_name}", name)
                            .replace("{prize_amount}", "$amount").replace("{crate_name}", crate.crateName)
                            .replace("{chance}", "$chance")

                    player.sendMessage(PM.returnStyledText(parsedMessage), false)
                }
            }

        }
    }

    data class CrateData(
        val crates: MutableList<CrateLocation> = mutableListOf()
    )

    data class CrateLocation(
        val crateId: String, val blockPos: Long, val world: String
    ) {
        var isRolling = false
        var lastRollTime = 0L
    }
}