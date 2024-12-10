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

package org.projectforge.carddav

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CardDavXmlUtilsTest {
    @Test
    fun `extract xml root element`() {
        "<d:multistatus xmlns:d=\"DAV:\" xmlns:cs=\"urn:ietf:params:xml:ns:carddav\">\n".let { xml ->
            Assertions.assertEquals("multistatus", CardDavXmlUtils.getRootElement(xml))
        }
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n  <multistatus xmlns:d=\"DAV:\" xmlns:cs=\"urn:ietf:params:xml:ns:carddav\">\n".let { xml ->
            Assertions.assertEquals("multistatus", CardDavXmlUtils.getRootElement(xml))
        }
    }

    @Test
    fun `extract address ids`() {
        """
            |<card:addressbook-multiget xmlns:card="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/" xmlns:d="DAV:">
            |  <d:prop>
            |    <d:getetag/>
            |    <card:address-data/>
            |  </d:prop>
            |  <d:href>1733690125261/ProjectForge-7833476.vcf</d:href>
            |  <d:href>1733690125261/ProjectForge-7858940.vcf</d:href>
            |  <d:href>1733690125261/ProjectForge-7859171.vcf</d:href>
            |</card:addressbook-multiget>
        """.trimMargin().let { xml ->
            CardDavXmlUtils.extractContactIds(xml).toList().let { ids ->
                Assertions.assertEquals(7833476, ids[0])
                Assertions.assertEquals(7858940, ids[1])
                Assertions.assertEquals(7859171, ids[2])
            }
        }
    }

    @Test
    fun `get element name`() {
        """<hurzel test=\"dkfsld\"><prop><etag/></prop></hurzel>""".let { xml ->
            Assertions.assertEquals("prop", CardDavXmlUtils.getElementName(xml, "prop"))
        }
        """<hurzel test=\"dkfsld\"><d:prop><etag/></d:prop></hurzel>""".let { xml ->
            Assertions.assertEquals("d:prop", CardDavXmlUtils.getElementName(xml, "prop"))
        }
    }

    @Test
    fun `escape xml string`() {
        """<hurzel test="dkfsld">  & '""".let { xml ->
            Assertions.assertEquals("&lt;hurzel test=&quot;dkfsld&quot;&gt;  &amp; &apos;", CardDavXmlUtils.escapeXml(xml))
        }
    }
}
