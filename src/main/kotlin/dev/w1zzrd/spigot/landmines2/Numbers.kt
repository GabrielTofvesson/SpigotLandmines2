package dev.w1zzrd.spigot.landmines2

import java.nio.ByteBuffer

private val threadLocalBuffer = ThreadLocal.withInitial { ByteBuffer.allocateDirect(9) }

private fun ByteBuffer.putUByte(value: ULong) = put((value and 0xFFUL).toByte())
private fun String.parseHex(index: Int) = ((this[index * 2].digitToInt(16) shl 4) or (this[1 + index * 2].digitToInt(16))).toULong() and 0xFFUL

fun Long.interlace() = (this shr 63).toULong() xor (this shl 1).toULong()
fun ULong.deInterlace() = (this shr 1).toLong() xor ((this shl 63).toLong() shr 63)

fun ULong.toPacked(target: ByteBuffer): Int {
    if (this <= 240UL) {
        target.putUByte(this)
        return 1
    }
    else if (this <= 2287UL) {
        target.putUByte(((this - 240UL) shr 8) + 241UL)
        target.putUByte(this - 240UL)
        return 2
    }
    else if (this <= 67823UL) {
        target.putUByte(249UL)
        target.putUByte((this - 2288UL) shr 8)
        target.putUByte(this - 2288UL)
        return 3
    }
    else {
        var header = 255UL
        var match = 0x00FF_FFFF_FFFF_FFFFUL

        while (this <= match) {
            --header
            match = match shr 8
        }

        target.putUByte(header)
        for (i in 0 until (header - 247UL).toInt())
            target.putUByte(this shr (i shl 3))

        return (header - 247UL).toInt()
    }
}

fun ULong.toPackedString(): String {
    val buffer = threadLocalBuffer.get().position(0)
    val len = toPacked(buffer)

    val builder = StringBuilder(len * 2)
    for (i in 0 until len) {
        builder.append(((buffer[i].toInt() ushr 4) and 0xF).toString(16))
        builder.append((buffer[i].toInt() and 0xF).toString(16))
    }

    return builder.toString()
}

fun ByteBuffer.readPacked(): Pair<ULong, Int> {
    val header = get().toULong() and 0xFFUL
    if (header <= 240UL) return header to 1
    else if (header <= 248UL) return (240UL + ((header - 241UL) shl 8) + (get().toULong() and 0xFFUL)) to 2
    else if (header == 249UL) return (2288UL + ((get().toULong() and 0xFFUL) shl 8) + (get().toULong() and 0xFFUL)) to 3

    var res = (get().toULong() and 0xFFUL) or ((get().toULong() and 0xFFUL) shl 8) or ((get().toULong() and 0xFFUL) shl 16)

    for (cmp in 3 until (header - 247UL).toInt())
        res = res or ((get().toULong() and 0xFFUL) shl (cmp shl 3))

    return res to (header - 247UL).toInt()
}

fun String.readPacked(): Pair<ULong, Int> {
    val header = parseHex(0)
    if (header <= 240UL) return header to 1
    else if (header <= 248UL) return (240UL + ((header - 241UL) shl 8) + parseHex(1)) to 2
    else if (header == 249UL) return (2288UL + (parseHex(1) shl 8) + parseHex(2)) to 3

    var res = parseHex(1) or (parseHex(2) shl 8) or (parseHex(3) shl 16)

    for (cmp in 3 until (header - 247UL).toInt())
        res = res or (parseHex(cmp + 1) shl (cmp shl 3))

    return res to (header - 247UL).toInt()
}