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

import java.io.Serializable;

import org.projectforge.framework.persistence.user.entities.TenantDO;

import de.micromata.genome.jpa.CustomEntityCopier;
import de.micromata.genome.jpa.DbRecord;
import de.micromata.genome.jpa.EntityCopyStatus;
import de.micromata.genome.jpa.IEmgr;

/**
 * TODO RK is no DO!
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public interface BaseDO<I extends Serializable>
    extends IdObject<I>, DbRecord<I>, CustomEntityCopier<BaseDO<I>>
{
  /**
   * @return The tenant for multi-tenancy.
   */
  public TenantDO getTenant();

  public Integer getTenantId();

  /**
   * Sets the tenant for multi-tenancy.
   * 
   * @return this for chaining.
   */
  public BaseDO<I> setTenant(TenantDO client);

  @Override
  public I getId();

  public void setId(I id);

  @Override
  default I getPk()
  {
    return getId();
  }

  @Override
  default void setPk(I id)
  {
    setId(id);
  }

  /**
   * Can be used for marking changes in a data object as minor changes. This means for example, that after minor changes
   * all dependent objects will not be re-indexed.
   * 
   * @return
   */
  public boolean isMinorChange();

  /**
   * @see #isMinorChanges()
   */
  public void setMinorChange(boolean value);

  /**
   * Free use-able multi purpose attributes.
   * 
   * @param key
   * @return
   */
  public Object getTransientAttribute(String key);

  public void setTransientAttribute(String key, Object value);

  /**
   * Copies all values from the given src object excluding the values created and lastUpdate. Do not overwrite created
   * and lastUpdate from the original database object. Null values will be excluded therefore for such null properties
   * the original properties will be preserved. If you want to delete such properties, please overwrite them manually.
   * <br/>
   * This method is required by BaseDao for example for updating DOs.
   * 
   * @param src
   * @return true, if any modifications are detected, otherwise false;
   */
  public ModificationStatus copyValuesFrom(BaseDO<? extends Serializable> src, String... ignoreFields);

  @Override
  public default EntityCopyStatus copyFrom(IEmgr<?> emgr, Class<? extends BaseDO<I>> iface, BaseDO<I> orig,
      String... ignoreCopyFields)
  {
    ModificationStatus mds = copyValuesFrom(orig, ignoreCopyFields);
    return mds.toEntityCopyStatus();
  }
}
