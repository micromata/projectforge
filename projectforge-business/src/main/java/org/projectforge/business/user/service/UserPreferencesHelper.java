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

package org.projectforge.business.user.service;


import org.projectforge.framework.configuration.ApplicationContextProvider;

/**
 * Use {@link UserPrefService} instead.
 */
@Deprecated
public class UserPreferencesHelper {
    /**
     * Stores the given value for the current user.
     *
     * @param key
     * @param value
     * @param persistent If true, the object will be persisted in the database.
     */
    public static void putEntry(final String key, final Object value, final boolean persistent) {
        getUserPreferencesService().putEntry(key, value, persistent);
    }

    /**
     * Gets the stored user preference entry.
     *
     * @param key
     * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
     * existing, otherwise null;
     */
    public static Object getEntry(final String key) {
        return getUserPreferencesService().getEntry(key);
    }

    private static UserXmlPreferencesService getUserPreferencesService() {
        return ApplicationContextProvider.getApplicationContext().getBean(UserXmlPreferencesService.class);
    }
}
