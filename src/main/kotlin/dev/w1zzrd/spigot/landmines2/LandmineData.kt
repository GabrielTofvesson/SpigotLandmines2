package dev.w1zzrd.spigot.landmines2

import java.nio.ByteBuffer
import java.util.*

typealias LocationPredicate = (LandmineData) -> Int

val SerializableLocation.locationPredicate: LocationPredicate
    get() = { data -> data.location.compareTo(this) }

private val threadLocalBuffer = ThreadLocal.withInitial { ByteBuffer.allocate(45) }

private fun unpackData(data: String, worlds: (UInt) -> UUID): Pair<SerializableLocation, UInt> {
    val buffer = threadLocalBuffer.get()
    buffer.position(0)
    Base64.getDecoder().decode(data.toByteArray(Charsets.ISO_8859_1), buffer.array())

    val placer = buffer.packedUInt
    return unpackLocation(buffer, worlds) to placer
}

data class LandmineData(val location: SerializableLocation, val placer: UInt) {
    private constructor(pair: Pair<SerializableLocation, UInt>): this(pair.first, pair.second)
    constructor(landmineData: String, worlds: (UInt) -> UUID): this(unpackData(landmineData, worlds))

    override fun toString() = "${placer.toULong().toPackedString()}$location"
    fun toPackedString(worlds: (UUID) -> UInt): String {
        val buffer = threadLocalBuffer.get()
        buffer.position(0)

        buffer.writePacked(placer)
        location.writePacked(buffer, worlds)
        return Base64.getEncoder().withoutPadding().encodeToString(Arrays.copyOf(buffer.array(), buffer.position()))
    }
}