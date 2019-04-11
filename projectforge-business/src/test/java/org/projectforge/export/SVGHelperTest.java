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

package org.projectforge.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SVGHelperTest
{
  @Test
  public void append()
  {
    assertArrayEquals(new String[] {}, SVGHelper.append(new String[] {}, (Object[]) null));
    assertArrayEquals(new String[] { "3" }, SVGHelper.append(new String[] {}, new Object[] { 3 }));
    assertArrayEquals(new String[] { "3", "vier" }, SVGHelper.append(new String[] {}, new Object[] { 3, "vier" }));
    assertArrayEquals(new String[] { "1" }, SVGHelper.append(new String[] { "1" }, new Object[] {}));
    assertArrayEquals(new String[] { "1", "3" }, SVGHelper.append(new String[] { "1" }, new Object[] { 3 }));
    assertArrayEquals(new String[] { "1", "3", "vier" },
        SVGHelper.append(new String[] { "1" }, new Object[] { 3, "vier" }));
    assertArrayEquals(new String[] { "1", "2", "3", "vier" },
        SVGHelper.append(new String[] { "1", "2" }, new Object[] { 3, "vier" }));
  }

  @Test
  public void prepend()
  {
    assertArrayEquals(new String[] {}, SVGHelper.prepend(new String[] {}, (Object[]) null));
    assertArrayEquals(new String[] { "1" }, SVGHelper.prepend(new String[] {}, new Object[] { 1 }));
    assertArrayEquals(new String[] { "1", "zwei" }, SVGHelper.prepend(new String[] {}, new Object[] { 1, "zwei" }));
    assertArrayEquals(new String[] { "3" }, SVGHelper.prepend(new String[] { "3" }, new Object[] {}));
    assertArrayEquals(new String[] { "1", "3" }, SVGHelper.prepend(new String[] { "3" }, new Object[] { 1 }));
    assertArrayEquals(new String[] { "1", "zwei", "3" },
        SVGHelper.prepend(new String[] { "3" }, new Object[] { 1, "zwei" }));
    assertArrayEquals(new String[] { "1", "zwei", "3", "vier" },
        SVGHelper.prepend(new String[] { "3", "vier" }, new Object[] { 1, "zwei" }));
  }

  @Test
  public void checkPositiveValues()
  {
    SVGHelper.checkPositiveValues("vars", 1.0, 0.1);
    try {
      SVGHelper.checkPositiveValues("vars", 1.0, 0.1, -0.1);
      fail("Exception expected");
    } catch (final IllegalArgumentException ex) {
      assertEquals("Values should be positive or zero and valid: {vars}=1.0, 0.1, -0.1", ex.getMessage());
    }
    try {
      SVGHelper.checkPositiveValues("vars", 1.0, 0.1, Double.NaN);
      fail("Exception expected");
    } catch (final IllegalArgumentException ex) {
      assertEquals("Values should be positive or zero and valid: {vars}=1.0, 0.1, NaN", ex.getMessage());
    }
    try {
      SVGHelper.checkPositiveValues("vars", 1.0, 0.1, Double.POSITIVE_INFINITY);
      fail("Exception expected");
    } catch (final IllegalArgumentException ex) {
      assertEquals("Values should be positive or zero and valid: {vars}=1.0, 0.1, Infinity", ex.getMessage());
    }
    try {
      SVGHelper.checkPositiveValues("vars", Double.NEGATIVE_INFINITY);
      fail("Exception expected");
    } catch (final IllegalArgumentException ex) {
      assertEquals("Values should be positive or zero and valid: {vars}=-Infinity", ex.getMessage());
    }
  }
}
