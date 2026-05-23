package com.juzgon.data.schema

import android.content.SharedPreferences
import com.juzgon.domain.schema.StoredSchemaVersionProvider

class SharedPrefsSchemaVersionProvider(
    private val prefs: SharedPreferences,
) : StoredSchemaVersionProvider {
    override fun getStoredVersion(): Int = prefs.getInt(KEY_SCHEMA_VERSION, 0)

    fun setStoredVersion(version: Int) {
        prefs.edit().putInt(KEY_SCHEMA_VERSION, version).apply()
    }

    companion object {
        const val KEY_SCHEMA_VERSION = "data_schema_version"
    }
}
