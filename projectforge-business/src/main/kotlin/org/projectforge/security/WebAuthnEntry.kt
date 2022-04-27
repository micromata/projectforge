package org.projectforge.security

import com.webauthn4j.authenticator.Authenticator

class WebAuthnEntry(val credentialId: ByteArray, val authenticator: Authenticator)
