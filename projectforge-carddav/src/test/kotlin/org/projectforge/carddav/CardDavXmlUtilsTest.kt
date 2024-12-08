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
}
