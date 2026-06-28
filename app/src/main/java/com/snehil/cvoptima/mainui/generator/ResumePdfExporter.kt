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

        // Setup densities and scaling sizes
        val density = profile.layoutConfig?.layoutDensity ?: "Normal"
        val scale = when (density) {
            "Compact" -> 0.80f
            "Spacious" -> 1.05f
            else -> 0.90f // make default slightly more compact to keep on 1 page
        }

        // Define margins
        val marginLeft = 40f
        val marginRight = 555f // pageWidth - 40
        val marginTop = 35f
        val marginBottom = 807f // pageHeight - 35
        val contentWidth = marginRight - marginLeft

        // Setup Paints
        val namePaint = Paint().apply {
            color = Color.BLACK
            textSize = 19f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val contactPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionHeadingPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val itemHeadingPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val datePaintBold = Paint().apply {
            color = Color.BLACK
            textSize = 10f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldBodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val italicPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val datePaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val linkPaint = Paint().apply {
            color = Color.parseColor("#0056b3") // LaTeX-style hyperlink blue
            textSize = 9.5f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isUnderlineText = true
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 0.5f // thin, sharp black divider line
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

        fun drawSectionHeader(title: String) {
            val spacing = sectionHeadingPaint.fontSpacing
            checkPageBreak(spacing + 8f)
            canvas.drawText(title, marginLeft, y, sectionHeadingPaint)
            y += 3f // 3pt gap directly below
            canvas.drawLine(marginLeft, y, marginRight, y, dividerPaint)
            y += 8f * scale
        }

        fun drawBoldPrefixLine(label: String, value: String) {
            val prefix = "$label: "
            val fullText = "$prefix$value"
            val lines = wrapText(fullText, contentWidth, bodyPaint)
            if (lines.isEmpty()) return

            checkPageBreak(bodyPaint.fontSpacing * lines.size)

            val firstLine = lines[0]
            if (firstLine.startsWith(prefix)) {
                // Draw bold prefix
                canvas.drawText(prefix, marginLeft, y, boldBodyPaint)
                val prefixWidth = boldBodyPaint.measureText(prefix)
                // Draw remaining regular text of the first line
                val remaining = firstLine.substring(prefix.length)
                canvas.drawText(remaining, marginLeft + prefixWidth, y, bodyPaint)
            } else {
                canvas.drawText(firstLine, marginLeft, y, bodyPaint)
            }
            y += bodyPaint.fontSpacing

            // Draw subsequent lines
            for (i in 1 until lines.size) {
                canvas.drawText(lines[i], marginLeft, y, bodyPaint)
                y += bodyPaint.fontSpacing
            }
            y += 2f * scale // tiny spacing between skill lines
        }

        // Header Section
        val name = (profile.name ?: "Resume Profile").uppercase()
        val nameWidth = namePaint.measureText(name)
        val nameX = (pageWidth - nameWidth) / 2f
        checkPageBreak(namePaint.fontSpacing)
        canvas.drawText(name, nameX, y, namePaint)
        y += namePaint.fontSpacing + 4f * scale

        // Build contact list
        val contactsList = mutableListOf<String>()
        if (!profile.email.isNullOrBlank()) {
            contactsList.add("✉ ${profile.email}")
        }
        if (!profile.contactNumber.isNullOrBlank()) {
            contactsList.add("☎ ${profile.contactNumber}")
        }
        if (!profile.linkedinUrl.isNullOrBlank()) {
            contactsList.add("LinkedIn")
        }
        if (!profile.githubUrl.isNullOrBlank()) {
            contactsList.add("Github")
        }
        if (!profile.portfolioUrl.isNullOrBlank()) {
            contactsList.add("Portfolio")
        }

        val contactsStr = contactsList.joinToString("   ")
        val contactsWidth = contactPaint.measureText(contactsStr)
        val contactsX = (pageWidth - contactsWidth) / 2f
        checkPageBreak(contactPaint.fontSpacing)
        canvas.drawText(contactsStr, contactsX, y, contactPaint)
        y += contactPaint.fontSpacing + 10f * scale

        // Profile Section
        if (!profile.professionalSummary.isNullOrBlank()) {
            drawSectionHeader("Profile")
            val lines = wrapText(profile.professionalSummary, contentWidth, bodyPaint)
            for (line in lines) {
                drawTextLine(line, marginLeft, bodyPaint)
            }
            y += 6f * scale
        }

        // Retrieve Section Ordering from layoutConfig
        val order = profile.layoutConfig?.sectionOrder ?: listOf("Skills", "Work Experiences", "Projects", "Certifications", "Education")

        for (section in order) {
            when (section) {
                "Skills" -> {
                    if (!profile.skillGroups.isNullOrEmpty()) {
                        drawSectionHeader("Technical Skills")
                        for (group in profile.skillGroups) {
                            drawBoldPrefixLine(group.label, group.skills?.joinToString(", ") ?: "")
                        }
                        y += 6f * scale
                    }
                }
                "Work Experiences", "Work Experience" -> {
                    if (!profile.experiences.isNullOrEmpty()) {
                        drawSectionHeader("Professional Experience")
                        for (exp in profile.experiences) {
                            checkPageBreak(28f * scale)

                            // Line 1: Company Name (Left, bold) and Date (Right, bold)
                            canvas.drawText(exp.company, marginLeft, y, itemHeadingPaint)
                            val dateStr = "${formatDate(exp.startDate)} – ${formatDate(exp.endDate)}"
                            canvas.drawText(dateStr, marginRight, y, datePaintBold)
                            y += itemHeadingPaint.fontSpacing

                            // Line 2: Title / Role (Left, Italic) and Location (Right, regular)
                            canvas.drawText(exp.title, marginLeft, y, italicPaint)
                            val locStr = exp.location ?: ""
                            canvas.drawText(locStr, marginRight, y, datePaint)
                            y += italicPaint.fontSpacing + 3f * scale

                            // Bullet points
                            val bullets = exp.bulletPoints ?: if (!exp.description.isNullOrBlank()) listOf(exp.description) else emptyList()
                            for (bullet in bullets) {
                                val wrapped = wrapText(bullet, contentWidth - 15f * scale, bodyPaint)
                                for (index in wrapped.indices) {
                                    val prefix = if (index == 0) "•  " else "   "
                                    drawTextLine(prefix + wrapped[index], marginLeft + 5f * scale, bodyPaint)
                                }
                            }
                            y += 4f * scale
                        }
                        y += 6f * scale
                    }
                }
                "Projects" -> {
                    if (!profile.projects.isNullOrEmpty()) {
                        drawSectionHeader("Projects")
                        for (proj in profile.projects) {
                            checkPageBreak(20f * scale)

                            // Line 1: Title (Left, bold) and Date (Right, bold)
                            canvas.drawText(proj.title, marginLeft, y, itemHeadingPaint)
                            val dateStr = formatDate(proj.date)
                            canvas.drawText(dateStr, marginRight, y, datePaintBold)
                            y += itemHeadingPaint.fontSpacing + 3f * scale

                            // Bullet points
                            val bullets = proj.bulletPoints ?: emptyList()
                            for (bullet in bullets) {
                                val wrapped = wrapText(bullet, contentWidth - 15f * scale, bodyPaint)
                                for (index in wrapped.indices) {
                                    val prefix = if (index == 0) "•  " else "   "
                                    drawTextLine(prefix + wrapped[index], marginLeft + 5f * scale, bodyPaint)
                                }
                            }

                            // Project link & tech stack centered footer
                            val linkPart = if (!proj.link.isNullOrBlank()) "Github" else ""
                            val separatorPart = if (linkPart.isNotEmpty() && !proj.techStack.isNullOrBlank()) " | " else ""
                            val techPart = if (!proj.techStack.isNullOrBlank()) {
                                if (proj.techStack.startsWith("[")) proj.techStack else "[${proj.techStack}]"
                            } else ""

                            val linkWidth = linkPaint.measureText(linkPart)
                            val separatorWidth = bodyPaint.measureText(separatorPart)
                            val techWidth = italicPaint.measureText(techPart)
                            val totalWidth = linkWidth + separatorWidth + techWidth

                            if (totalWidth > 0) {
                                checkPageBreak(bodyPaint.fontSpacing + 4f * scale)
                                var curX = (pageWidth - totalWidth) / 2f
                                if (linkPart.isNotEmpty()) {
                                    canvas.drawText(linkPart, curX, y, linkPaint)
                                    curX += linkWidth
                                }
                                if (separatorPart.isNotEmpty()) {
                                    canvas.drawText(separatorPart, curX, y, bodyPaint)
                                    curX += separatorWidth
                                }
                                if (techPart.isNotEmpty()) {
                                    canvas.drawText(techPart, curX, y, italicPaint)
                                }
                                y += bodyPaint.fontSpacing + 4f * scale
                            }
                            y += 4f * scale
                        }
                        y += 6f * scale
                    }
                }
                "Certifications", "Achievements" -> {
                    if (!profile.certifications.isNullOrEmpty()) {
                        drawSectionHeader("Achievements")
                        for (cert in profile.certifications) {
                            if (cert.title.equals("Achievements", ignoreCase = true)) {
                                val bullets = cert.bulletPoints ?: emptyList()
                                for (bullet in bullets) {
                                    val isSubBullet = bullet.trimStart().startsWith("-")
                                    if (isSubBullet) {
                                        // Draw as sub-bullet: indent further
                                        val cleanText = bullet.trimStart().substring(1).trim()
                                        val subIndent = 25f * scale
                                        val wrapped = wrapText(cleanText, contentWidth - subIndent, bodyPaint)
                                        for (index in wrapped.indices) {
                                            val prefix = if (index == 0) "- " else "  "
                                            drawTextLine(prefix + wrapped[index], marginLeft + 15f * scale, bodyPaint)
                                        }
                                    } else {
                                        // Draw as main bullet: might have a bold prefix like "Problem Solving:"
                                        val cleanText = bullet.trim()
                                        val hasColon = cleanText.contains(":")
                                        if (hasColon) {
                                            val colonIdx = cleanText.indexOf(":")
                                            val prefixBold = cleanText.substring(0, colonIdx + 1)
                                            val suffixNormal = cleanText.substring(colonIdx + 1)

                                            val wrapped = wrapText(cleanText, contentWidth - 15f * scale, bodyPaint)
                                            for (index in wrapped.indices) {
                                                val line = wrapped[index]
                                                if (index == 0) {
                                                    canvas.drawText("•  ", marginLeft + 5f * scale, y, bodyPaint)
                                                    val bulletWidth = bodyPaint.measureText("•  ")

                                                    if (line.startsWith(prefixBold)) {
                                                        canvas.drawText(prefixBold, marginLeft + 5f * scale + bulletWidth, y, boldBodyPaint)
                                                        val boldWidth = boldBodyPaint.measureText(prefixBold)
                                                        val remaining = line.substring(prefixBold.length)
                                                        canvas.drawText(remaining, marginLeft + 5f * scale + bulletWidth + boldWidth, y, bodyPaint)
                                                    } else {
                                                        canvas.drawText(line, marginLeft + 5f * scale + bulletWidth, y, bodyPaint)
                                                    }
                                                } else {
                                                    canvas.drawText(line, marginLeft + 15f * scale, y, bodyPaint)
                                                }
                                                y += bodyPaint.fontSpacing
                                            }
                                        } else {
                                            val wrapped = wrapText(cleanText, contentWidth - 15f * scale, bodyPaint)
                                            for (index in wrapped.indices) {
                                                val prefix = if (index == 0) "•  " else "   "
                                                drawTextLine(prefix + wrapped[index], marginLeft + 5f * scale, bodyPaint)
                                            }
                                        }
                                    }
                                }
                            } else {
                                checkPageBreak(20f * scale)
                                canvas.drawText(cert.title, marginLeft, y, itemHeadingPaint)
                                val dateStr = formatDate(cert.date)
                                canvas.drawText(dateStr, marginRight, y, datePaint)
                                y += itemHeadingPaint.fontSpacing

                                var subline = cert.issuer ?: ""
                                if (!cert.link.isNullOrBlank()) {
                                    subline += " | Link: ${cert.link}"
                                }
                                if (subline.isNotEmpty()) {
                                    drawTextLine(subline, marginLeft, contactPaint)
                                    y += 2f * scale
                                }

                                val bullets = cert.bulletPoints ?: emptyList()
                                for (bullet in bullets) {
                                    val wrapped = wrapText(bullet, contentWidth - 15f * scale, bodyPaint)
                                    for (index in wrapped.indices) {
                                        val prefix = if (index == 0) "•  " else "   "
                                        drawTextLine(prefix + wrapped[index], marginLeft + 5f * scale, bodyPaint)
                                    }
                                }
                            }
                            y += 4f * scale
                        }
                        y += 6f * scale
                    }
                }
                "Education", "Educations" -> {
                    if (!profile.educations.isNullOrEmpty()) {
                        drawSectionHeader("Education")
                        for (edu in profile.educations) {
                            checkPageBreak(24f * scale)

                            // Line 1: Institution (Left, bold) and Date (Right, bold)
                            canvas.drawText(edu.institution, marginLeft, y, itemHeadingPaint)
                            val dateStr = "${formatDate(edu.startDate)} – ${formatDate(edu.endDate)}"
                            canvas.drawText(dateStr, marginRight, y, datePaintBold)
                            y += itemHeadingPaint.fontSpacing

                            // Line 2: Degree / Major (Left, Italic) and Location (Right, regular)
                            val field = if (edu.fieldOfStudy.isNullOrBlank()) "" else " in ${edu.fieldOfStudy}"
                            var degreeStr = "${edu.degree}$field"
                            val scoreStr = edu.score ?: edu.gpa?.toString() ?: ""
                            if (scoreStr.isNotEmpty()) {
                                degreeStr += " (CGPA: $scoreStr)"
                            }
                            canvas.drawText(degreeStr, marginLeft, y, italicPaint)
                            val loc = edu.location ?: ""
                            canvas.drawText(loc, marginRight, y, datePaint)
                            y += italicPaint.fontSpacing + 6f * scale
                        }
                        y += 6f * scale
                    }
                }
            }
        }

        // Finish the final page
        pdfDocument.finishPage(currentPage)
        return pdfDocument
    }

    private fun formatDate(date: String?): String {
        if (date.isNullOrBlank()) return ""
        return try {
            if (date.contains("-")) {
                val parts = date.split("-")
                if (parts.size >= 2) {
                    val year = parts[0]
                    val monthInt = parts[1].toIntOrNull() ?: 1
                    val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    val monthStr = if (monthInt in 1..12) months[monthInt] else ""
                    if (monthStr.isNotEmpty()) {
                        "$monthStr $year"
                    } else {
                        date
                    }
                } else {
                    date
                }
            } else {
                date
            }
        } catch (e: Exception) {
            date
        }
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

    fun exportMarkdownPdf(context: Context, title: String, markdown: String): Boolean {
        val namePart = title.replace("\\s+".toRegex(), "_")
        val fileName = "CVOptima_Tailored_${namePart}.pdf"

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
                        val doc = generateMarkdownPdf(title, markdown)
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
                    val doc = generateMarkdownPdf(title, markdown)
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

    private fun generateMarkdownPdf(title: String, markdown: String): PdfDocument {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()

        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        var yPosition = 50f
        val margin = 40f

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val headingPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        // Draw title
        canvas.drawText(title.replace("_", " "), margin, yPosition, titlePaint)
        yPosition += 30f

        val lines = markdown.split("\n")
        for (line in lines) {
            val cleanLine = line.trim()
            if (cleanLine.isEmpty()) {
                yPosition += 8f
                continue
            }

            var drawPaint = textPaint
            var textToDraw = cleanLine
            var leftIndent = margin

            when {
                cleanLine.startsWith("# ") -> {
                    yPosition += 12f
                    drawPaint = titlePaint.apply { textSize = 16f }
                    textToDraw = cleanLine.substring(2)
                }
                cleanLine.startsWith("## ") -> {
                    yPosition += 10f
                    drawPaint = headingPaint
                    textToDraw = cleanLine.substring(3)
                }
                cleanLine.startsWith("### ") -> {
                    yPosition += 8f
                    drawPaint = headingPaint.apply { textSize = 11f }
                    textToDraw = cleanLine.substring(4)
                }
                cleanLine.startsWith("- ") || cleanLine.startsWith("* ") -> {
                    if (yPosition > pageHeight - 50f) {
                        pdfDocument.finishPage(currentPage)
                        currentPage = pdfDocument.startPage(pageInfo)
                        canvas = currentPage.canvas
                        yPosition = 50f
                    }
                    canvas.drawCircle(margin + 5f, yPosition - 5f, 2f, Paint().apply { color = Color.BLACK; isAntiAlias = true })
                    leftIndent = margin + 15f
                    textToDraw = cleanLine.substring(2)
                }
            }

            val wrappedLines = wrapText(textToDraw, pageWidth - margin - leftIndent, drawPaint)
            for (wLine in wrappedLines) {
                if (yPosition > pageHeight - 50f) {
                    pdfDocument.finishPage(currentPage)
                    currentPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPage.canvas
                    yPosition = 50f
                }
                canvas.drawText(wLine, leftIndent, yPosition, drawPaint)
                yPosition += drawPaint.textSize + 4f
            }
        }

        pdfDocument.finishPage(currentPage)
        return pdfDocument
    }
}
