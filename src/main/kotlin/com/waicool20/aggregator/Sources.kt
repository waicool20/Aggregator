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

import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.ZoneId

interface TorrentSource {
    val torrents: List<Torrent>
    fun refresh()
}

open class RssTorrentSource(val url: URL) : TorrentSource {
    private val logger = LoggerFactory.getLogger(javaClass)

    var lastRawFeed: SyndFeed? = null

    protected fun rawFeed(): SyndFeed {
        logger.debug("Reading feed for $url")
        return SyndFeedInput().build(XmlReader(url)).apply {
            logger.debug("Finished reading feed for $url")
        }
    }

    override fun refresh() {
        lastRawFeed = rawFeed()
    }

    override val torrents: List<Torrent>
        get() {
            val feed = lastRawFeed ?: rawFeed().apply { lastRawFeed = this }
            return feed.entries.map { Torrent(it.title, it.link, it.publishedDate.toInstant().atZone(ZoneId.systemDefault())) }
        }
}

class HorribleSubsRss : RssTorrentSource(URL("http://horriblesubs.info/rss.php?res=all"))
class GJMRss : RssTorrentSource(URL("https://www.goodjobmedia.com/temp-rss.php"))
class AniDexRss : RssTorrentSource(URL("https://anidex.info/rss/?filter_mode=1&lang_id=1&group_id=0"))
class TokyoToshoRss : RssTorrentSource(URL("https://www.tokyotosho.info/rss.php?filter=1"))
class DeadFishRss : RssTorrentSource(URL("https://www.acgnx.se/rss-user-30.xml")) {
    override val torrents: List<Torrent>
        get() {
            val feed = lastRawFeed ?: rawFeed().apply { lastRawFeed = this }
            return feed.entries.mapNotNull {
                val date = it.publishedDate.time.div(1000)
                val hash = it.link.replace("https://www.acgnx.se/show-", "").replace(".html", "")
                Torrent(it.title, "https://www.acgnx.se/down.php?date=$date&hash=$hash", it.publishedDate.toInstant().atZone(ZoneId.systemDefault()))
            }
        }
}
