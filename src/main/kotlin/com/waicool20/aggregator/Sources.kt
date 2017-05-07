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

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.slf4j.LoggerFactory
import java.net.URL

interface TorrentSource {
    val torrents: List<Torrent>
}

open class RssTorrentSource(val url: URL) : TorrentSource {
    private val logger = LoggerFactory.getLogger(javaClass)
    val rawFeed by lazy {
        logger.debug("Reading feed for $url")
        SyndFeedInput().build(XmlReader(url)).apply {
            logger.debug("Finished reading feed for $url")
        }
    }

    override val torrents by lazy { rawFeed.entries.map { Torrent(it.title, it.link) } }
}

class HorribleSubsRss : RssTorrentSource(URL("http://horriblesubs.info/rss.php?res=all"))
class GJMRss : RssTorrentSource(URL("https://www.goodjobmedia.com/temp-rss.php"))
class AniDexRss : RssTorrentSource(URL("https://anidex.info/rss/?page=torrents&batch=1&raw=1&hentai=0&reencode=1&filter_mode=1&lang_id=1&group_id=0"))
