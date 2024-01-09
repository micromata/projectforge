/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.sipgate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.sipgate.SipgateContact
import org.projectforge.framework.json.JsonUtils


class SipgateTest {
  @Test
  fun base64Test() {
    val tokenId = "token-FQ1V12"
    val token = "e68ead46-a7db-46cd-8a1a-44aed1e4e372"
    Assertions.assertEquals(
      "dG9rZW4tRlExVjEyOmU2OGVhZDQ2LWE3ZGItNDZjZC04YTFhLTQ0YWVkMWU0ZTM3Mg==",
      SipgateClient.base64Credentials(tokenId, token)
    )
  }

  @Test
  fun contactTest() {
    var contact = SipgateContact()
    Assertions.assertNull(contact.organizationArray)
    contact.organization = "Micromta GmbH"
    Assertions.assertEquals(1, contact.organizationArray!!.first().size)
    Assertions.assertEquals("Micromta GmbH", contact.organizationArray!!.first().first())
    contact.organization = null
    contact.division = "DevOps"
    Assertions.assertEquals(2, contact.organizationArray!!.first().size)
    Assertions.assertEquals("", contact.organizationArray!!.first().first())
    Assertions.assertEquals("DevOps", contact.organizationArray!!.first().get(1))

    contact.organizationArray = arrayOf(arrayOf("Micromata GmbH"))
    Assertions.assertEquals("Micromata GmbH", contact.organization)
    contact.organizationArray = arrayOf(arrayOf("Micromata GmbH", "DevOps"))
    Assertions.assertEquals("Micromata GmbH", contact.organization)
    Assertions.assertEquals("DevOps", contact.division)

    contact = JsonUtils.fromJson(json, SipgateContact::class.java)!!
    Assertions.assertEquals(SipgateContact.EmailType.HOME, contact.emails!!.first().type)
    Assertions.assertEquals("home", contact.emails!!.first().typeArray!!.first())

  }

  private val json = "{\n" +
      "      \"id\": \"E946CEAA-9C3E-11ED-B292-BEA196FC1130\",\n" +
      "      \"name\": \"Hurzel Meier\",\n" +
      "      \"picture\": null,\n" +
      "      \"emails\": [\n" +
      "        {\n" +
      "          \"email\": \"kai@acme.com\",\n" +
      "          \"type\": [\n" +
      "            \"home\"\n" +
      "          ]\n" +
      "        },\n" +
      "        {\n" +
      "          \"email\": \"kai@business.com\",\n" +
      "          \"type\": [\n" +
      "            \"work\"\n" +
      "          ]\n" +
      "        },\n" +
      "        {\n" +
      "          \"email\": \"kai@sonstiges.com\",\n" +
      "          \"type\": [\n" +
      "            \"other\"\n" +
      "          ]\n" +
      "        }\n" +
      "      ],\n" +
      "      \"numbers\": [\n" +
      "        {\n" +
      "          \"number\": \"01\",\n" +
      "          \"type\": [\n" +
      "            \"home\"\n" +
      "          ]\n" +
      "        },\n" +
      "        {\n" +
      "          \"number\": \"02\",\n" +
      "          \"type\": [\n" +
      "            \"work\"\n" +
      "          ]\n" +
      "        },\n" +
      "        {\n" +
      "          \"number\": \"03\",\n" +
      "          \"type\": [\n" +
      "            \"cell\"\n" +
      "          ]\n" +
      "        },\n" +
      "        {\n" +
      "          \"number\": \"04\",\n" +
      "          \"type\": [\n" +
      "            \"fax\",\n" +
      "            \"home\"\n" +
      "          ]\n" +
      "        },\n" +
      "        {\n" +
      "          \"number\": \"05\",\n" +
      "          \"type\": [\n" +
      "            \"fax\",\n" +
      "            \"work\"\n" +
      "          ]\n" +
      "        },\n" +
      "        {\n" +
      "          \"number\": \"06\",\n" +
      "          \"type\": [\n" +
      "            \"pager\"\n" +
      "          ]\n" +
      "        },\n" +
      "        {\n" +
      "          \"number\": \"07\",\n" +
      "          \"type\": [\n" +
      "            \"other\"\n" +
      "          ]\n" +
      "        }\n" +
      "      ],\n" +
      "      \"addresses\": [\n" +
      "        {\n" +
      "          \"poBox\": null,\n" +
      "          \"extendedAddress\": null,\n" +
      "          \"streetAddress\": \"adr1\",\n" +
      "          \"locality\": null,\n" +
      "          \"region\": null,\n" +
      "          \"postalCode\": null,\n" +
      "          \"country\": null\n" +
      "        },\n" +
      "        {\n" +
      "          \"poBox\": null,\n" +
      "          \"extendedAddress\": null,\n" +
      "          \"streetAddress\": \"adr2\",\n" +
      "          \"locality\": null,\n" +
      "          \"region\": null,\n" +
      "          \"postalCode\": null,\n" +
      "          \"country\": null\n" +
      "        },\n" +
      "        {\n" +
      "          \"poBox\": null,\n" +
      "          \"extendedAddress\": null,\n" +
      "          \"streetAddress\": \"adr3\",\n" +
      "          \"locality\": null,\n" +
      "          \"region\": null,\n" +
      "          \"postalCode\": null,\n" +
      "          \"country\": null\n" +
      "        }\n" +
      "      ],\n" +
      "      \"organization\": [\n" +
      "        [\n" +
      "          \"Micromata GmbH\",\n" +
      "          \"DevOps\"\n" +
      "        ]\n" +
      "      ],\n" +
      "      \"scope\": \"SHARED\"\n" +
      "    }"
}
