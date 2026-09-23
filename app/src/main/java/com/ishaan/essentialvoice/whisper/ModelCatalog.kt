package com.ishaan.essentialvoice.whisper

import android.content.Context
import java.io.File

/**
 * One downloadable file: the pair of a name and the exact size it must end up.
 *
 * The size is not decoration. [ModelDownloader] resumes with a Range request
 * and treats "the file is this many bytes" as the only proof a download
 * finished, so a wrong number here is a model that can never install.
 */
data class ModelVariant(val fileName: String, val bytes: Long) {

    val sizeMb: Int get() = ((bytes + 500_000) / 1_000_000).toInt()

    val url: String get() = "${ModelCatalog.BASE_URL}$fileName"

    fun file(context: Context): File = File(ModelCatalog.dir(context), fileName)

    fun isInstalled(context: Context): Boolean {
        val f = file(context)
        return f.isFile && f.length() == bytes
    }
}

/**
 * Personal fork modification (2026-09-23): add multilingual speech-to-English
 * modes alongside the original English-only tiers. Translation timings have
 * not been measured on the user's phone; zero means no measured timing.
 */
data class QualityTier(
    val id: String,
    val label: String,
    val sub: String,
    /** The downloaded model used by this mode. */
    val model: ModelVariant,
    /** >1 selects beam search; 1 means greedy sampling. */
    val beamSize: Int,
    /** Candidates the sampler keeps. */
    val bestOf: Int,
    val millisPer10s: Int,
    val sourceLanguage: String = "en",
    val translateToEnglish: Boolean = false,
) {
    /** Human reading of [millisPer10s]: "1.5s", "6s". */
    val waitLabel: String
        get() {
            val s = millisPer10s / 1000f
            return if (s < 3f) "%.1fs".format(s) else "${s.toInt()}s"
        }

    fun file(context: Context): File = model.file(context)
    fun isInstalled(context: Context): Boolean = model.isInstalled(context)
    val sizeMb: Int get() = model.sizeMb
}

object ModelCatalog {

    const val BASE_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"
    const val DEFAULT_TIER_ID = "auto_english"
    private val multilingualSmall = ModelVariant("ggml-small.bin", 487_601_967L)

    val tiers = listOf(
        QualityTier(
            id = "auto_english",
            label = "Auto to English",
            sub = "Speak English or Marathi. Detects the language and outputs English. " +
                "Use a complete sentence; short or mixed speech can be misdetected.",
            model = multilingualSmall,
            beamSize = 5,
            bestOf = 5,
            millisPer10s = 0,
            sourceLanguage = "auto",
            translateToEnglish = true,
        ),
        QualityTier(
            id = "marathi_english",
            label = "Marathi to English",
            sub = "For Marathi speech when automatic detection gets the language wrong. " +
                "Uses the same download as Auto to English.",
            model = multilingualSmall,
            beamSize = 5,
            bestOf = 5,
            millisPer10s = 0,
            sourceLanguage = "mr",
            translateToEnglish = true,
        ),
        QualityTier(
            id = "fast",
            label = "Fast",
            sub = "Short commands and notes to self. Will miss names and jargon.",
            model = ModelVariant("ggml-tiny.en.bin", 77_704_715L),
            beamSize = 1,
            bestOf = 2,
            millisPer10s = 1_500,
        ),
        QualityTier(
            id = "balanced",
            label = "Balanced",
            sub = "The everyday setting. Clean punctuation on ordinary speech.",
            model = ModelVariant("ggml-base.en.bin", 147_964_211L),
            beamSize = 1,
            bestOf = 2,
            millisPer10s = 2_200,
        ),
        QualityTier(
            id = "accurate",
            label = "Accurate",
            sub = "Holds up to accents, background noise and technical words.",
            model = ModelVariant("ggml-small.en.bin", 487_614_201L),
            beamSize = 1,
            bestOf = 2,
            millisPer10s = 5_800,
        ),
        QualityTier(
            id = "maximum",
            label = "Maximum",
            sub = "The Accurate model, searched harder. Nothing extra to download — " +
                "it just thinks for longer before committing to a word.",
            model = ModelVariant("ggml-small.en.bin", 487_614_201L),
            beamSize = 5,
            bestOf = 5,
            millisPer10s = 7_800,
        ),
    )

    fun byId(id: String): QualityTier = tiers.firstOrNull { it.id == id } ?: tiers.first { it.id == DEFAULT_TIER_ID }

    fun dir(context: Context): File =
        File(context.filesDir, "models").apply { if (!exists()) mkdirs() }

    /**
     * Everything on disk. Deduplicated by file, because two tiers share one
     * model — Maximum is Accurate searched harder, not a second download.
     */
    fun installedBytes(context: Context): Long =
        tiers.map { it.model }
            .distinctBy { it.fileName }
            .filter { it.isInstalled(context) }
            .sumOf { it.bytes }
}
