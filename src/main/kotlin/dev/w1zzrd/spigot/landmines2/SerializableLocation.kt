package dev.w1zzrd.spigot.landmines2

import org.bukkit.Location
import org.bukkit.Server
import java.nio.ByteBuffer
import java.util.*

// Low-overhead, thread-safe serialization
private val threadLocalBuffer = ThreadLocal.withInitial { ByteBuffer.allocate(31) }


fun parseLocation(encoded: String): SerializableLocation {
    val buffer = threadLocalBuffer.get()
    buffer.position(0)

    Base64.getDecoder().decode(encoded.toByteArray(Charsets.ISO_8859_1), buffer.array())

    return SerializableLocation(UUID(buffer.long, buffer.long), buffer.packedInt, buffer.packedInt, buffer.packedInt)
}

fun unpackLocation(buffer: ByteBuffer, worlds: (UInt) -> UUID): SerializableLocation {
    return SerializableLocation(worlds(buffer.packedUInt), buffer.packedInt, buffer.packedInt, buffer.packedInt)
}

val Location.serializable: SerializableLocation
    get() = SerializableLocation(world!!.uid, blockX, blockY, blockZ)

class SerializableLocation(val world: UUID, val x: Int, val y: Int, val z: Int): Comparable<SerializableLocation> {
    fun getBukkitLocation(server: Server) = Location(server.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())

    override fun compareTo(other: SerializableLocation) =
        compareByOrder(
            other,
            SerializableLocation::world,
            SerializableLocation::x,
            SerializableLocation::y,
            SerializableLocation::z
        )

    override fun equals(other: Any?) =
        other is SerializableLocation &&
                world == other.world &&
                x == other.x &&
                y == other.y &&
                z == other.z

    override fun toString(): String {
        val buffer = threadLocalBuffer.get()
        buffer.position(0)

        buffer.putLong(world.mostSignificantBits)
        buffer.putLong(world.leastSignificantBits)
        buffer.writePacked(x)
        buffer.writePacked(y)
        buffer.writePacked(z)

        return Base64.getEncoder().withoutPadding().encodeToString(Arrays.copyOf(buffer.array(), buffer.position()))
    }

    fun writePacked(buffer: ByteBuffer, worlds: (UUID) -> UInt) {
        buffer.writePacked(worlds(world))
        buffer.writePacked(x)
        buffer.writePacked(y)
        buffer.writePacked(z)
    }

    override fun hashCode(): Int {
        var result = world.hashCode()
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }
}