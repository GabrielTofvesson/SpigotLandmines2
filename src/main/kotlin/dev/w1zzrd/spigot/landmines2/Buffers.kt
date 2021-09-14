package dev.w1zzrd.spigot.landmines2

import java.nio.ByteBuffer

fun ByteBuffer.writePacked(value: ULong) = value.toPacked(this)
fun ByteBuffer.writePacked(value: UInt) = writePacked(value.toULong())
fun ByteBuffer.writePacked(value: UShort) = writePacked(value.toULong())

fun ByteBuffer.writePacked(value: Long) = value.interlace().toPacked(this)
fun ByteBuffer.writePacked(value: Int) = writePacked(value.toLong())
fun ByteBuffer.writePacked(value: Short) = writePacked(value.toLong())

val ByteBuffer.packedULong: ULong get() = readPacked().first
val ByteBuffer.packedUInt: UInt get() = readPacked().first.toUInt()
val ByteBuffer.packedUShort: UShort get() = readPacked().first.toUShort()

val ByteBuffer.packedLong: Long get() = readPacked().first.deInterlace()
val ByteBuffer.packedInt: Int get() = readPacked().first.deInterlace().toInt()
val ByteBuffer.packedShort: Short get() = readPacked().first.deInterlace().toShort()