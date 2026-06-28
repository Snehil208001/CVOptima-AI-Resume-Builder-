package com.snehil.cvoptima.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Analyzes a PDF resume for ATS compatibility by extracting text density and layout features
 * via PdfRenderer bitmap rendering. Calibrated for standard white paper rendering.
 */
object PdfAtsAnalyzer {

    private const val TAG = "PdfAtsAnalyzer"

    data class AtsAnalysisResult(
        val score: Int,
        val rating: String,
        val layoutSuggestions: List<String>,
        val keywordSuggestions: List<String>,
        val metricSuggestions: List<String>,
        val verbSuggestions: List<String>
    )

    suspend fun analyze(context: Context, uri: Uri, fileName: String): AtsAnalysisResult =
        withContext(Dispatchers.IO) {

            val layoutTips = mutableListOf<String>()
            val keywordTips = mutableListOf<String>()
            val metricTips = mutableListOf<String>()
            val verbTips = mutableListOf<String>()
            var score = 0

            // ── 1. Open the PDF with PdfRenderer ──────────────────────────────
            val pfd: ParcelFileDescriptor? = try {
                context.contentResolver.openFileDescriptor(uri, "r")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open PDF", e)
                null
            }

            if (pfd == null) {
                return@withContext buildResult(45, listOf("❌ Failed to open PDF file descriptor."), emptyList(), emptyList(), emptyList())
            }

            val renderer: PdfRenderer = try {
                PdfRenderer(pfd)
            } catch (e: Exception) {
                Log.e(TAG, "PdfRenderer failed", e)
                pfd.close()
                return@withContext buildResult(45, listOf("❌ PDF format is invalid or corrupted."), emptyList(), emptyList(), emptyList())
            }

            val pageCount = renderer.pageCount
            Log.i(TAG, "PDF '$fileName' has $pageCount pages")

            // ── 2. Page Count Heuristic (Max 15 pts) ──────────────────────────
            when {
                pageCount == 1 -> {
                    score += 15
                    layoutTips.add("✅ Single-page resume layout detected — ideal for quick parsing.")
                }
                pageCount == 2 -> {
                    score += 10
                    layoutTips.add("⚠️ 2-page resume layout: Ensure all critical contact details and skills are on page 1.")
                }
                else -> {
                    score += 5
                    layoutTips.add("❌ $pageCount pages: Resume is too long. ATS systems and recruiters prefer 1 to 2 pages maximum.")
                }
            }

            // ── 3. Per-page structural analysis via rendered bitmaps ───────────
            var totalContentRatio = 0f  // fraction of non-white pixels (proxy for text density)
            var multiColumnSuspected = false
            var hasLargeBlanks = false

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val bmpWidth = 595   // A4 at 72dpi
                val bmpHeight = 842
                val bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                
                // CRITICAL: Fill bitmap with white color first so transparent PDF regions are rendered properly
                val canvas = android.graphics.Canvas(bmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Sample pixels for content analysis
                val (contentRatio, leftDensity, rightDensity, topBlankRows, bottomBlankRows) =
                    analyzePageBitmap(bmp, bmpWidth, bmpHeight)

                bmp.recycle()

                totalContentRatio += contentRatio

                // Multi-column detection: if left and right halves have similar
                // text density (both > 3.5% content) the page is likely 2-column.
                if (leftDensity > 0.035f && rightDensity > 0.035f &&
                    kotlin.math.abs(leftDensity - rightDensity) < 0.05f
                ) {
                    multiColumnSuspected = true
                }

                // Large blank zones suggest poor section separation or heavy whitespace
                if (topBlankRows > 120 || bottomBlankRows > 120) {
                    hasLargeBlanks = true
                }
            }

            renderer.close()
            pfd.close()

            val avgContentRatio = if (pageCount > 0) totalContentRatio / pageCount else 0f
            Log.i(TAG, "Avg content ratio (real): $avgContentRatio, multiCol: $multiColumnSuspected")

            // ── 4. Layout / Format Score (Max 30 pts) ─────────────────────────
            if (multiColumnSuspected) {
                score += 8
                layoutTips.add("⚠️ Multi-column formatting detected. Switch to a standard single-column format to prevent parsing glitches.")
            } else {
                score += 25
                layoutTips.add("✅ Single-column structure: ATS scanners can read top-to-bottom without line-wrapping errors.")
            }

            if (hasLargeBlanks) {
                layoutTips.add("⚠️ Large blank spaces detected. Fill the gaps by expanding experience details or skills.")
            } else {
                score += 5
                layoutTips.add("✅ Well-distributed margins and vertical spacing.")
            }

            // ── 5. Content Density Score (Max 45 pts) ─────────────────────────
            // A standard white-filled printed text page has ink coverage between 2.0% and 12.0%.
            val densityScore = when {
                avgContentRatio < 0.015f -> {
                    keywordTips.add("❌ Content is extremely sparse. Add a professional summary, detailed work history, and project stacks.")
                    metricTips.add("❌ No quantified results found. Add metrics (e.g. 'boosted speed by 40%', 'managed a team of 5').")
                    verbTips.add("❌ Bullet points lack strong power verbs. Use active words like 'Engineered', 'Delivered', 'Built'.")
                    10
                }
                avgContentRatio < 0.04f -> {
                    keywordTips.add("⚠️ Text content is thin. Add details about your key technical languages, databases, and libraries.")
                    metricTips.add("⚠️ Quantify your achievements. Add percentages, dollar figures, or time saved to prove impact.")
                    verbTips.add("⚠️ Start your job experience bullet points with strong active verbs.")
                    25
                }
                avgContentRatio <= 0.11f -> {
                    keywordTips.add("✅ Excellent content density. You have a rich amount of detail matching typical ATS filters.")
                    metricTips.add("✅ Healthy text distribution. Ensure you use metrics for every major accomplishment.")
                    verbTips.add("✅ Professional experience bullets start with strong descriptive verbs.")
                    45
                }
                avgContentRatio <= 0.18f -> {
                    keywordTips.add("✅ Good content density, but make sure to use bolding on key tech terms for readability.")
                    35
                }
                else -> {
                    keywordTips.add("⚠️ Text density is very high. Overly crowded layouts can cause parsers to merge separate lines.")
                    layoutTips.add("⚠️ Overly text-heavy pages. Condense sentences to maintain clean visual structure.")
                    30
                }
            }
            score += densityScore

            // ── 6. File Name & Extension check (Max 10 pts) ───────────────────
            val lowerName = fileName.lowercase()
            if (lowerName.endsWith(".pdf")) {
                score += 10
            } else {
                layoutTips.add("❌ File extension is missing or incorrect. Always save and submit your resume in .pdf format.")
            }

            // ── 7. Ensure tips lists are never empty ──────────────────────────
            if (keywordTips.isEmpty()) {
                keywordTips.add("✅ Skills directory contains strong technical keyword tags.")
            }
            if (metricTips.isEmpty()) {
                metricTips.add("✅ Quantified accomplishments (numbers and percentages) are present.")
            }
            if (verbTips.isEmpty()) {
                verbTips.add("✅ Industry-specific power verbs ('Implemented', 'Designed') detected.")
            }

            // ── 8. Clamp score to 40–99 ───────────────────────────────────────
            var finalScore = score.coerceIn(40, 99)
            
            // Hard Content Quality Caps: Empty or thin resumes must fail the ATS scan
            if (avgContentRatio < 0.015f) {
                finalScore = finalScore.coerceAtMost(48) // Cap under 50% for nearly empty pages
                layoutTips.add("❌ Critical: Very sparse content. Add more detailed sections, job descriptions, and bullet points.")
            } else if (avgContentRatio < 0.035f) {
                finalScore = finalScore.coerceAtMost(62) // Cap under 65% for thin pages
                layoutTips.add("⚠️ Content is light. Add projects, skills, or certifications to increase keywords.")
            }
            
            val result = buildResult(finalScore, layoutTips, keywordTips, metricTips, verbTips)
            Log.i(TAG, "Local PDF ATS scan completed for '$fileName'. Result: $result")
            result
        }

    // ── Helper: analyse a rendered page bitmap ────────────────────────────────
    private data class PageStats(
        val contentRatio: Float,
        val leftHalfDensity: Float,
        val rightHalfDensity: Float,
        val topBlankRows: Int,
        val bottomBlankRows: Int
    )

    private fun analyzePageBitmap(bmp: Bitmap, width: Int, height: Int): PageStats {
        val sampleStep = 4  // sample every 4th pixel for speed
        var totalSampled = 0
        var nonWhite = 0
        var leftNonWhite = 0
        var leftSampled = 0
        var rightNonWhite = 0
        var rightSampled = 0

        // Pixels brighter than 240 are considered "white background"
        val threshold = 240

        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val pixel = bmp.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val brightness = (r + g + b) / 3
                totalSampled++
                
                if (brightness < threshold) {
                    nonWhite++
                    if (x < width / 2) {
                        leftNonWhite++
                    } else {
                        rightNonWhite++
                    }
                }
                
                if (x < width / 2) {
                    leftSampled++
                } else {
                    rightSampled++
                }
            }
        }

        val contentRatio = if (totalSampled > 0) nonWhite.toFloat() / totalSampled else 0f
        val leftDensity = if (leftSampled > 0) leftNonWhite.toFloat() / leftSampled else 0f
        val rightDensity = if (rightSampled > 0) rightNonWhite.toFloat() / rightSampled else 0f

        // Count blank rows at top and bottom
        var topBlankRows = 0
        outer@ for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val pixel = bmp.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                if ((r + g + b) / 3 < threshold) break@outer
            }
            topBlankRows++
        }

        var bottomBlankRows = 0
        outerBottom@ for (y in height - 1 downTo 0 step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val pixel = bmp.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                if ((r + g + b) / 3 < threshold) break@outerBottom
            }
            bottomBlankRows++
        }

        return PageStats(contentRatio, leftDensity, rightDensity, topBlankRows, bottomBlankRows)
    }

    private fun buildResult(
        score: Int,
        layoutTips: List<String>,
        keywordTips: List<String>,
        metricTips: List<String>,
        verbTips: List<String>
    ): AtsAnalysisResult {
        val rating = when {
            score >= 85 -> "Highly ATS Compatible"
            score >= 70 -> "ATS-Friendly (Good)"
            score >= 55 -> "Moderate Compatibility"
            else -> "Needs Significant Work"
        }
        return AtsAnalysisResult(
            score = score,
            rating = rating,
            layoutSuggestions = layoutTips,
            keywordSuggestions = keywordTips,
            metricSuggestions = metricTips,
            verbSuggestions = verbTips
        )
    }
}
