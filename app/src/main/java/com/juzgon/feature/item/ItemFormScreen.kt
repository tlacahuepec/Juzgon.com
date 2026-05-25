@file:Suppress("FunctionName", "LongMethod", "LongParameterList", "TooManyFunctions")

package com.juzgon.feature.item

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.juzgon.domain.AttributeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ItemFormRoute(
    categoryName: String,
    itemId: String? = null,
    onBackClick: () -> Unit,
    onSaveCompleted: () -> Unit,
    onDeleteCompleted: () -> Unit = {},
    onNavigateToGeminiSettings: () -> Unit = {},
    viewModel: ItemFormViewModel = hiltViewModel(),
) {
    LaunchedEffect(categoryName, itemId) {
        viewModel.loadCategory(categoryName, itemId)
    }

    val context = LocalContext.current
    var pendingImageAttributeId by remember { mutableStateOf<String?>(null) }
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val attributeId = pendingImageAttributeId ?: return@rememberLauncherForActivityResult
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            val selectedImages =
                uris.mapNotNull { uri ->
                    if (!context.contentResolver.persistReadAccessForPickedImage(uri)) {
                        null
                    } else {
                        val metadata = context.contentResolver.imageMetadata(uri)
                        SelectedImageMetadata(
                            sourceUri = uri.toString(),
                            mimeType = metadata.mimeType,
                            sizeBytes = metadata.sizeBytes,
                            width = metadata.width,
                            height = metadata.height,
                            displayName = metadata.displayName,
                        )
                    }
                }
            if (selectedImages.isEmpty()) {
                viewModel.onImageSelectionFailed()
                return@rememberLauncherForActivityResult
            }
            viewModel.onImagesSelected(
                attributeId = attributeId,
                selectedImages = selectedImages,
            )
            if (selectedImages.size < uris.size) {
                viewModel.onImageSelectionFailed()
            }
        }

    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            onSaveCompleted()
        }
    }
    LaunchedEffect(state.deleteCompleted) {
        if (state.deleteCompleted) {
            onDeleteCompleted()
        }
    }

    ItemFormScreen(
        state = state,
        onTitleChange = viewModel::onTitleChanged,
        onNotesChange = viewModel::onNotesChanged,
        onScoreChange = viewModel::onScoreChanged,
        onScoreIncrement = viewModel::onScoreIncrement,
        onScoreDecrement = viewModel::onScoreDecrement,
        onValueChange = viewModel::onValueChanged,
        onImageSelectClick = { attributeId ->
            pendingImageAttributeId = attributeId
            imagePicker.launch(arrayOf(IMAGE_PICKER_MIME_TYPE))
        },
        onImageRemoveClick = viewModel::onImageRemoved,
        onSaveClick = viewModel::onSaveClick,
        onBackClick = onBackClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteCancel = viewModel::onDeleteCancel,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onSuggestClick = viewModel::onSuggestClick,
        onSuggestionAccepted = viewModel::onSuggestionAccepted,
        onSuggestionDismissed = viewModel::onSuggestionDismissed,
        onNavigateToGeminiSettings = onNavigateToGeminiSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormScreen(
    state: ItemFormUiState,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onScoreChange: (String, String) -> Unit,
    onScoreIncrement: (String) -> Unit = {},
    onScoreDecrement: (String) -> Unit = {},
    onValueChange: (String, String) -> Unit = { _, _ -> },
    onImageSelectClick: (String) -> Unit = {},
    onImageRemoveClick: (String, String) -> Unit = { _, _ -> },
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onDeleteCancel: () -> Unit = {},
    onDeleteConfirm: () -> Unit = {},
    onSuggestClick: (String) -> Unit = {},
    onSuggestionAccepted: () -> Unit = {},
    onSuggestionDismissed: () -> Unit = {},
    onNavigateToGeminiSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val titleError = if (state.showValidationErrors) state.titleError else null
    val scoreErrors =
        if (state.showValidationErrors) {
            state.scoreErrors
        } else {
            List(state.scores.size) { ItemScoreValidationError() }
        }
    val valueErrors =
        if (state.showValidationErrors) {
            state.valueErrors
        } else {
            List(state.values.size) { ItemValueValidationError() }
        }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDeleteCancel,
            title = { Text("Delete item?") },
            text = { Text("This will permanently delete the item and all its ratings.") },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = onDeleteCancel) { Text("Cancel") }
            },
        )
    }

    if (state.enrichmentSheet != EnrichmentSheetState.Hidden) {
        EnrichmentSuggestionSheet(
            state = state.enrichmentSheet,
            onAccept = onSuggestionAccepted,
            onDismiss = onSuggestionDismissed,
            onNavigateToSettings = onNavigateToGeminiSettings,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.mode == ItemFormMode.Edit) "Edit item" else "Add item") },
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
                    if (state.mode == ItemFormMode.Edit) {
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
            else ->
                ItemFormContent(
                    state = state,
                    titleError = titleError,
                    scoreErrors = scoreErrors,
                    valueErrors = valueErrors,
                    onTitleChange = onTitleChange,
                    onNotesChange = onNotesChange,
                    onScoreChange = onScoreChange,
                    onScoreIncrement = onScoreIncrement,
                    onScoreDecrement = onScoreDecrement,
                    onValueChange = onValueChange,
                    onImageSelectClick = onImageSelectClick,
                    onImageRemoveClick = onImageRemoveClick,
                    onSuggestClick = onSuggestClick,
                    enrichmentLoading = state.enrichmentSheet is EnrichmentSheetState.Loading,
                    onSaveClick = onSaveClick,
                    modifier = Modifier.padding(innerPadding),
                )
        }
    }
}

@Composable
private fun ItemFormContent(
    state: ItemFormUiState,
    titleError: String?,
    scoreErrors: List<ItemScoreValidationError>,
    valueErrors: List<ItemValueValidationError>,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onScoreChange: (String, String) -> Unit,
    onScoreIncrement: (String) -> Unit,
    onScoreDecrement: (String) -> Unit,
    onValueChange: (String, String) -> Unit,
    onImageSelectClick: (String) -> Unit,
    onImageRemoveClick: (String, String) -> Unit,
    onSuggestClick: (String) -> Unit,
    enrichmentLoading: Boolean,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
        Text(
            text = state.categoryName,
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            enabled = state.titleEditable,
            label = { Text("Item title") },
            isError = titleError != null,
            supportingText = {
                titleError?.let { Text(it) }
            },
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Item title"
                    },
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Item notes"
                    },
        )

        state.scores.forEachIndexed { index, scoreInput ->
            ItemScoreField(
                scoreInput = scoreInput,
                validationError = scoreErrors[index],
                onScoreChange = onScoreChange,
                onScoreIncrement = onScoreIncrement,
                onScoreDecrement = onScoreDecrement,
            )
        }

        state.values.forEachIndexed { index, valueInput ->
            ItemAttributeValueField(
                valueInput = valueInput,
                validationError = valueErrors[index],
                onValueChange = onValueChange,
                onImageSelectClick = onImageSelectClick,
                onImageRemoveClick = onImageRemoveClick,
                onSuggestClick = onSuggestClick,
                enrichmentLoading = enrichmentLoading,
            )
        }

        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = onSaveClick,
            enabled = state.saveEnabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Save item"
                        role = Role.Button
                    },
        ) {
            Text(if (state.isSaving) "Saving" else "Save item")
        }
    }
}

@Composable
private fun ItemScoreField(
    scoreInput: ItemScoreInput,
    validationError: ItemScoreValidationError,
    onScoreChange: (String, String) -> Unit,
    onScoreIncrement: (String) -> Unit,
    onScoreDecrement: (String) -> Unit,
) {
    val attributeId = scoreInput.attribute.id
    val label = "$attributeId score"
    val sliderValue = scoreInput.scoreText.toIntOrNull()?.toFloat() ?: SCORE_MIN.toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Slider(
            value = sliderValue,
            onValueChange = { onScoreChange(attributeId, it.toInt().toString()) },
            valueRange = SCORE_MIN.toFloat()..SCORE_MAX.toFloat(),
            steps = SCORE_MAX - SCORE_MIN - 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "$attributeId slider" },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onScoreDecrement(attributeId) },
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics {
                            contentDescription = "Decrease $label"
                            role = Role.Button
                        },
            ) {
                Text(text = "−", style = MaterialTheme.typography.titleLarge)
            }
            OutlinedTextField(
                value = scoreInput.scoreText,
                onValueChange = { onScoreChange(attributeId, it) },
                label = { Text(label) },
                isError = validationError.score != null,
                supportingText = {
                    validationError.score?.let { Text(it) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = label
                        },
            )
            IconButton(
                onClick = { onScoreIncrement(attributeId) },
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics {
                            contentDescription = "Increase $label"
                            role = Role.Button
                        },
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun ItemAttributeValueField(
    valueInput: ItemValueInput,
    validationError: ItemValueValidationError,
    onValueChange: (String, String) -> Unit,
    onImageSelectClick: (String) -> Unit,
    onImageRemoveClick: (String, String) -> Unit,
    onSuggestClick: (String) -> Unit,
    enrichmentLoading: Boolean,
) {
    val attributeId = valueInput.attribute.id
    val cd = "$attributeId value"
    when (valueInput.attribute.type) {
        AttributeType.BOOLEAN -> {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = cd },
            ) {
                Text(text = attributeId, style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = valueInput.valueText == "true",
                    onCheckedChange = { checked -> onValueChange(attributeId, checked.toString()) },
                )
            }
        }
        AttributeType.IMAGE -> {
            ImageAttributeValueField(
                valueInput = valueInput,
                validationError = validationError,
                onImageSelectClick = onImageSelectClick,
                onImageRemoveClick = onImageRemoveClick,
            )
        }
        AttributeType.NATIONALITY -> {
            NationalityAutocompleteField(
                attributeId = attributeId,
                valueText = valueInput.valueText,
                onValueChange = onValueChange,
                isError = validationError.value != null,
                errorText = validationError.value,
            )
        }
        AttributeType.DATE -> {
            val showSuggest =
                valueInput.attribute.displayName.contains("Birth Date", ignoreCase = true)
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = valueInput.valueText,
                    onValueChange = { onValueChange(attributeId, it) },
                    label = { Text(attributeId) },
                    placeholder = { Text("YYYY-MM-DD") },
                    isError = validationError.value != null,
                    supportingText = {
                        validationError.value?.let { Text(it) }
                    },
                    singleLine = true,
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics { contentDescription = cd },
                )
                if (showSuggest) {
                    IconButton(
                        onClick = { onSuggestClick(attributeId) },
                        enabled = !enrichmentLoading,
                        modifier =
                            Modifier
                                .padding(top = 8.dp)
                                .semantics {
                                    contentDescription = "Suggest ${valueInput.attribute.displayName}"
                                },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
        else -> {
            OutlinedTextField(
                value = valueInput.valueText,
                onValueChange = { onValueChange(attributeId, it) },
                label = { Text(attributeId) },
                isError = validationError.value != null,
                supportingText = {
                    validationError.value?.let { Text(it) }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = cd },
            )
        }
    }
}

@Composable
private fun ImageAttributeValueField(
    valueInput: ItemValueInput,
    validationError: ItemValueValidationError,
    onImageSelectClick: (String) -> Unit,
    onImageRemoveClick: (String, String) -> Unit,
) {
    val attributeId = valueInput.attribute.id
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$attributeId image value" },
    ) {
        Text(text = attributeId, style = MaterialTheme.typography.bodyLarge)
        if (valueInput.imageReferences.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    items = valueInput.imageReferences,
                    key = { imageReference -> imageReference.id },
                ) { imageReference ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ImageAttributePreview(
                            imageReference = imageReference,
                            contentDescription = "$attributeId image preview",
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 120.dp, maxWidth = 120.dp)
                                    .height(100.dp),
                        )
                        TextButton(
                            onClick = { onImageRemoveClick(attributeId, imageReference.id) },
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Remove $attributeId image ${imageReference.id}"
                                    role = Role.Button
                                },
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { onImageSelectClick(attributeId) },
                modifier =
                    Modifier.semantics {
                        contentDescription = "Select $attributeId image"
                        role = Role.Button
                    },
            ) {
                Text(if (valueInput.imageReferences.isEmpty()) "Select images" else "Add images")
            }
            if (valueInput.imageReferences.isNotEmpty()) {
                TextButton(
                    onClick = { onImageRemoveClick(attributeId, valueInput.imageReferences.last().id) },
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Remove $attributeId image"
                            role = Role.Button
                        },
                ) {
                    Text("Remove image")
                }
            }
        }
        validationError.value?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ImageAttributePreview(
    imageReference: ItemImageReference,
    contentDescription: String,
    modifier: Modifier = Modifier,
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
                        maxDimensionPx = 384,
                    )
                }
        }
    val resolvedBitmap = bitmap
    if (resolvedBitmap != null) {
        Image(
            bitmap = resolvedBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier =
                modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.small,
                    ).semantics { this.contentDescription = contentDescription },
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

private data class PickedImageMetadata(
    val mimeType: String?,
    val sizeBytes: Long?,
    val displayName: String?,
    val width: Int?,
    val height: Int?,
)

private fun ContentResolver.imageMetadata(uri: Uri): PickedImageMetadata {
    var sizeBytes: Long? = null
    var displayName: String? = null
    query(uri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst()) {
            sizeBytes = cursor.longOrNull(sizeIndex)
            displayName = cursor.stringOrNull(displayNameIndex)
        }
    }
    val bounds = imageBounds(uri)
    return PickedImageMetadata(
        mimeType = getType(uri),
        sizeBytes = sizeBytes,
        displayName = displayName,
        width = bounds?.first,
        height = bounds?.second,
    )
}

private fun android.database.Cursor.longOrNull(columnIndex: Int): Long? =
    if (columnIndex >= 0 && !isNull(columnIndex)) getLong(columnIndex) else null

private fun android.database.Cursor.stringOrNull(columnIndex: Int): String? =
    if (columnIndex >= 0 && !isNull(columnIndex)) getString(columnIndex) else null

private fun ContentResolver.imageBounds(uri: Uri): Pair<Int, Int>? {
    val options =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
    return runCatching {
        openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }
        if (options.outWidth > 0 && options.outHeight > 0) {
            options.outWidth to options.outHeight
        } else {
            null
        }
    }.getOrNull()
}

private fun imageBitmapFromValue(
    contentResolver: ContentResolver,
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
    val width = outWidth
    val height = outHeight
    if (width <= 0 || height <= 0) return 1

    var sampleSize = 1
    while (width / sampleSize > maxDimensionPx || height / sampleSize > maxDimensionPx) {
        sampleSize *= 2
    }
    return sampleSize
}
