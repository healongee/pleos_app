package com.example.myapplication

import com.example.myapplication.data.InteriorCameraConsent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InteriorCameraConsentTest {
    @Test fun deniedUntilExplicitOptInAndNeverAllowsOwnerAccessOffline() {
        val consent = InteriorCameraConsent()
        assertFalse(consent.customerOptedIn)
        consent.setCustomerOptIn(true)
        assertFalse(consent.customerOptedIn)
        consent.startPreview()
        assertFalse(consent.customerOptedIn)
        consent.setCustomerOptIn(true)
        assertTrue(consent.customerOptedIn)
        assertFalse(consent.ownerAccessAllowed)
        consent.setCustomerOptIn(false)
        assertFalse(consent.customerOptedIn)
        consent.setCustomerOptIn(true)
        consent.endPreview()
        assertFalse(consent.customerOptedIn)
        assertFalse(consent.ownerAccessAllowed)
        consent.setCustomerOptIn(true)
        assertFalse(consent.customerOptedIn)
        consent.startPreview()
        assertFalse(consent.customerOptedIn)
    }
}
