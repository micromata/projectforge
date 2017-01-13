/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.apache.lucene.util.Version;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRight;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.InternalErrorException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.database.DatabaseDao;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.history.HibernateSearchDependentObjectsReindexer;
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter;
import org.projectforge.framework.persistence.history.SimpleHistoryEntry;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.jpa.impl.BaseDaoJpaAdapter;
import org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils;
import org.projectforge.framework.persistence.search.BaseDaoReindexRegistry;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.time.DateHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.micromata.genome.db.jpa.history.api.DiffEntry;
import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.genome.util.runtime.ClassUtils;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public abstract class BaseDao<O extends ExtendedBaseDO<Integer>>
    implements IDao<O>, IPersistenceService<O>
{

  public static final String EXCEPTION_HISTORIZABLE_NOTDELETABLE = "Could not delete of Historizable objects (contact your software developer): ";
  /**
   * @see Version#LUCENE_31
   */
  // HIBERNATE5  public static final Version LUCENE_VERSION = Version.LUCENE_31;
  public static final Version LUCENE_VERSION = Version.LUCENE_5_3_1;
  /**
   * Maximum allowed mass updates within one massUpdate call.
   */
  public static final int MAX_MASS_UPDATE = 100;
  public static final String MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N = "massUpdate.error.maximumNumberOfAllowedMassUpdatesExceeded";
  private static final List<DisplayHistoryEntry> EMPTY_HISTORY_ENTRIES = new ArrayList<DisplayHistoryEntry>();
  private static final Logger log = Logger.getLogger(BaseDao.class);
  /**
   * DEBUG flag. remove later
   */
  public static boolean NO_UPDATE_MAGIC = true;
  /**
   * DEBUG flag. remove later
   */
  public static boolean USE_SEARCH_SERVIVE = false;
  /**
   * DEBUG flag. Not sure, if always has be flushed.
   */
  public static boolean LUCENE_FLUSH_ALWAYS = true;

  protected Class<O> clazz;

  @Autowired
  protected AccessChecker accessChecker;

  @Autowired
  protected DatabaseDao databaseDao;

  @Autowired
  @Deprecated
  protected TransactionTemplate txTemplate;

  @Autowired
  protected TenantChecker tenantChecker;

  @Autowired
  protected SearchService searchService;

  protected String[] searchFields;

  protected BaseDaoReindexRegistry baseDaoReindexRegistry = BaseDaoReindexRegistry.getSingleton();

  protected IUserRightId userRightId = null;
  /**
   * Should the id check (on null) be avoided before save (in save method)? This is use-full if the derived dao manages
   * the id itself (as e. g. KundeDao, Kost2ArtDao).
   */
  protected boolean avoidNullIdCheckBeforeSave;
  /**
   * Set this to true if you overload {@link #afterUpdate(ExtendedBaseDO, ExtendedBaseDO)} and you need the origin data
   * base entry in this method.
   */
  protected boolean supportAfterUpdate = false;

  @Autowired
  private PfEmgrFactory emgrFactory;

  @Autowired
  private HibernateTemplate hibernateTemplate;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private UserRightService userRights;

  @Autowired
  private HibernateSearchDependentObjectsReindexer hibernateSearchDependentObjectsReindexer;

  /**
   * The setting of the DO class is required.
   *
   * @param clazz
   */
  protected BaseDao(final Class<O> clazz)
  {
    this.clazz = clazz;
  }

  /**
   * Get all declared hibernate search fields. These fields are defined over annotations in the database object class.
   * The names are the property names or, if defined the name declared in the annotation of a field. <br/>
   * The user can search in these fields explicit by typing e. g. authors:beck (<field>:<searchString>)
   *
   * @return
   */
  @Override
  public synchronized String[] getSearchFields()
  {
    if (searchFields != null) {
      return searchFields;
    }
    searchFields = HibernateSearchFilterUtils.determineSearchFields(clazz, getAdditionalSearchFields());
    return searchFields;
  }

  /**
   * Overwrite this method for adding search fields manually (e. g. for embedded objects). For example see TimesheetDao.
   *
   * @return
   */
  protected String[] getAdditionalSearchFields()
  {
    return null;
  }

  public Class<O> getDOClass()
  {
    return this.clazz;
  }

  @Override
  public abstract O newInstance();

  /**
   * getOrLoad checks first weather the id is valid or not. Default implementation: id != 0 && id &gt; 0. Overload this,
   * if the id of the DO can be 0 for example.
   *
   * @param id
   * @return
   */
  protected boolean isIdValid(final Integer id)
  {
    return (id != null && id > 0);
  }

  /**
   * If the user has select access then the object will be returned. If not, the hibernate proxy object will be get via
   * getSession().load();
   *
   * @param id
   * @return
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public O getOrLoad(final Integer id)
  {
    if (isIdValid(id) == false) {
      return null;
    } else {
      final O obj = internalGetById(id);
      if (obj == null) {
        //throw new RuntimeException("Object with id " + id + " not found for class " + clazz);
        return null;
      }
      if (tenantChecker.isPartOfCurrentTenant(obj) == true
          && hasLoggedInUserSelectAccess(obj, false) == true) {
        return obj;
      }
    }
    final O result = getSession().load(clazz, id);
    return result;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<O> internalLoadAllNotDeleted()
  {
    return internalLoadAll().stream().filter(o -> o.isDeleted() == false).collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<O> internalLoadAll()
  {
    return emgrFactory.runRoTrans(emgr -> {
      return emgr.select(clazz, "SELECT t FROM " + clazz.getSimpleName() + " t");
    });
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<O> internalLoadAll(final TenantDO tenant)
  {
    if (tenant == null) {
      return emgrFactory.runRoTrans(emgr -> {
        return emgr.select(clazz, "SELECT t FROM " + clazz.getSimpleName() + " t WHERE t.tenant IS NULL");
      });
    }
    if (tenant.isDefault() == true) {
      return emgrFactory.runRoTrans(emgr -> {
        return emgr.select(clazz, "SELECT t FROM " + clazz.getSimpleName() + " t WHERE t.tenant = :tenant OR t.tenant IS NULL", "tenant", tenant);
      });
    } else {
      return emgrFactory.runRoTrans(emgr -> {
        return emgr.select(clazz, "SELECT t FROM " + clazz.getSimpleName() + " t WHERE t.tenant = :tenant", "tenant", tenant);
      });
    }
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<O> internalLoad(final Collection<? extends Serializable> idList)
  {
    if (idList == null) {
      return null;
    }
    final Session session = getSession();
    final Criteria criteria = session.createCriteria(clazz).add(Restrictions.in("id", idList));
    @SuppressWarnings("unchecked")
    final List<O> list = selectUnique(criteria.list());
    return list;
  }

  /**
   * This method is used by the searchDao and calls {@link #getList(BaseSearchFilter)} by default.
   *
   * @param filter
   * @return A list of found entries or empty list. PLEASE NOTE: Returns null only if any error occured.
   * @see #getList(BaseSearchFilter)
   */
  public List<O> getListForSearchDao(final BaseSearchFilter filter)
  {
    return getList(filter);
  }

  /**
   * Builds query filter by simply calling constructor of QueryFilter with given search filter and calls
   * getList(QueryFilter). Override this method for building more complex query filters.
   *
   * @param filter
   * @return A list of found entries or empty list. PLEASE NOTE: Returns null only if any error occured.
   */
  @Override
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<O> getList(final BaseSearchFilter filter)
  {
    final QueryFilter queryFilter = createQueryFilter(filter);
    return getList(queryFilter);
  }

  protected QueryFilter createQueryFilter(final BaseSearchFilter filter)
  {
    return new QueryFilter(filter);
  }

  /**
   * Gets the list filtered by the given filter.
   *
   * @param filter
   * @return
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<O> getList(final QueryFilter filter) throws AccessException
  {
    long begin = System.currentTimeMillis();
    if (USE_SEARCH_SERVIVE == true) {
      return searchService.getList(filter, getEntityClass());
    }
    checkLoggedInUserSelectAccess();
    if (accessChecker.isRestrictedUser() == true) {
      return new ArrayList<>();
    }
    List<O> list = internalGetList(filter);
    if (list == null || list.size() == 0) {
      return list;
    }
    list = extractEntriesWithSelectAccess(list);
    List<O> result = sort(list);
    long end = System.currentTimeMillis();
    log.info(
        "BaseDao.getList for entity class: " + getEntityClass().getSimpleName() + " took: " + (end - begin) + " ms.");
    return result;
  }

  /**
   * Gets the list filtered by the given filter.
   *
   * @param filter
   * @return
   */
  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<O> internalGetList(final QueryFilter filter) throws AccessException
  {
    final BaseSearchFilter searchFilter = filter.getFilter();
    filter.clearErrorMessage();
    if (searchFilter.isIgnoreDeleted() == false) {
      filter.add(Restrictions.eq("deleted", searchFilter.isDeleted()));
    }
    if (searchFilter.getModifiedSince() != null) {
      filter.add(Restrictions.ge("lastUpdate", searchFilter.getModifiedSince()));
    }

    List<O> list = null;
    Session session = getSession();
    {
      final Criteria criteria = filter.buildCriteria(session, clazz);
      setCacheRegion(criteria);
      if (searchFilter.isSearchNotEmpty() == true) {
        final String searchString = HibernateSearchFilterUtils.modifySearchString(searchFilter.getSearchString());
        final String[] searchFields = searchFilter.getSearchFields() != null ? searchFilter.getSearchFields()
            : getSearchFields();
        try {
          //          String nsearch = StringUtils.replace(searchString, "*", "");
          FullTextSession fullTextSession = Search.getFullTextSession(session);
          final org.apache.lucene.search.Query query = HibernateSearchFilterUtils.createFullTextQuery(fullTextSession,
              searchFields, filter, searchString, clazz);
          final FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(query, clazz);
          fullTextQuery.setCriteriaQuery(criteria);
          list = fullTextQuery.list(); // return a list of managed objects
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
        list = selectUnique(list);
        if (list.size() > 0 && searchFilter.isUseModificationFilter() == true) {
          // Search now all history entries which were modified by the given user and/or in the given time period.
          final Set<Integer> idSet = getHistoryEntries(getSession(), searchFilter);
          final List<O> result = new ArrayList<O>();
          for (final O entry : list) {
            if (contains(idSet, entry) == true) {
              result.add(entry);
            }
          }
          list = result;
        }
      }
    }
    if (searchFilter.isSearchHistory() == true && searchFilter.isSearchNotEmpty() == true) {
      // Search now all history for the given search string.
      final Set<Integer> idSet = searchHistoryEntries(getSession(), searchFilter);
      if (CollectionUtils.isNotEmpty(idSet) == true) {
        for (final O entry : list) {
          if (idSet.contains(entry.getId()) == true) {
            idSet.remove(entry.getId()); // Object does already exist in list.
          }
        }
        if (idSet.isEmpty() == false) {
          final Criteria criteria = filter.buildCriteria(getSession(), clazz);
          setCacheRegion(criteria);
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

  /**
   * idSet.contains(entry.getId()) at default.
   *
   * @param idSet
   * @param entry
   * @see org.projectforge.business.fibu.AuftragDao#contains(Set, org.projectforge.business.fibu.AuftragDO)
   */
  protected boolean contains(final Set<Integer> idSet, final O entry)
  {
    if (idSet == null) {
      return false;
    }
    return idSet.contains(entry.getId());
  }

  protected List<O> selectUnique(final List<O> list)
  {
    @SuppressWarnings("unchecked")
    final List<O> result = (List<O>) CollectionUtils.select(list, PredicateUtils.uniquePredicate());
    return result;
  }

  protected List<O> extractEntriesWithSelectAccess(final List<O> origList)
  {
    final List<O> result = new ArrayList<O>();
    for (final O obj : origList) {
      if ((TenantChecker.isSuperAdmin(ThreadLocalUserContext.getUser()) == true
          || tenantChecker.isPartOfCurrentTenant(obj) == true)
          && hasLoggedInUserSelectAccess(obj, false) == true) {
        result.add(obj);
        afterLoad(obj);
      }
    }
    return result;
  }

  /**
   * Overwrite this method for own list sorting. This method returns only the given list.
   *
   * @param list
   */
  public List<O> sort(final List<O> list)
  {
    return list;
  }

  /**
   * @param id primary key of the base object.
   * @return
   */
  @Override
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public O getById(final Serializable id) throws AccessException
  {
    if (accessChecker.isRestrictedUser() == true) {
      return null;
    }
    checkLoggedInUserSelectAccess();
    final O obj = internalGetById(id);
    if (obj == null) {
      return null;
    }
    checkPartOfCurrentTenant(obj, OperationType.SELECT);
    checkLoggedInUserSelectAccess(obj);
    return obj;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public O internalGetById(final Serializable id)
  {
    if (id == null) {
      return null;
    }
    final O obj = emgrFactory.runRoTrans(emgr -> {
      try {
        return emgr.selectByPkDetached(clazz, id);
      } catch (NoResultException e) {
        log.warn("No result fund for entity " + clazz.getSimpleName() + " and id: " + id);
        return null;
      }
    });
    afterLoad(obj);
    return obj;
  }

  /**
   * Gets the history entries of the object.
   *
   * @param id The id of the object.
   * @return
   */
  @SuppressWarnings("rawtypes")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public HistoryEntry[] getHistoryEntries(final O obj)
  {
    accessChecker.checkRestrictedUser();
    checkPartOfCurrentTenant(obj, OperationType.SELECT);
    checkLoggedInUserHistoryAccess(obj);
    return internalGetHistoryEntries(obj);
  }

  @SuppressWarnings("rawtypes")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public HistoryEntry[] internalGetHistoryEntries(final BaseDO<?> obj)
  {
    accessChecker.checkRestrictedUser();
    return HistoryBaseDaoAdapter.getHistoryFor(obj);
  }

  /**
   * Gets the history entries of the object in flat format.<br/>
   * Please note: If user has no access an empty list will be returned.
   *
   * @param id The id of the object.
   * @return
   */
  @Override
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final O obj)
  {
    if (obj.getId() == null || hasLoggedInUserHistoryAccess(obj, false) == false) {
      return EMPTY_HISTORY_ENTRIES;
    }
    return internalGetDisplayHistoryEntries(obj);
  }

  public List<DisplayHistoryEntry> internalGetDisplayHistoryEntries(final BaseDO<?> obj)
  {
    accessChecker.checkRestrictedUser();
    final List<DisplayHistoryEntry> result = hibernateTemplate
        .execute(new HibernateCallback<List<DisplayHistoryEntry>>()
        {
          @SuppressWarnings("rawtypes")
          @Override
          public List<DisplayHistoryEntry> doInHibernate(Session session) throws HibernateException
          {
            final HistoryEntry[] entries = internalGetHistoryEntries(obj);
            if (entries == null) {
              return null;
            }
            return convertAll(entries, session);
          }
        });
    return result;
  }

  @SuppressWarnings("rawtypes")
  protected List<DisplayHistoryEntry> convertAll(final HistoryEntry[] entries, final Session session)
  {
    final List<DisplayHistoryEntry> list = new ArrayList<DisplayHistoryEntry>();
    for (final HistoryEntry entry : entries) {
      final List<DisplayHistoryEntry> l = convert(entry, session);
      list.addAll(l);
    }
    return list;
  }

  public List<DisplayHistoryEntry> convert(final HistoryEntry<?> entry, final Session session)
  {
    if (entry.getDiffEntries().isEmpty() == true) {
      final DisplayHistoryEntry se = new DisplayHistoryEntry(getUserGroupCache(), entry);
      return Collections.singletonList(se);
    }
    List<DisplayHistoryEntry> result = new ArrayList<>();
    for (DiffEntry prop : entry.getDiffEntries()) {
      DisplayHistoryEntry se = new DisplayHistoryEntry(getUserGroupCache(), entry, prop, session);
      result.add(se);
    }

    return result;
  }

  /**
   * Gets the history entries of the object in flat format.<br/>
   * Please note: No check access will be done! Please check the access before getting the object.
   *
   * @param id The id of the object.
   * @return
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<SimpleHistoryEntry> getSimpleHistoryEntries(final O obj)
  {
    return HistoryBaseDaoAdapter.getSimpleHistoryEntries(obj, getUserGroupCache());
  }

  /**
   * @param obj
   * @return the generated identifier, if save method is used, otherwise null.
   * @throws AccessException
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  public Serializable saveOrUpdate(final O obj) throws AccessException
  {
    Serializable id = null;
    if (obj.getId() != null) {
      update(obj);
    } else {
      id = save(obj);
    }
    return id;
  }

  /**
   * @param obj
   * @return the generated identifier, if save method is used, otherwise null.
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  public Serializable internalSaveOrUpdate(final O obj)
  {
    Serializable id = null;
    if (obj.getId() != null) {
      internalUpdate(obj);
    } else {
      id = internalSave(obj);
    }
    return id;
  }

  /**
   * Call save(O) for every object in the given list.
   *
   * @param objects
   * @return the generated identifier.
   * @throws AccessException
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  public void save(final List<O> objects) throws AccessException
  {
    Validate.notNull(objects);
    for (final O obj : objects) {
      save(obj);
    }
  }

  /**
   * @param obj
   * @return the generated identifier.
   * @throws AccessException
   */
  @Override
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public Integer save(final O obj) throws AccessException
  {
    long begin = System.currentTimeMillis();
    Validate.notNull(obj);
    if (avoidNullIdCheckBeforeSave == false) {
      Validate.isTrue(obj.getId() == null);
    }
    checkPartOfCurrentTenant(obj, OperationType.INSERT);
    checkLoggedInUserInsertAccess(obj);
    accessChecker.checkRestrictedOrDemoUser();
    Integer result = internalSave(obj);
    long end = System.currentTimeMillis();
    log.info("BaseDao.save took: " + (end - begin) + " ms.");
    return result;
  }

  @Override
  public Integer insert(O obj) throws AccessException
  {
    return save(obj);
  }

  /**
   * This method will be called after loading an object from the data base. Does nothing at default. This method is not
   * called by internalLoadAll.
   */
  public void afterLoad(final O obj)
  {

  }

  /**
   * This method will be called after inserting, updating, deleting or marking the data object as deleted. This method
   * is for example needed for expiring the UserGroupCache after inserting or updating a user or group data object. Does
   * nothing at default.
   */
  protected void afterSaveOrModify(final O obj)
  {
  }

  /**
   * This method will be called after inserting. Does nothing at default.
   *
   * @param obj The inserted object
   */
  protected void afterSave(final O obj)
  {
  }

  /**
   * This method will be called before inserting. Does nothing at default.
   */
  protected void onSave(final O obj)
  {
  }

  /**
   * This method will be called before inserting, updating, deleting or marking the data object as deleted. Does nothing
   * at default.
   */
  protected void onSaveOrModify(final O obj)
  {
  }

  /**
   * This method will be called after updating. Does nothing at default. PLEASE NOTE: If you overload this method don't
   * forget to set {@link #supportAfterUpdate} to true, otherwise you won't get the origin data base object!
   *
   * @param obj   The modified object
   * @param dbObj The object from data base before modification.
   */
  protected void afterUpdate(final O obj, final O dbObj)
  {
  }

  /**
   * This method will be called after updating. Does nothing at default. PLEASE NOTE: If you overload this method don't
   * forget to set {@link #supportAfterUpdate} to true, otherwise you won't get the origin data base object!
   *
   * @param obj        The modified object
   * @param dbObj      The object from data base before modification.
   * @param isModified is true if the object was changed, false if the object wasn't modified.
   */
  protected void afterUpdate(final O obj, final O dbObj, final boolean isModified)
  {
  }

  /**
   * This method will be called before updating the data object. Will also called if in internalUpdate no modification
   * was detected. Please note: Do not modify the object oldVersion! Does nothing at default.
   *
   * @param obj   The changed object.
   * @param dbObj The current data base version of this object.
   */
  protected void onChange(final O obj, final O dbObj)
  {
  }

  /**
   * This method will be called before deleting. Does nothing at default.
   *
   * @param obj The deleted object.
   */
  protected void onDelete(final O obj)
  {
  }

  /**
   * This method will be called after deleting as well as after object is marked as deleted. Does nothing at default.
   *
   * @param obj The deleted object.
   */
  protected void afterDelete(final O obj)
  {
  }

  /**
   * This method will be called after undeleting. Does nothing at default.
   *
   * @param obj The deleted object.
   */
  protected void afterUndelete(final O obj)
  {
  }

  /**
   * This method is for internal use e. g. for updating objects without check access.
   *
   * @param obj
   * @return the generated identifier.
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public Integer internalSave(final O obj)
  {
    Validate.notNull(obj);
    //TODO: Muss der richtige Tenant gesetzt werden. Ist nur Workaround.
    if (obj.getTenant() == null) {
      obj.setTenant(getDefaultTenant());
    }
    obj.setCreated();
    obj.setLastUpdate();
    onSave(obj);
    onSaveOrModify(obj);
    BaseDaoJpaAdapter.prepareInsert(obj);
    Session session = hibernateTemplate.getSessionFactory().getCurrentSession();
    Integer id = (Integer) session.save(obj);
    log.info("New object added (" + id + "): " + obj.toString());
    prepareHibernateSearch(obj, OperationType.INSERT);
    if (NO_UPDATE_MAGIC == true) {
      // safe will assocated not working
      hibernateTemplate.merge(obj);
    }
    flushSession();
    flushSearchSession();
    HistoryBaseDaoAdapter.inserted(obj);
    afterSaveOrModify(obj);
    afterSave(obj);

    return id;
  }

  private TenantDO getDefaultTenant()
  {
    return emgrFactory.runRoTrans(emgr -> {
      try {
        return emgr.selectByPkDetached(TenantDO.class, 1);
      } catch (NoResultException e) {
        log.warn("Default tenant with id 1 not found!");
        return null;
      }
    });
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
  public void saveOrUpdate(final Collection<O> col)
  {
    for (final O obj : col) {
      saveOrUpdate(obj);
    }
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
  public void saveOrUpdate(final BaseDao<O> currentProxy, final Collection<O> col, final int blockSize)
  {
    final List<O> list = new ArrayList<O>();
    int counter = 0;
    // final BaseDao<O> currentProxy = (BaseDao<O>) AopContext.currentProxy();
    for (final O obj : col) {
      list.add(obj);
      if (++counter >= blockSize) {
        counter = 0;
        currentProxy.saveOrUpdate(list);
        list.clear();
      }
    }
    currentProxy.saveOrUpdate(list);
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
  public void internalSaveOrUpdate(final Collection<O> col)
  {
    for (final O obj : col) {
      internalSaveOrUpdate(obj);
    }
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  public void internalSaveOrUpdate(final BaseDao<O> currentProxy, final Collection<O> col, final int blockSize)
  {
    final List<O> list = new ArrayList<O>();
    int counter = 0;
    // final BaseDao<O> currentProxy = (BaseDao<O>) AopContext.currentProxy();
    for (final O obj : col) {
      list.add(obj);
      if (++counter >= blockSize) {
        counter = 0;
        currentProxy.internalSaveOrUpdate(list);
        list.clear();
      }
    }
    currentProxy.internalSaveOrUpdate(list);
  }

  /**
   * @param obj
   * @return true, if modifications were done, false if no modification detected.
   * @throws AccessException
   * @see #internalUpdate(ExtendedBaseDO, boolean)
   */
  @Override
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public ModificationStatus update(final O obj) throws AccessException
  {
    Validate.notNull(obj);
    if (obj.getId() == null) {
      final String msg = "Could not update object unless id is not given:" + obj.toString();
      log.error(msg);
      throw new RuntimeException(msg);
    }
    return internalUpdate(obj, true);
  }

  /**
   * This method is for internal use e. g. for updating objects without check access.
   *
   * @param obj
   * @return true, if modifications were done, false if no modification detected.
   * @see #internalUpdate(ExtendedBaseDO, boolean)
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
  public ModificationStatus internalUpdate(final O obj)
  {
    return internalUpdate(obj, false);
  }

  /**
   * This method is for internal use e. g. for updating objects without check access.<br/>
   * Please note: update ignores the field deleted. Use markAsDeleted, delete and undelete methods instead.
   *
   * @param obj
   * @param checkAccess If false, any access check will be ignored.
   * @return true, if modifications were done, false if no modification detected.
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
  public ModificationStatus internalUpdate(final O obj, final boolean checkAccess)
  {
    onSaveOrModify(obj);
    if (checkAccess == true) {
      accessChecker.checkRestrictedOrDemoUser();
    }
    final O dbObj = hibernateTemplate.load(clazz, obj.getId(), LockMode.PESSIMISTIC_WRITE);
    if (checkAccess == true) {
      checkPartOfCurrentTenant(obj, OperationType.UPDATE);
      checkLoggedInUserUpdateAccess(obj, dbObj);
    }
    onChange(obj, dbObj);
    final O dbObjBackup;
    if (supportAfterUpdate == true) {
      dbObjBackup = getBackupObject(dbObj);
    } else {
      dbObjBackup = null;
    }
    final boolean wantsReindexAllDependentObjects = wantsReindexAllDependentObjects(obj, dbObj);
    ModificationStatus result = HistoryBaseDaoAdapter.wrappHistoryUpdate(dbObj, () -> {
      // Copy all values of modified user to database object, ignore field 'deleted'.
      final ModificationStatus tresult = copyValues(obj, dbObj, "deleted");

      if (tresult != ModificationStatus.NONE) {
        BaseDaoJpaAdapter.prepareUpdate(dbObj);
        dbObj.setLastUpdate();
        log.info("Object updated: " + dbObj.toString());
      } else {
        log.info("No modifications detected (no update needed): " + dbObj.toString());
      }
      prepareHibernateSearch(obj, OperationType.UPDATE);
      // TODO HIBERNATE5 Magie nicht notwendig?!?!?!
      if (NO_UPDATE_MAGIC == true) {
        // update doesn't work, because of referenced objects
        hibernateTemplate.merge(dbObj);
      }
      flushSession();
      flushSearchSession();
      return tresult;
    });

    afterSaveOrModify(obj);
    if (supportAfterUpdate == true) {
      afterUpdate(obj, dbObjBackup, result != ModificationStatus.NONE);
      afterUpdate(obj, dbObjBackup);
    } else {
      afterUpdate(obj, null, result != ModificationStatus.NONE);
      afterUpdate(obj, null);
    }
    if (wantsReindexAllDependentObjects == true) {
      reindexDependentObjects(obj);
    }
    return result;
  }

  /**
   * @return If true (default if not minor Change) all dependent data-base objects will be re-indexed. For e. g.
   * PFUserDO all time-sheets etc. of this user will be re-indexed. It's called after internalUpdate. Refer
   * UserDao to see more.
   * @see BaseDO#isMinorChange()
   */
  protected boolean wantsReindexAllDependentObjects(final O obj, final O dbObj)
  {
    return obj.isMinorChange() == false;
  }

  /**
   * Used by internal update if supportAfterUpdate is true for storing db object version for afterUpdate. Override this
   * method to implement your own copy method.
   *
   * @param dbObj
   * @return
   */
  protected O getBackupObject(final O dbObj)
  {
    final O backupObj = newInstance();
    copyValues(dbObj, backupObj);
    return backupObj;
  }

  /**
   * Overwrite this method if you have lazy exceptions while Hibernate-Search re-indexes. See e. g. AuftragDao.
   *
   * @param obj
   */
  protected void prepareHibernateSearch(final O obj, final OperationType operationType)
  {
  }

  /**
   * Object will be marked as deleted (boolean flag), therefore undelete is always possible without any loss of data.
   *
   * @param obj
   */
  @Override
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void markAsDeleted(final O obj) throws AccessException
  {
    Validate.notNull(obj);
    if (obj.getId() == null) {
      final String msg = "Could not delete object unless id is not given:" + obj.toString();
      log.error(msg);
      throw new RuntimeException(msg);
    }
    final O dbObj = hibernateTemplate.load(clazz, obj.getId(), LockMode.PESSIMISTIC_WRITE);
    checkPartOfCurrentTenant(obj, OperationType.DELETE);
    checkLoggedInUserDeleteAccess(obj, dbObj);
    accessChecker.checkRestrictedOrDemoUser();
    internalMarkAsDeleted(obj);
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void internalMarkAsDeleted(final O obj)
  {
    if (HistoryBaseDaoAdapter.isHistorizable(obj) == false) {
      log.error(
          "Object is not historizable. Therefore marking as deleted is not supported. Please use delete instead.");
      throw new InternalErrorException();
    }
    onDelete(obj);
    final O dbObj = hibernateTemplate.load(clazz, obj.getId(), LockMode.PESSIMISTIC_WRITE);
    onSaveOrModify(obj);

    HistoryBaseDaoAdapter.wrappHistoryUpdate(dbObj, () -> {
      BaseDaoJpaAdapter.beforeUpdateCopyMarkDelete(dbObj, obj);
      copyValues(obj, dbObj, "deleted"); // If user has made additional changes.
      dbObj.setDeleted(true);
      dbObj.setLastUpdate();
      flushSession();
      flushSearchSession();
      return null;
    });

    afterSaveOrModify(obj);
    afterDelete(obj);
    flushSession();
    log.info("Object marked as deleted: " + dbObj.toString());
  }

  protected void flushSession()
  {

    Session session = getSession();
    session.flush();
    //    Search.getFullTextSession(session).flushToIndexes();
  }

  protected void flushSearchSession()
  {
    long begin = System.currentTimeMillis();
    if (LUCENE_FLUSH_ALWAYS == true) {
      Search.getFullTextSession(getSession()).flushToIndexes();
    }
    long end = System.currentTimeMillis();
    log.info("BaseDao.flushSearchSession took: " + (end - begin) + " ms.");
  }

  /**
   * Object will be deleted finally out of the data base.
   *
   * @param obj
   */
  @Override
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void delete(final O obj) throws AccessException
  {
    Validate.notNull(obj);
    if (HistoryBaseDaoAdapter.isHistorizable(obj) == true) {
      final String msg = EXCEPTION_HISTORIZABLE_NOTDELETABLE + obj.toString();
      log.error(msg);
      throw new RuntimeException(msg);
    }
    if (obj.getId() == null) {
      final String msg = "Could not destroy object unless id is not given: " + obj.toString();
      log.error(msg);
      throw new RuntimeException(msg);
    }
    accessChecker.checkRestrictedOrDemoUser();
    onDelete(obj);
    final O dbObj = hibernateTemplate.load(clazz, obj.getId(), LockMode.PESSIMISTIC_WRITE);
    checkPartOfCurrentTenant(obj, OperationType.DELETE);
    checkLoggedInUserDeleteAccess(obj, dbObj);
    hibernateTemplate.delete(dbObj);
    log.info("Object deleted: " + obj.toString());
    afterSaveOrModify(obj);
    afterDelete(obj);
  }

  /**
   * Object will be marked as deleted (booelan flag), therefore undelete is always possible without any loss of data.
   *
   * @param obj
   */
  @Override
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void undelete(final O obj) throws AccessException
  {
    Validate.notNull(obj);
    if (obj.getId() == null) {
      final String msg = "Could not undelete object unless id is not given:" + obj.toString();
      log.error(msg);
      throw new RuntimeException(msg);
    }
    checkPartOfCurrentTenant(obj, OperationType.INSERT);
    checkLoggedInUserInsertAccess(obj);
    accessChecker.checkRestrictedOrDemoUser();
    internalUndelete(obj);
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void internalUndelete(final O obj)
  {
    final O dbObj = hibernateTemplate.load(clazz, obj.getId(), LockMode.PESSIMISTIC_WRITE);
    onSaveOrModify(obj);

    HistoryBaseDaoAdapter.wrappHistoryUpdate(dbObj, () -> {
      BaseDaoJpaAdapter.beforeUpdateCopyMarkUnDelete(dbObj, obj);
      copyValues(obj, dbObj, "deleted"); // If user has made additional changes.
      dbObj.setDeleted(false);
      obj.setDeleted(false);
      dbObj.setLastUpdate();
      obj.setLastUpdate(dbObj.getLastUpdate());
      flushSession();
      flushSearchSession();
      return null;
    });

    afterSaveOrModify(obj);
    afterUndelete(obj);
    log.info("Object undeleted: " + dbObj.toString());
  }

  protected void checkPartOfCurrentTenant(final O obj, final OperationType operationType)
  {
    tenantChecker.checkPartOfCurrentTenant(obj);
  }

  /**
   * Checks the basic select access right. Overload this method if your class supports this right.
   *
   * @return
   */
  public void checkLoggedInUserSelectAccess() throws AccessException
  {
    if (hasSelectAccess(ThreadLocalUserContext.getUser(), true) == false) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  protected void checkLoggedInUserSelectAccess(final O obj) throws AccessException
  {
    if (hasSelectAccess(ThreadLocalUserContext.getUser(), obj, true) == false) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  protected void checkLoggedInUserHistoryAccess(final O obj) throws AccessException
  {
    if (hasHistoryAccess(ThreadLocalUserContext.getUser(), true) == false
        || hasLoggedInUserHistoryAccess(obj, true) == false) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  protected void checkLoggedInUserInsertAccess(final O obj) throws AccessException
  {
    checkInsertAccess(ThreadLocalUserContext.getUser(), obj);
  }

  protected void checkInsertAccess(final PFUserDO user, final O obj) throws AccessException
  {
    if (hasInsertAccess(user, obj, true) == false) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  /**
   * @param dbObj The original object (stored in the database)
   * @param obj
   * @throws AccessException
   */
  protected void checkLoggedInUserUpdateAccess(final O obj, final O dbObj) throws AccessException
  {
    checkUpdateAccess(ThreadLocalUserContext.getUser(), obj, dbObj);
  }

  /**
   * @param dbObj The original object (stored in the database)
   * @param obj
   * @throws AccessException
   */
  protected void checkUpdateAccess(final PFUserDO user, final O obj, final O dbObj) throws AccessException
  {
    if (hasUpdateAccess(user, obj, dbObj, true) == false) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  protected void checkLoggedInUserDeleteAccess(final O obj, final O dbObj) throws AccessException
  {
    if (hasLoggedInUserDeleteAccess(obj, dbObj, true) == false) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  /**
   * Checks the basic select access right. Overwrite this method if the basic select access should be checked.
   *
   * @return true at default or if readWriteUserRightId is given hasReadAccess(boolean).
   * @see #hasReadAccess(boolean)
   */
  public boolean hasLoggedInUserSelectAccess(final boolean throwException)
  {
    return hasSelectAccess(ThreadLocalUserContext.getUser(), throwException);
  }

  /**
   * Checks the basic select access right. Overwrite this method if the basic select access should be checked.
   *
   * @return true at default or if readWriteUserRightId is given hasReadAccess(boolean).
   * @see #hasReadAccess(boolean)
   */
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return hasAccess(user, null, null, OperationType.SELECT, throwException);
  }

  /**
   * If userRightId is given then {@link AccessChecker#hasAccess(UserRightId, Object, Object, OperationType, boolean)}
   * is called and returned. If not given a UnsupportedOperationException is thrown. Checks the user's access to the
   * given object.
   *
   * @param obj           The object.
   * @param obj           The old version of the object (is only given for operationType {@link OperationType#UPDATE}).
   * @param operationType The operation type (select, insert, update or delete)
   * @return true, if the user has the access right for the given operation type and object.
   */
  public boolean hasLoggedInUserAccess(final O obj, final O oldObj, final OperationType operationType,
      final boolean throwException)
  {
    return hasAccess(ThreadLocalUserContext.getUser(), obj, oldObj, operationType, throwException);
  }

  /**
   * If userRightId is given then {@link AccessChecker#hasAccess(UserRightId, Object, Object, OperationType, boolean)}
   * is called and returned. If not given a UnsupportedOperationException is thrown. Checks the user's access to the
   * given object.
   *
   * @param user          Check the access for the given user instead of the logged-in user.
   * @param obj           The object.
   * @param obj           The old version of the object (is only given for operationType {@link OperationType#UPDATE}).
   * @param operationType The operation type (select, insert, update or delete)
   * @return true, if the user has the access right for the given operation type and object.
   */
  public boolean hasAccess(final PFUserDO user, final O obj, final O oldObj, final OperationType operationType,
      final boolean throwException)
  {
    if (userRightId != null) {
      return accessChecker.hasAccess(user, userRightId, obj, oldObj, operationType, throwException);
    }
    throw new UnsupportedOperationException(
        "readWriteUserRightId not given. Override this method or set readWriteUserRightId in constructor.");
  }

  /**
   * @param obj Check access to this object.
   * @return
   * @see #hasLoggedInUserAccess(Object, Object, OperationType, boolean)
   */
  public boolean hasLoggedInUserSelectAccess(final O obj, final boolean throwException)
  {
    return hasSelectAccess(ThreadLocalUserContext.getUser(), obj, throwException);
  }

  /**
   * @param user Check the access for the given user instead of the logged-in user. Checks select access right by
   *             calling hasAccess(obj, OperationType.SELECT).
   * @param obj  Check access to this object.
   * @return
   * @see #hasAccess(user, Object, Object, OperationType, boolean)
   */
  public boolean hasSelectAccess(final PFUserDO user, final O obj, final boolean throwException)
  {
    return hasAccess(user, obj, null, OperationType.SELECT, throwException);
  }

  /**
   * Has the user access to the history of the given object. At default this method calls hasHistoryAccess(boolean)
   * first and then hasSelectAccess.
   *
   * @param throwException
   */
  public boolean hasLoggedInUserHistoryAccess(final O obj, final boolean throwException)
  {
    return hasHistoryAccess(ThreadLocalUserContext.getUser(), obj, throwException);
  }

  /**
   * Has the user access to the history of the given object. At default this method calls hasHistoryAccess(boolean)
   * first and then hasSelectAccess.
   *
   * @param throwException
   */
  public boolean hasHistoryAccess(final PFUserDO user, final O obj, final boolean throwException)
  {
    if (hasHistoryAccess(user, throwException) == false) {
      return false;
    }
    if (userRightId != null) {
      return accessChecker.hasHistoryAccess(user, userRightId, obj, throwException);
    }
    return hasSelectAccess(user, obj, throwException);
  }

  /**
   * Has the user access to the history in general of the objects. At default this method calls hasSelectAccess.
   *
   * @param throwException
   */
  public boolean hasLoggedInUserHistoryAccess(final boolean throwException)
  {
    return hasHistoryAccess(ThreadLocalUserContext.getUser(), throwException);
  }

  /**
   * Has the user access to the history in general of the objects. At default this method calls hasSelectAccess.
   *
   * @param throwException
   */
  public boolean hasHistoryAccess(final PFUserDO user, final boolean throwException)
  {
    if (userRightId != null) {
      return accessChecker.hasHistoryAccess(user, userRightId, null, throwException);
    }
    return hasSelectAccess(user, throwException);
  }

  /**
   * Checks insert access right by calling hasAccess(obj, OperationType.INSERT).
   *
   * @param obj Check access to this object.
   * @return
   * @see #hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasLoggedInUserInsertAccess(final O obj, final boolean throwException)
  {
    return hasInsertAccess(ThreadLocalUserContext.getUser(), obj, throwException);
  }

  /**
   * Checks insert access right by calling hasAccess(obj, OperationType.INSERT).
   *
   * @param obj Check access to this object.
   * @return
   * @see #hasAccess(Object, OperationType)
   */
  public boolean hasInsertAccess(final PFUserDO user, final O obj, final boolean throwException)
  {
    return hasAccess(user, obj, null, OperationType.INSERT, throwException);
  }

  /**
   * Checks write access of the readWriteUserRight. If not given, true is returned at default. This method should only
   * be used for checking the insert access to show an insert button or not. Before inserting any object the write
   * access is checked by has*Access(...) independent of the result of this method.
   *
   * @see org.projectforge.framework.persistence.api.IDao#hasLoggedInUserInsertAccess()
   */
  @Override
  public boolean hasLoggedInUserInsertAccess()
  {
    return hasInsertAccess(ThreadLocalUserContext.getUser());
  }

  /**
   * Checks write access of the readWriteUserRight. If not given, true is returned at default. This method should only
   * be used for checking the insert access to show an insert button or not. Before inserting any object the write
   * access is checked by has*Access(...) independent of the result of this method.
   *
   * @see org.projectforge.framework.persistence.api.IDao#hasInsertAccess()
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user)
  {
    if (userRightId != null) {
      return accessChecker.hasInsertAccess(user, userRightId, false);
    }
    return true;
  }

  /**
   * Checks update access right by calling hasAccess(obj, OperationType.UPDATE).
   *
   * @param dbObj The original object (stored in the database)
   * @param obj   Check access to this object.
   * @return
   * @see #hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasLoggedInUserUpdateAccess(final O obj, final O dbObj, final boolean throwException)
  {
    return hasUpdateAccess(ThreadLocalUserContext.getUser(), obj, dbObj, throwException);
  }

  /**
   * Checks update access right by calling hasAccess(obj, OperationType.UPDATE).
   *
   * @param dbObj The original object (stored in the database)
   * @param obj   Check access to this object.
   * @return
   * @see #hasAccess(Object, OperationType)
   */
  public boolean hasUpdateAccess(final PFUserDO user, final O obj, final O dbObj, final boolean throwException)
  {
    return hasAccess(user, obj, dbObj, OperationType.UPDATE, throwException);
  }

  /**
   * Checks delete access right by calling hasAccess(obj, OperationType.DELETE).
   *
   * @param obj   Check access to this object.
   * @param dbObj current version of this object in the data base.
   * @return
   * @see #hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasLoggedInUserDeleteAccess(final O obj, final O dbObj, final boolean throwException)
  {
    return hasDeleteAccess(ThreadLocalUserContext.getUser(), obj, dbObj, throwException);
  }

  /**
   * Checks delete access right by calling hasAccess(obj, OperationType.DELETE).
   *
   * @param obj   Check access to this object.
   * @param dbObj current version of this object in the data base.
   * @return
   * @see #hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasDeleteAccess(final PFUserDO user, final O obj, final O dbObj, final boolean throwException)
  {
    return hasAccess(user, obj, dbObj, OperationType.DELETE, throwException);
  }

  public UserRight getUserRight()
  {
    if (userRightId != null) {
      return userRights.getRight(userRightId);
    } else {
      return null;
    }
  }

  /**
   * Overload this method for copying field manually. Used for modifiing fields inside methods: update, markAsDeleted
   * and undelete.
   *
   * @param src
   * @param dest
   * @return true, if any field was modified, otherwise false.
   * @see BaseDO#copyValuesFrom(BaseDO, String...)
   */
  protected ModificationStatus copyValues(final O src, final O dest, final String... ignoreFields)
  {
    return dest.copyValuesFrom(src, ignoreFields);
  }

  protected void createHistoryEntry(final Object entity, final Number id, final String property,
      final Class<?> valueClass,
      final Object oldValue, final Object newValue)
  {
    accessChecker.checkRestrictedOrDemoUser();
    final PFUserDO contextUser = ThreadLocalUserContext.getUser();
    final String userPk = contextUser != null ? contextUser.getId().toString() : null;
    if (userPk == null) {
      log.warn("No user found for creating history entry.");
    }
    HistoryBaseDaoAdapter.createHistoryEntry(entity, id, userPk, property, valueClass, oldValue, newValue);
  }

  /**
   * Only generic check access will be done. The matching entries will not be checked!
   *
   * @param property     Property of the data base entity.
   * @param searchString String the user has typed in.
   * @return All matching entries (like search) for the given property modified or updated in the last 2 years.
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<String> getAutocompletion(final String property, final String searchString)
  {
    checkLoggedInUserSelectAccess();
    if (StringUtils.isBlank(searchString) == true) {
      return null;
    }
    final String hql = "select distinct "
        + property
        + " from "
        + clazz.getSimpleName()
        + " t where deleted=false and lastUpdate > ? and lower(t."
        + property
        + ") like ?) order by t."
        + property;
    final Query query = getSession().createQuery(hql);
    final DateHolder dh = new DateHolder();
    dh.add(Calendar.YEAR, -2); // Search only for entries of the last 2 years.
    query.setDate(0, dh.getDate());
    query.setString(1, "%" + StringUtils.lowerCase(searchString) + "%");
    final List<String> list = query.list();
    return list;
  }

  /**
   * Re-indexes the entries of the last day, 1,000 at max.
   *
   * @see DatabaseDao#createReindexSettings(boolean)
   */
  @Override
  public void rebuildDatabaseIndex4NewestEntries()
  {
    final ReindexSettings settings = DatabaseDao.createReindexSettings(true);
    databaseDao.rebuildDatabaseSearchIndices(clazz, settings);
    databaseDao.rebuildDatabaseSearchIndices(PfHistoryMasterDO.class, settings);
  }

  /**
   * Re-indexes all entries (full re-index).
   */
  @Override
  public void rebuildDatabaseIndex()
  {
    final ReindexSettings settings = DatabaseDao.createReindexSettings(false);
    databaseDao.rebuildDatabaseSearchIndices(clazz, settings);
  }

  /**
   * Re-index all dependent objects manually (hibernate search). Hibernate doesn't re-index these objects, does it?
   *
   * @param obj
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void reindexDependentObjects(final O obj)
  {
    hibernateSearchDependentObjectsReindexer.reindexDependents(obj);
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  public void massUpdate(final List<O> list, final O master)
  {
    if (list == null || list.size() == 0) {
      // No entries to update.
      return;
    }
    if (list.size() > MAX_MASS_UPDATE) {
      throw new UserException(MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N, new Object[] { MAX_MASS_UPDATE });
    }
    final Object store = prepareMassUpdateStore(list, master);
    for (final O entry : list) {
      if (massUpdateEntry(entry, master, store) == true) {
        try {
          update(entry);
        } catch (final Exception ex) {
          log.info("Exception occured while updating entry inside mass update: " + entry);
        }
      }
    }
  }

  /**
   * Object pass thru every massUpdateEntry call.
   *
   * @param list
   * @param master
   * @return null if not overloaded.
   */
  protected Object prepareMassUpdateStore(final List<O> list, final O master)
  {
    return null;
  }

  /**
   * Overload this method for mass update support.
   *
   * @param entry
   * @param master
   * @param store  Object created with prepareMassUpdateStore if needed. Null at default.
   * @return true, if entry is ready for update otherwise false (no update will be done for this entry).
   */
  protected boolean massUpdateEntry(final O entry, final O master, final Object store)
  {
    throw new UnsupportedOperationException("Mass update is not supported by this dao for: " + clazz.getName());
  }

  private Set<Integer> getHistoryEntries(final Session session, final BaseSearchFilter filter)
  {
    if (hasLoggedInUserSelectAccess(false) == false || hasLoggedInUserHistoryAccess(false) == false) {
      // User has in general no access to history entries of the given object type (clazz).
      return null;
    }
    final Set<Integer> idSet = new HashSet<Integer>();
    HibernateSearchFilterUtils.getHistoryEntriesDirect(session, filter, idSet, clazz);
    if (getAdditionalHistorySearchDOs() != null) {
      for (final Class<?> aclazz : getAdditionalHistorySearchDOs()) {
        HibernateSearchFilterUtils.getHistoryEntriesDirect(session, filter, idSet, aclazz);
      }
    }
    return idSet;
  }

  private Set<Integer> searchHistoryEntries(final Session session, final BaseSearchFilter filter)
  {
    if (hasLoggedInUserSelectAccess(false) == false || hasLoggedInUserHistoryAccess(false) == false) {
      // User has in general no access to history entries of the given object type (clazz).
      return null;
    }
    final Set<Integer> idSet = new HashSet<Integer>();
    HibernateSearchFilterUtils.getHistoryEntriesFromSearch(session, filter, idSet, clazz);

    return idSet;
  }

  // TODO RK entweder so oder ueber annots. 
  // siehe org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils.getNestedHistoryEntities(Class<?>)
  protected Class<?>[] getAdditionalHistorySearchDOs()
  {
    return null;
  }

  /**
   * @return The type of the data object (BaseDO) this dao is responsible for.
   */
  public Class<?> getDataObjectType()
  {
    return clazz;
  }

  public TenantRegistry getTenantRegistry()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  /**
   * @return the UserGroupCache with groups and rights (tenant specific).
   */
  public UserGroupCache getUserGroupCache()
  {
    return getTenantRegistry().getUserGroupCache();
  }

  /**
   * @return Wether the data object (BaseDO) this dao is responsible for is from type Historizable or not.
   */
  @Override
  public boolean isHistorizable()
  {
    return HistoryBaseDaoAdapter.isHistorizable(clazz);
  }

  /**
   * If true then a eh cache region is used for this dao for every criteria search of this class. <br/>
   * Please note: If you write your own criteria searches in extended classes, don't forget to call
   * {@link #setCacheRegion(Criteria)}. <br/>
   * Don't forget to add your base dao class name in ehcache.xml.
   *
   * @return false at default.
   */
  protected boolean useOwnCriteriaCacheRegion()
  {
    return false;
  }

  private void setCacheRegion(final Criteria criteria)
  {
    criteria.setCacheable(true);
    if (useOwnCriteriaCacheRegion() == false) {
      return;
    }
    criteria.setCacheRegion(this.getClass().getName());
  }

  public Session getSession()
  {
    return sessionFactory.getCurrentSession();
  }

  public HibernateTemplate getHibernateTemplate()
  {
    return hibernateTemplate;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<O> getEntityClass()
  {
    Class<O> ret = (Class<O>) ClassUtils.getGenericTypeArgument(getClass(), 0);
    return ret;
  }

  @Override
  public O selectByPkDetached(Integer pk) throws AccessException
  {
    return getById(pk);
  }
}
