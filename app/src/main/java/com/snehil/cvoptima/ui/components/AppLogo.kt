package com.snehil.cvoptima.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height

        // 1. Draw smooth rounded bounding box with a linear gradient
        val gradient = Brush.linearGradient(
            colors = listOf(primaryColor, tertiaryColor),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        drawRoundRect(
            brush = gradient,
            cornerRadius = CornerRadius(w * 0.24f)
        )

        // 2. Draw centered stylized document vector icon
        val docW = w * 0.44f
        val docH = h * 0.56f
        val docLeft = w * 0.28f
        val docTop = h * 0.22f
        val docRight = docLeft + docW
        val docBottom = docTop + docH
        val foldSize = w * 0.14f

        val documentPath = Path().apply {
            moveTo(docLeft, docBottom)
            lineTo(docRight, docBottom)
            lineTo(docRight, docTop + foldSize)
            lineTo(docRight - foldSize, docTop)
            lineTo(docLeft, docTop)
            close()
        }

        // Draw document base
        drawPath(
            path = documentPath,
            color = Color.White.copy(alpha = 0.95f)
        )

        // Draw folded corner back side
        val foldPath = Path().apply {
            moveTo(docRight - foldSize, docTop)
            lineTo(docRight - foldSize, docTop + foldSize)
            lineTo(docRight, docTop + foldSize)
            close()
        }
        drawPath(
            path = foldPath,
            color = Color.White
        )
        // Fold shadow layer
        drawPath(
            path = foldPath,
            color = Color.Black.copy(alpha = 0.12f)
        )

        // Draw text lines inside the document
        val lineStrokeWidth = w * 0.026f
        val lineAlpha = 0.35f
        val lineColor = primaryColor.copy(alpha = lineAlpha)

        // Line 1
        drawLine(
            color = lineColor,
            start = Offset(docLeft + docW * 0.18f, docTop + docH * 0.36f),
            end = Offset(docRight - docW * 0.18f, docTop + docH * 0.36f),
            strokeWidth = lineStrokeWidth,
            cap = StrokeCap.Round
        )

        // Line 2
        drawLine(
            color = lineColor,
            start = Offset(docLeft + docW * 0.18f, docTop + docH * 0.52f),
            end = Offset(docRight - docW * 0.42f, docTop + docH * 0.52f),
            strokeWidth = lineStrokeWidth,
            cap = StrokeCap.Round
        )

        // Line 3
        drawLine(
            color = lineColor,
            start = Offset(docLeft + docW * 0.18f, docTop + docH * 0.68f),
            end = Offset(docRight - docW * 0.28f, docTop + docH * 0.68f),
            strokeWidth = lineStrokeWidth,
            cap = StrokeCap.Round
        )

        // 3. Draw interlaced connected network node vectors (AI components)
        // Define node coordinate positions
        val nodeA = Offset(w * 0.20f, h * 0.35f)
        val nodeB = Offset(w * 0.38f, h * 0.14f)
        val nodeC = Offset(w * 0.13f, h * 0.62f)
        val nodeD = Offset(w * 0.81f, h * 0.46f)
        val nodeE = Offset(w * 0.64f, h * 0.83f)

        val linkColor = Color.White.copy(alpha = 0.55f)
        val linkStrokeWidth = w * 0.015f

        // Draw connection lines
        drawLine(
            color = linkColor,
            start = nodeA,
            end = nodeB,
            strokeWidth = linkStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = linkColor,
            start = nodeA,
            end = nodeC,
            strokeWidth = linkStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = linkColor,
            start = nodeC,
            end = Offset(docLeft, h * 0.62f),
            strokeWidth = linkStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = linkColor,
            start = nodeD,
            end = nodeE,
            strokeWidth = linkStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = linkColor,
            start = nodeD,
            end = Offset(docRight, h * 0.46f),
            strokeWidth = linkStrokeWidth,
            cap = StrokeCap.Round
        )

        // Draw glowing nodes (circles)
        val nodes = listOf(nodeA, nodeB, nodeC, nodeD, nodeE)
        nodes.forEach { center ->
            // Outer glow ring
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = w * 0.045f,
                center = center
            )
            // Inner node dot
            drawCircle(
                color = Color.White,
                radius = w * 0.022f,
                center = center
            )
            // Inner node dot border for contrast
            drawCircle(
                color = primaryColor.copy(alpha = 0.8f),
                radius = w * 0.022f,
                center = center,
                style = Stroke(width = w * 0.005f)
            )
        }
    }
}
