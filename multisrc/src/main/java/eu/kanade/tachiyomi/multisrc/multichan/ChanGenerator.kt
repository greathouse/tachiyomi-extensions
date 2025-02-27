package eu.kanade.tachiyomi.multisrc.multichan

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class ChanGenerator : ThemeSourceGenerator {

    override val themePkg = "multichan"

    override val themeClass = "MultiChan"

    override val baseVersionCode: Int = 1

    override val sources = listOf(
        SingleLang("MangaChan", "https://manga-chan.me", "ru", overrideVersionCode = 14),
        SingleLang("HenChan", "https://y.hentaichan.live", "ru", isNsfw = true, overrideVersionCode = 35),
        SingleLang("YaoiChan", "https://yaoi-chan.me", "ru", isNsfw = true, overrideVersionCode = 4)
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ChanGenerator().createAll()
        }
    }
}
