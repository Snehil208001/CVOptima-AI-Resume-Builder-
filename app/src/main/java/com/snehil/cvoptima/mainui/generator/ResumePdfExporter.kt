package com.snehil.cvoptima.mainui.generator

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.snehil.cvoptima.data.remote.model.UserProfileDto
import java.io.File
import java.io.FileOutputStream

object ResumePdfExporter {

    fun exportPdf(context: Context, profile: UserProfileDto): Boolean {
        val rawName = profile.name ?: "Compiled"
        val namePart = rawName.replace("\\s+".toRegex(), "_")
        val fileName = "CVOptima_Resume_${namePart}.pdf"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        val doc = generatePdf(profile)
                        doc.writeTo(out)
                        doc.close()
                    }
                    true
                } else false
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { out ->
                    val doc = generatePdf(profile)
                    doc.writeTo(out)
                    doc.close()
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun generatePdf(profile: UserProfileDto): PdfDocument {
        val pdfDocument = PdfDocument()

        // Page dimensions (A4 size: 595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()

        // Start first page
        var pageNumber = 1
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        // Define margins
        val marginLeft = 45f
        val marginRight = 550f // pageWidth - 45
        val marginTop = 45f
        val marginBottom = 797f // pageHeight - 45
        val contentWidth = marginRight - marginLeft

        // Setup densities and scaling sizes
        val density = profile.layoutConfig?.layoutDensity ?: "Normal"
        val scale = when (density) {
            "Compact" -> 0.85f
            "Spacious" -> 1.15f
            else -> 1.0f
        }

        // Setup Paints
        val namePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val contactPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionHeadingPaint = Paint().apply {
            color = Color.parseColor("#1B365D") // Premium deep blue
            textSize = 13f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val itemHeadingPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val datePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9.5f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        var y = marginTop

        // Helper to check page break and wrap lines
        fun checkPageBreak(requiredHeight: Float) {
            if (y + requiredHeight > marginBottom) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                y = marginTop
            }
        }

        fun drawTextLine(text: String, x: Float, paint: Paint) {
            val spacing = paint.fontSpacing
            checkPageBreak(spacing)
            canvas.drawText(text, x, y, paint)
            y += spacing
        }

        fun drawDivider() {
            checkPageBreak(10f)
            y += 5f
            canvas.drawLine(marginLeft, y, marginRight, y, dividerPaint)
            y += 12f
        }

        // Header Section
        val name = profile.name ?: "Resume Profile"
        checkPageBreak(namePaint.fontSpacing)
        canvas.drawText(name, marginLeft, y, namePaint)
        y += namePaint.fontSpacing + 4f

        val contacts = listOfNotNull(
            profile.email,
            profile.contactNumber,
            profile.linkedinUrl?.substringAfter("linkedin.com/in/")?.substringBefore("/"),
            profile.githubUrl?.substringAfter("github.com/")?.substringBefore("/"),
            profile.location
        ).joinToString("  |  ")

        drawTextLine(contacts, marginLeft, contactPaint)

        if (!profile.portfolioUrl.isNullOrBlank()) {
            drawTextLine("Portfolio: ${profile.portfolioUrl}", marginLeft, contactPaint)
        }

        y += 8f

        // Professional Summary
        if (!profile.professionalSummary.isNullOrBlank()) {
            y += 6f
            drawTextLine("PROFESSIONAL SUMMARY", marginLeft, sectionHeadingPaint)
            drawDivider()

            val lines = wrapText(profile.professionalSummary, contentWidth, bodyPaint)
            for (line in lines) {
                drawTextLine(line, marginLeft, bodyPaint)
            }
            y += 10f
        }

        // Retrieve Section Ordering from layoutConfig
        val order = profile.layoutConfig?.sectionOrder ?: listOf("Skills", "Work Experiences", "Projects", "Certifications")

        for (section in order) {
            when (section) {
                "Skills" -> {
                    if (!profile.skillGroups.isNullOrEmpty()) {
                        drawTextLine("TECHNICAL SKILLS", marginLeft, sectionHeadingPaint)
                        drawDivider()

                        for (group in profile.skillGroups) {
                            val skillsText = "${group.label}: ${group.skills?.joinToString(", ") ?: ""}"
                            val lines = wrapText(skillsText, contentWidth, bodyPaint)
                            for (line in lines) {
                                drawTextLine(line, marginLeft, bodyPaint)
                            }
                            y += 2f
                        }
                        y += 10f
                    }
                }
                "Work Experiences", "Work Experience" -> {
                    if (!profile.experiences.isNullOrEmpty()) {
                        drawTextLine("WORK EXPERIENCE", marginLeft, sectionHeadingPaint)
                        drawDivider()

                        for (exp in profile.experiences) {
                            checkPageBreak(24f)

                            // Draw Title/Company (Left) and Location (Right)
                            canvas.drawText("${exp.title} - ${exp.company}", marginLeft, y, itemHeadingPaint)
                            val loc = exp.location ?: ""
                            canvas.drawText(loc, marginRight, y, datePaint)
                            y += itemHeadingPaint.fontSpacing

                            // Draw Date (Right) and Type (Left)
                            val dateStr = "${exp.startDate ?: ""} - ${exp.endDate ?: ""}"
                            canvas.drawText(dateStr, marginRight, y, datePaint)
                            if (!exp.type.isNullOrBlank()) {
                                canvas.drawText(exp.type, marginLeft, y, contactPaint)
                            }
                            y += contactPaint.fontSpacing + 4f

                            // Draw Bullet Points
                            val bullets = exp.bulletPoints ?: if (!exp.description.isNullOrBlank()) listOf(exp.description) else emptyList()
                            for (bullet in bullets) {
                                val wrapped = wrapText(bullet, contentWidth - 15f, bodyPaint)
                                for (index in wrapped.indices) {
                                    val prefix = if (index == 0) "•  " else "   "
                                    drawTextLine(prefix + wrapped[index], marginLeft + 5f, bodyPaint)
                                }
                            }
                            y += 8f
                        }
                        y += 10f
                    }
                }
                "Projects" -> {
                    if (!profile.projects.isNullOrEmpty()) {
                        drawTextLine("PROJECTS", marginLeft, sectionHeadingPaint)
                        drawDivider()

                        for (proj in profile.projects) {
                            checkPageBreak(20f)

                            // Draw Title (Left) and Date (Right)
                            canvas.drawText(proj.title, marginLeft, y, itemHeadingPaint)
                            val dateStr = proj.date ?: ""
                            canvas.drawText(dateStr, marginRight, y, datePaint)
                            y += itemHeadingPaint.fontSpacing

                            // Draw Link / Tech Stack
                            var subline = ""
                            if (!proj.techStack.isNullOrBlank()) {
                                subline += "Technologies: ${proj.techStack}  "
                            }
                            if (!proj.link.isNullOrBlank()) {
                                subline += "| Link: ${proj.link}"
                            }
                            if (subline.isNotEmpty()) {
                                drawTextLine(subline, marginLeft, contactPaint)
                                y += 2f
                            }

                            // Draw Bullet Points
                            val bullets = proj.bulletPoints ?: emptyList()
                            for (bullet in bullets) {
                                val wrapped = wrapText(bullet, contentWidth - 15f, bodyPaint)
                                for (index in wrapped.indices) {
                                    val prefix = if (index == 0) "•  " else "   "
                                    drawTextLine(prefix + wrapped[index], marginLeft + 5f, bodyPaint)
                                }
                            }
                            y += 8f
                        }
                        y += 10f
                    }
                }
                "Certifications" -> {
                    if (!profile.certifications.isNullOrEmpty()) {
                        drawTextLine("CERTIFICATIONS & ACHIEVEMENTS", marginLeft, sectionHeadingPaint)
                        drawDivider()

                        for (cert in profile.certifications) {
                            checkPageBreak(20f)

                            // Draw Title (Left) and Date (Right)
                            canvas.drawText(cert.title, marginLeft, y, itemHeadingPaint)
                            val dateStr = cert.date ?: ""
                            canvas.drawText(dateStr, marginRight, y, datePaint)
                            y += itemHeadingPaint.fontSpacing

                            // Draw Issuer / Link
                            var subline = cert.issuer ?: ""
                            if (!cert.link.isNullOrBlank()) {
                                subline += " | Link: ${cert.link}"
                            }
                            if (subline.isNotEmpty()) {
                                drawTextLine(subline, marginLeft, contactPaint)
                                y += 2f
                            }

                            // Draw Bullet Points
                            val bullets = cert.bulletPoints ?: emptyList()
                            for (bullet in bullets) {
                                val wrapped = wrapText(bullet, contentWidth - 15f, bodyPaint)
                                for (index in wrapped.indices) {
                                    val prefix = if (index == 0) "•  " else "   "
                                    drawTextLine(prefix + wrapped[index], marginLeft + 5f, bodyPaint)
                                }
                            }
                            y += 8f
                        }
                        y += 10f
                    }
                }
                "Education", "Educations" -> {
                    if (!profile.educations.isNullOrEmpty()) {
                        drawTextLine("EDUCATION", marginLeft, sectionHeadingPaint)
                        drawDivider()

                        for (edu in profile.educations) {
                            checkPageBreak(24f)

                            // Draw Institution (Left) and Date (Right)
                            canvas.drawText(edu.institution, marginLeft, y, itemHeadingPaint)
                            val dateStr = "${edu.startDate ?: ""} - ${edu.endDate ?: ""}"
                            canvas.drawText(dateStr, marginRight, y, datePaint)
                            y += itemHeadingPaint.fontSpacing

                            // Draw Degree / Major / Score
                            val field = if (edu.fieldOfStudy.isNullOrBlank()) "" else " in ${edu.fieldOfStudy}"
                            var degreeStr = "${edu.degree}$field"
                            if (!edu.location.isNullOrBlank()) {
                                degreeStr += " | ${edu.location}"
                            }
                            val scoreStr = edu.score ?: edu.gpa?.toString() ?: ""
                            if (scoreStr.isNotEmpty()) {
                                degreeStr += " (CGPA: $scoreStr)"
                            }
                            drawTextLine(degreeStr, marginLeft, contactPaint)
                            y += 4f
                        }
                        y += 10f
                    }
                }
            }
        }

        // Finish the final page
        pdfDocument.finishPage(currentPage)
        return pdfDocument
    }

    private fun wrapText(text: String, width: Float, paint: Paint): List<String> {
        val words = text.split("\\s+".toRegex())
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= width) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
}
