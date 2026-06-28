package com.snehil.cvoptima.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height

        // Premium linear gradient for the AI elements
        val gradient = Brush.linearGradient(
            colors = listOf(primaryColor, secondaryColor),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        // Draw an outer minimalist document frame with a folded top-right corner
        val frameW = w * 0.65f
        val frameH = h * 0.85f
        val left = (w - frameW) / 2f
        val top = (h - frameH) / 2f
        val right = left + frameW
        val bottom = top + frameH
        val foldSize = w * 0.18f

        // Document outline path with folded top-right corner
        val docOutlinePath = Path().apply {
            moveTo(left, bottom - w * 0.08f)
            // Bottom-left corner
            quadraticTo(left, bottom, left + w * 0.08f, bottom)
            
            // Bottom line
            lineTo(right - w * 0.08f, bottom)
            // Bottom-right corner
            quadraticTo(right, bottom, right, bottom - w * 0.08f)
            
            // Right line
            lineTo(right, top + foldSize)
            
            // Diagonal fold line
            lineTo(right - foldSize, top)
            
            // Top line
            lineTo(left + w * 0.08f, top)
            // Top-left corner
            quadraticTo(left, top, left, top + w * 0.08f)
            
            close()
        }

        // Draw document background outline with soft alpha
        drawPath(
            path = docOutlinePath,
            color = outlineColor.copy(alpha = 0.15f)
        )

        // Draw document border stroke
        drawPath(
            path = docOutlinePath,
            brush = gradient,
            style = Stroke(width = w * 0.035f, cap = StrokeCap.Round)
        )

        // Draw the folded corner back flap
        val foldPath = Path().apply {
            moveTo(right - foldSize, top)
            lineTo(right - foldSize, top + foldSize - w * 0.04f)
            quadraticTo(right - foldSize, top + foldSize, right - foldSize + w * 0.04f, top + foldSize)
            lineTo(right, top + foldSize)
        }
        drawPath(
            path = foldPath,
            brush = gradient,
            style = Stroke(width = w * 0.035f, cap = StrokeCap.Round)
        )

        // Draw elegant minimalist lines representing structured data (resume content)
        val lineY1 = top + frameH * 0.35f
        val lineY2 = top + frameH * 0.5f
        val lineY3 = top + frameH * 0.65f
        val lineStartX = left + frameW * 0.2f
        val lineThickness = w * 0.025f

        // Line 1 (longer)
        drawLine(
            color = primaryColor.copy(alpha = 0.25f),
            start = Offset(lineStartX, lineY1),
            end = Offset(right - frameW * 0.4f, lineY1),
            strokeWidth = lineThickness,
            cap = StrokeCap.Round
        )

        // Line 2 (medium)
        drawLine(
            color = primaryColor.copy(alpha = 0.25f),
            start = Offset(lineStartX, lineY2),
            end = Offset(right - frameW * 0.3f, lineY2),
            strokeWidth = lineThickness,
            cap = StrokeCap.Round
        )

        // Line 3 (shorter)
        drawLine(
            color = primaryColor.copy(alpha = 0.25f),
            start = Offset(lineStartX, lineY3),
            end = Offset(right - frameW * 0.5f, lineY3),
            strokeWidth = lineThickness,
            cap = StrokeCap.Round
        )

        // Draw a glowing, premium four-pointed AI Star overlapping the bottom right or center
        val starCx = right - frameW * 0.2f
        val starCy = bottom - frameH * 0.25f
        val starRadiusOuter = w * 0.16f

        val starPath = Path().apply {
            moveTo(starCx, starCy - starRadiusOuter)
            quadraticTo(starCx, starCy, starCx + starRadiusOuter, starCy)
            quadraticTo(starCx, starCy, starCx, starCy + starRadiusOuter)
            quadraticTo(starCx, starCy, starCx - starRadiusOuter, starCy)
            quadraticTo(starCx, starCy, starCx, starCy - starRadiusOuter)
            close()
        }

        // Draw AI Star fill
        drawPath(
            path = starPath,
            brush = gradient
        )

        // Small decorative satellite star for depth
        val satCx = starCx - w * 0.2f
        val satCy = starCy - h * 0.15f
        val satRadiusOuter = w * 0.06f

        val satPath = Path().apply {
            moveTo(satCx, satCy - satRadiusOuter)
            quadraticTo(satCx, satCy, satCx + satRadiusOuter, satCy)
            quadraticTo(satCx, satCy, satCx, satCy + satRadiusOuter)
            quadraticTo(satCx, satCy, satCx - satRadiusOuter, satCy)
            quadraticTo(satCx, satCy, satCx, satCy - satRadiusOuter)
            close()
        }

        drawPath(
            path = satPath,
            brush = gradient
        )
    }
}
