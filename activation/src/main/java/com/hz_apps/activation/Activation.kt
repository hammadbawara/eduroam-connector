package com.hz_apps.activation

import java.security.MessageDigest

object Activation {
    fun generateSecureActivationCode(uuid: String, rounds: Int = 10): String {
        return generateSecureActivationCode1(uuid, rounds)

    }

    private fun generateSecureActivationCode1(uuid: String, rounds: Int = 10): String {
        // Convert the UUID to a string and get the initial byte array
        var hash = uuid.toByteArray(Charsets.UTF_8)

        // Perform SHA-256 hashing in multiple rounds
        val md = MessageDigest.getInstance("SHA-256")
        repeat(rounds) {
            hash = md.digest(hash)
        }

        // Convert the final hash to a hexadecimal string
        return hash.joinToString("") { "%02x".format(it) }
    }
}

