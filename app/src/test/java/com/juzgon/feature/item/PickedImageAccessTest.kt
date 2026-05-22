package com.juzgon.feature.item

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PickedImageAccessTest {
    @Test
    fun persistReadAccessForPickedImageRequestsReadPermission() {
        val uri = Uri.parse("content://images/roadster")
        var capturedUri: Uri? = null
        var capturedFlags: Int? = null

        val result =
            persistReadAccessForPickedImage(uri) { requestedUri, requestedFlags ->
                capturedUri = requestedUri
                capturedFlags = requestedFlags
            }

        assertTrue(result)
        assertEquals(uri, capturedUri)
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, capturedFlags)
    }

    @Test
    fun persistReadAccessForPickedImageReturnsFalseWhenPermissionCannotBePersisted() {
        val uri = Uri.parse("content://images/roadster")

        val result =
            persistReadAccessForPickedImage(uri) { _, _ ->
                throw SecurityException("No persistable permission")
            }

        assertFalse(result)
    }
}
