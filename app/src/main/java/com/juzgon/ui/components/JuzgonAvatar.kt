@file:Suppress("FunctionName", "LongParameterList", "MagicNumber")

package com.juzgon.ui.components

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juzgon.ui.theme.JuzgonVisualTheme
import java.util.Locale

internal fun imageBitmapFromValue(
    contentResolver: ContentResolver,
    value: String,
    maxDimensionPx: Int = 1024,
): Bitmap? =
    runCatching {
        val uri = Uri.parse(value)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimensionPx ||
                options.outHeight / sampleSize > maxDimensionPx
            ) {
                sampleSize *= 2
            }
            contentResolver.openInputStream(uri)?.use { stream2 ->
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeStream(stream2, null, decodeOptions)
            }
        }
    }.getOrNull()

@Composable
internal fun JuzgonAvatar(
    name: String,
    modifier: Modifier = Modifier,
    imageBitmap: Bitmap? = null,
    imageValue: String? = null,
    size: Dp = 48.dp,
    shape: Shape = CircleShape,
    contentDescription: String? = null,
) {
    val tokens = JuzgonVisualTheme.tokens
    val context = LocalContext.current
    val resolvedBitmap =
        imageBitmap ?: remember(imageValue) {
            if (!imageValue.isNullOrBlank()) {
                imageBitmapFromValue(context.contentResolver, imageValue)
            } else {
                null
            }
        }

    val cd = contentDescription ?: "$name avatar"

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .clip(shape)
                .semantics { this.contentDescription = cd },
    ) {
        if (resolvedBitmap != null) {
            Image(
                bitmap = resolvedBitmap.asImageBitmap(),
                contentDescription = cd,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val initial =
                name
                    .trim()
                    .take(1)
                    .uppercase(Locale.US)
                    .ifBlank { "?" }
            val gradientBrush =
                Brush.linearGradient(
                    listOf(
                        tokens.palette.secondaryGlow,
                        tokens.palette.primaryGlow,
                    ),
                )
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(gradientBrush),
            ) {
                Text(
                    text = initial,
                    color = tokens.palette.textStrong,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42f).coerceAtLeast(10f).sp,
                )
            }
        }
    }
}
