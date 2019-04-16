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

package org.projectforge.business.fibu.kost;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.projectforge.business.fibu.RechnungsPositionDO;
import org.projectforge.test.AbstractTestBase;
import org.junit.jupiter.api.Test;

public class KostZuweisungenCopyHelperTest extends AbstractTestBase
{
  @Test
  public void copy()
  {
    final RechnungsPositionDO srcPos = new RechnungsPositionDO();
    final RechnungsPositionDO destPos = new RechnungsPositionDO();

    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(0, destPos.getKostZuweisungen().size());

    final KostZuweisungDO kostZuweisung = new KostZuweisungDO().setNetto(BigDecimal.ONE).setComment("1");
    kostZuweisung.setId(4711); // simulate non deletable
    srcPos.addKostZuweisung(kostZuweisung);
    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(1, destPos.getKostZuweisungen().size());
    assertEquals(srcPos.getKostZuweisungen().get(0), destPos.getKostZuweisungen().get(0));

    destPos.addKostZuweisung(new KostZuweisungDO().setNetto(BigDecimal.ONE).setComment("1"));
    assertEquals(2, destPos.getKostZuweisungen().size());

    // srcPos "overwrites" dstPos
    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(1, destPos.getKostZuweisungen().size());
    assertEquals(srcPos.getKostZuweisungen().get(0), destPos.getKostZuweisungen().get(0));

    srcPos.getKostZuweisung(0).setNetto(BigDecimal.TEN).setComment("10");
    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(1, destPos.getKostZuweisungen().size());
    assertEquals(BigDecimal.TEN, destPos.getKostZuweisung(0).getNetto());
    assertEquals("10", destPos.getKostZuweisung(0).getComment());

    srcPos.addKostZuweisung(new KostZuweisungDO().setNetto(BigDecimal.ONE).setComment("2"));
    srcPos.addKostZuweisung(new KostZuweisungDO().setNetto(BigDecimal.ONE).setComment("3"));
    srcPos.addKostZuweisung(new KostZuweisungDO().setNetto(BigDecimal.ONE).setComment("4"));
    srcPos.addKostZuweisung(new KostZuweisungDO().setNetto(BigDecimal.ONE).setComment("5"));
    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(5, destPos.getKostZuweisungen().size());
    assertEquals(srcPos.getKostZuweisungen().get(0), destPos.getKostZuweisungen().get(0));
    assertEquals(srcPos.getKostZuweisungen().get(1), destPos.getKostZuweisungen().get(1));
    assertEquals(srcPos.getKostZuweisungen().get(2), destPos.getKostZuweisungen().get(2));
    assertEquals(srcPos.getKostZuweisungen().get(3), destPos.getKostZuweisungen().get(3));
    assertEquals(srcPos.getKostZuweisungen().get(4), destPos.getKostZuweisungen().get(4));

    srcPos.deleteKostZuweisung(3);
    srcPos.deleteKostZuweisung(2);
    srcPos.deleteKostZuweisung(1);
    srcPos.deleteKostZuweisung(0); // is not deletable, see above
    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(2, destPos.getKostZuweisungen().size());
    assertEquals(srcPos.getKostZuweisungen().get(0), destPos.getKostZuweisungen().get(0));
    assertEquals(srcPos.getKostZuweisungen().get(1), destPos.getKostZuweisungen().get(1));
    assertEquals(srcPos.getKostZuweisungen().get(0).getNetto(), destPos.getKostZuweisungen().get(0).getNetto());
    assertEquals(srcPos.getKostZuweisungen().get(1).getNetto(), destPos.getKostZuweisungen().get(1).getNetto());
    assertEquals(4, srcPos.getKostZuweisungen().get(1).getIndex());
  }
}
