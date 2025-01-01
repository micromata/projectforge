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

package org.projectforge.business.user;

import org.projectforge.common.StringHelper;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.api.UserPrefArea;

import java.util.*;

/**
 * All user pref areas. They will shown in the list of 'my settings'.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class UserPrefAreaRegistry
{
  private static final UserPrefAreaRegistry instance = new UserPrefAreaRegistry();

  private Set<UserPrefArea> set = new HashSet<>();

  public static UserPrefAreaRegistry instance()
  {
    return instance;
  }

  public UserPrefAreaRegistry register(final UserPrefArea userPrefArea)
  {
    set.add(userPrefArea);
    return this;
  }

  public UserPrefArea getEntry(final String id)
  {
    if (id == null) {
      return null;
    }
    for (final UserPrefArea area : set) {
      if (id.equals(area.getId())) {
        return area;
      }
    }
    return null;
  }

  /**
   * @param locale
   * @return The list ordered by the translation of the i18n keys.
   */
  public List<UserPrefArea> getOrderedEntries(final Locale locale)
  {
    final List<UserPrefArea> list = new ArrayList<>(set);
    list.sort(new Comparator<UserPrefArea>() {
      @Override
      public int compare(final UserPrefArea o1, final UserPrefArea o2) {
        return StringHelper.compareTo(
            I18nHelper.getLocalizedMessage(locale, o1.getI18nKey()),
            I18nHelper.getLocalizedMessage(locale, o2.getI18nKey())
        );
      }
    });
    return list;
  }

  private UserPrefAreaRegistry()
  {
    set.add(UserPrefArea.KUNDE_FAVORITE);
    set.add(UserPrefArea.PROJEKT_FAVORITE);
    set.add(UserPrefArea.TASK_FAVORITE);
    set.add(UserPrefArea.TIMESHEET_TEMPLATE);
    set.add(UserPrefArea.USER_FAVORITE);
  }
}
