package com.fraudlens.sdk.image.model

/**
 * Shared shape for image/video multipart uploads.
 */
data class MediaUploadRequest(
    val bytes: ByteArray,
    val filename: String,
    val contentType: String,
    val partName: String = "file",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MediaUploadRequest
        if (!bytes.contentEquals(other.bytes)) return false
        if (filename != other.filename) return false
        if (contentType != other.contentType) return false
        if (partName != other.partName) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + partName.hashCode()
        return result
    }
}
