/*
 * GPLv3 License
 *  Copyright (c) aggregator by waicool20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.waicool20.aggregator

import org.slf4j.LoggerFactory
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val aggregator = Aggregator(10, Paths.get("aggregator.state"))
    aggregator.start()
}

data class Torrent(val name: String, val source: String) {
    fun isMagnet() = source.startsWith("magnet", true)
    fun isTorrent() = !isMagnet()
}

val sources = listOf<TorrentSource>(
        HorribleSubsRss(),
        GJMRss(),
        AniDexRss()
)

class Aggregator(val scanInterval: Long, val stateFile: Path? = null, val outputDir: Path = Paths.get("torrents")) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val timer = Timer()
    private val converter by lazy {
        MagnetToTorrentConverter(outputDir = outputDir).apply {
            stateFile?.let {
                if (Files.exists(it)) this.loadState(it)
            }
        }
    }
    private val downloader by lazy { TorrentDownloader(outputDir = outputDir) }
    private val history = mutableListOf<String>()
    var isRunning = false
        private set

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.debug("Saving libtorrent state")
            stateFile?.let { converter.saveState(it) }
            logger.debug("Saving libtorrent state complete!")
            converter.dispose()
        })
    }

    private val task = object : TimerTask() {
        override fun run() {
            sources.flatMap { it.torrents }.filterNot { history.contains(it.name) }.parallelForEach({
                when {
                    it.isMagnet() -> {
                        logger.debug("Converting ${it.name} from magnet to torrent")
                        measureTimeMillis { converter.convert(it.source) }
                                .let { time -> logger.debug("Converting ${it.name} to torrent complete! Took $time ms") }
                    }
                    it.isTorrent() -> {
                        logger.debug("Downloading torrent ${it.name} directly!")
                        measureTimeMillis { downloader.download(URL(it.source), "${it.name}.torrent") }
                                .let { time -> logger.debug("Downloading torrent ${it.name} complete! Took $time ms") }
                    }
                }
                history.add(it.name)
            })
            LocalDateTime.now().plusMinutes(scanInterval).let {
                logger.debug("Next check at ${it.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
            }
        }
    }

    fun start() {
        if (!isRunning) {
            timer.schedule(task, 0, TimeUnit.MINUTES.toMillis(scanInterval))
            isRunning = true
        }
    }

    fun stop() {
        if (isRunning) {
            task.cancel()
            isRunning = false
        }
    }
}
