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

package org.projectforge.common;

import org.projectforge.framework.utils.GZIPHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GZipHelperTest
{
  @Test
  public void testCompressUncompress()
  {
    Assertions.assertNull(GZIPHelper.compress(null));
    Assertions.assertEquals("", GZIPHelper.compress(""));
    Assertions.assertNull(GZIPHelper.uncompress(null));
    Assertions.assertEquals("", GZIPHelper.uncompress(""));
    test("<tag>Hurzel Hurzel</tag>");
    test(
        "<tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag><tag>Hurzel Hurzel</tag>");
  }

  private void test(final String str)
  {
    final String compressed = GZIPHelper.compress(str);
    final String uncompressed = GZIPHelper.uncompress(compressed);
    Assertions.assertEquals(str, uncompressed);
  }
}
