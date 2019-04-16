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

package org.projectforge.web.wicket.components;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YearListCoiceRendererTest {
  @Test
  public void listToString() {
    List<Integer> years = Arrays.asList();
    YearListCoiceRenderer renderer = new YearListCoiceRenderer(years);
    assertEquals(String.valueOf(YearListCoiceRenderer.CURRENT_YEAR), renderer.getDisplayValue(-1));
    assertEquals("2005", renderer.getDisplayValue(2005));

    years = Arrays.asList(-1);
    renderer = new YearListCoiceRenderer(years);
    assertEquals(String.valueOf(YearListCoiceRenderer.CURRENT_YEAR), renderer.getDisplayValue(-1));
    assertEquals("2005", renderer.getDisplayValue(2005));

    years = Arrays.asList(-1, 2007);
    renderer = new YearListCoiceRenderer(years);
    assertEquals("2007", renderer.getDisplayValue(-1));
    assertEquals("2005", renderer.getDisplayValue(2005));

    years = Arrays.asList(-1, 2007, 2008);
    renderer = new YearListCoiceRenderer(years);
    assertEquals("2007-2008", renderer.getDisplayValue(-1));
    assertEquals("2005", renderer.getDisplayValue(2005));
  }
}
