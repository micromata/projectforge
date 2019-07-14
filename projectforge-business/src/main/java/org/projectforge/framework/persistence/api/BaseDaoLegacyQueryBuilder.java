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

package org.projectforge.framework.persistence.api;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * With ProjectForge 7 MagicFilter's where introduced. The former technology for building queries using
 * BaseSearchFilter and QueryFilter is now handled through this class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class BaseDaoLegacyQueryBuilder {

  private static final Logger log = LoggerFactory.getLogger(BaseDao.class);

  private AccessChecker accessChecker;

  @Autowired
  BaseDaoLegacyQueryBuilder(AccessChecker accessChecker) {
    this.accessChecker = accessChecker;
  }

  /**
   * Gets the list filtered by the given filter.
   *
   * @param filter
   * @return
   */
  public <O extends ExtendedBaseDO<Integer>> List<O> getList(BaseDao<O> baseDao, final QueryFilter filter) throws AccessException {
    long begin = System.currentTimeMillis();
    baseDao.checkLoggedInUserSelectAccess();
    if (accessChecker.isRestrictedUser() == true) {
      return new ArrayList<>();
    }
    List<O> list = internalGetList(baseDao, filter);
    if (list == null || list.size() == 0) {
      return list;
    }
    list = baseDao.extractEntriesWithSelectAccess(list);
    List<O> result = baseDao.sort(list);
    long end = System.currentTimeMillis();
    if (end - begin > 2000) {
      // Show only slow requests.
      log.info(
              "BaseDao.getList for entity class: " + baseDao.getEntityClass().getSimpleName() + " took: " + (end - begin) + " ms (>2s).");
    }
    return result;
  }

  /**
   * Gets the list filtered by the given filter.
   *
   * @param filter
   * @return
   */
  @SuppressWarnings("unchecked")
  public  <O extends ExtendedBaseDO<Integer>>  List<O> internalGetList(BaseDao<O> baseDao, final QueryFilter filter) throws AccessException {
    final BaseSearchFilter searchFilter = filter.getFilter();
    filter.clearErrorMessage();
    if (searchFilter.isIgnoreDeleted() == false) {
      filter.add(Restrictions.eq("deleted", searchFilter.isDeleted()));
    }
    if (searchFilter.getModifiedSince() != null) {
      filter.add(Restrictions.ge("lastUpdate", searchFilter.getModifiedSince()));
    }

    List<O> list = null;
    Session session = baseDao.getSession();
    {
      if (searchFilter.isSearchNotEmpty() == true) {
        final String searchString = HibernateSearchFilterUtils.modifySearchString(searchFilter.getSearchString());
        final String[] searchFields = searchFilter.getSearchFields() != null ? searchFilter.getSearchFields() : baseDao.getSearchFields();
        try {

          int firstIndex = 0;
          int maxIndex = 32000; // maximum numbers of values in IN statements in postgres
          List<O> result;
          final List<O> allResult = new ArrayList<>();

          do {
            final Criteria criteria = filter.buildCriteria(session, baseDao.clazz);
            setCacheRegion(baseDao, criteria);

            FullTextSession fullTextSession = Search.getFullTextSession(session);
            org.apache.lucene.search.Query query = HibernateSearchFilterUtils.createFullTextQuery(fullTextSession, searchFields, filter, searchString, baseDao.clazz);
            FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(query, baseDao.clazz);

            fullTextQuery.setCriteriaQuery(criteria);
            fullTextQuery.setFirstResult(firstIndex);
            fullTextQuery.setMaxResults(maxIndex);

            firstIndex += maxIndex;

            result = fullTextQuery.list(); // return a list of managed objects
            allResult.addAll(result);
          } while (result.isEmpty() == false);

          list = allResult;
        } catch (final Exception ex) {
          final String errorMsg = "Lucene error message: "
                  + ex.getMessage()
                  + " (for "
                  + this.getClass().getSimpleName()
                  + ": "
                  + searchString
                  + ").";
          filter.setErrorMessage(errorMsg);
          log.info(errorMsg);
        }
      } else {
        final Criteria criteria = filter.buildCriteria(session, baseDao.clazz);
        setCacheRegion(baseDao, criteria);
        list = criteria.list();
      }
      if (list != null) {
        list = baseDao.selectUnique(list);
        if (list.size() > 0 && searchFilter.applyModificationFilter()) {
          // Search now all history entries which were modified by the given user and/or in the given time period.
          final Set<Integer> idSet = baseDao.getHistoryEntries(baseDao.getSession(), searchFilter);
          final List<O> result = new ArrayList<O>();
          for (final O entry : list) {
            if (baseDao.contains(idSet, entry) == true) {
              result.add(entry);
            }
          }
          list = result;
        }
      }
    }
    if (searchFilter.isSearchHistory() == true && searchFilter.isSearchNotEmpty() == true) {
      // Search now all history for the given search string.
      final Set<Integer> idSet = baseDao.searchHistoryEntries(baseDao.getSession(), searchFilter);
      if (CollectionUtils.isNotEmpty(idSet) == true) {
        for (final O entry : list) {
          if (idSet.contains(entry.getId()) == true) {
            idSet.remove(entry.getId()); // Object does already exist in list.
          }
        }
        if (idSet.isEmpty() == false) {
          final Criteria criteria = filter.buildCriteria(baseDao.getSession(), baseDao.clazz);
          setCacheRegion(baseDao, criteria);
          criteria.add(Restrictions.in("id", idSet));
          final List<O> historyMatchingEntities = criteria.list();
          list.addAll(historyMatchingEntities);
        }
      }
    }
    if (list == null) {
      // History search without search string.
      list = new ArrayList<O>();
    }
    return list;
  }

  private void setCacheRegion(BaseDao baseDao, final Criteria criteria) {
    criteria.setCacheable(true);
    if (baseDao.useOwnCriteriaCacheRegion() == false) {
      return;
    }
    criteria.setCacheRegion(baseDao.getClass().getName());
  }
}
