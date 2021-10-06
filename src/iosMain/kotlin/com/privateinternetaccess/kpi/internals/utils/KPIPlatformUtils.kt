package com.privateinternetaccess.kpi.internals.utils

import platform.Foundation.*


internal actual fun encodeToUtf8Base64(srcString: String): String {
    val data = NSString.create(string = srcString).dataUsingEncoding(encoding = NSUTF8StringEncoding)
    return data?.base64EncodedStringWithOptions(0) ?: throw RuntimeException("cannot encode string to utf8 data")
}
