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

import org.projectforge.business.fibu.kost.KostHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class KostHelperTest
{
  @Test
  public void parseKostString()
  {
    Assert.assertNull(KostHelper.parseKostString(null));
    Assert.assertNull(KostHelper.parseKostString(""));
    Assert.assertNull(KostHelper.parseKostString("123456789"));
    Assert.assertNull(KostHelper.parseKostString("123456789"));
    Assert.assertNull(KostHelper.parseKostString("123456789012"));

    assertKost(1, 234, 56, 78, KostHelper.parseKostString("12345678"));
    assertKost(1, 234, 56, 78, KostHelper.parseKostString("1.234.56.78"));
  }

  private void assertKost(final int v0, final int v1, final int v2, final int v3, int[] result)
  {
    Assert.assertNotNull(result);
    Assert.assertEquals(4, result.length);
    Assert.assertEquals(v0, result[0]);
    Assert.assertEquals(v1, result[1]);
    Assert.assertEquals(v2, result[2]);
    Assert.assertEquals(v3, result[3]);
  }
}
