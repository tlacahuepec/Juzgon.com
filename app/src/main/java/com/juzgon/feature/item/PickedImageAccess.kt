package com.juzgon.feature.item

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

internal const val IMAGE_PICKER_MIME_TYPE = "image/*"
private const val PERSISTABLE_IMAGE_PERMISSION_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION

internal fun ContentResolver.persistReadAccessForPickedImage(uri: Uri): Boolean =
    persistReadAccessForPickedImage(uri, ::takePersistableUriPermission)

internal fun persistReadAccessForPickedImage(
    uri: Uri,
    takePersistableUriPermission: (Uri, Int) -> Unit,
): Boolean =
    runCatching {
        takePersistableUriPermission(uri, PERSISTABLE_IMAGE_PERMISSION_FLAGS)
    }.isSuccess
