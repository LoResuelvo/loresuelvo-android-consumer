package com.loresuelvo.consumer.data.api.mapper

/**
 * Best-effort ISO-8601 parser for the backend's
 * `YYYY-MM-DDTHH:MM:SS[.SSSSSS][Z]` shape. We can't use
 * `java.time.Instant` because `minSdk = 24`; `SimpleDateFormat`
 * is API-1 friendly and sufficient for every wire in this repo.
 *
 * The dev backend emits microseconds + trailing `Z` (e.g.
 * `2026-08-07T15:46:40.928659Z`); an older endpoint revision
 * emits bare seconds. Tries each shape in turn and returns
 * `null` when nothing parses — the caller falls back to `0L` so
 * the row still renders rather than crashing on a backend
 * regression.
 *
 * `internal` so any mapper in `data/api/mapper/` can reuse it
 * without going through the public API surface. The legacy
 * `parseIsoMillisOrZero` copies in [ConversationDtoMapper] and
 * [DiagnosisDtoMapper] predate this helper and are left alone
 * (each carries a slightly different pattern list per its
 * wire); a future cleanup can collapse them once all three are
 * migrated.
 */
internal fun parseIsoTimestampMillisOrZero(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    // `SimpleDateFormat` is lenient on trailing characters by
    // default; the literal `'Z'` (RFC 822 TZ letter) in the
    // pattern is interpreted as a literal `Z` (not the RFC 822
    // offset). Combined with leniency, the parser accepts both
    // `…Z` and `…+0000` suffixes plus any microsecond tail.
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .parse(value)?.time
        }.getOrNull()
    }
}