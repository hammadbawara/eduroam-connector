package com.hz_apps.autowificonnector

import java.security.MessageDigest

fun generateSecureActivationCode1(uuid: String, rounds: Int = 10): String {
    var hash = uuid.toByteArray(Charsets.UTF_8)
    val md = MessageDigest.getInstance("SHA-256")
    repeat(rounds) {
        hash = md.digest(hash)
    }
    return hash.joinToString("") { "%02x".format(it) }
}