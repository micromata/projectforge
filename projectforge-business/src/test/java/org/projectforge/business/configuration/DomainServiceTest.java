/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DomainServiceTest {
  @Test
  void testDomain() {
    test("https://projectforge.org/projectforge",
            "https://projectforge.org",
            "projectforge",
            "https://projectforge.org/projectforge",
            "https");

    test("http://projectforge.org/projectforge/",
            "http://projectforge.org",
            "projectforge",
            "http://projectforge.org/projectforge",
            "http");

    test("http://projectforge.org//projectforge/",
            "http://projectforge.org",
            "projectforge",
            "http://projectforge.org/projectforge",
            "http");

    test("https://projectforge.org:443/",
            "https://projectforge.org:443",
            "",
            "https://projectforge.org:443",
            "https");

    test("https://projectforge.org/a",
            "https://projectforge.org",
            "a",
            "https://projectforge.org/a",
            "https");

    test("http://localhost:8080",
            "http://localhost:8080",
            "",
            "http://localhost:8080",
    "http");
  }

  private void test(String urlString, String expectedDomain, String expectedContext, String expectedUrl, String expectedProtocol) {
    DomainService service = DomainService.internalCreate(urlString);
    assertEquals(expectedDomain, service.getDomain());
    assertEquals(expectedContext, service.getContextPath());
    assertEquals(expectedUrl, service.getDomainWithContextPath());
    assertEquals(expectedProtocol, service.getProtocol());
  }
}
