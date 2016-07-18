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

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.projectforge.web.common.MultiChoiceListHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class AssignListHelperTest
{
  @Test
  public void execute()
  {
    MultiChoiceListHelper<String> assignList = new MultiChoiceListHelper<String>(create("1", "2", "3", "4"), null);
    assertSet(create(), assignList.getItemsToAssign());
    assertSet(create(), assignList.getItemsToUnassign());
    assignList.setAssignedItems(create("1"));
    assertSet(create("1"), assignList.getItemsToAssign());
    assertSet(create(), assignList.getItemsToUnassign());
    assignList.setAssignedItems(create("1", "4"));
    assertSet(create("1", "4"), assignList.getItemsToAssign());
    assertSet(create(), assignList.getItemsToUnassign());

    assignList = new MultiChoiceListHelper<String>(create("1", "2", "3", "4", "5"), create("1", "3", "5"));
    assertSet(create(), assignList.getItemsToAssign());
    assertSet(create(), assignList.getItemsToUnassign());
    assignList.setAssignedItems(create("1", "2"));
    assertSet(create("2"), assignList.getItemsToAssign());
    assertSet(create("3", "5"), assignList.getItemsToUnassign());
  }

  private SortedSet<String> create(final String... items)
  {
    final SortedSet<String> set = new TreeSet<String>();
    for (final String item : items) {
      set.add(item);
    }
    return set;
  }

  private void assertSet(final Set<String> expected, final Set<String> actual)
  {
    Assert.assertNotNull(actual);
    Assert.assertEquals(expected.size(), actual.size());
    for (final String item : expected) {
      Assert.assertTrue(actual.contains(item));
    }
  }
}
