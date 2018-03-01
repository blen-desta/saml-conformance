/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.compliance.bindings

import org.codice.compliance.RELAY_STATE
import org.codice.compliance.SAMLComplianceException
import org.codice.compliance.idpMetadata
import org.codice.security.sign.SimpleSign
import org.w3c.dom.Node
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Verify the response for a redirect binding
 */
fun verifyRedirect(responseDomElement: Node, parsedResponse: Map<String, String>, givenRelayState: Boolean) {
    parsedResponse["RelayState"]?.let { verifyRedirectRelayState(it, givenRelayState) }
    parsedResponse["Signature"]?.let { verifyRedirectSignature(it, parsedResponse) }
    parsedResponse["SigAlg"]?.let { verifyRedirectSigAlg(it) }
}

/**
 * Verifies the signature algorithm according to the redirect binding rules in the binding spec
 */
fun verifyRedirectSigAlg(sigAlg: String) {

}

/**
 * Verifies the signature according to the redirect binding rules in the binding spec
 * 3.4.4.1 DEFLATE Encoding
 */
fun verifyRedirectSignature(signature: String, parsedResponse: Map<String, String>) {
    val verify: Boolean
    try {
        verify = SimpleSign().validateSignature(
                "SAMLResponse",
                parsedResponse["SAMLResponse"],
                parsedResponse["RelayState"],
                signature,
                parsedResponse["SigAlg"],
                idpMetadata.signingCertificate)
    } catch (e: SimpleSign.SignatureException) {
        throw SAMLComplianceException.create("SAMLBindings.3.4.4.1_d")
    }
    if (!verify)
        throw SAMLComplianceException.create("SAMLBindings.3.4.4.1_d")
}

/**
 * Verifies the relay state according to the redirect binding rules in the binding spec
 * 3.4.3 RelayState
 * 3.4.4.1 DEFLATE Encoding
 */
fun verifyRedirectRelayState(encodedRelayState: String, givenRelayState: Boolean) {
    val decodedRelayState: String

    try {
        decodedRelayState = URLDecoder.decode(encodedRelayState, StandardCharsets.UTF_8.name())
    } catch (e: UnsupportedEncodingException) {
        throw SAMLComplianceException.create("SAMLBindings.3.4.4.1_c1")
    }

    if (decodedRelayState.toByteArray().size > 80) {
        throw SAMLComplianceException.create("SAMLBindings.3.4.3_a")
    }

    if (givenRelayState) {
        if (decodedRelayState != RELAY_STATE) {
            if (encodedRelayState == RELAY_STATE) {
                throw SAMLComplianceException.create("SAMLBindings.3.4.4.1_c1")
            }
            throw SAMLComplianceException.create("SAMLBindings.3.4.3_b1")
        }
    }
}