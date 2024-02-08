package tech.sethi.pebbles.crates.entity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.client.render.model.json.ModelTransformationMode
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.util.math.AffineTransformation
import net.minecraft.util.math.BlockPos
import org.joml.Vector3f
import tech.sethi.pebbles.crates.config.ConfigHandler
import tech.sethi.pebbles.crates.config.CrateLoader
import tech.sethi.pebbles.crates.impl.ItemDisplayEntityImpl

@Suppress("UNCHECKED_CAST")
class RollItemDisplayEntity(
    val player: ServerPlayerEntity,
    private val pos: BlockPos,
    private val prizes: List<CrateLoader.Prize>,
    private val crate: CrateLoader.CrateConfig,
    val crateLocation: CrateLoader.CrateLocation
) : ItemDisplayEntity(EntityType.ITEM_DISPLAY, player.serverWorld) {

    private val ticksPerRoll = 10
    private var ticks = 0
    private var currentPrize = 0

    private var lastPrizeTick = 0

    private var animationTicks = 0
    private val animationDuration = 5

    private var isRolling = true

    private val itemDisplayImpl: ItemDisplayEntityImpl
        get() = this as ItemDisplayEntityImpl


    init {
        this.noClip = true
        this.setPosition(pos.x.toDouble() + 0.5, pos.y.toDouble() + 1.25, pos.z.toDouble() + 0.5)

        player.world.spawnEntity(this)

        itemDisplayImpl.`pebbles_crates$publicSetTransformationMode`(ModelTransformationMode.HEAD)
//        setTransformationModeUsingReflection(ModelTransformationMode.HEAD)
    }

    override fun tick() {
        super.tick()

        if (this.isRemoved.not()) {
            (world as ServerWorld).chunkManager.addTicket(ChunkTicketType.FORCED, this.chunkPos, 3, this.chunkPos)
        }

        yaw = player.yaw + 180.0f
        pitch = 0.0f
        headYaw = player.headYaw + 180.0f

        if (isRolling && ticks >= ticksPerRoll) {
            ticks = 0
            val nextStack = prizes[currentPrize].toItemStack()
            itemDisplayImpl.`pebbles_crates$publicSetStack`(nextStack)
//            setStack(nextStack)
            val soundIdentifier = Identifier(ConfigHandler.config.shuffleSound.sound)
            val soundEvent = SoundEvent.of(soundIdentifier)
            player.serverWorld.playSound(
                null,
                pos,
                soundEvent,
                SoundCategory.BLOCKS,
                ConfigHandler.config.shuffleSound.volume,
                ConfigHandler.config.shuffleSound.pitch
            )

            animationTicks = 0

            currentPrize++
            if (currentPrize >= prizes.size) {
                isRolling = false
                lastPrizeTick = 0
                currentPrize = prizes.size - 1
            }
        }

        if (animationTicks < animationDuration) {
            animationTicks++
        }

        if (!isRolling) {
            lastPrizeTick++
            if (lastPrizeTick == 10) {
                crateLocation.isRolling = false
                itemDisplayImpl.`pebbles_crates$publicSetTransformation`(
                    AffineTransformation(
                        null, null, Vector3f(1.25f, 1.25f, 1.25f), null
                    )
                )
//                setTransformationUsingReflection(AffineTransformation(null, null, Vector3f(1.25f, 1.25f, 1.25f), null))
                CoroutineScope(Dispatchers.IO).launch {
                    val particlePacket = ParticleS2CPacket(
                        ParticleTypes.FIREWORK, false, x, y + 0.25, z, 0.05f, 0f, 0.05f, 0.15f, 15
                    )

                    val nearbyPlayers = player.serverWorld.getPlayers { distanceTo(player) < 32.0 }

                    for (player in nearbyPlayers) {
                        player.networkHandler.sendPacket(particlePacket)
                    }
                }

                val soundIdentifier = Identifier(ConfigHandler.config.rewardSound.sound)
                val soundEvent = SoundEvent.of(soundIdentifier)
                player.serverWorld.playSound(
                    null,
                    pos,
                    soundEvent,
                    SoundCategory.BLOCKS,
                    ConfigHandler.config.rewardSound.volume,
                    ConfigHandler.config.rewardSound.pitch
                )

            }
            if (lastPrizeTick == 30) {
                discard()
                val prize = prizes[currentPrize]
                prize.onReward(player, crate)
            }
        }

        ticks++
    }

//    private fun setStack(stack: ItemStack) {
//        try {
//            val method = this.javaClass.getMethod("pebbles_crates\$publicSetStack", ItemStack::class.java)
//            method.invoke(this, stack)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }

//    private fun setTransformationUsingReflection(transformation: AffineTransformation) {
//        try {
//            val method =
//                this.javaClass.getMethod("pebbles_crates\$publicSetTransformation", AffineTransformation::class.java)
//            method.invoke(this, transformation)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    fun setTransformationModeUsingReflection(mode: ModelTransformationMode) {
//        try {
//            val method = this.javaClass.getMethod(
//                "pebbles_items\$publicSetTransformationMode", ModelTransformationMode::class.java
//            )
//            method.invoke(this, mode)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
}