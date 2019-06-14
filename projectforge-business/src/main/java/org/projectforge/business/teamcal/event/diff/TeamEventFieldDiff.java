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

package org.projectforge.business.teamcal.event.diff;

/**
 * @author Stefan Niemczyk (s.niemczyk@micromata.de)
 */
public class TeamEventFieldDiff<E>
{
  private TeamEventField field;
  private E newState;
  private E oldState;
  private TeamEventFieldDiffType type;

  protected TeamEventFieldDiff(final TeamEventField field, final E newState, final E oldState)
  {
    this.field = field;
    this.newState = newState;
    this.oldState = oldState;

    if (newState == null && oldState != null) {
      this.type = TeamEventFieldDiffType.REMOVED;
    } else if (newState != null && oldState == null) {
      this.type = TeamEventFieldDiffType.SET;
    } else if (newState == null && oldState == null) {
      this.type = TeamEventFieldDiffType.NONE;
    } else if (newState.equals(oldState)) {
      this.type = TeamEventFieldDiffType.NONE;
    } else {
      this.type = TeamEventFieldDiffType.UPDATED;
    }
  }

  public boolean isDiff()
  {
    return this.type != TeamEventFieldDiffType.NONE;
  }

  public TeamEventField getTeamEventField()
  {
    return field;
  }

  public E getNewState()
  {
    return newState;
  }

  public void setNewState(final E newState)
  {
    this.newState = newState;
  }

  public E getOldState()
  {
    return oldState;
  }

  public void setOldState(final E oldState)
  {
    this.oldState = oldState;
  }

  public TeamEventFieldDiffType getType()
  {
    return type;
  }

  public void setType(final TeamEventFieldDiffType type)
  {
    this.type = type;
  }
}
