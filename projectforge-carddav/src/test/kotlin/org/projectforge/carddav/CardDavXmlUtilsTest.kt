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
            CardDavXmlUtils.extractAddressIds(xml).toList().let { ids ->
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
