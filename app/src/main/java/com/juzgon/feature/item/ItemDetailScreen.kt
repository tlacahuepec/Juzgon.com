@file:Suppress("FunctionName", "LongMethod", "LongParameterList", "TooManyFunctions")

package com.juzgon.feature.item

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.juzgon.domain.AttributeType
import com.juzgon.ui.theme.JuzgonVisualTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val CHART_MIN_POINTS = 3
private const val CHART_GRID_RINGS = 4
private const val CHART_RADIUS_FRACTION = 0.38f

@Composable
fun ItemDetailRoute(
    itemId: String,
    categoryName: String = "",
    activeProfileId: String? = null,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteCompleted: () -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId, categoryName, activeProfileId)
    }

    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.deleteCompleted) {
        if (state.deleteCompleted) {
            onDeleteCompleted()
        }
    }
    ItemDetailScreen(
        state = state,
        onBackClick = onBackClick,
        onEditClick = onEditClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteDialogDismissed = viewModel::onDeleteDialogDismissed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    state: ItemDetailUiState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onDeleteConfirmed: () -> Unit = {},
    onDeleteDialogDismissed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (state.showDeleteConfirmDialog) {
        DeleteItemDialog(
            itemId = state.itemId,
            isDeleting = state.isDeleting,
            onConfirm = onDeleteConfirmed,
            onDismiss = onDeleteDialogDismissed,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.itemId.ifBlank { "Item" }) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier =
                            Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics {
                                    contentDescription = "Back"
                                    role = Role.Button
                                },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    if (!state.isLoading && state.errorMessage == null) {
                        IconButton(
                            onClick = onEditClick,
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .semantics {
                                        contentDescription = "Edit item"
                                        role = Role.Button
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .semantics {
                                        contentDescription = "Delete item"
                                        role = Role.Button
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        when {
            state.isLoading ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    CircularProgressIndicator()
                }
            state.errorMessage != null ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            else ->
                ItemDetailContent(
                    state = state,
                    modifier = Modifier.padding(innerPadding),
                )
        }
    }
}

@Composable
private fun DeleteItemDialog(
    itemId: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete item") },
        text = { Text("Delete $itemId? This will permanently remove the item and its ratings.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                modifier = Modifier.semantics { contentDescription = "Confirm delete item" },
            ) {
                Text(if (isDeleting) "Deleting" else "Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                modifier = Modifier.semantics { contentDescription = "Cancel delete item" },
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun FullImagePreviewDialog(
    imageReference: ItemImageReference,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap by
        produceState<Bitmap?>(
            initialValue = null,
            key1 = imageReference.id,
            key2 = imageReference.sourceUri,
        ) {
            value =
                withContext(Dispatchers.IO) {
                    imageBitmapFromValue(
                        contentResolver = context.contentResolver,
                        value = imageReference.sourceUri,
                        maxDimensionPx = 2048,
                    )
                }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(imageReference.displayName ?: "Image") },
        text = {
            val resolvedBitmap = bitmap
            if (resolvedBitmap == null) {
                Text("Image unavailable")
            } else {
                Image(
                    bitmap = resolvedBitmap.asImageBitmap(),
                    contentDescription = "Full size image preview",
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 360.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun ItemDetailContent(
    state: ItemDetailUiState,
    modifier: Modifier = Modifier,
) {
    var selectedImageForPreview by remember { mutableStateOf<ItemImageReference?>(null) }
    selectedImageForPreview?.let { imageReference ->
        FullImagePreviewDialog(
            imageReference = imageReference,
            onDismiss = { selectedImageForPreview = null },
        )
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
        PrimaryImageSection(
            itemId = state.itemId,
            imageReference = state.primaryImage,
            onImageClick = { imageReference -> selectedImageForPreview = imageReference },
        )
        OverallScoreSection(overallScoreText = state.overallScoreText)
        ProfileBreakdownSection(profileBreakdown = state.profileBreakdown)
        HorizontalDivider()
        DiamondChartSection(points = state.diamondChartPoints)
        HorizontalDivider()
        RankedAttributeProgressCards(rankedAttributes = state.rankedAttributes)
        if (state.attributeValues.isNotEmpty()) {
            HorizontalDivider()
            AttributeValuesSection(
                attributeValues = state.attributeValues,
                onImageClick = { imageReference -> selectedImageForPreview = imageReference },
            )
        }
        if (state.notes.isNotBlank()) {
            HorizontalDivider()
            NotesSection(notes = state.notes)
        }
    }
}

@Composable
private fun OverallScoreSection(overallScoreText: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Overall", style = MaterialTheme.typography.titleMedium)
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = overallScoreText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ProfileBreakdownSection(profileBreakdown: ItemProfileBreakdown?) {
    if (profileBreakdown == null) return
    val tokens = JuzgonVisualTheme.tokens
    val breakdownDescription =
        "Profile breakdown, ${profileBreakdown.profileName}, rank ${profileBreakdown.profileRank} " +
            "of ${profileBreakdown.totalItems}, score ${profileBreakdown.profileScoreText}"

    Surface(
        color = tokens.palette.panelBackground,
        contentColor = tokens.palette.textStrong,
        shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = breakdownDescription },
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(tokens.spacing.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = profileBreakdown.profileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "#${profileBreakdown.profileRank} of ${profileBreakdown.totalItems}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Surface(
                color = tokens.palette.ratingAccent,
                contentColor = tokens.palette.baseBackground,
                shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
            ) {
                Text(
                    text = profileBreakdown.profileScoreText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun DiamondChartSection(points: List<DiamondChartPoint>) {
    val tokens = JuzgonVisualTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Diamond chart", style = MaterialTheme.typography.titleSmall)
        if (points.size < CHART_MIN_POINTS) {
            Surface(
                color = tokens.palette.panelBackground,
                contentColor = tokens.palette.textSoft,
                shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .semantics { contentDescription = "Diamond chart empty state" },
            ) {
                Text(
                    text = "Add at least 3 numeric chart attributes",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            return
        }

        Surface(
            color = tokens.palette.panelBackground,
            contentColor = tokens.palette.textStrong,
            shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Diamond chart surface, ${points.size} attributes" },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                modifier = Modifier.padding(tokens.spacing.large),
            ) {
                ItemAttributeDiamondChart(points = points)
                points.forEach { point ->
                    Text(
                        text = "${point.label}: ${point.value} / ${point.maxValue}",
                        color = tokens.palette.textSoft,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemAttributeDiamondChart(points: List<DiamondChartPoint>) {
    val tokens = JuzgonVisualTheme.tokens
    val gridColor = tokens.palette.contrastAccentSoft.copy(alpha = 0.38f)
    val polygonFill = tokens.palette.ratingAccent.copy(alpha = 0.36f)
    val polygonStroke = tokens.palette.contrastAccent
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp)
                .height(240.dp)
                .semantics { contentDescription = "Attribute diamond chart" },
    ) {
        val center =
            androidx.compose.ui.geometry
                .Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) * CHART_RADIUS_FRACTION
        repeat(CHART_GRID_RINGS) { index ->
            val scale = (index + 1) / CHART_GRID_RINGS.toFloat()
            drawPath(
                path = points.regularPolygonPath(center, radius * scale),
                color = gridColor,
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(width = 1.dp.toPx()),
            )
        }
        points.forEachIndexed { index, _ ->
            val outer = chartOffset(center, radius, index, points.size)
            drawLine(color = gridColor, start = center, end = outer, strokeWidth = 1.dp.toPx())
        }
        drawPath(
            path = points.valuePolygonPath(center, radius),
            color = polygonFill,
        )
        drawPath(
            path = points.valuePolygonPath(center, radius),
            color = polygonStroke,
            style =
                androidx.compose.ui.graphics.drawscope
                    .Stroke(width = 3.dp.toPx()),
        )
    }
}

private fun List<DiamondChartPoint>.regularPolygonPath(
    center: androidx.compose.ui.geometry.Offset,
    radius: Float,
): Path =
    mapIndexed { index, _ -> chartOffset(center, radius, index, size) }
        .toPath()

private fun List<DiamondChartPoint>.valuePolygonPath(
    center: androidx.compose.ui.geometry.Offset,
    radius: Float,
): Path =
    mapIndexed { index, point -> chartOffset(center, radius * point.fraction, index, size) }
        .toPath()

private fun List<androidx.compose.ui.geometry.Offset>.toPath(): Path =
    Path().also { path ->
        forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }
        path.close()
    }

private fun chartOffset(
    center: androidx.compose.ui.geometry.Offset,
    radius: Float,
    index: Int,
    count: Int,
): androidx.compose.ui.geometry.Offset {
    val angle = -PI / 2.0 + (2.0 * PI * index / count)
    return androidx.compose.ui.geometry.Offset(
        x = center.x + (cos(angle) * radius).toFloat(),
        y = center.y + (sin(angle) * radius).toFloat(),
    )
}

@Composable
private fun PrimaryImageSection(
    itemId: String,
    imageReference: ItemImageReference?,
    onImageClick: (ItemImageReference) -> Unit,
) {
    if (imageReference == null) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .semantics { contentDescription = "$itemId image placeholder" },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(text = "No image", style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        ImageAttributePreview(
            imageReference = imageReference,
            contentDescription = "$itemId image preview",
            height = 220.dp,
            onClick = { onImageClick(imageReference) },
        )
    }
}

@Composable
private fun RankedAttributeProgressCards(rankedAttributes: List<RankedAttributeCardUiModel>) {
    val tokens = JuzgonVisualTheme.tokens
    Column(
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(tokens.gradients.heroSurface),
                    shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
                ).semantics { contentDescription = "Ranked attribute score list" }
                .padding(tokens.spacing.large),
    ) {
        Text(text = "Ranked attributes", style = MaterialTheme.typography.titleSmall)
        rankedAttributes.forEach { rankedAttribute ->
            RankedAttributeCard(rankedAttribute)
        }
    }
}

@Composable
private fun RankedAttributeCard(rankedAttribute: RankedAttributeCardUiModel) {
    val sizeStyle = rankedAttribute.sizeVariant.cardSizeStyle()
    val tokens = JuzgonVisualTheme.tokens
    Surface(
        color = tokens.palette.baseBackground.copy(alpha = 0.72f),
        contentColor = tokens.palette.textStrong,
        shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = sizeStyle.minHeight)
                .testTag(rankedAttribute.testTag)
                .semantics(mergeDescendants = true) {
                    contentDescription = rankedAttribute.accessibleDescription
                },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier.padding(
                    horizontal = sizeStyle.horizontalPadding,
                    vertical = sizeStyle.verticalPadding,
                ),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "#${rankedAttribute.rank}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = rankedAttribute.label,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    AttributeMovementIndicators(rankedAttribute.movement)
                }
                Text(
                    text = "${rankedAttribute.valueText} / ${rankedAttribute.maxText}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LinearProgressIndicator(
                progress = { rankedAttribute.progressFraction },
                color = tokens.palette.ratingAccent,
                trackColor = tokens.palette.elevatedBackground,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(sizeStyle.progressHeight),
            )
        }
    }
}

@Composable
private fun AttributeMovementIndicators(movement: AttributeMovement?) {
    if (movement == null) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AttributeMovementIndicator(label = "Rank", direction = movement.rank)
        AttributeMovementIndicator(label = "Value", direction = movement.value)
    }
}

@Composable
private fun AttributeMovementIndicator(
    label: String,
    direction: AttributeMovementDirection,
) {
    val symbol =
        when (direction) {
            AttributeMovementDirection.Improved -> "↑"
            AttributeMovementDirection.Declined -> "↓"
            AttributeMovementDirection.Unchanged -> "="
        }
    val color =
        when (direction) {
            AttributeMovementDirection.Improved -> MaterialTheme.colorScheme.primary
            AttributeMovementDirection.Declined -> MaterialTheme.colorScheme.error
            AttributeMovementDirection.Unchanged -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Text(
        text = "$label $symbol",
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun AttributeValuesSection(
    attributeValues: List<ItemDetailAttributeValue>,
    onImageClick: (ItemImageReference) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Attributes", style = MaterialTheme.typography.titleSmall)
        attributeValues.forEach { attributeValue ->
            AttributeValueRow(
                attributeValue = attributeValue,
                onImageClick = onImageClick,
            )
        }
    }
}

@Composable
private fun AttributeValueRow(
    attributeValue: ItemDetailAttributeValue,
    onImageClick: (ItemImageReference) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = attributeValue.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (attributeValue.type == AttributeType.IMAGE) {
            ImageAttributeGallery(
                label = attributeValue.label,
                imageReferences = attributeValue.imageReferences,
                onImageClick = onImageClick,
            )
        } else if (attributeValue.type == AttributeType.URL) {
            UrlAttributeValue(attributeValue)
        } else {
            Text(text = attributeValue.displayValue, style = MaterialTheme.typography.bodyMedium)
            attributeValue.ageText?.let { age ->
                Text(
                    text = age,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UrlAttributeValue(attributeValue: ItemDetailAttributeValue) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = attributeValue.displayValue,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
        modifier =
            Modifier
                .clickable(
                    role = Role.Button,
                    onClick = { uriHandler.openUri(attributeValue.value) },
                ).semantics {
                    contentDescription = "Open ${attributeValue.label} URL"
                    role = Role.Button
                },
    )
}

@Composable
private fun ImageAttributeGallery(
    label: String,
    imageReferences: List<ItemImageReference>,
    onImageClick: (ItemImageReference) -> Unit,
) {
    if (imageReferences.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .semantics { contentDescription = "$label image preview" },
        ) {
            Text(
                text = "Image unavailable",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$label image preview" },
    ) {
        items(
            items = imageReferences,
            key = { imageReference -> imageReference.id },
        ) { imageReference ->
            ImageAttributePreview(
                imageReference = imageReference,
                contentDescription = "$label image preview",
                width = 132.dp,
                height = 96.dp,
                onClick = { onImageClick(imageReference) },
            )
        }
    }
}

@Composable
private fun ImageAttributePreview(
    imageReference: ItemImageReference,
    contentDescription: String,
    width: Dp? = null,
    height: Dp = 160.dp,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val bitmap by
        produceState<Bitmap?>(
            initialValue = null,
            key1 = imageReference.id,
            key2 = imageReference.thumbnailUri,
        ) {
            value =
                withContext(Dispatchers.IO) {
                    imageBitmapFromValue(
                        contentResolver = context.contentResolver,
                        value = imageReference.thumbnailUri,
                        maxDimensionPx = 512,
                    )
                }
        }
    val imageModifier =
        (
            if (width != null) {
                Modifier.width(width)
            } else {
                Modifier.fillMaxWidth()
            }
        ).height(height)
            .let { modifier ->
                if (onClick == null) {
                    modifier
                } else {
                    modifier.clickable(role = Role.Button, onClick = onClick)
                }
            }
    val resolvedBitmap = bitmap
    if (resolvedBitmap != null) {
        Image(
            bitmap = resolvedBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = imageModifier,
        )
    } else {
        Box(
            modifier =
                imageModifier
                    .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Image",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun imageBitmapFromValue(
    contentResolver: android.content.ContentResolver,
    value: String,
    maxDimensionPx: Int,
) = runCatching {
    val uri = Uri.parse(value)
    val bounds =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
    contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, bounds)
    }
    val sampleSize = bounds.sampleSizeFor(maxDimensionPx)
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, decodeOptions)
    }
}.getOrNull()

private fun BitmapFactory.Options.sampleSizeFor(maxDimensionPx: Int): Int {
    if (outWidth <= 0 || outHeight <= 0) return 1
    var sampleSize = 1
    while (outWidth / sampleSize > maxDimensionPx || outHeight / sampleSize > maxDimensionPx) {
        sampleSize *= 2
    }
    return sampleSize
}

private data class RankedAttributeCardSizeStyle(
    val minHeight: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val progressHeight: Dp,
)

private fun AttributeRankSizeVariant.cardSizeStyle(): RankedAttributeCardSizeStyle =
    when (this) {
        AttributeRankSizeVariant.Rank1 ->
            RankedAttributeCardSizeStyle(
                minHeight = 136.dp,
                horizontalPadding = 20.dp,
                verticalPadding = 22.dp,
                progressHeight = 10.dp,
            )
        AttributeRankSizeVariant.Rank2 ->
            RankedAttributeCardSizeStyle(
                minHeight = 128.dp,
                horizontalPadding = 20.dp,
                verticalPadding = 20.dp,
                progressHeight = 9.dp,
            )
        AttributeRankSizeVariant.Rank3 ->
            RankedAttributeCardSizeStyle(
                minHeight = 120.dp,
                horizontalPadding = 18.dp,
                verticalPadding = 18.dp,
                progressHeight = 8.dp,
            )
        AttributeRankSizeVariant.Rank4 ->
            RankedAttributeCardSizeStyle(
                minHeight = 112.dp,
                horizontalPadding = 18.dp,
                verticalPadding = 16.dp,
                progressHeight = 7.dp,
            )
        AttributeRankSizeVariant.Rank5 ->
            RankedAttributeCardSizeStyle(
                minHeight = 104.dp,
                horizontalPadding = 16.dp,
                verticalPadding = 14.dp,
                progressHeight = 6.dp,
            )
        AttributeRankSizeVariant.Standard ->
            RankedAttributeCardSizeStyle(
                minHeight = 96.dp,
                horizontalPadding = 16.dp,
                verticalPadding = 14.dp,
                progressHeight = 6.dp,
            )
    }

@Composable
private fun NotesSection(notes: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier.semantics(mergeDescendants = false) {
                contentDescription = "Notes"
            },
    ) {
        Text(text = "Notes", style = MaterialTheme.typography.titleSmall)
        Text(text = notes, style = MaterialTheme.typography.bodyMedium)
    }
}
