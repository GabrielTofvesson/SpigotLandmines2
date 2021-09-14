package dev.w1zzrd.spigot.landmines2

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

private val LANDMINE_COMPARATOR = Comparator<LandmineData> { a, b -> a.location.compareTo(b.location) }
private const val GC_TRIGGER = 10000

private const val DEFAULT_EXPLOSION_STRENGTH = 2.0

private const val PATH_LANDMINES = "landmines"
private const val PATH_EXPLOSION_STRENGTH = "explosionStrength"
private const val PATH_PLAYER_REGISTRY = "players"
private const val PATH_WORLD_REGISTRY = "worlds"
private const val PATH_GC_TRIGGER = "gcTrigger"

private val threadLocalBuffer = ThreadLocal.withInitial { ByteBuffer.allocate(16) }

class LandmineManager(
    private val plugin: Plugin,
    private val landmineData: YamlFile
): Listener {
    private var landmines: SortedList<LandmineData> = SortedList.create(comparator = LANDMINE_COMPARATOR)
    private lateinit var players: MutableList<String>
    private lateinit var worlds: MutableList<String>
    private var explosionStrength = DEFAULT_EXPLOSION_STRENGTH
    private var damageTracker: LandmineData? = null

    private var isEnabled = false

    private val conf: FileConfiguration
        get() = plugin.config



    fun onEnable() {
        // Ensure manager cannot be double-enabled
        if (isEnabled) throw IllegalStateException("Manager already enabled!")
        isEnabled = true

        reload()

        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun onDisable() {
        if (!isEnabled)
            throw IllegalStateException("Manager not enabled!")

        HandlerList.unregisterAll(this)

        save()

        // Mark manager as disabled *after* disable routine completes
        isEnabled = false
    }

    fun reload() {
        val shouldGC = landmines.size >= conf.getInt(PATH_GC_TRIGGER, GC_TRIGGER)

        landmineData.reload()
        players = landmineData.getStringList(PATH_PLAYER_REGISTRY)
        worlds = landmineData.getStringList(PATH_WORLD_REGISTRY)


        landmines = if (landmineData.contains(PATH_LANDMINES)) {
            val stringList = landmineData.getStringList(PATH_LANDMINES)
            val decodeBuffer = ByteBuffer.allocate(16)
            SortedList.create(
                LANDMINE_COMPARATOR,
                stringList.mapTo(ArrayList(stringList.size)) { entry ->
                    LandmineData(entry) { index ->
                        decodeBuffer.position(0)
                        Base64.getDecoder().decode(worlds[index.toInt()].toByteArray(Charsets.ISO_8859_1), decodeBuffer.array())
                        UUID(decodeBuffer.long, decodeBuffer.long)
                    }
                }
            )
        }
        else SortedList.create(comparator = LANDMINE_COMPARATOR)

        explosionStrength = conf.getDouble(PATH_EXPLOSION_STRENGTH, DEFAULT_EXPLOSION_STRENGTH)

        // If we anticipate a very large amount of garbage, trigger gc manually
        if (shouldGC)
            Runtime.getRuntime().gc()
    }

    private fun save() {
        landmineData.set(PATH_LANDMINES, landmines.map { it.toPackedString(this::getWorldIndex) })
        landmineData.set(PATH_PLAYER_REGISTRY, players)
        landmineData.set(PATH_WORLD_REGISTRY, worlds)
        landmineData.save()

        conf.set(PATH_EXPLOSION_STRENGTH, explosionStrength)
    }

    private fun placeMine(player: OfflinePlayer, location: Location): Boolean {
        val serializable = location.serializable
        val index = landmines.binarySearch(comparison = serializable.locationPredicate)
        if (index >= 0) return false

        landmines.add(LandmineData(serializable, getPlayerNameIndex(player)))
        return true
    }

    private fun findPlayerName(index: UInt): String? {
        if (index.toInt() !in 0 until players.size) return null
        val buffer = threadLocalBuffer.get()
        buffer.position(0)
        Base64.getDecoder().decode(players[index.toInt()].toByteArray(Charsets.ISO_8859_1), buffer.array())

        return plugin.server.getPlayer(UUID(buffer.long, buffer.long))?.name
    }

    private fun getWorldIndex(world: UUID) = worlds.getIncrementalIndex(world)
    private fun getPlayerNameIndex(player: OfflinePlayer) = players.getIncrementalIndex(player.uniqueId)
    private fun MutableList<String>.getIncrementalIndex(uuid: UUID): UInt {
        val buffer = threadLocalBuffer.get()
        buffer.position(0)
        buffer.putLong(uuid.mostSignificantBits)
        buffer.putLong(uuid.leastSignificantBits)

        val b64String = Base64.getEncoder().withoutPadding().encodeToString(buffer.array())

        val index = indexOf(b64String)
        if (index >= 0) return index.toUInt()

        add(b64String)
        return (size - 1).toUInt()
    }


    @EventHandler
    fun onPlayerMove(moveEvent: PlayerMoveEvent) {
        val index = landmines.binarySearch(comparison = (moveEvent.to ?: return).serializable.locationPredicate)
        if (index >= 0) {
            val landmine = landmines.removeAt(index)

            damageTracker = landmine
            moveEvent.to!!.world!!.createExplosion(
                landmine.location.getBukkitLocation(moveEvent.player.server),
                explosionStrength.toFloat(),
                false,
                false,
                null
            )
            damageTracker = null
        }
    }

    @EventHandler
    fun onPlayerDeath(deathEvent: PlayerDeathEvent) {
        if (damageTracker != null) {
            val placer = findPlayerName(damageTracker!!.placer)
            deathEvent.deathMessage =
                if (placer == null) "${deathEvent.entity.name} stepped on a landmine"
                else "${deathEvent.entity.name} stepped on a landmine placed by $placer"
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerPlaceMine(placeEvent: BlockPlaceEvent) {
        if (placeEvent.blockPlaced.type == Material.STONE_PRESSURE_PLATE && !placeEvent.isCancelled) {
            if (placeMine(placeEvent.player, placeEvent.blockPlaced.location)) {
                --placeEvent.player.inventory.getItem(placeEvent.hand).amount
            }

            placeEvent.isCancelled = true
        }
    }
}