package tech.sethi.pebbles.crates.particles

import dev.architectury.event.events.common.TickEvent
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

object SpiralParticle {
    private val assignedCrates = ConcurrentHashMap<BlockPos, World>()
    private val addQueue = LinkedList<Pair<BlockPos, World>>()
    private val removeQueue = LinkedList<BlockPos>()
    private var stepX = 1

    private const val particles = 2
    private const val particlesPerRotation = 16
    private const val radius = 1

    private var checkCrateLocationTick = 0

    init {
        TickEvent.SERVER_PRE.register {
            processQueues()
            assignedCrates.forEach { crate ->
                circlingParticleEffect(crate.key, crate.value)
            }

            updateTimers()

            checkCrateLocationTick++
        }
    }

    fun circlingParticleEffect(pos: BlockPos, world: World) {
        for (stepY in 0 until 60 step (120 / particles)) {
            val dx = -(cos(((stepX + stepY) / particlesPerRotation.toDouble()) * Math.PI * 2)) * radius
            val dy = stepY / particlesPerRotation.toDouble() / 2.0
            val dz = -(sin(((stepX + stepY) / particlesPerRotation.toDouble()) * Math.PI * 2)) * radius

            val x = pos.x + 0.5 + dx
            val y = pos.y + 1.5 + dy
            val z = pos.z + 0.5 + dz

            val particlePacket = ParticleS2CPacket(
                ParticleTypes.FIREWORK, false, x, y, z, 0.0f, 0.0f, 0.0f, 0.0f, 1
            )

            // player within 16 blocks from the crate
            (world as ServerWorld).getPlayers { it.squaredDistanceTo(x, y, z) < 256.0 }
                .forEach { it.networkHandler.sendPacket(particlePacket) }
        }
    }

    private fun updateTimers() {
        stepX++
    }

    private fun processQueues() {
        // Process additions
        while (addQueue.isNotEmpty()) {
            val (pos, world) = addQueue.poll()
            assignedCrates[pos] = world
        }

        // Process removals
        while (removeQueue.isNotEmpty()) {
            val pos = removeQueue.poll()
            assignedCrates.remove(pos)
        }
    }

    fun queueAdd(pos: BlockPos, world: World) {
        addQueue.add(pos to world)
    }

    fun queueRemove(pos: BlockPos) {
        removeQueue.add(pos)
    }

    fun clearAll() {
        assignedCrates.clear()
    }
}