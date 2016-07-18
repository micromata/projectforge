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

import static org.testng.AssertJUnit.assertEquals;

import java.util.Locale;

import org.testng.annotations.Test;

public class MenuEntryConfigTest
{
  @Test
  public void testTranslations()
  {
    final MenuEntryConfig menu = new MenuEntryConfig().addTranslation("de", "Anmelden").addTranslation("en_US", "Logon")
        .addTranslation(
            "en_US_CA", "Asta la vista")
        .setLabel("Login");
    assertEquals("Login", menu.getLabel());
    assertEquals("Login", menu.getLabel(Locale.ENGLISH));
    assertEquals("Login", menu.getLabel(new Locale("en")));
    assertEquals("Anmelden", menu.getLabel(Locale.GERMAN));
    assertEquals("Anmelden", menu.getLabel(new Locale("de")));
    assertEquals("Anmelden", menu.getLabel(new Locale("de", "DE")));
    assertEquals("Anmelden", menu.getLabel(new Locale("de", "DE", "V1")));
    assertEquals("Login", menu.getLabel(new Locale("en", "GB")));
    assertEquals("Logon", menu.getLabel(new Locale("en", "US")));
    assertEquals("Asta la vista", menu.getLabel(new Locale("en", "US", "CA")));
  }
}
