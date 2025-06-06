/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web;

import org.junit.jupiter.api.Test;
import org.projectforge.business.utils.HtmlHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HtmlHelperTest
{
  @Test
  public void testAttribute()
  {
    assertEquals(" hallo=\"test\"", HtmlHelper.attribute("hallo", "test"));
  }

  @Test
  public void testFormatText()
  {
    assertEquals("", HtmlHelper.formatText(null, true));
    assertEquals("", HtmlHelper.formatText("", true));
    assertEquals("<br/>", HtmlHelper.formatText("\n", true));
    assertEquals("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", HtmlHelper.formatText("\t ", true));
    assertEquals(
        "Name:&nbsp;&nbsp;&nbsp;Reinhard<br/>Vorname:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Kai<br/>Test &nbsp;&nbsp;&nbsp;",
        HtmlHelper.formatText("Name:\tReinhard\r\nVorname:\tKai\nTest    ", true));
  }

  @Test
  public void escapeHtml()
  {
    assertNull(HtmlHelper.escapeHtml(null, true));
    assertNull(HtmlHelper.escapeHtml(null, false));
    assertEquals("", HtmlHelper.escapeHtml("", true));
    assertEquals("", HtmlHelper.escapeHtml("", false));
    assertEquals("St&eacute;phanie&lt;<br/>\n", HtmlHelper.escapeHtml("Stéphanie<\n", true));
    assertEquals("St&eacute;phanie&lt;\n", HtmlHelper.escapeHtml("Stéphanie<\n", false));
    assertEquals("St&eacute;phanie&lt;<br/>\nGermany", HtmlHelper.escapeHtml("Stéphanie<\nGermany", true));
    assertEquals("St&eacute;phanie&lt;\nGermany", HtmlHelper.escapeHtml("Stéphanie<\nGermany", false));

    assertEquals("St&eacute;phanie&lt;<br/>\r\n", HtmlHelper.escapeHtml("Stéphanie<\r\n", true));
    assertEquals("St&eacute;phanie&lt;\r\n", HtmlHelper.escapeHtml("Stéphanie<\r\n", false));
    assertEquals("Test\nSt&eacute;phanie&lt;<br/>\r\nGermany",
        HtmlHelper.escapeHtml("Test\nStéphanie<\r\nGermany", true));
    assertEquals("St&eacute;phanie&lt;\r\nGermany", HtmlHelper.escapeHtml("Stéphanie<\r\nGermany", false));
  }
}
