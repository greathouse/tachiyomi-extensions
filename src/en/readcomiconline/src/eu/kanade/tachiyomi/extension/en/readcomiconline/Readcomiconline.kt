package eu.kanade.tachiyomi.extension.en.readcomiconline

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.util.Log
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.*

class Readcomiconline : ConfigurableSource, ParsedHttpSource() {

    override val name = "ReadComicOnline (kofspades)"

    override val baseUrl = "https://readcomiconline.li"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun headersBuilder() = Headers.Builder().apply {
        Log.e("ROBERT", "ROBERT - headersBuilder")
        add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.53 Safari/537.36")
    }

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override fun popularMangaSelector() = ".list-comic > .item > a:first-child"

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/ComicList/MostPopular?page=$page", headers)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/ComicList/LatestUpdate?page=$page", headers)
    }

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            setUrlWithoutDomain(element.attr("abs:href"))
            title = element.text()
            thumbnail_url = element.selectFirst("img")!!.attr("abs:src")
        }
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun popularMangaNextPageSelector() = "li > a:contains(Next)"

    override fun latestUpdatesNextPageSelector(): String = "ul.pager > li > a:contains(Next)"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val form = FormBody.Builder().apply {
            add("comicName", query)

            for (filter in if (filters.isEmpty()) getFilterList() else filters) {
                when (filter) {
                    is Status -> add("status", arrayOf("", "Completed", "Ongoing")[filter.state])
                    is GenreList -> filter.state.forEach { genre -> add("genres", genre.state.toString()) }
                }
            }
        }
        return POST("$baseUrl/AdvanceSearch", headers, form.build())
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun searchMangaNextPageSelector(): String? = null

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select("div.barContent").first()

        val manga = SManga.create()
        manga.artist = infoElement.select("p:has(span:contains(Artist:)) > a").first()?.text()
        manga.author = infoElement.select("p:has(span:contains(Writer:)) > a").first()?.text()
        manga.genre = infoElement.select("p:has(span:contains(Genres:)) > *:gt(0)").text()
        manga.description = infoElement.select("p:has(span:contains(Summary:)) ~ p").text()
        manga.status = infoElement.select("p:has(span:contains(Status:))").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = document.select(".rightBox:eq(0) img").first()?.absUrl("src")
        return manga
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return super.fetchPageList(chapter)
    }

    override fun fetchImageUrl(page: Page): Observable<String> {
        return super.fetchImageUrl(page)
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> SManga.ONGOING
        status.contains("Completed") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = "table.listing tr:gt(1)"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a").first()

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("td:eq(1)").first()?.text()?.let {
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(it)?.time ?: 0L
        } ?: 0
        return chapter
    }

    override fun pageListRequest(chapter: SChapter) = GET(baseUrl + chapter.url + "&quality=${qualitypref()}", headers)

    override fun pageListParse(document: Document): List<Page> {
        Log.e("ROBERT", "ROBERT - pageListParse")
        val script = document.selectFirst("script:containsData(lstImages.push)")?.data()
            ?: return emptyList()

        val mapIndexed = CHAPTER_IMAGES_REGEX.findAll(script).toList()
            .mapIndexed { i, match -> Page(i, "", match.groupValues[1]) }
        Log.e("ROBERT", "ROBERT - imageRequest::Found Script: $mapIndexed")
        return mapIndexed
    }

    override fun imageUrlParse(document: Document) = ""

    override fun imageRequest(page: Page): Request {
        Log.e("ROBERT", "ROBERT - imageRequest")
//        if (page.imageUrl!!.startsWith("https")) {
//            return super.imageRequest(page)
//        }

        val scrambledUrl = page.imageUrl!!
//        val containsS0 = scrambledUrl.contains("=s0")
//        val imagePathResult = runCatching {
//            scrambledUrl
//                .substring(0, scrambledUrl.length - (if (containsS0) 3 else 6))
//                .let { it.substring(4, 21) + it.substring(24) }
//                .let { it.substring(0, it.length - 6) + it[it.length - 2] + it[it.length - 1] }
//                .let { Base64.decode(it, Base64.DEFAULT).toString(Charsets.UTF_8) }
//                .let { it.substring(0, 11) + it.substring(14) }
//                .let { it.substring(0, it.length - 2) + if (containsS0) "=s0" else "=s1600" }
//        }
//
//        val imagePath = imagePathResult.getOrNull()
//            ?: throw Exception("Failed to decrypt the image URL.")

        val imagePath = decode(scrambledUrl)
        Log.e("ROBERT", "ROBERT - " +  imagePath);
        return GET(imagePath)
    }

    fun decode(scrambled: String): String {
        val w = scrambled.replace("_x236".toRegex(), "d")
            .replace("_x945".toRegex(), "g")
        if (w.indexOf("https") != 0) {
            var m = w
            val x = m.substring(m.indexOf("?"))
            m = if (m.indexOf("=s0?") > 0) m.substring(0, m.indexOf("=s0?")) else m.substring(0, m.indexOf("=s1600?"))
            m = m.substring(4, 22) + m.substring(25)
            m = m.substring(0, m.length - 6) + m[m.length - 2] + m[m.length - 1]
            m = Uri.decode(String(Base64.decode(m, Base64.DEFAULT)))
            m = m.substring(0, 13) + m.substring(17)
            m = if (w.indexOf("=s0") > 0) m.substring(0, m.length - 2) + "=s0" else m.substring(0, m.length - 2) + "=s1600"
            m += x
//            println("https://2.bp.blogspot.com/$m")
            return "https://2.bp.blogspot.com/$m"
        }
        return w
    }

    private class Status : Filter.TriState("Completed")
    private class Genre(name: String) : Filter.TriState(name)
    private class GenreList(genres: List<Genre>) : Filter.Group<Genre>("Genres", genres)

    override fun getFilterList() = FilterList(
        Status(),
        GenreList(getGenreList())
    )

    // $("select[name=\"genres\"]").map((i,el) => `Genre("${$(el).next().text().trim()}", ${i})`).get().join(',\n')
    // on https://readcomiconline.li/AdvanceSearch
    private fun getGenreList() = listOf(
        Genre("Action"),
        Genre("Adventure"),
        Genre("Anthology"),
        Genre("Anthropomorphic"),
        Genre("Biography"),
        Genre("Children"),
        Genre("Comedy"),
        Genre("Crime"),
        Genre("Drama"),
        Genre("Family"),
        Genre("Fantasy"),
        Genre("Fighting"),
        Genre("Graphic Novels"),
        Genre("Historical"),
        Genre("Horror"),
        Genre("Leading Ladies"),
        Genre("LGBTQ"),
        Genre("Literature"),
        Genre("Manga"),
        Genre("Martial Arts"),
        Genre("Mature"),
        Genre("Military"),
        Genre("Movies & TV"),
        Genre("Music"),
        Genre("Mystery"),
        Genre("Mythology"),
        Genre("Personal"),
        Genre("Political"),
        Genre("Post-Apocalyptic"),
        Genre("Psychological"),
        Genre("Pulp"),
        Genre("Religious"),
        Genre("Robots"),
        Genre("Romance"),
        Genre("School Life"),
        Genre("Sci-Fi"),
        Genre("Slice of Life"),
        Genre("Sport"),
        Genre("Spy"),
        Genre("Superhero"),
        Genre("Supernatural"),
        Genre("Suspense"),
        Genre("Thriller"),
        Genre("Vampires"),
        Genre("Video Games"),
        Genre("War"),
        Genre("Western"),
        Genre("Zombies")
    )
    // Preferences Code

    override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) {
        val qualitypref = androidx.preference.ListPreference(screen.context).apply {
            key = QUALITY_PREF_Title
            title = QUALITY_PREF_Title
            entries = arrayOf("High Quality", "Low Quality")
            entryValues = arrayOf("hq", "lq")
            summary = "%s"

            setOnPreferenceChangeListener { _, newValue ->
                val selected = newValue as String
                val index = this.findIndexOfValue(selected)
                val entry = entryValues[index] as String
                preferences.edit().putString(QUALITY_PREF, entry).commit()
            }
        }
        screen.addPreference(qualitypref)
    }

    private fun qualitypref() = preferences.getString(QUALITY_PREF, "hq")

    companion object {
        private const val QUALITY_PREF_Title = "Image Quality Selector"
        private const val QUALITY_PREF = "qualitypref"

        private val CHAPTER_IMAGES_REGEX = "lstImages\\.push\\(\'(.*)\'\\)".toRegex()
    }
}
