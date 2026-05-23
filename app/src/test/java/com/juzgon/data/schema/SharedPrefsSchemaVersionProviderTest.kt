package com.juzgon.data.schema

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SharedPrefsSchemaVersionProviderTest {
    private lateinit var prefs: SharedPreferences
    private lateinit var provider: SharedPrefsSchemaVersionProvider

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences("test_schema_version", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        provider = SharedPrefsSchemaVersionProvider(prefs)
    }

    @Test
    fun `returns zero when no version stored`() {
        assertEquals(0, provider.getStoredVersion())
    }

    @Test
    fun `stores and retrieves version`() {
        provider.setStoredVersion(3)
        assertEquals(3, provider.getStoredVersion())
    }

    @Test
    fun `overwrites previous version`() {
        provider.setStoredVersion(1)
        provider.setStoredVersion(2)
        assertEquals(2, provider.getStoredVersion())
    }
}
