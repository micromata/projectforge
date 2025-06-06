/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.AbstractRechnungsPositionDO;

import java.util.List;

public class KostZuweisungenCopyHelper
{
  private static final String[] IGNORE_FIELDS = { "rechnungsPosition" };

  public static void copy(final List<KostZuweisungDO> srcList, final AbstractRechnungsPositionDO destPosition)
  {
    final List<KostZuweisungDO> destList = destPosition.ensureAndGetKostzuweisungen();

    // first remove every deletable entry
    destList.removeIf(destPosition::isKostZuweisungDeletable);

    // then copy values from src to dest entry or create new dest entry if it does not exist
    if (srcList != null) {
      for (final KostZuweisungDO srcEntry : srcList) {
        final KostZuweisungDO destEntry;

        // checks if the destList already contains an entry which equals the source entry regarding their IDs and not necessarily their values
        // see KostZuweisungDO.equals()
        final int index = destList.indexOf(srcEntry);
        if (index >= 0) {
          // dest entry already exists, get it
          destEntry = destList.get(index);
        } else {
          // create new dest entry
          destEntry = new KostZuweisungDO();
          destPosition.addKostZuweisung(destEntry);
        }
        destEntry.copyValuesFrom(srcEntry, IGNORE_FIELDS);
      }
    }
  }

}
