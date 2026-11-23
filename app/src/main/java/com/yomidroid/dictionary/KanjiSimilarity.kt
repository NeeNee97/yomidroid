package com.yomidroid.dictionary

import android.content.Context
import org.json.JSONObject

/**
 * Lookup table of visually similar kanji, used to suggest replacements when
 * OCR misreads a character. Backed by Lars Yencken's jōyō kanji confusion
 * dataset (stroke-edit-distance + Yeh & Li radical similarity, blended).
 *
 * Asset: assets/kanji_similarity/yencken_top.json — `{kanji: "neighbors..."}`
 * License: CC-BY 3.0 (Yencken, 2010).
 */
object KanjiSimilarity {

    private const val ASSET_PATH = "kanji_similarity/yencken_top.json"

    @Volatile private var table: Map<Char, String>? = null

    fun ensureLoaded(context: Context) {
        if (table != null) return
        synchronized(this) {
            if (table != null) return
            val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            val map = HashMap<Char, String>(obj.length())
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (k.length == 1) map[k[0]] = obj.getString(k)
            }
            table = map
        }
    }

    /**
     * Top visually similar kanji for [ch], or an empty list when [ch] is not
     * in the table (e.g. non-jōyō, kana, punctuation).
     */
    fun neighbors(ch: Char): List<Char> {
        val kana = kanaNeighbors(ch)
        if (kana.isNotEmpty()) return kana
        val t = table ?: return emptyList()
        val s = t[ch] ?: return emptyList()
        return s.toList()
    }

    // Kana (and a few kanji/symbols) that OCR commonly confuses, including kana whose
    // strokes are split apart. Each string is one group of mutual lookalikes.
    private val kanaGroups = listOf(
        "ソンシツリノ", "ミニ二", "ハ八ヘ", "テラチ", "ヘへ", "リり", "ロ口", "カ力", "エ工", "ト卜",
        "タ夕", "ナ十", "コ二ニ", "ケけ", "サせ", "ヨヱ", "ワウ", "ヌス",
        "うらち", "こ二に", "たなに", "けはほ", "ぬめ", "ねれわ", "きさ", "いり", "のめ", "ふぶ"
    )

    private fun isKana(ch: Char) = ch in '\u3041'..'\u30FF'

    private fun kanaNeighbors(ch: Char): List<Char> {
        if (!isKana(ch)) return emptyList()
        val out = LinkedHashSet<Char>()
        for (group in kanaGroups) {
            if (group.indexOf(ch) >= 0) {
                for (c in group) if (c != ch) out.add(c)
            }
        }
        return out.toList()
    }
}
