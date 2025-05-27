package me.thatonedevil.redisbungeeexpansion

import com.google.common.io.ByteArrayDataOutput
import com.google.common.io.ByteStreams
import me.clip.placeholderapi.expansion.Cacheable
import me.clip.placeholderapi.expansion.Configurable
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.clip.placeholderapi.expansion.Taskable
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.io.ByteArrayInputStream
import java.io.DataInputStream

class RedisBungeeExpansion : PlaceholderExpansion(), PluginMessageListener, Taskable, Cacheable, Configurable {

    private val servers: MutableMap<String, Int> = mutableMapOf()
    private var total: Int = 0
    private var task: BukkitTask? = null
    private val channel = "legacy:redisbungee"
    private var fetchInterval = 60
    private var registered = false

    init {
        if (!registered) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(placeholderAPI, channel)
            Bukkit.getMessenger().registerIncomingPluginChannel(placeholderAPI, channel, this)
            registered = true
        }
    }

    override fun register(): Boolean {
        val srvs = getStringList("tracked_servers")
        if (srvs.isNotEmpty()) {
            srvs.forEach { servers[it] = 0 }
        }
        return super.register()
    }

    override fun getIdentifier(): String = "redisbungee"

    override fun canRegister(): Boolean = true

    override fun getAuthor(): String = "thatonedevil"

    override fun getVersion(): String = "3.0.0"

    override fun getDefaults(): Map<String, Any> = mapOf(
        "check_interval" to 30,
        "tracked_servers" to listOf("Hub", "LifeSteal")
    )

    private fun getPlayers(server: String) {
        if (Bukkit.getOnlinePlayers().isEmpty()) return
        val out: ByteArrayDataOutput = ByteStreams.newDataOutput()
        try {
            out.writeUTF("PlayerCount")
            out.writeUTF(server)
            Bukkit.getOnlinePlayers().first().sendPluginMessage(placeholderAPI, channel, out.toByteArray())
        } catch (_: Exception) {
        }
    }

    override fun onPlaceholderRequest(player: Player?, identifier: String): String? {
        return when {
            identifier.equals("total", ignoreCase = true) || identifier.equals("all", ignoreCase = true) -> total.toString()
            servers.isEmpty() -> {
                servers[identifier] = 0
                "0"
            }
            else -> {
                servers.entries.find { it.key.equals(identifier, ignoreCase = true) }?.value?.toString()
                    ?: run {
                        servers[identifier] = 0
                        null
                    }
            }
        }
    }

    override fun start() {
        task = object : BukkitRunnable() {
            override fun run() {
                if (servers.isEmpty()) {
                    getPlayers("ALL")
                    return
                }
                getPlayers("LifeSteal")
                getPlayers("ALL")
            }
        }.runTaskTimer(placeholderAPI, 100L, 20L * fetchInterval)
    }

    override fun stop() {
        try {
            task?.cancel()
        } catch (_: Exception) { }
        task = null
    }

    override fun clear() {
        servers.clear()
        if (registered) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(placeholderAPI, channel)
            Bukkit.getMessenger().unregisterIncomingPluginChannel(placeholderAPI, channel, this)
            registered = false
        }
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != this.channel) return

        val input = DataInputStream(ByteArrayInputStream(message))
        try {
            val subChannel = input.readUTF()
            when (subChannel) {
                "PlayerCount" -> {
                    val server = input.readUTF()
                    if (input.available() > 0) {
                        val count = input.readInt()
                        if (server == "ALL") {
                            total = count
                        } else {
                            servers[server] = count
                        }
                    }
                }

                "GetServers" -> {
                    val serverList = input.readUTF().split(", ")
                    if (serverList.isEmpty()) return
                    serverList.forEach { server ->
                        if (server !in servers) servers[server] = 0
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

}
