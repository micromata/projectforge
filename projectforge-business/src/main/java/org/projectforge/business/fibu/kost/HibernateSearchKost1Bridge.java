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

package org.projectforge.business.fibu.kost;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.framework.configuration.ApplicationContextProvider;


/**
 * Kost2Bridge for hibernate search: Kostenträger kann à la 6.201.57 gesucht werden.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class HibernateSearchKost1Bridge implements TwoWayStringBridge {
  private KostCache kostCache = null;

  private KostCache getKostCache() {
    if (kostCache == null)
      kostCache = ApplicationContextProvider.getApplicationContext().getBean(KostCache.class);
    return kostCache;
  }

  @Override
  public Object stringToObject(String stringValue) {
    return getKostCache().getKost1(stringValue);
  }

  @Override
  public String objectToString(Object object) {
    if (object instanceof String) {
      return (String)object;
    }
    final Kost1DO kost1 = (Kost1DO) object;
    final StringBuilder buf = new StringBuilder();
    buf.append(KostFormatter.format(kost1));
    buf.append(' ');
    buf.append(KostFormatter.format(kost1, true));
    return buf.toString();
  }
}
