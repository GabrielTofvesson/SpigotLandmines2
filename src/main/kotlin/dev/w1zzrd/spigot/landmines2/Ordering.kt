package dev.w1zzrd.spigot.landmines2

import kotlin.reflect.KProperty1

fun <T> T.compareByOrder(other: T, vararg comparables: KProperty1<T, Comparable<*>>): Int where T: Comparable<T> {
    for (comparable in comparables) {
        @Suppress("UNCHECKED_CAST")
        val result = (comparable(this) as Comparable<Any?>).compareTo(comparable(other))

        if (result != 0)
            return result
    }

    return 0
}