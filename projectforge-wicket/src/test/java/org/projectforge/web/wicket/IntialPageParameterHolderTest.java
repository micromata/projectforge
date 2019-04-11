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

package org.projectforge.web.wicket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntialPageParameterHolderTest {

  @Test
  public void constructor() {
    test("taskId|task", "", "taskId", "task");
    test("taskId", "", "taskId", "taskId");
    test("p.taskId|task", "p.", "taskId", "task");
    test("p.taskId", "p.", "taskId", "taskId");
  }

  private void test(final String propertyString, final String expectedPrefix, final String expectedProperty,
                    final String expectedAlias) {
    final InitialPageParameterHolder holder = new InitialPageParameterHolder(propertyString);
    assertEquals(expectedPrefix, holder.prefix);
    assertEquals(expectedProperty, holder.property);
    assertEquals(expectedAlias, holder.alias);
  }
}
