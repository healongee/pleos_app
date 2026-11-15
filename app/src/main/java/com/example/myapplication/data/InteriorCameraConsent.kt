package com.example.myapplication.data

/** Offline, in-memory consent preview. Never opens a camera or grants remote owner access. */
class InteriorCameraConsent {
    var previewActive: Boolean = false
        private set
    var customerOptedIn: Boolean = false
        private set

    fun startPreview() {
        previewActive = true
        customerOptedIn = false
    }

    fun setCustomerOptIn(allowed: Boolean) {
        customerOptedIn = previewActive && allowed
    }

    fun endPreview() {
        customerOptedIn = false
        previewActive = false
    }

    // An offline preview cannot authenticate a real customer or owner or authorize a stream.
    val ownerAccessAllowed: Boolean get() = false
}
