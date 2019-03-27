/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import org.projectforge.framework.xstream.XmlHelper;
import org.projectforge.framework.xstream.XmlObjectReader;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.test.AbstractTestNGBase;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

public class MenuBuilderTest extends AbstractTestNGBase
{
  private final static String xml = XmlHelper.replaceQuotes(XmlHelper.XML_HEADER
      + "\n"
      + "<menu-entry>\n"
      + "  <sub-menu>\n"
      + "    <menu-entry id='IMAGE_CROPPER' visible='false' />\n"
      + "    <menu-entry id='PROJECT_MANAGEMENT' visible='true' />\n"
      + "  </sub-menu>\n"
      + "</menu-entry>\n");

  @Test
  public void testTranslations()
  {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(MenuEntryConfig.class);
    final MenuEntryConfig root = (MenuEntryConfig) reader.read(xml);
    MenuEntryConfig menu0 = root.getChildren().get(0);
    assertFalse(menu0.isVisible());
    assertEquals(MenuItemDefId.IMAGE_CROPPER.getId(), menu0.getMenuItemId());
    MenuEntryConfig menu1 = root.getChildren().get(1);
    assertTrue(menu1.isVisible());
    assertEquals(MenuItemDefId.PROJECT_MANAGEMENT.getId(), menu1.getMenuItemId());
  }
}
