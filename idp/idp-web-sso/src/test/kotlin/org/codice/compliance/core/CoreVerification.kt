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
package org.codice.compliance.core

import org.codice.compliance.SAMLComplianceException
import org.codice.compliance.children
import org.w3c.dom.Node

/**
 * Verify response against the Core Spec document
 */
fun verifyCore(response: Node) {
    verifyEncryptedId(response)
    verifyCoreAssertion(response)
    verifyEncryptedAssertion(response)
}

/**
 * Verify the <EncryptedId> Element against the Core Spec document
 * 2.2.4 Element <EncryptedID>
 */
fun verifyEncryptedId(response: Node) {
    val encryptedIds = response.children("EncryptedID")
    if (encryptedIds.isNotEmpty()) {
        if (encryptedIds.any { it.children("EncryptedData").isEmpty() })
            throw SAMLComplianceException.create("10") // The <EncryptedID> element contains the following element: <xenc:EncryptedData> [Required]

        verifyEncryptedData(encryptedIds)

        // todo - (EncryptedData) For The encrypted content MUST contain an element that has a type of NameIDType or AssertionType, or a type that is derived from BaseIDAbstractType, NameIDType, or AssertionType.

        // todo Encrypted identifiers are intended as a privacy protection mechanism when the plain-text value passes through an intermediary. As such, the ciphertext MUST be unique to any given encryption operation. For more on such issues, see [XMLEnc] Section 6.3.
    }
}

/**
 * Verify the <Assertion> Element against the Core Spec document
 * 2.3.3 Element <Assertion>
 */
fun verifyCoreAssertion(response: Node) {
    val assertions = response.children("Assertion")
    if (assertions.isNotEmpty()) {
        for (assertion in assertions) {

            if (assertion.attributes.getNamedItem("Version").textContent != "2.0"
                    || assertion.attributes.getNamedItem("ID") == null
                    || assertion.attributes.getNamedItem("IssueInstant") == null)
                throw SAMLComplianceException.create("10") // The Version, ID, and IssueInstant attributes are required


            // Get assertion Assertions.children
            val issuers = assertion.children("Issuer")
            val signatures = assertion.children("Signature")
            val subjects = assertion.children("Subject")
            val conditions = assertion.children("Conditions")
            val advices = assertion.children("Advice")
            val statements = assertion.children("Statement")
            val authnStatements = assertion.children("AuthnStatement")
            val authzDecisionStatements = assertion.children("AuthzDecisionStatement")
            val attributeStatements = assertion.children("AttributeStatement")

            if (issuers.isEmpty()) throw SAMLComplianceException.create("10") // The Issuer attribute is required

            if (statements.isNotEmpty() && statements.any { it.attributes.getNamedItem("xsi:type") == null })
                throw SAMLComplianceException.create("10") // An xsi:type attribute MUST be used to indicate the actual statement type.


            if (statements.isEmpty()
                    && authnStatements.isEmpty()
                    && authzDecisionStatements.isEmpty()
                    && attributeStatements.isEmpty()
                    && subjects.isEmpty())
                throw SAMLComplianceException.create("10") // An assertion with no statements MUST contain a <Subject> element. Such an assertion identifies a principal in a manner which can be referenced or confirmed using SAML methods, but asserts no further information associated with that principal.
        }

    }
}

/**
 * Verify the <Assertion> Element against the Core Spec document
 * 2.3.4 Element <EncryptedAssertion>
 */
fun verifyEncryptedAssertion(response: Node) {
    val encryptedAssertion = response.children("EncryptedAssertion")
    if (encryptedAssertion.isNotEmpty()) {
        verifyEncryptedData(encryptedAssertion)
    }
}

/**
 * Verifies the <EncryptedData> element against the Core spec:
 *
 * "The encrypted content and associated encryption details, as defined by the XML Encryption
 * Syntax and Processing specification [XMLEnc]. The attribute SHOULD be present and, if Type
 * present, MUST contain a value of http://www.w3.org/2001/04/xmlenc#Element"
 *
 * @param node - a list of parent nodes containing the <EncryptedData> element
 */
private fun verifyEncryptedData(node: List<Node>) {
    if (!node
            .flatMap { it -> it.children("EncryptedData") }
            .filter { it.attributes.getNamedItem("Type") != null }
            .all { it.attributes.getNamedItem("Type").textContent == "http://www.w3.org/2001/04/xmlenc#Element" })
        throw SAMLComplianceException.create("10") // The Type attribute [for an EncryptedData] SHOULD be present and, if present, MUST contain a value of http://www.w3.org/2001/04/xmlenc#Element.
}