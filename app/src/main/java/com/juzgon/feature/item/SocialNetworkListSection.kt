@file:Suppress("FunctionName")

package com.juzgon.feature.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.juzgon.domain.social.SocialNetworkCodec
import com.juzgon.domain.social.SocialNetworkEntry
import com.juzgon.domain.social.SocialPlatformIcons

@Composable
internal fun SocialNetworkListSection(value: String) {
    val entries = SocialNetworkCodec.parse(value)
    if (entries.isEmpty()) return
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { entry ->
            SocialNetworkRow(
                entry = entry,
                onClick = { uriHandler.openUri(entry.profileUrl) },
            )
        }
    }
}

@Composable
private fun SocialNetworkRow(
    entry: SocialNetworkEntry,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(
            painter = painterResource(SocialPlatformIcons.iconRes(entry.platform)),
            contentDescription = entry.platform.displayName,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = entry.handle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
