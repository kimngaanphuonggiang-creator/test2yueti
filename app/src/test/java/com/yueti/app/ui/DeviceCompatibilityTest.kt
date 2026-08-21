package com.yueti.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCompatibilityTest {
    @Test
    fun meizuUsesStableControlsEvenOnWideScreen() {
        assertTrue(requiresStableMaterialControls(840, 1f, manufacturer = "Meizu", brand = "meizu"))
    }

    @Test
    fun compactOrLargeTextUsesStableControls() {
        assertTrue(requiresStableMaterialControls(412, 1f, manufacturer = "Google", brand = "google"))
        assertTrue(requiresStableMaterialControls(840, 1.3f, manufacturer = "Google", brand = "google"))
    }

    @Test
    fun standardTabletMayUseExpressiveControls() {
        assertFalse(requiresStableMaterialControls(840, 1f, manufacturer = "Google", brand = "google"))
    }
}
