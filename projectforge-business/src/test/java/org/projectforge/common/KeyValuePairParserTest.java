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

package org.projectforge.common;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Map;

import org.projectforge.framework.utils.KeyValuePairParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class KeyValuePairParserTest
{

  private String replaceQuotationMark(final String str)
  {
    return str.replace("'", "\"");
  }

  @Test
  public void testParse() throws Exception
  {
    final StringReader in = new StringReader(
        replaceQuotationMark("a1=,a2='1970-11-21 13:17:57.742',a3=5,a4='Hallo',a5='Hallo ''Kai''',a6=1.2"));
    final KeyValuePairParser parser = new KeyValuePairParser(in);
    final Map<String, String> pairs = parser.parse();
    Assertions.assertEquals(6, pairs.size());
    Assertions.assertNull(pairs.get("a1"));
    Assertions.assertEquals("1970-11-21 13:17:57.742", pairs.get("a2"));
    Assertions.assertEquals(5, parser.getInteger("a3").intValue());
    Assertions.assertEquals("Hallo", pairs.get("a4"));
    Assertions.assertEquals("Hallo \"Kai\"", pairs.get("a5"));
    Assertions.assertEquals(new BigDecimal("1.2"), parser.getBigDecimal("a6"));
  }
}
