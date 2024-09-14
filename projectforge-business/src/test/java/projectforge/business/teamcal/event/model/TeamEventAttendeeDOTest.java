/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package projectforge.business.teamcal.event.model;

import org.junit.jupiter.api.Test;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by blumenstein on 19.12.16.
 */
public class TeamEventAttendeeDOTest
{
  @Test
  public void testEqualsHashCode()
  {
    AddressDO exampleAddress = new AddressDO();
    exampleAddress.setId(4712L);

    TeamEventAttendeeDO first = new TeamEventAttendeeDO();
    TeamEventAttendeeDO second = new TeamEventAttendeeDO();
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());

    first.setUrl("test@test.de");
    assertNotEquals(first, second);
    assertFalse(first.hashCode() == second.hashCode());
    second.setUrl("test@test.de");
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());

    first.setId(4711L);
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    second.setId(4711L);
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());

    first.setUrl(null);
    assertEquals(first, second);
    //Equals/HashCode contract is broken
    assertFalse(first.hashCode() == second.hashCode());

    first.setId(null);
    first.setAddress(exampleAddress);
    assertNotEquals(first, second);
    assertFalse(first.hashCode() == second.hashCode());

    second.setUrl(null);
    second.setAddress(exampleAddress);
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());

  }

  @Test
  public void testReomoveFromSet()
  {
    Set<TeamEventAttendeeDO> testSet = new HashSet<>();
    TeamEventAttendeeDO first = new TeamEventAttendeeDO();
    testSet.add(first);
    TeamEventAttendeeDO second = new TeamEventAttendeeDO();
    testSet.remove(second);
    assertTrue(testSet.isEmpty());
    first.setId(4711L);
    testSet.add(first);
    second.setId(4711L);
    testSet.remove(second);
    assertTrue(testSet.isEmpty());
    first.setId(null);
    first.setUrl("test@test.de");
    testSet.add(first);
    second.setUrl("test@test.de");
    testSet.remove(second);
    assertTrue(testSet.isEmpty());
  }

}
