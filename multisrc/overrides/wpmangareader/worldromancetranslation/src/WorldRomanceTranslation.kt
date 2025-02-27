package eu.kanade.tachiyomi.extension.id.worldromancetranslation

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Headers
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class WorldRomanceTranslation : WPMangaReader(
    "World Romance Translation",
    "https://wrt.my.id",
    "id",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("id"))
) {
    override val projectPageString = "/project-wrt"

    override val hasProjectPage = true

    override fun headersBuilder(): Headers.Builder {
        return super.headersBuilder().add("Referer", baseUrl)
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val thumbnail = document.select(seriesThumbnailSelector)

        return super.mangaDetailsParse(document).apply {
            thumbnail_url = when {
                thumbnail.hasAttr("data-lazy-src") -> thumbnail.attr("abs:data-lazy-src") // lazyload
                thumbnail.hasAttr("data-src") -> thumbnail.attr("abs:data-src") // with javascript
                else -> thumbnail.attr("src")
            }
        }
    }
}
