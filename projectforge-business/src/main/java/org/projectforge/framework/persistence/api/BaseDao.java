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

import de.micromata.genome.db.jpa.history.api.DiffEntry;
import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.genome.util.runtime.ClassUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.search.jpa.Search;
import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRight;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.impl.DBQuery;
import org.projectforge.framework.persistence.api.impl.HibernateSearchMeta;
import org.projectforge.framework.persistence.database.DatabaseDao;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.history.HibernateSearchDependentObjectsReindexer;
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.projectforge.framework.time.PFDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public abstract class BaseDao<O extends ExtendedBaseDO<Integer>>
        implements IDao<O>, IPersistenceService<O> {

  public static final String EXCEPTION_HISTORIZABLE_NOTDELETABLE = "Could not delete of Historizable objects (contact your software developer): ";

  /**
   * Maximum allowed mass updates within one massUpdate call.
   */
  public static final int MAX_MASS_UPDATE = 100;
  public static final String MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N = "massUpdate.error.maximumNumberOfAllowedMassUpdatesExceeded";
  private static final List<DisplayHistoryEntry> EMPTY_HISTORY_ENTRIES = new ArrayList<>();
  private static final Logger log = LoggerFactory.getLogger(BaseDao.class);
  /**
   * DEBUG flag. remove later
   */
  static final boolean NO_UPDATE_MAGIC = true;
  /**
   * DEBUG flag. Not sure, if always has be flushed.
   */
  private static final boolean LUCENE_FLUSH_ALWAYS = true;

  protected Class<O> clazz;

  protected boolean logDatabaseActions = true;

  @Autowired
  protected AccessChecker accessChecker;

  @Autowired
  protected DBQuery dbQuery;

  @Autowired
  protected DatabaseDao databaseDao;

  @PersistenceContext
  protected EntityManager em;

  @Autowired
  protected TenantChecker tenantChecker;

  @Autowired
  protected TenantService tenantService;

  private String[] searchFields;

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
  protected PfEmgrFactory emgrFactory;

  @Autowired
  private UserRightService userRights;

  @Autowired
  private HibernateSearchDependentObjectsReindexer hibernateSearchDependentObjectsReindexer;

  /**
   * The setting of the DO class is required.
   */
  protected BaseDao(final Class<O> clazz) {
    this.clazz = clazz;
  }

  /**
   * Get all declared hibernate search fields. These fields are defined over annotations in the database object class.
   * The names are the property names or, if defined the name declared in the annotation of a field. <br/>
   * The user can search in these fields explicit by typing e. g. authors:beck (<field>:<searchString>)
   */
  public synchronized String[] getSearchFields() {
    return HibernateSearchMeta.INSTANCE.getClassInfo(this).getAllFieldNames();
  }

  /**
   * Overwrite this method for adding search fields manually (e. g. for embedded objects). For example see TimesheetDao.
   */
  public String[] getAdditionalSearchFields() {
    return null;
  }

  public Class<O> getDOClass() {
    return this.clazz;
  }

  @Override
  public abstract O newInstance();

  /**
   * getOrLoad checks first weather the id is valid or not. Default implementation: id != 0 && id &gt; 0. Overload this,
   * if the id of the DO can be 0 for example.
   */
  protected boolean isIdValid(final Integer id) {
    return (id != null && id > 0);
  }

  /**
   * If the user has select access then the object will be returned. If not, the hibernate proxy object will be get via
   * getSession().load();
   */
  public O getOrLoad(final Integer id) {
    if (!isIdValid(id)) {
      return null;
    }
    final O obj = internalGetById(id);
    if (obj == null) {
      log.error("Can't load object of type " + getDOClass().getName() + ". Object with given id #" + id + " not found.");
      return null;
    }
    if (tenantChecker.isPartOfCurrentTenant(obj)
            && hasLoggedInUserSelectAccess(obj, false)) {
      return obj;
    }
    return em.getReference(clazz, id);
  }

  public List<O> internalLoadAllNotDeleted() {
    return internalLoadAll().stream().filter(o -> !o.isDeleted()).collect(Collectors.toList());
  }

  public List<O> internalLoadAll() {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<O> cq = cb.createQuery(clazz);
    CriteriaQuery<O> query = cq.select(cq.from(clazz));
    return em.createQuery(query).getResultList();
  }

  public List<O> internalLoadAll(final TenantDO tenant) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<O> cq = em.getCriteriaBuilder().createQuery(clazz);
    Root<O> root = cq.from(clazz);
    CriteriaQuery<O> query;
    if (tenant == null) {
      query = cq.where(cb.isNull(root.get("tenant").get("id")));
    } else if (tenant.isDefault()) {
      query = cq.where(cb.or(
              cb.equal(root.get("tenant").get("id"), tenant.getId()),
              cb.isNull(root.get("tenant").get("id"))));
      // FROM clazz WHERE tenant.id=:tid or tenant.id is null
    } else {
      query = cq.where(cb.equal(root.get("tenant").get("id"), tenant.getId()));
      // FROM clazz WHERE tenant.id=:tid
    }
    return em.createQuery(query).getResultList();
  }

  public List<O> internalLoad(final Collection<? extends Serializable> idList) {
    if (idList == null) {
      return null;
    }
    CriteriaQuery<O> cr = em.getCriteriaBuilder().createQuery(clazz);
    Root<O> root = cr.from(clazz);
    cr.select(root).where(root.get(idProperty).in(idList)).distinct(true);
    List<O> results = em.createQuery(cr).getResultList();
    return results;
  }

  protected String idProperty = "id";

  public List<O> getListByIds(final Collection<? extends Serializable> idList) {
    if (idList == null) {
      return null;
    }
    final List<O> list = internalLoad(idList);
    return extractEntriesWithSelectAccess(list);
  }

  /**
   * This method is used by the searchDao and calls {@link #getList(BaseSearchFilter)} by default.
   *
   * @return A list of found entries or empty list. PLEASE NOTE: Returns null only if any error occured.
   * @see #getList(BaseSearchFilter)
   */
  public List<O> getListForSearchDao(final BaseSearchFilter filter) {
    return getList(filter);
  }

  /**
   * Builds query filter by simply calling constructor of QueryFilter with given search filter and calls
   * getList(QueryFilter). Override this method for building more complex query filters.
   *
   * @return A list of found entries or empty list. PLEASE NOTE: Returns null only if any error occured.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Override
  public List<O> getList(final BaseSearchFilter filter) {
    final QueryFilter queryFilter = createQueryFilter(filter);
    return getList(queryFilter);
  }

  protected QueryFilter createQueryFilter(final BaseSearchFilter filter) {
    return new QueryFilter(filter, false);
  }

  /**
   * Gets the list filtered by the given filter.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public List<O> getList(final QueryFilter filter) throws AccessException {
    return dbQuery.getList(this, filter, true, filter.getIgnoreTenant());
  }

  /**
   * Gets the list filtered by the given filter.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public List<O> internalGetList(final QueryFilter filter) throws AccessException {
    return dbQuery.getList(this, filter, false, filter.getIgnoreTenant());
  }

  /**
   * idSet.contains(entry.getId()) at default.
   */
  public boolean contains(final Set<Integer> idSet, final O entry) {
    if (idSet == null) {
      return false;
    }
    return idSet.contains(entry.getId());
  }

  /**
   * idSet.contains(entry.getId()) at default.
   */
  public boolean containsLong(final Set<Long> idSet, final O entry) {
    if (idSet == null) {
      return false;
    }
    return idSet.contains(entry.getId().longValue());
  }

  protected List<O> selectUnique(final List<O> list) {
    @SuppressWarnings("unchecked") final List<O> result = (List<O>) CollectionUtils.select(list, PredicateUtils.uniquePredicate());
    return result;
  }

  List<O> extractEntriesWithSelectAccess(final List<O> origList) {
    final List<O> result = new ArrayList<>();
    final boolean superAdmin = TenantChecker.isSuperAdmin(ThreadLocalUserContext.getUser());
    final PFUserDO loggedInUser = ThreadLocalUserContext.getUser();
    for (final O obj : origList) {
      if (hasSelectAccess(obj, loggedInUser, superAdmin)) {
        result.add(obj);
        afterLoad(obj);
      }
    }
    return result;
  }

  /**
   * @param obj          The object to check.
   * @param loggedInUser The currend logged in user.
   * @param superAdmin   Super admin has access to entries of all tenants
   * @return true if loggedInUser has select access.
   * @see TenantChecker#isPartOfCurrentTenant(BaseDO)
   * @see #hasUserSelectAccess(PFUserDO, ExtendedBaseDO, boolean)
   */
  public boolean hasSelectAccess(O obj, PFUserDO loggedInUser, boolean superAdmin) {
    return (superAdmin || tenantChecker.isPartOfCurrentTenant(obj))
            && hasUserSelectAccess(loggedInUser, obj, false);
  }

  /**
   * Overwrite this method for own list sorting. This method returns only the given list.
   */
  public List<O> sort(final List<O> list) {
    return list;
  }

  /**
   * @param id primary key of the base object.
   */
  @Override
  public O getById(final Serializable id) throws AccessException {
    if (accessChecker.isRestrictedUser()) {
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

  public O internalGetById(final Serializable id) {
    if (id == null) {
      return null;
    }
    O obj = SQLHelper.ensureUniqueResult(em.createQuery(
            "select t from " + clazz.getName() + " t where t.id = :id", clazz)
            .setParameter("id", id));
    if (obj == null) {
      return null;
    }
    afterLoad(obj);
    return obj;
  }

  /**
   * Gets the history entries of the object.
   */
  @SuppressWarnings("rawtypes")
  public HistoryEntry[] getHistoryEntries(final O obj) {
    accessChecker.checkRestrictedUser();
    checkPartOfCurrentTenant(obj, OperationType.SELECT);
    checkLoggedInUserHistoryAccess(obj);
    return internalGetHistoryEntries(obj);
  }

  @SuppressWarnings("rawtypes")
  public HistoryEntry[] internalGetHistoryEntries(final BaseDO<?> obj) {
    accessChecker.checkRestrictedUser();
    return HistoryBaseDaoAdapter.getHistoryFor(obj);
  }

  /**
   * Gets the history entries of the object in flat format.<br/>
   * Please note: If user has no access an empty list will be returned.
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final O obj) {
    if (obj.getId() == null || !hasLoggedInUserHistoryAccess(obj, false)) {
      return EMPTY_HISTORY_ENTRIES;
    }
    return internalGetDisplayHistoryEntries(obj);
  }

  protected List<DisplayHistoryEntry> internalGetDisplayHistoryEntries(final BaseDO<?> obj) {
    accessChecker.checkRestrictedUser();
    final HistoryEntry[] entries = internalGetHistoryEntries(obj);
    if (entries == null) {
      return null;
    }
    return convertAll(entries, em);
  }

  @SuppressWarnings("rawtypes")
  private List<DisplayHistoryEntry> convertAll(final HistoryEntry[] entries, final EntityManager em) {
    final List<DisplayHistoryEntry> list = new ArrayList<>();
    for (final HistoryEntry entry : entries) {
      final List<DisplayHistoryEntry> l = convert(entry, em);
      list.addAll(l);
    }
    return list;
  }

  public List<DisplayHistoryEntry> convert(final HistoryEntry<?> entry, final EntityManager em) {
    if (entry.getDiffEntries().isEmpty()) {
      final DisplayHistoryEntry se = new DisplayHistoryEntry(getUserGroupCache(), entry);
      return Collections.singletonList(se);
    }
    List<DisplayHistoryEntry> result = new ArrayList<>();
    for (DiffEntry prop : entry.getDiffEntries()) {
      DisplayHistoryEntry se = new DisplayHistoryEntry(getUserGroupCache(), entry, prop, em);
      result.add(se);
    }

    return result;
  }

  /**
   * @return the generated identifier, if save method is used, otherwise null.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public Serializable saveOrUpdate(final O obj) throws AccessException {
    Serializable id = null;
    if (obj.getId() != null && obj.getCreated() != null) { // obj.created is needed for KundeDO (id isn't null for inserting new customers).
      update(obj);
    } else {
      id = save(obj);
    }
    return id;
  }

  /**
   * @return the generated identifier, if save method is used, otherwise null.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public Serializable internalSaveOrUpdate(final O obj) {
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
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void save(final List<O> objects) throws AccessException {
    Validate.notNull(objects);
    for (final O obj : objects) {
      save(obj);
    }
  }

  /**
   * @return the generated identifier.
   */
  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public Integer save(final O obj) throws AccessException {
    //long begin = System.currentTimeMillis();
    Validate.notNull(obj);
    if (!avoidNullIdCheckBeforeSave) {
      Validate.isTrue(obj.getId() == null);
    }
    beforeSaveOrModify(obj);
    checkPartOfCurrentTenant(obj, OperationType.INSERT);
    checkLoggedInUserInsertAccess(obj);
    accessChecker.checkRestrictedOrDemoUser();
    Integer result = internalSave(obj);
    //long end = System.currentTimeMillis();
    //log.info("BaseDao.save took: " + (end - begin) + " ms.");
    return result;
  }

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public Integer insert(O obj) throws AccessException {
    return save(obj);
  }

  /**
   * This method will be called after loading an object from the data base. Does nothing at default. This method is not
   * called by internalLoadAll.
   */
  public void afterLoad(final O obj) {

  }

  /**
   * This method will be called after inserting, updating, deleting or marking the data object as deleted. This method
   * is for example needed for expiring the UserGroupCache after inserting or updating a user or group data object. Does
   * nothing at default.
   */
  protected void afterSaveOrModify(final O obj) {
  }

  /**
   * This method will be called after inserting. Does nothing at default.
   *
   * @param obj The inserted object
   */
  protected void afterSave(final O obj) {
  }

  /**
   * This method will be called before inserting. Does nothing at default.
   */
  protected void onSave(final O obj) {
  }

  /**
   * This method will be called before inserting, updating, deleting or marking the data object as deleted. Does nothing
   * at default.
   */
  protected void onSaveOrModify(final O obj) {
  }

  /**
   * This method will be called before access check of inserting and updating the object. Does nothing
   * at default.
   */
  protected void beforeSaveOrModify(final O obj) {
  }

  /**
   * This method will be called after updating. Does nothing at default. PLEASE NOTE: If you overload this method don't
   * forget to set {@link #supportAfterUpdate} to true, otherwise you won't get the origin data base object!
   *
   * @param obj   The modified object
   * @param dbObj The object from data base before modification.
   */
  protected void afterUpdate(final O obj, final O dbObj) {
  }

  /**
   * This method will be called after updating. Does nothing at default. PLEASE NOTE: If you overload this method don't
   * forget to set {@link #supportAfterUpdate} to true, otherwise you won't get the origin data base object!
   *
   * @param obj        The modified object
   * @param dbObj      The object from data base before modification.
   * @param isModified is true if the object was changed, false if the object wasn't modified.
   */
  protected void afterUpdate(final O obj, final O dbObj, final boolean isModified) {
  }

  /**
   * This method will be called before updating the data object. Will also called if in internalUpdate no modification
   * was detected. Please note: Do not modify the object oldVersion! Does nothing at default.
   *
   * @param obj   The changed object.
   * @param dbObj The current data base version of this object.
   */
  protected void onChange(final O obj, final O dbObj) {
  }

  /**
   * This method will be called before deleting. Does nothing at default.
   *
   * @param obj The deleted object.
   */
  protected void onDelete(final O obj) {
  }

  /**
   * This method will be called after deleting as well as after object is marked as deleted. Does nothing at default.
   *
   * @param obj The deleted object.
   */
  protected void afterDelete(final O obj) {
  }

  /**
   * This method will be called after undeleting. Does nothing at default.
   *
   * @param obj The deleted object.
   */
  protected void afterUndelete(final O obj) {
  }

  /**
   * This method is for internal use e. g. for updating objects without check access.
   *
   * @return the generated identifier.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public Integer internalSave(final O obj) {
    return BaseDaoSupport.internalSave(this, obj);
  }

  TenantDO getDefaultTenant() {
    return em.find(TenantDO.class, 1);
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void saveOrUpdate(final Collection<O> col) {
    for (final O obj : col) {
      saveOrUpdate(obj);
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void saveOrUpdate(final BaseDao<O> currentProxy, final Collection<O> col, final int blockSize) {
    final List<O> list = new ArrayList<>();
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

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void internalSaveOrUpdate(final Collection<O> col) {
    for (final O obj : col) {
      internalSaveOrUpdate(obj);
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void internalSaveOrUpdate(final BaseDao<O> currentProxy, final Collection<O> col, final int blockSize) {
    final List<O> list = new ArrayList<>();
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
   * @return true, if modifications were done, false if no modification detected.
   * @see #internalUpdate(ExtendedBaseDO, boolean)
   */
  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public ModificationStatus update(final O obj) throws AccessException {
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
   * @return true, if modifications were done, false if no modification detected.
   * @see #internalUpdate(ExtendedBaseDO, boolean)
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public ModificationStatus internalUpdate(final O obj) {
    return internalUpdate(obj, false);
  }

  /**
   * This method is for internal use e. g. for updating objects without check access.<br/>
   * Please note: update ignores the field deleted. Use markAsDeleted, delete and undelete methods instead.
   *
   * @param checkAccess If false, any access check will be ignored.
   * @return true, if modifications were done, false if no modification detected.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public ModificationStatus internalUpdate(final O obj, final boolean checkAccess) {
    return BaseDaoSupport.internalUpdate(this, obj, checkAccess);
  }

  /**
   * @return If true (default if not minor Change) all dependent data-base objects will be re-indexed. For e. g.
   * PFUserDO all time-sheets etc. of this user will be re-indexed. It's called after internalUpdate. Refer
   * UserDao to see more.
   * @see BaseDO#isMinorChange()
   */
  protected boolean wantsReindexAllDependentObjects(final O obj, final O dbObj) {
    return !obj.isMinorChange();
  }

  /**
   * Used by internal update if supportAfterUpdate is true for storing db object version for afterUpdate. Override this
   * method to implement your own copy method.
   */
  protected O getBackupObject(final O dbObj) {
    final O backupObj = newInstance();
    copyValues(dbObj, backupObj);
    return backupObj;
  }

  /**
   * Overwrite this method if you have lazy exceptions while Hibernate-Search re-indexes. See e. g. AuftragDao.
   */
  protected void prepareHibernateSearch(final O obj, final OperationType operationType) {
  }

  /**
   * Object will be marked as deleted (boolean flag), therefore undelete is always possible without any loss of data.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Override
  public void markAsDeleted(final O obj) throws AccessException {
    Validate.notNull(obj);
    if (obj.getId() == null) {
      final String msg = "Could not delete object unless id is not given:" + obj.toString();
      log.error(msg);
      throw new RuntimeException(msg);
    }
    final O dbObj = em.find(clazz, obj.getId());
    checkPartOfCurrentTenant(obj, OperationType.DELETE);
    checkLoggedInUserDeleteAccess(obj, dbObj);
    accessChecker.checkRestrictedOrDemoUser();
    internalMarkAsDeleted(obj);
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void internalMarkAsDeleted(final O obj) {
    BaseDaoSupport.internalMarkAsDeleted(this, obj);
  }

  void flushSearchSession(EntityManager em) {
    long begin = System.currentTimeMillis();
    if (LUCENE_FLUSH_ALWAYS) {
      Search.getFullTextEntityManager(em).flushToIndexes();
    }
    long end = System.currentTimeMillis();
    if (end - begin > 1000) {
      log.info("BaseDao.flushSearchSession took: " + (end - begin) + " ms (> 1s).");
    }
  }

  /**
   * Object will be deleted finally out of the data base.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Override
  public void delete(final O obj) throws AccessException {
    Validate.notNull(obj);
    if (HistoryBaseDaoAdapter.isHistorizable(obj)) {
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
    checkPartOfCurrentTenant(obj, OperationType.DELETE);
    emgrFactory.runInTrans(emgr -> {
      EntityManager em = emgr.getEntityManager();
      final O dbObj = em.find(clazz, obj.getId());
      checkLoggedInUserDeleteAccess(obj, dbObj);
      em.remove(dbObj);
      return null;
    });
    if (logDatabaseActions) {
      log.info(clazz.getSimpleName() + " deleted: " + obj.toString());
    }
    afterSaveOrModify(obj);
    afterDelete(obj);
  }

  /**
   * Object will be marked as deleted (booelan flag), therefore undelete is always possible without any loss of data.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Override
  public void undelete(final O obj) throws AccessException {
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

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void internalUndelete(final O obj) {
    BaseDaoSupport.internalUndelete(this, obj);
  }

  void checkPartOfCurrentTenant(final O obj, final OperationType operationType) {
    tenantChecker.checkPartOfCurrentTenant(obj);
  }

  /**
   * Checks the basic select access right. Overload this method if your class supports this right.
   */
  public void checkLoggedInUserSelectAccess() throws AccessException {
    if (!hasUserSelectAccess(ThreadLocalUserContext.getUser(), true)) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  protected void checkLoggedInUserSelectAccess(final O obj) throws AccessException {
    if (!hasUserSelectAccess(ThreadLocalUserContext.getUser(), obj, true)) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  private void checkLoggedInUserHistoryAccess(final O obj) throws AccessException {
    if (!hasHistoryAccess(ThreadLocalUserContext.getUser(), true)
            || !hasLoggedInUserHistoryAccess(obj, true)) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  private void checkLoggedInUserInsertAccess(final O obj) throws AccessException {
    checkInsertAccess(ThreadLocalUserContext.getUser(), obj);
  }

  protected void checkInsertAccess(final PFUserDO user, final O obj) throws AccessException {
    if (!hasInsertAccess(user, obj, true)) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  /**
   * @param dbObj The original object (stored in the database)
   */
  void checkLoggedInUserUpdateAccess(final O obj, final O dbObj) throws AccessException {
    checkUpdateAccess(ThreadLocalUserContext.getUser(), obj, dbObj);
  }

  /**
   * @param dbObj The original object (stored in the database)
   */
  protected void checkUpdateAccess(final PFUserDO user, final O obj, final O dbObj) throws AccessException {
    if (!hasUpdateAccess(user, obj, dbObj, true)) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  private void checkLoggedInUserDeleteAccess(final O obj, final O dbObj) throws AccessException {
    if (!hasLoggedInUserDeleteAccess(obj, dbObj, true)) {
      // Should not occur!
      log.error("Development error: Subclass should throw an exception instead of returning false.");
      throw new UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM);
    }
  }

  /**
   * Checks the basic select access right. Overwrite this method if the basic select access should be checked.
   *
   * @return true at default or if readWriteUserRightId is given hasReadAccess(boolean).
   * @see #hasUserSelectAccess(PFUserDO, ExtendedBaseDO, boolean)
   */
  public boolean hasLoggedInUserSelectAccess(final boolean throwException) {
    return hasUserSelectAccess(ThreadLocalUserContext.getUser(), throwException);
  }

  /**
   * Checks the basic select access right. Overwrite this method if the basic select access should be checked.
   *
   * @return true at default or if readWriteUserRightId is given hasReadAccess(boolean).
   * @see #hasAccess(PFUserDO, ExtendedBaseDO, ExtendedBaseDO, OperationType, boolean)
   */
  public boolean hasUserSelectAccess(final PFUserDO user, final boolean throwException) {
    return hasAccess(user, null, null, OperationType.SELECT, throwException);
  }

  /**
   * If userRightId is given then {@link AccessChecker#hasAccess(PFUserDO, IUserRightId, Object, Object, OperationType, boolean)}
   * is called and returned. If not given a UnsupportedOperationException is thrown. Checks the user's access to the
   * given object.
   *
   * @param obj           The object.
   * @param oldObj        The old version of the object (is only given for operationType {@link OperationType#UPDATE}).
   * @param operationType The operation type (select, insert, update or delete)
   * @return true, if the user has the access right for the given operation type and object.
   */
  public boolean hasLoggedInUserAccess(final O obj, final O oldObj, final OperationType operationType,
                                       final boolean throwException) {
    return hasAccess(ThreadLocalUserContext.getUser(), obj, oldObj, operationType, throwException);
  }

  /**
   * If userRightId is given then {@link AccessChecker#hasAccess(PFUserDO, IUserRightId, Object, Object, OperationType, boolean)}
   * is called and returned. If not given a UnsupportedOperationException is thrown. Checks the user's access to the
   * given object.
   *
   * @param user          Check the access for the given user instead of the logged-in user.
   * @param obj           The object.
   * @param oldObj        The old version of the object (is only given for operationType {@link OperationType#UPDATE}).
   * @param operationType The operation type (select, insert, update or delete)
   * @return true, if the user has the access right for the given operation type and object.
   */
  public boolean hasAccess(final PFUserDO user, final O obj, final O oldObj, final OperationType operationType,
                           final boolean throwException) {
    if (userRightId != null) {
      return accessChecker.hasAccess(user, userRightId, obj, oldObj, operationType, throwException);
    }
    throw new UnsupportedOperationException(
            "readWriteUserRightId not given. Override this method or set readWriteUserRightId in constructor.");
  }

  /**
   * @param obj Check access to this object.
   * @see #hasUserSelectAccess(PFUserDO, ExtendedBaseDO, boolean)
   */
  public boolean hasLoggedInUserSelectAccess(final O obj, final boolean throwException) {
    return hasUserSelectAccess(ThreadLocalUserContext.getUser(), obj, throwException);
  }

  /**
   * @param user Check the access for the given user instead of the logged-in user. Checks select access right by
   *             calling hasAccess(obj, OperationType.SELECT).
   * @param obj  Check access to this object.
   * @see #hasAccess(PFUserDO, ExtendedBaseDO, ExtendedBaseDO, OperationType, boolean)
   */
  public boolean hasUserSelectAccess(final PFUserDO user, final O obj, final boolean throwException) {
    return hasAccess(user, obj, null, OperationType.SELECT, throwException);
  }

  /**
   * Has the user access to the history of the given object. At default this method calls hasHistoryAccess(boolean)
   * first and then hasSelectAccess.
   */
  public boolean hasLoggedInUserHistoryAccess(final O obj, final boolean throwException) {
    return hasHistoryAccess(ThreadLocalUserContext.getUser(), obj, throwException);
  }

  /**
   * Has the user access to the history of the given object. At default this method calls hasHistoryAccess(boolean)
   * first and then hasSelectAccess.
   */
  public boolean hasHistoryAccess(final PFUserDO user, final O obj, final boolean throwException) {
    if (!hasHistoryAccess(user, throwException)) {
      return false;
    }
    if (userRightId != null) {
      return accessChecker.hasHistoryAccess(user, userRightId, obj, throwException);
    }
    return hasUserSelectAccess(user, obj, throwException);
  }

  /**
   * Has the user access to the history in general of the objects. At default this method calls hasSelectAccess.
   */
  public boolean hasLoggedInUserHistoryAccess(final boolean throwException) {
    return hasHistoryAccess(ThreadLocalUserContext.getUser(), throwException);
  }

  /**
   * Has the user access to the history in general of the objects. At default this method calls hasSelectAccess.
   */
  public boolean hasHistoryAccess(final PFUserDO user, final boolean throwException) {
    if (userRightId != null) {
      return accessChecker.hasHistoryAccess(user, userRightId, null, throwException);
    }
    return hasUserSelectAccess(user, throwException);
  }

  /**
   * Checks insert access right by calling hasAccess(obj, OperationType.INSERT).
   *
   * @param obj Check access to this object.
   * @see #hasInsertAccess(PFUserDO, ExtendedBaseDO, boolean)
   */
  @Override
  public boolean hasLoggedInUserInsertAccess(final O obj, final boolean throwException) {
    return hasInsertAccess(ThreadLocalUserContext.getUser(), obj, throwException);
  }

  /**
   * Checks insert access right by calling hasAccess(obj, OperationType.INSERT).
   *
   * @param obj Check access to this object.
   * @see #hasAccess(PFUserDO, ExtendedBaseDO, ExtendedBaseDO, OperationType, boolean)
   */
  public boolean hasInsertAccess(final PFUserDO user, final O obj, final boolean throwException) {
    return hasAccess(user, obj, null, OperationType.INSERT, throwException);
  }

  /**
   * Checks write access of the readWriteUserRight. If not given, true is returned at default. This method should only
   * be used for checking the insert access to show an insert button or not. Before inserting any object the write
   * access is checked by has*Access(...) independent of the result of this method.
   *
   * @see org.projectforge.framework.persistence.api.IDao#hasInsertAccess(PFUserDO)
   */
  @Override
  public boolean hasLoggedInUserInsertAccess() {
    return hasInsertAccess(ThreadLocalUserContext.getUser());
  }

  /**
   * Checks write access of the readWriteUserRight. If not given, true is returned at default. This method should only
   * be used for checking the insert access to show an insert button or not. Before inserting any object the write
   * access is checked by has*Access(...) independent of the result of this method.
   *
   * @see AccessChecker#hasInsertAccess(PFUserDO, IUserRightId, boolean)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user) {
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
   * @see #hasUpdateAccess(PFUserDO, ExtendedBaseDO, ExtendedBaseDO, boolean)
   */
  @Override
  public boolean hasLoggedInUserUpdateAccess(final O obj, final O dbObj, final boolean throwException) {
    return hasUpdateAccess(ThreadLocalUserContext.getUser(), obj, dbObj, throwException);
  }

  /**
   * Checks update access right by calling hasAccess(obj, OperationType.UPDATE).
   *
   * @param dbObj The original object (stored in the database)
   * @param obj   Check access to this object.
   * @see #hasAccess(PFUserDO, ExtendedBaseDO, ExtendedBaseDO, OperationType, boolean)
   */
  public boolean hasUpdateAccess(final PFUserDO user, final O obj, final O dbObj, final boolean throwException) {
    return hasAccess(user, obj, dbObj, OperationType.UPDATE, throwException);
  }

  /**
   * Checks delete access right by calling hasAccess(obj, OperationType.DELETE).
   *
   * @param obj   Check access to this object.
   * @param dbObj current version of this object in the data base.
   * @see #hasDeleteAccess(PFUserDO, ExtendedBaseDO, ExtendedBaseDO, boolean)
   */
  @Override
  public boolean hasLoggedInUserDeleteAccess(final O obj, final O dbObj, final boolean throwException) {
    return hasDeleteAccess(ThreadLocalUserContext.getUser(), obj, dbObj, throwException);
  }

  /**
   * Checks delete access right by calling hasAccess(obj, OperationType.DELETE).
   *
   * @param obj   Check access to this object.
   * @param dbObj current version of this object in the data base.
   * @see #hasAccess(PFUserDO, ExtendedBaseDO, ExtendedBaseDO, OperationType, boolean)
   */
  @Override
  public boolean hasDeleteAccess(final PFUserDO user, final O obj, final O dbObj, final boolean throwException) {
    return hasAccess(user, obj, dbObj, OperationType.DELETE, throwException);
  }

  public UserRight getUserRight() {
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
   * @return true, if any field was modified, otherwise false.
   * @see BaseDO#copyValuesFrom(BaseDO, String...)
   */
  protected ModificationStatus copyValues(final O src, final O dest, final String... ignoreFields) {
    return dest.copyValuesFrom(src, ignoreFields);
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  protected void createHistoryEntry(final Object entity, final Number id, final String property,
                                    final Class<?> valueClass,
                                    final Object oldValue, final Object newValue) {
    accessChecker.checkRestrictedOrDemoUser();
    final PFUserDO contextUser = ThreadLocalUserContext.getUser();
    final String userPk = contextUser != null ? contextUser.getId().toString() : null;
    if (userPk == null) {
      log.warn("No user found for creating history entry.");
    }
    HistoryBaseDaoAdapter.createHistoryEntry(entity, id, userPk, property, valueClass, oldValue, newValue);
  }

  /**
   * SECURITY ADVICE:
   * For security reasons every property must be enabled for autocompletion. Otherwise the user may select
   * to much information, because only generic select access of an entity is checked. Example: The user has
   * select access to users, therefore he may select all password fields!!!
   * <br/>
   * Refer implementation of ContractDao as example.
   */
  public boolean isAutocompletionPropertyEnabled(String property) {
    return false;
  }

  /**
   * SECURITY ADVICE:
   * Only generic check access will be done. The matching entries will not be checked!
   *
   * @param property     Property of the data base entity.
   * @param searchString String the user has typed in.
   * @return All matching entries (like search) for the given property modified or updated in the last 2 years.
   */
  @Override
  public List<String> getAutocompletion(final String property, final String searchString) {
    checkLoggedInUserSelectAccess();
    if (!isAutocompletionPropertyEnabled(property)) {
      log.warn("Security alert: The user tried to select property '" + property + "' of entity '" + this.clazz.getName() + "'.");
      return new ArrayList<>();
    }
    if (StringUtils.isBlank(searchString)) {
      return new ArrayList<>();
    }
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<String> cr = cb.createQuery(String.class);
    Root<O> root = cr.from(clazz);
    Date yearsAgo = PFDateTime.now().minusYears(2).getUtilDate();
    cr.select(root.get(property)).where(
            cb.equal(root.get("deleted"), false),
            cb.greaterThan(root.get("lastUpdate"), yearsAgo),
            cb.like(cb.lower(root.get(property)), "%" + StringUtils.lowerCase(searchString) + "%"))
            .orderBy(cb.asc(root.get(property)))
            .distinct(true);
    return em.createQuery(cr).getResultList();
  }

  /**
   * Re-indexes the entries of the last day, 1,000 at max.
   *
   * @see DatabaseDao#createReindexSettings(boolean)
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Override
  public void rebuildDatabaseIndex4NewestEntries() {
    final ReindexSettings settings = DatabaseDao.createReindexSettings(true);
    databaseDao.rebuildDatabaseSearchIndices(clazz, settings);
    databaseDao.rebuildDatabaseSearchIndices(PfHistoryMasterDO.class, settings);
  }

  /**
   * Re-indexes all entries (full re-index).
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Override
  public void rebuildDatabaseIndex() {
    final ReindexSettings settings = DatabaseDao.createReindexSettings(false);
    emgrFactory.runInTrans(emgr -> {
      databaseDao.rebuildDatabaseSearchIndices(clazz, settings);
      return null;
    });
  }

  /**
   * Re-index all dependent objects manually (hibernate search). Hibernate doesn't re-index these objects, does it?
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void reindexDependentObjects(final O obj) {
    hibernateSearchDependentObjectsReindexer.reindexDependents(obj);
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void massUpdate(final List<O> list, final O master) {
    if (list == null || list.size() == 0) {
      // No entries to update.
      return;
    }
    if (list.size() > MAX_MASS_UPDATE) {
      throw new UserException(MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N, MAX_MASS_UPDATE);
    }
    final Object store = prepareMassUpdateStore(list, master);
    for (final O entry : list) {
      if (massUpdateEntry(entry, master, store)) {
        try {
          update(entry);
        } catch (final IllegalArgumentException ex) {
          log.error("Exception occured while updating entry inside mass update: " + entry + ex.getMessage());
          throw new UserException("error", ex.getMessage());
        }
      }
    }
  }

  /**
   * Object pass thru every massUpdateEntry call.
   *
   * @return null if not overloaded.
   */
  protected Object prepareMassUpdateStore(final List<O> list, final O master) {
    return null;
  }

  /**
   * Overload this method for mass update support.
   *
   * @param store Object created with prepareMassUpdateStore if needed. Null at default.
   * @return true, if entry is ready for update otherwise false (no update will be done for this entry).
   */
  protected boolean massUpdateEntry(final O entry, final O master, final Object store) {
    throw new UnsupportedOperationException("Mass update is not supported by this dao for: " + clazz.getName());
  }

  // TODO RK entweder so oder ueber annots.
  // siehe org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils.getNestedHistoryEntities(Class<?>)
  protected Class<?>[] getAdditionalHistorySearchDOs() {
    return null;
  }

  public TenantRegistry getTenantRegistry() {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  /**
   * @return the UserGroupCache with groups and rights (tenant specific).
   */
  public UserGroupCache getUserGroupCache() {
    return getTenantRegistry().getUserGroupCache();
  }

  /**
   * @return Wether the data object (BaseDO) this dao is responsible for is from type Historizable or not.
   */
  @Override
  public boolean isHistorizable() {
    return HistoryBaseDaoAdapter.isHistorizable(clazz);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<O> getEntityClass() {
    return (Class<O>) ClassUtils.getGenericTypeArgument(getClass(), 0);
  }

  @Override
  public O selectByPkDetached(Integer pk) throws AccessException {
    // TODO RK not detached here
    return getById(pk);
  }
}
