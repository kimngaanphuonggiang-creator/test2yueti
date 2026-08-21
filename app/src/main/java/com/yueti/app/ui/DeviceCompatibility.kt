package com.yueti.app.ui

import android.os.Build

internal fun requiresStableMaterialControls(
    screenWidthDp: Int,
    fontScale: Float,
    manufacturer: String = Build.MANUFACTURER,
    brand: String = Build.BRAND,
): Boolean = screenWidthDp < 600 ||
    fontScale > 1.1f ||
    manufacturer.equals("Meizu", ignoreCase = true) ||
    brand.equals("Meizu", ignoreCase = true)
