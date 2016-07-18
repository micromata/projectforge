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

package org.projectforge.fibu.kost;

import static org.testng.AssertJUnit.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.testng.annotations.Test;

public class Kost2Test
{
  private Map<Integer, Kost2ArtDO> map = new HashMap<Integer, Kost2ArtDO>();

  @Test
  public void testGetNummer()
  {
    Kost2DO kost2 = createKost2(5, 0, 0, 0);
    assertEquals(50000000, kost2.getNummer().intValue());
    kost2 = createKost2(5, 999, 99, 99);
    assertEquals(59999999, kost2.getNummer().intValue());
    kost2 = createKost2(1, 1, 2, 3);
    assertEquals(10010203, kost2.getNummer().intValue());
  }

  private Kost2DO createKost2(int nummernkreis, int bereich, int teilbereich, int art)
  {
    Kost2DO kost2 = new Kost2DO();
    kost2.setNummernkreis(nummernkreis);
    kost2.setBereich(bereich);
    kost2.setTeilbereich(teilbereich);
    Kost2ArtDO kost2Art = map.get(art);
    if (kost2Art == null) {
      kost2Art = new Kost2ArtDO();
      kost2Art.setId(art);
      map.put(art, kost2Art);
    }
    kost2.setKost2Art(kost2Art);
    return kost2;
  }
}
