/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.gateway.compare

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardDavNormalizerTest {
    private val baseUrls = listOf("https://pf.example.com", "https://gw.example.com")

    private fun response(base: String, id: Long, etag: String, uid: String) =
        "<d:response><d:href>$base/carddav/users/joe/addressbooks/ProjectForge-$id.vcf</d:href><d:propstat><d:prop>" +
                "<d:getetag>\"$etag\"</d:getetag><card:address-data>BEGIN:VCARD\r\nUID:$uid\r\nREV:$etag\r\n" +
                "PHOTO;VALUE=URI:/carddav/photos/contact-$id.jpg\r\nEND:VCARD</card:address-data></d:prop></d:propstat></d:response>"

    private fun multistatus(vararg responses: String) =
        "<?xml version=\"1.0\"?><d:multistatus xmlns:d=\"DAV:\">${responses.joinToString("")}" +
                "<d:sync-token>\"sync-${responses.size}\"</d:sync-token></d:multistatus>"

    @Test
    fun ignoresBaseUrlIdsEtagsSyncTokenRevAndOrder() {
        val main = multistatus(response("https://pf.example.com", 1, "e1", "u1"), response("https://pf.example.com", 2, "e2", "u2"))
        val gateway = multistatus(response("https://gw.example.com", 22, "x2", "u2"), response("https://gw.example.com", 11, "x1", "u1"))
        assertEquals(CardDavNormalizer.normalize(main, baseUrls), CardDavNormalizer.normalize(gateway, baseUrls))
        val other = multistatus(response("https://gw.example.com", 22, "x2", "u2"), response("https://gw.example.com", 11, "x1", "u3"))
        assertEquals(
            listOf("-UID:u1", "+UID:u3"),
            GatewayCompare.lineDiff(CardDavNormalizer.normalize(main, baseUrls), CardDavNormalizer.normalize(other, baseUrls)),
        )
    }

    @Test
    fun extractsValuesAndHrefs() {
        val main = multistatus(response("", 1, "e1", "u1"), response("", 2, "e2", "u2"))
        assertEquals(listOf("\"e1\"", "\"e2\""), CardDavNormalizer.values(main, "getetag"))
        assertEquals(
            listOf("/carddav/users/joe/addressbooks/ProjectForge-1.vcf", "/carddav/users/joe/addressbooks/ProjectForge-2.vcf"),
            CardDavNormalizer.vcfHrefs(main),
        )
    }

    @Test
    fun masksEncryptedParamsOfSubscriptionUrls() {
        assertEquals(
            "https://pf.example.com/export/ProjectForge.ics?user=42&q=***",
            GatewayCompare.maskUrl("https://pf.example.com/export/ProjectForge.ics?user=42&q=secret-AbC_12"),
        )
    }
}
