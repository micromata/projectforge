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

package org.projectforge.fibu;

import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuftragDOTest
{
  @Test
  public void getProjektKundeAsString()
  {
    AuftragDO auftrag = createAuftrag("Micromata", "ACME", "ProjectForge", "Hurzel ltd.");
    assertEquals("ACME; Micromata; Hurzel ltd. - ProjectForge", auftrag.getProjektKundeAsString());
    auftrag = new AuftragDO();
    assertEquals("", auftrag.getProjektKundeAsString());
    auftrag = createAuftrag(null, "ACME", "ProjectForge", "Hurzel ltd.");
    assertEquals("ACME; Hurzel ltd. - ProjectForge", auftrag.getProjektKundeAsString());
    auftrag = createAuftrag("Micromata", null, "ProjectForge", "Hurzel ltd.");
    assertEquals("Micromata; Hurzel ltd. - ProjectForge", auftrag.getProjektKundeAsString());
    auftrag = createAuftrag(null, null, "ProjectForge", "Hurzel ltd.");
    assertEquals("Hurzel ltd. - ProjectForge", auftrag.getProjektKundeAsString());
    auftrag = createAuftrag(null, null, "ProjectForge", null);
    assertEquals("ProjectForge", auftrag.getProjektKundeAsString());
    auftrag = createAuftrag("Micromata", "ACME", "ProjectForge", "Micromata");
    assertEquals("ACME; Micromata - ProjectForge", auftrag.getProjektKundeAsString());
  }

  private AuftragDO createAuftrag(String kundeName, String kundeText, String projektName, String projektKundename)
  {
    AuftragDO auftrag = new AuftragDO();
    if (kundeName != null) {
      KundeDO kunde = new KundeDO();
      kunde.setName(kundeName);
      auftrag.setKunde(kunde);
    }
    auftrag.setKundeText(kundeText);
    if (projektName != null) {
      ProjektDO projekt = new ProjektDO();
      projekt.setName(projektName);
      if (projektKundename != null) {
        KundeDO kunde = new KundeDO();
        kunde.setName(projektKundename);
        projekt.setKunde(kunde);
      }
      auftrag.setProjekt(projekt);
    }
    return auftrag;
  }
}
