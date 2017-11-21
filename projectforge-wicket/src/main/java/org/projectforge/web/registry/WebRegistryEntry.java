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

package org.projectforge.web.registry;

import java.io.Serializable;

import org.apache.commons.lang3.Validate;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.registry.Registry;
import org.projectforge.registry.RegistryEntry;
import org.projectforge.web.wicket.IListPageColumnsCreator;

/**
 * Contains more information than a RegistryEntry. This is e. g. needed by general search page.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class WebRegistryEntry implements Serializable
{
  private static final long serialVersionUID = 8289071922222570636L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WebRegistryEntry.class);

  private final RegistryEntry registryEntry;

  private Class<? extends IListPageColumnsCreator<?>> listPageColumnsCreatorClass;

  /**
   * Id must be found in {@link Registry}.
   * 
   * @param id
   */
  public WebRegistryEntry(Registry registry, final String id)
  {
    Validate.notNull(id);
    registryEntry = registry.getEntry(id);
    Validate.notNull(registryEntry);
  }

  /**
   * Id must be found in {@link Registry}.
   * 
   * @param id
   * @param listPageColumnsCreatorClass Needed for displaying the result-sets by the general search page.
   */
  public WebRegistryEntry(Registry registry, final String id,
      final Class<? extends IListPageColumnsCreator<?>> listPageColumnsCreatorClass)
  {
    Validate.notNull(id);
    registryEntry = registry.getEntry(id);
    this.listPageColumnsCreatorClass = listPageColumnsCreatorClass;
    if (registryEntry == null) {
      log.error("Object with id '" + id + "' isn't registered in Registry!");
    }
  }

  public WebRegistryEntry(final RegistryEntry registryEntry)
  {
    this.registryEntry = registryEntry;
  }

  /**
   * @return the registryEntry
   */
  public RegistryEntry getRegistryEntry()
  {
    return registryEntry;
  }

  public WebRegistryEntry setListPageColumnsCreatorClass(
      final Class<? extends IListPageColumnsCreator<?>> listPageColumnsCreatorClass)
  {
    this.listPageColumnsCreatorClass = listPageColumnsCreatorClass;
    return this;
  }

  /**
   * Needed for displaying the result-sets by the general search page.
   */
  public Class<? extends IListPageColumnsCreator<?>> getListPageColumnsCreatorClass()
  {
    return listPageColumnsCreatorClass;
  }

  public BaseDao<?> getDao()
  {
    return registryEntry.getDao();
  }

  public Class<? extends BaseDao<?>> getDaoClassType()
  {
    return registryEntry.getDaoClassType();
  }

  public Class<? extends BaseDO<?>> getDOClass()
  {
    return registryEntry.getDOClass();
  }

  public String getI18nPrefix()
  {
    return registryEntry.getI18nPrefix();
  }

  public String getI18nTitleHeading()
  {
    return registryEntry.getI18nTitleHeading();
  }

  public String getId()
  {
    return registryEntry.getId();
  }

  public final Class<? extends BaseSearchFilter> getSearchFilterClass()
  {
    return registryEntry.getSearchFilterClass();
  }

  /**
   * If true (default) then the search in the web search page is supported for this area. Otherwise this area will not
   * be included in the search.
   * 
   * @return the searchable
   * @see RegistryEntry#isSearchable()
   */
  public boolean isSearchable()
  {
    return registryEntry.isSearchable();
  }
}
