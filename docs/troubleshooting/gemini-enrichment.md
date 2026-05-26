# Gemini Enrichment API

## Summary

Troubleshooting guide for the AI-powered attribute enrichment feature that calls the Google Gemini REST API. Use this guide when the "Suggest" button fails or returns unexpected errors.

## Affected commands or workflows

- Tap the **Suggest** button on a supported attribute (e.g., Birth Date) in the item form.
- The app calls `https://generativelanguage.googleapis.com/v1beta/models/<model>:generateContent`.

## Debugging with logcat

### PowerShell (Windows)

```powershell
# All enrichment lifecycle events (start, success, failure, accept, dismiss)
adb logcat -s JuzgonEnrichment

# Error-level logs mentioning Gemini (includes HTTP code and response body)
adb logcat *:E | Select-String -Pattern "gemini" -CaseSensitive:$false
```

### Bash (macOS / Linux)

```bash
# All enrichment lifecycle events
adb logcat -s JuzgonEnrichment

# Error-level logs mentioning Gemini
adb logcat *:E | grep -i gemini
```

## Symptoms

| User-facing message | Likely failure code | Meaning |
|---|---|---|
| "Invalid API key. Check your Gemini key in settings." | `INVALID_API_KEY` | HTTP 401 or 403 from Gemini |
| "Network error. Check your connection and try again." | `NETWORK_ERROR` | IOException (no connectivity) |
| "Too many requests. Please wait and try again." | `RATE_LIMITED` | HTTP 429 |
| "Couldn't connect to Gemini. Check your API key, connection, or quota." | `PROVIDER_ERROR` or `INVALID_RESPONSE_FORMAT` | Catch-all — check logcat for detail |

## Common issues

### Model deprecated (HTTP 404)

```text
Gemini API error: HTTP 404 body={"error":{"message":"This model models/gemini-2.0-flash is no longer available..."}}
```

**Root cause:** Google periodically sunsets older model versions.

**Fix:** Update the `MODEL` constant in `GeminiApiClient.kt`:

```kotlin
private companion object {
    const val MODEL = "gemini-2.5-flash"  // Update to current available model
}
```

Verify available models in Google AI Studio at https://aistudio.google.com/ under your API key's quota page.

### Invalid API key (HTTP 401 / 403)

**Root cause:** Key is revoked, restricted to specific APIs, or has IP restrictions.

**Fix:** Generate a new key in Google AI Studio and update it in app Settings.

### Rate limited (HTTP 429)

**Root cause:** Free-tier keys have low RPM limits (as low as 2 RPM for some models).

**Fix:** Wait and retry, or upgrade to a paid tier.

### Response parsing failure (`INVALID_RESPONSE_FORMAT`)

**Root cause:** Model returned non-JSON text or wrapped JSON in markdown fences.

**Fix:** The `stripMarkdownFences()` method in `GeminiApiClient` handles common cases. If a new format appears, update that method.

## Verification

After fixing, rebuild and run:

```powershell
adb logcat -s JuzgonEnrichment
```

Expected success output:

```text
D JuzgonEnrichment: Enrichment started provider=Gemini attribute=... catalogType=...
D JuzgonEnrichment: Enrichment succeeded provider=Gemini attribute=... confidence=HIGH sourceCount=1 durationMs=...
```

## Related files

- `app/src/main/java/com/juzgon/data/enrichment/GeminiApiClient.kt` — HTTP client, model name, request/response handling
- `app/src/main/java/com/juzgon/data/enrichment/GeminiAttributeEnrichmentProvider.kt` — orchestration and error mapping
- `app/src/main/java/com/juzgon/data/enrichment/EnrichmentLogger.kt` — structured audit logging
- `app/src/main/java/com/juzgon/data/enrichment/GeminiResponseParser.kt` — JSON response parsing
- `app/src/main/java/com/juzgon/feature/item/EnrichmentSuggestionSheet.kt` — UI error messages
