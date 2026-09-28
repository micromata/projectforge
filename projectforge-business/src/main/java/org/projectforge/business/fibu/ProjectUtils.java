/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu;

import org.apache.commons.collections4.CollectionUtils;
import org.projectforge.business.PfCaches;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Some useful helper methods. That are used in groovy scripts. The static methods are used here because groovy scripts
 * call them in static context.
 *
 * @author Kai Reinhard
 */
public class ProjectUtils {
  private static ProjektDao projektDao;
  private static PfCaches pfCaches;

  /**
   * @param username
   * @return List of all projects of which the given user (by login name) is member of the project manager group.
   */
  public static Collection<ProjektDO> getProjectsOfManager(final String username) {
    final PFUserDO user = UserGroupCache.getInstance().getUser(username);
    return getProjectsOfManager(user);
  }

  /**
   * @param userId
   * @return List of all projects of which the given user (by user id) is member of the project manager group.
   */
  public static Collection<ProjektDO> getProjectsOfManager(final Long userId) {
    final PFUserDO user = UserGroupCache.getInstance().getUser(userId);
    return getProjectsOfManager(user);
  }

  /**
   * @param user
   * @return List of all projects of which the given user is member of the project manager group.
   */
  public static Collection<ProjektDO> getProjectsOfManager(final PFUserDO user) {
    final Collection<ProjektDO> result = new LinkedList<>();
    final ProjektFilter filter = new ProjektFilter();
    ensureProjectDao();
    final List<ProjektDO> projects = projektDao.select(filter);
    if (CollectionUtils.isEmpty(projects)) {
      return result;
    }
    final UserGroupCache userGroupCache = UserGroupCache.getInstance();
    for (final ProjektDO project : projects) {
      final Long groupId = project.getProjektManagerGroupId();
      if (groupId == null) {
        // No manager group defined.
        continue;
      }
      if (!userGroupCache.isUserMemberOfGroup(user, groupId)) {
        continue;
      }
      result.add(project);
    }
    return initialize(result);
  }

  public static Collection<ProjektDO> getAllProjects() {
    final BaseSearchFilter filter = new BaseSearchFilter();
    filter.setDeleted(false);
    ensureProjectDao();
    return initialize(projektDao.select(filter));
  }

  /**
   * Resolves the lazy kunde/task (and other) proxies of the given projects from the in-memory caches. Callers (mostly
   * scripts) typically read {@code project.getKunde()} / {@code project.getTask()} afterwards; without this, each such
   * read would trigger a separate SELECT (n+1). The identifier reads inside {@link PfCaches#initialize(ProjektDO)} are
   * free, so this stays load-free.
   */
  private static Collection<ProjektDO> initialize(final Collection<ProjektDO> projects) {
    if (CollectionUtils.isNotEmpty(projects)) {
      ensurePfCaches();
      for (final ProjektDO project : projects) {
        pfCaches.initialize(project);
      }
    }
    return projects;
  }

  private static void ensureProjectDao() {
    if (projektDao == null) {
      projektDao = ApplicationContextProvider.getApplicationContext().getBean(ProjektDao.class);
    }
  }

  private static void ensurePfCaches() {
    if (pfCaches == null) {
      pfCaches = ApplicationContextProvider.getApplicationContext().getBean(PfCaches.class);
    }
  }
}
