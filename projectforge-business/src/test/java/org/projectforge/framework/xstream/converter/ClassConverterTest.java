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

package org.projectforge.framework.xstream.converter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class ClassConverterTest
{
  @Test
  public void testConverter()
  {
    ClassConverter.log = Mockito.mock(Logger.class);
    final ClassConverter converter = new ClassConverter();
    assertNull(converter.fromString(null));
    assertNull(converter.fromString(""));
    assertNull(converter.fromString("\t \n"));
    assertEquals(ClassConverterTest.class,
        converter.fromString("org.projectforge.framework.xstream.converter.ClassConverterTest"));
    assertEquals(null, converter.fromString("org.projectforge.xml.stream.converter.NotFound"));
  }
}
