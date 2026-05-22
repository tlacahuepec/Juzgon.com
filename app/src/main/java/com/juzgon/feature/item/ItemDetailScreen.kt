@file:Suppress("FunctionName", "LongMethod")

package com.juzgon.feature.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun ItemDetailRoute(
    itemId: String,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    val state by viewModel.state.collectAsState()
    ItemDetailScreen(
        state = state,
        onBackClick = onBackClick,
        onEditClick = onEditClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    state: ItemDetailUiState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
private fun ItemDetailContent(
    state: ItemDetailUiState,
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
        OverallScoreSection(overallScoreText = state.overallScoreText)
        HorizontalDivider()
        RankedAttributeProgressCards(rankedAttributes = state.rankedAttributes)
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
private fun RankedAttributeProgressCards(rankedAttributes: List<RankedAttributeCardUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Ranked attributes", style = MaterialTheme.typography.titleSmall)
        rankedAttributes.forEach { rankedAttribute ->
            RankedAttributeCard(rankedAttribute)
        }
    }
}

@Composable
private fun RankedAttributeCard(rankedAttribute: RankedAttributeCardUiModel) {
    val sizeStyle = rankedAttribute.sizeVariant.cardSizeStyle()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
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
