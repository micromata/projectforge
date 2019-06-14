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

package org.projectforge.framework.persistence.history;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.hibernate.history.delta.PropertyDelta;

/**
 * @author wolle
 * 
 */
public class PrintingHistoryFormatter extends DefaultHistoryFormatter
{
  /** The logger */
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PrintingHistoryFormatter.class);

  public PrintingHistoryFormatter(final String resourceBundleName)
  {
    super(resourceBundleName);
  }

  @Override
  public String formatUser(final Session session, final Locale locale, final Object changed,
      final HistoryEntry historyEntry, final PropertyDelta delta)
  {
    final String[] users = StringUtils.split(historyEntry.getUserName(), ",");
    if (users != null && users.length > 0) {
      try {
        final PFUserDO user = session.load(PFUserDO.class, Integer.valueOf(users[0]));
        return "<img src=\"images/user.gif\" valign=\"middle\" width=\"20\" height=\"20\" border=\"0\"  /> "
            + escapeHtml(user.getFullname());
      } catch (final HibernateException ex) {
        log.warn("Can't load history-user " + historyEntry.getUserName());
        return "unknown";
      }
    }
    return escapeHtml(historyEntry.getUserName());
  }

}
