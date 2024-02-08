package tech.sethi.pebbles.crates.particles

import dev.architectury.event.events.common.TickEvent
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object SparkleParticle {
    private val assignedCrates = ConcurrentHashMap<BlockPos, World>()
    private val addQueue = LinkedList<Pair<BlockPos, World>>()
    private val removeQueue = LinkedList<BlockPos>()

    private const val particles = 3
    private var sparkleTimer = 0

    init {
        TickEvent.SERVER_PRE.register {
            sparkleTimer++
            processQueues()
            if (sparkleTimer % 40 == 0) {
                sparkleTimer = 0
                assignedCrates.forEach { crate ->
                    sparkleParticleEffect(crate.key, crate.value)
                }
            }

        }
    }

    private fun sparkleParticleEffect(pos: BlockPos, world: World) {
        for (i in 0 until particles) {
            val random = world.random
            val x = pos.x + 0.5
            val y = pos.y + 0.75
            val z = pos.z + 0.5
            // Range is -0.75 to 0.75
            val dx = (random.nextDouble() - 0.5) * 2.0 * 0.75
            val dy = (random.nextDouble() - 0.5) * 2.0 * 0.75
            val dz = (random.nextDouble() - 0.5) * 2.0 * 0.75

            val particlePacket = ParticleS2CPacket(
                ParticleTypes.END_ROD, false, x + dx, y + dy, z + dz, 0.0f, 0.0f, 0.0f, 0.0f, 1
            )

            // player within 16 blocks from the crate
            (world as ServerWorld).getPlayers { it.squaredDistanceTo(x, y, z) < 256.0 }
                .forEach { it.networkHandler.sendPacket(particlePacket) }
        }
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