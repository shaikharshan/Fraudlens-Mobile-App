package com.fraudlens.sdk.internal

/** Retrofit requires a trailing slash on base URL. */
internal fun String.withTrailingSlash(): String =
    if (endsWith('/')) this else "$this/"
