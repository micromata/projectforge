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

package org.projectforge.registry;

import java.io.Serializable;

import org.projectforge.business.scripting.ScriptingDao;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.database.DatabaseDao;

/**
 * For registering a dao object and its scripting dao (optional).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class RegistryEntry implements Serializable
{
  private static final long serialVersionUID = -5464929865679294316L;

  private final String id;

  private final String i18nPrefix;

  private transient BaseDao<?> dao;

  private Class<? extends BaseDO<?>>[] nestedDOClasses;

  private transient ScriptingDao<?> scriptingDao;

  private boolean supressScriptingDao;

  private boolean fullTextSearchSupport = true;

  private boolean searchable = true;

  private Class<? extends BaseSearchFilter> searchFilterClass;

  private final Class<? extends BaseDao<?>> daoClassType;

  /**
   * @param id
   * @param daoClassType Needed because dao is a proxy or whatever object.
   * @param dao
   */
  RegistryEntry(final String id, final Class<? extends BaseDao<?>> daoClassType, final BaseDao<?> dao)
  {
    this(id, daoClassType, dao, null);
  }

  /**
   * @param id
   * @param daoClassType Needed because dao is a proxy or whatever object.
   * @param dao
   * @param i18nPrefix The i18n prefix (if different to id) used e. g. by SearchForm (&lt;prefix&gt;.title.heading.
   */
  public RegistryEntry(final String id, final Class<? extends BaseDao<?>> daoClassType, final BaseDao<?> dao,
      final String i18nPrefix)
  {
    this.id = id;
    this.daoClassType = daoClassType;
    this.dao = dao;
    this.i18nPrefix = (i18nPrefix != null) ? i18nPrefix : id;
  }

  public RegistryEntry setSearchFilterClass(final Class<? extends BaseSearchFilter> searchFilterClass)
  {
    this.searchFilterClass = searchFilterClass;
    return this;
  }

  /**
   * Register an own ScriptingDao. If this method isn't call than the generic ScriptingDao is used.
   * 
   * @param scriptingDao
   * @return this for chaining.
   */
  public RegistryEntry setScriptingDao(final ScriptingDao<?> scriptingDao)
  {
    this.scriptingDao = scriptingDao;
    return this;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public ScriptingDao<?> getScriptingDao()
  {
    if (this.supressScriptingDao == true) {
      return null;
    }
    if (this.scriptingDao == null) {
      this.scriptingDao = new ScriptingDao(this.dao);
    }
    return scriptingDao;
  }

  /**
   * If set to true then no scripting dao is used (e. g. for using in the scripting engine). At default a scripting dao
   * is automatically available.
   * 
   * @param supressScriptingDao
   * @return this for chaining.
   */
  public RegistryEntry setSupressScriptingDao(final boolean supressScriptingDao)
  {
    this.supressScriptingDao = supressScriptingDao;
    return this;
  }

  /**
   * @return true (default) if the re-indexing should be called directly, false otherwise (if no full text search is
   *         wanted or if this object is a sub object (dependant object).
   * @see DatabaseDao#rebuildDatabaseSearchIndices()
   */
  public boolean isFullTextSearchSupport()
  {
    return fullTextSearchSupport;
  }

  /**
   * @param fullTextSearchSupport
   * @return this for chaining.
   */
  public RegistryEntry setFullTextSearchSupport(final boolean fullTextSearchSupport)
  {
    this.fullTextSearchSupport = fullTextSearchSupport;
    return this;
  }

  /**
   * @return The dao specific filter or null if not registered.
   */
  public final Class<? extends BaseSearchFilter> getSearchFilterClass()
  {
    return this.searchFilterClass;
  }

  public String getId()
  {
    return id;
  }

  public Class<? extends BaseDO<?>> getDOClass()
  {
    return dao.getDOClass();
  }

  /**
   * The nested do classes are used e. g. by the full text search engine for re-indexing.
   * 
   * @return Nested (dependent do classes with no own registry entry) if given, otherwise null.
   */
  public Class<? extends BaseDO<?>>[] getNestedDOClasses()
  {
    return nestedDOClasses;
  }

  /**
   * @param nestedDOClasses
   * @return this for chaining.
   */
  public RegistryEntry setNestedDOClasses(final Class<? extends BaseDO<?>>... nestedDOClasses)
  {
    this.nestedDOClasses = nestedDOClasses;
    return this;
  }

  public Class<? extends BaseDao<?>> getDaoClassType()
  {
    return daoClassType;
  }

  /**
   * Is used e. g. by {@link org.projectforge.web.core.SearchForm}: &lt;i18nPrefix&gt;.title.heading.
   * 
   * @return The prefix of the i18n keys to prepend, e. g. "fibu.kost1". If not given, than the id will be used as
   *         prefix.
   */
  public String getI18nPrefix()
  {
    return i18nPrefix;
  }

  public String getI18nTitleHeading()
  {
    return i18nPrefix + ".title.heading";
  }

  /**
   * If true (default) then the search in the web search page is supported for this area. Otherwise this area will not
   * be included in the search.
   * 
   * @return the searchable
   */
  public boolean isSearchable()
  {
    return searchable;
  }

  /**
   * @param searchable the searchable to set
   * @return this for chaining.
   */
  public RegistryEntry setSearchable(final boolean searchable)
  {
    this.searchable = searchable;
    return this;
  }

  public BaseDao<?> getDao()
  {
    return dao;
  }
}
