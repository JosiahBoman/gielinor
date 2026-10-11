package org.gielinorcore.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.gielinorcore.cache.Cache
import org.gielinorcore.cache.store.DiskStore
import org.gielinorcore.game.World
import org.gielinorcore.script.PluginLoader
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists

data class ServerConfig(
    val port: Int = 43594,
    val cacheDir: Path = Path.of("data/cache"),
    val tickMillis: Long = 600,
)

fun main(args: Array<String>) {
    val config = ServerConfig(port = args.firstOrNull()?.toIntOrNull() ?: 43594)
    val world = World(config.tickMillis)

    val cache = if (config.cacheDir.resolve(DiskStore.DATA_FILE).exists()) {
        Cache(DiskStore.open(config.cacheDir)).also { println("cache: ${it.store.archives().size} archives from ${config.cacheDir}") }
    } else {
        println("cache: none at ${config.cacheDir} (see data/README.md); running without game data")
        null
    }

    val plugins = PluginLoader.loadAll(world)
    println("plugins: ${plugins.joinToString { it.id }}")

    val ticker = Executors.newSingleThreadScheduledExecutor { Thread(it, "game").apply { isDaemon = true } }
    ticker.scheduleAtFixedRate({ runCatching(world::tick).onFailure { it.printStackTrace() } }, 0, config.tickMillis, TimeUnit.MILLISECONDS)

    val boss = NioEventLoopGroup(1)
    val workers = NioEventLoopGroup()
    try {
        val channel = ServerBootstrap()
            .group(boss, workers)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    // DELTA P2/P3: handshake decoder -> login -> game packet codec -> GameQueue
                    ch.pipeline().addLast(HandshakeHandler(world))
                }
            })
            .bind(config.port).sync().channel()
        println("listening on ${config.port}")
        channel.closeFuture().sync()
    } finally {
        boss.shutdownGracefully()
        workers.shutdownGracefully()
        ticker.shutdownNow()
        cache?.close()
    }
}
