package com.privateinternetaccess.kpi.internals.utils

import android.util.Base64
import kotlin.text.toByteArray

internal actual fun encodeToUtf8Base64(srcString: String): String = Base64.encodeToString(srcString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
