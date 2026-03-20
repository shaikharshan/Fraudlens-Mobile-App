package com.fraudlens.sdk.audio.model

/**
 * @param partName Multipart form field name expected by the server (default `file`; change to match your API)
 * @param contentType MIME type, e.g. `audio/wav`, `audio/mpeg`
 */
data class VoiceDetectionRequest(
    val audioBytes: ByteArray,
    val filename: String,
    val contentType: String,
    val partName: String = "file",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VoiceDetectionRequest
        if (!audioBytes.contentEquals(other.audioBytes)) return false
        if (filename != other.filename) return false
        if (contentType != other.contentType) return false
        if (partName != other.partName) return false
        return true
    }

    override fun hashCode(): Int {
        var result = audioBytes.contentHashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + partName.hashCode()
        return result
    }
}
