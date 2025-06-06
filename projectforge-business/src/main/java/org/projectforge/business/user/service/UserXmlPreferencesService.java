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

import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 * Use {@link UserPrefService} instead.
 */
@Deprecated
@Service
public class UserXmlPreferencesService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserXmlPreferencesService.class);

    @Autowired
    private AccessChecker accessChecker;

    @Autowired
    private UserXmlPreferencesCache userXmlPreferencesCache;

    /**
     * Stores the given value for the current user.
     *
     * @param key
     * @param value
     * @param persistent If true, the object will be persisted in the database.
     * @see UserXmlPreferencesCache#putEntry(Long, String, Object, boolean)
     */
    public void putEntry(final String key, final Object value, final boolean persistent) {
        final PFUserDO user = ThreadLocalUserContext.getLoggedInUser();
        if (user == null || value == null) {
            // Should only occur, if user is not logged in.
            return;
        }
        if (AccessChecker.isDemoUser(user) && value instanceof Serializable) {
            // Store user pref for demo user only in user's session.
            // Do nothing for demo user: MySession.get().setAttribute(key, (Serializable) value);
            return;
        }
        try {
            userXmlPreferencesCache.putEntry(null, key, value, persistent, user.getId());
        } catch (final Exception ex) {
            log.error("Should only occur in maintenance mode: " + ex.getMessage(), ex);
        }
    }

    /**
     * Gets the stored user preference entry.
     *
     * @param key
     * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
     * existing, otherwise null;
     * @see UserXmlPreferencesCache#getEntry(Long, String)
     */
    public Object getEntry(final String key) {
        final PFUserDO user = ThreadLocalUserContext.getLoggedInUser();
        if (user == null) {
            // Should only occur, if user is not logged in.
            return null;
        }
        final Long userId = user.getId();
        try {
            return userXmlPreferencesCache.getEntry(null, key, userId);
        } catch (final Exception ex) {
            log.error("Should only occur in maintenance mode: " + ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Gets the stored user preference entry.
     *
     * @param key
     * @param expectedType Checks the type of the user pref entry (if found) and returns only this object if the object is
     *                     from the expected type, otherwise null is returned.
     * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
     * existing, otherwise null;
     * @see UserXmlPreferencesCache#getEntry(Long, String)
     */
    public <T> T getEntry(Class<T> expectedType, String key) {
        final Object entry = getEntry(key);
        if (entry == null) {
            return null;
        }
        if (expectedType.isAssignableFrom(entry.getClass())) {
            return (T) entry;
        }
        // Probably a new software release results in an incompatibility of old and new object format.
        log.info("Could not get user preference entry: (old) type "
                + entry.getClass().getName()
                + " is not assignable to (new) required type "
                + expectedType.getName()
                + " (OK, probably new software release).");
        return null;
    }

    /**
     * Removes the entry under the given key.
     *
     * @param key
     * @return The removed entry if found.
     */
    public void removeEntry(final String key) {
        final PFUserDO user = ThreadLocalUserContext.getLoggedInUser();
        if (user == null) {
            // Should only occur, if user is not logged in.
            return;
        }
        if (AccessChecker.isDemoUser(user)) {
            return;
        }
        userXmlPreferencesCache.removeEntry(null, key, user.getId());
    }
}
