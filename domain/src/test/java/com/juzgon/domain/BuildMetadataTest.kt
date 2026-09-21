package com.juzgon.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildMetadataTest {
    @Test
    fun buildMetadata_exposesVersionName() {
        val metadata =
            BuildMetadata(
                versionName = "1.0",
                versionCode = 1,
                channel = "dev",
                gitSha = "abc1234",
                buildTimestamp = "1700000000000",
            )

        assertEquals("1.0", metadata.versionName)
    }

    @Test
    fun buildMetadata_exposesChannel() {
        val metadata =
            BuildMetadata(
                versionName = "2.0",
                versionCode = 5,
                channel = "release",
                gitSha = "def5678",
                buildTimestamp = "1700000000000",
            )

        assertEquals("release", metadata.channel)
    }

    @Test
    fun buildMetadataProvider_returnsMetadata() {
        val provider =
            BuildMetadataProvider {
                BuildMetadata(
                    versionName = "1.0",
                    versionCode = 1,
                    channel = "dev",
                    gitSha = "abc1234",
                    buildTimestamp = "1700000000000",
                )
            }

        val result = provider.get()

        assertEquals("1.0", result.versionName)
        assertEquals(1, result.versionCode)
        assertEquals("dev", result.channel)
        assertEquals("abc1234", result.gitSha)
        assertEquals("1700000000000", result.buildTimestamp)
    }
}
