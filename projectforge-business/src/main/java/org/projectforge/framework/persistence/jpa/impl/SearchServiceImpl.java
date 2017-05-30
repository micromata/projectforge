package org.projectforge.framework.persistence.jpa.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.api.FallbackBaseDaoService;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SearchService;
import org.projectforge.framework.persistence.jpa.PfEmgr;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of SearchService.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Service
@Deprecated
public class SearchServiceImpl implements SearchService
{
  private static final Logger log = Logger.getLogger(SearchServiceImpl.class);
  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  protected TenantChecker tenantChecker;

  @Autowired
  private PfEmgrFactory emf;
  @Autowired
  private FallbackBaseDaoService fallbackBaseDaoService;

  /**
   * {@inheritDoc}
   */

  @Override
  public String[] getSearchFields(Class<? extends BaseDO<?>> entClass)
  {
    String[] searchFields = HibernateSearchFilterUtils.determineSearchFields(entClass,
        new String[] {});
    return searchFields;
  }

  @Override
  public <ENT extends ExtendedBaseDO<Integer>> List<ENT> getList(final QueryFilter filter, Class<ENT> entClazz)
      throws AccessException
  {
    BaseDao<ENT> baseDao = fallbackBaseDaoService.getBaseDaoForEntity(entClazz);

    baseDao.checkLoggedInUserSelectAccess();
    if (accessChecker.isRestrictedUser() == true) {
      return new ArrayList<>();
    }

    List<ENT> list = internalGetList(filter, entClazz);
    list = extractEntriesWithSelectAccess(list, entClazz, baseDao);
    return baseDao.sort(list);
  }

  protected <ENT extends ExtendedBaseDO<Integer>> List<ENT> extractEntriesWithSelectAccess(List<ENT> origList,
      Class<ENT> entClazz, BaseDao<ENT> baseDao)
  {
    final List<ENT> result = new ArrayList<ENT>();

    for (final ENT obj : origList) {
      if ((tenantChecker.isSuperAdmin(ThreadLocalUserContext.getUser()) == true
          || tenantChecker.isPartOfCurrentTenant(obj) == true)
          && baseDao.hasLoggedInUserSelectAccess(obj, false) == true) {
        result.add(obj);
        baseDao.afterLoad(obj);
      }
    }
    return result;
  }

  public <ENT extends ExtendedBaseDO<Integer>> List<ENT> internalGetList(QueryFilter filter, Class<ENT> entClazz)
      throws AccessException
  {
    return emf.runWoTrans((emgr) -> getListInternal(emgr, filter, entClazz));

  }

  private <ENT extends ExtendedBaseDO<Integer>> List<ENT> getListInternal(PfEmgr emgr, QueryFilter filter,
      Class<ENT> entClazz)
      throws AccessException
  {
    final BaseSearchFilter searchFilter = filter.getFilter();
    filter.clearErrorMessage();
    if (searchFilter.isIgnoreDeleted() == false) {
      filter.add(Restrictions.eq("deleted", searchFilter.isDeleted()));
    }
    if (searchFilter.getModifiedSince() != null) {
      filter.add(Restrictions.ge("lastUpdate", searchFilter.getModifiedSince()));
    }

    List<ENT> list = null;
    Session session = emgr.getSession();
    {
      final Criteria criteria = filter.buildCriteria(session, entClazz);
      //      TODO RK setCacheRegion(criteria);
      if (searchFilter.isSearchNotEmpty() == true) {
        final String searchString = HibernateSearchFilterUtils.modifySearchString(searchFilter.getSearchString());
        String[] searchFields = HibernateSearchFilterUtils.determineSearchFields(entClazz);
        try {
          FullTextSession fullTextSession = Search.getFullTextSession(session);
          final org.apache.lucene.search.Query query = HibernateSearchFilterUtils.createFullTextQuery(fullTextSession,
              searchFields, filter, searchString, entClazz);
          final FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(query, entClazz);
          fullTextQuery.setCriteriaQuery(criteria);
          list = fullTextQuery.list(); // return a list of managed objects
          // go wron on autocomplate emgr.detach(list);
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
        list = criteria.list();
      }
      if (list != null) {
        list = (List<ENT>) CollectionUtils.select(list, PredicateUtils.uniquePredicate());
        if (list.size() > 0 && searchFilter.applyModificationFilter()) {
          // Search now all history entries which were modified by the given user and/or in the given time period.
          Set<Integer> idSet = getHistoryEntries(session, searchFilter, entClazz);
          List<ENT> result = new ArrayList<ENT>();
          for (final ENT entry : list) {
            if (idSet.contains(entry.getId()) == true) {
              result.add(entry);
            }
          }
          list = result;
        }
      }
    }
    if (searchFilter.isSearchHistory() == true && searchFilter.isSearchNotEmpty() == true) {
      // Search now all history for the given search string.
      final Set<Integer> idSet = searchHistoryEntries(session, searchFilter, entClazz);
      if (CollectionUtils.isNotEmpty(idSet) == true) {
        for (final ENT entry : list) {
          if (idSet.contains(entry.getId()) == true) {
            idSet.remove(entry.getId()); // Object does already exist in list.
          }
        }
        if (idSet.isEmpty() == false) {
          final Criteria criteria = filter.buildCriteria(session, entClazz);
          // TODO RK setCacheRegion(criteria);
          criteria.add(Restrictions.in("id", idSet));
          List<ENT> historyMatchingEntities = criteria.list();
          emgr.detach(historyMatchingEntities);
          list.addAll(historyMatchingEntities);
        }
      }
    }
    if (list == null) {
      // History search without search string.
      list = new ArrayList<ENT>();
    }
    return list;
  }

  private Set<Integer> getHistoryEntries(final Session session, final BaseSearchFilter filter,
      Class<? extends BaseDO<?>> entClass)
  {

    if (accessChecker.hasLoggedInUserAccess(entClass, OperationType.SELECT) == false
        || accessChecker.hasLoggedInUserHistoryAccess(entClass) == false) {
      // User has in general no access to history entries of the given object type (clazz).
      return Collections.emptySet();
    }
    final Set<Integer> idSet = new HashSet<Integer>();
    HibernateSearchFilterUtils.getHistoryEntriesDirect(session, filter, idSet, entClass);

    return idSet;
  }

  private Set<Integer> searchHistoryEntries(final Session session, final BaseSearchFilter filter,
      Class<? extends BaseDO<?>> entClass)
  {

    if (accessChecker.hasLoggedInUserAccess(entClass, OperationType.SELECT) == false
        || accessChecker.hasLoggedInUserHistoryAccess(entClass) == false) {
      // User has in general no access to history entries of the given object type (clazz).
      return Collections.emptySet();
    }
    final Set<Integer> idSet = new HashSet<Integer>();
    HibernateSearchFilterUtils.getHistoryEntriesFromSearch(session, filter, idSet, entClass);

    return idSet;
  }
}
