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

package org.projectforge.business.fibu.kost;

import java.util.List;

import org.projectforge.business.fibu.AbstractRechnungsPositionDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.utils.ListCopyHelper;

public class KostZuweisungenCopyHelper extends ListCopyHelper<KostZuweisungDO>
{
  public static final String[] IGNORE_FIELDS = { "rechnungsPosition"};

  /**
   * @see org.projectforge.framework.persistence.utils.ListCopyHelper#copy(java.util.List, java.util.List, java.lang.Object[])
   */
  @Override
  public ModificationStatus copy(final List<KostZuweisungDO> srcList, final List<KostZuweisungDO> destList, final Object... objects)
  {
    throw new IllegalArgumentException("Please call mycopy with AbstractRechnungsPositionDO instead!");
  }

  public ModificationStatus mycopy(final List<KostZuweisungDO> srcList, final List<KostZuweisungDO> destList,
      final AbstractRechnungsPositionDO destPosition)
  {
    return super.copy(srcList, destList, destPosition);
  }

  @Override
  protected ModificationStatus copyFrom(final KostZuweisungDO srcEntry, final KostZuweisungDO destEntry, final Object... objects)
  {
    return destEntry.copyValuesFrom(srcEntry, IGNORE_FIELDS);
  }

  @Override
  protected void appendDestEntry(final List<KostZuweisungDO> destList, final KostZuweisungDO srcEntry, final Object... objects)
  {
    final AbstractRechnungsPositionDO destPositionDO = (AbstractRechnungsPositionDO) objects[0];
    final KostZuweisungDO destEntry = new KostZuweisungDO();
    destEntry.copyValuesFrom(srcEntry, IGNORE_FIELDS);
    destPositionDO.addKostZuweisung(destEntry);
  }

  @Override
  protected void removeDestEntry(final List<KostZuweisungDO> destList, final KostZuweisungDO destEntry, final int pos,
      final Object... objects)
  {
    final AbstractRechnungsPositionDO destPositionDO = (AbstractRechnungsPositionDO) objects[0];
    destPositionDO.deleteKostZuweisung(destEntry.getIndex());
  }
}
