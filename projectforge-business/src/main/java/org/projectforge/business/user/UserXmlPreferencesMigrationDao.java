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

package org.projectforge.business.user;

import org.apache.commons.lang3.Validate;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Stores all user persistent objects such as filter settings, personal settings and persists them to the database.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class UserXmlPreferencesMigrationDao {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
          .getLogger(UserXmlPreferencesMigrationDao.class);

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private UserXmlPreferencesDao userXmlPreferencesDao;

  @Autowired
  private UserXmlPreferencesCache userXmlPreferencesCache;

  @Autowired
  private EntityManager em;

  @SuppressWarnings("unchecked")
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  public String migrateAllUserPrefs() {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    final StringBuilder buf = new StringBuilder();
    final List<UserXmlPreferencesDO> list = em.createQuery("from " + UserXmlPreferencesDO.class.getSimpleName() + " t order by user.id, key",
            UserXmlPreferencesDO.class)
            .getResultList();
    int versionNumber = Integer.MAX_VALUE;
    for (final UserXmlPreferencesDO userPrefs : list) {
      buf.append(migrateUserPrefs(userPrefs));
      if (userPrefs.getVersion() < versionNumber) {
        versionNumber = userPrefs.getVersion();
      }
    }
    migrate(versionNumber);
    userXmlPreferencesCache.refresh();
    return buf.toString();
  }

  /**
   * Here you can insert update or delete statements for all user xml pref entries (e. g. delete all entries with an
   * unused key).
   *
   * @param version Version number of oldest entry.
   */
  protected void migrate(final int version) {
    if (version < 4) {
      // deleteOldKeys("org.projectforge.web.humanresources.HRViewForm:Filter");
      // deleteOldKeys("org.projectforge.web.fibu.AuftragListAction:Filter");
      // deleteOldKeys("OLD-VERSION-1.1");
      // hibernateTemplate.flush();
    }
  }

  /**
   * Unsupported or unused keys should be deleted. This method deletes all entries with the given key.
   *
   * @param key Key of the entries to delete.
   */
  protected void deleteOldKeys(final String key) {
    final TypedQuery<UserXmlPreferencesDO> query = em.createQuery("delete from " + UserXmlPreferencesDO.class.getSimpleName() + " where key = '" + key + "'",
            UserXmlPreferencesDO.class);
    final int numberOfUpdatedEntries = query.executeUpdate();
    log.info(numberOfUpdatedEntries + " '" + key + "' entries deleted.");
  }

  protected String migrateUserPrefs(final UserXmlPreferencesDO userPrefs) {
    final Integer userId = userPrefs.getUserId();
    Validate.notNull(userId);
    final StringBuilder buf = new StringBuilder();
    buf.append("Checking user preferences for user '");
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final PFUserDO user = userGroupCache.getUser(userPrefs.getUserId());
    if (user != null) {
      buf.append(user.getUsername());
    } else {
      buf.append(userPrefs.getUserId());
    }
    buf.append("': " + userPrefs.getKey() + " ... ");
    if (userPrefs.getVersion() >= UserXmlPreferencesDO.Companion.getCurrentVersion()) {
      buf.append("version ").append(userPrefs.getVersion()).append(" (up to date)\n");
      return buf.toString();
    }
    migrate(userPrefs);
    final Object data = userXmlPreferencesDao.deserialize(null, userPrefs, true);
    buf.append("version ");
    buf.append(userPrefs.getVersion());
    if (data != null || "<null/>" .equals(userPrefs.getSerializedSettings())) {
      buf.append(" OK ");
    } else {
      buf.append(" ***not re-usable*** ");
    }
    buf.append("\n");
    if (data == null) {
      return buf.toString();
    }
    return buf.toString();
  }

  /**
   * Fixes incompatible versions of user preferences before de-serialization.
   *
   * @param userPrefs
   */
  protected static void migrate(final UserXmlPreferencesDO userPrefs) {
    if (userPrefs.getVersion() < 4) {
      userPrefs.setVersion(4);
    }
  }
}
