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

import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentInfo
import org.slf4j.LoggerFactory
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer

class MagnetToTorrentConverter(val stateFile: Path? = null, val outputDir: Path) {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        initializeLibTorrent()
    }

    val session: SessionManager = SessionManager().apply {
        start()
        logger.debug("Initializing new session")
        val signal = CountDownLatch(1)
        timer(period = 1000, action = {
            stats().dhtNodes().let {
                if (it >= 10) {
                    logger.debug("DHT now contains 10 nodes!")
                    signal.countDown()
                    this.cancel()
                    stateFile?.let { saveState(it) }
                }
            }
        })
        logger.debug("Trying to wait for 10 nodes in DHT for 10s")
        if (!signal.await(10, TimeUnit.SECONDS)) {
            logger.debug("DHT contains less than 10 nodes after waiting for 10s, source resolving might be slow!")
        }
    }


    fun convert(magnet: String) {
        session.fetchMagnet(magnet, 30000).let {
            val info = TorrentInfo.bdecode(it)
            Files.createDirectories(outputDir)
            Files.write(outputDir.resolve("${info.name()}.torrent"), it)
        }
    }

    fun dispose() = session.stop()

    fun loadState(path: Path) {
        session.loadState(Files.readAllBytes(path))
    }

    fun saveState(path: Path) = Files.write(path, session.saveState())

    private fun initializeLibTorrent() {
        val libraryFile = "libjlibtorrent${OS.libraryExtention}"
        if (!System.getProperty("java.library.path").contains(libraryFile)) {
            val tmp = Files.createTempDirectory("libjlibtorrent")
            val tmpLib = tmp.resolve(libraryFile)
            val arch = if (OS.is64Bit()) "x86_64" else "x86"
            Files.copy(ClassLoader.getSystemClassLoader().getResourceAsStream("lib/$arch/${tmpLib.fileName}"), tmpLib)
            SystemUtils.loadLibrary(tmpLib)
            logger.debug("Loaded libtorrent from $tmpLib")
            Runtime.getRuntime().addShutdownHook(Thread {
                tmp.toFile().deleteRecursively()
            })
        }
    }
}

class TorrentDownloader(val outputDir: Path) {
    fun download(url: URL, filename: String) {
        with (url.openConnection()) {
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
            outputDir.resolve(filename).toFile().outputStream().channel.transferFrom(Channels.newChannel(getInputStream()), 0, Long.MAX_VALUE)
        }
    }
}
