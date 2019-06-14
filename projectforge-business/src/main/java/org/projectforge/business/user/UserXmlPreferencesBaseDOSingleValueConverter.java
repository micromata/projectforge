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

import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.BaseDao;
import org.springframework.context.ApplicationContext;

import com.thoughtworks.xstream.converters.SingleValueConverter;

/**
 * Converts BaseDO from and to strings (using the id).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class UserXmlPreferencesBaseDOSingleValueConverter implements SingleValueConverter
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(UserXmlPreferencesBaseDOSingleValueConverter.class);

  private final Class<? extends BaseDao<?>> daoClass;

  private final Class<? extends BaseDO<?>> doClass;

  ApplicationContext applicationContext;

  /**
   * Marshals only the id and unmarshals by loading the instance by id from the dao.
   * 
   * @param daoClass Class of the dao.
   * @param doClass Class of the DO which will be converted.
   * @see BaseDao#getOrLoad(Integer)
   */
  public UserXmlPreferencesBaseDOSingleValueConverter(ApplicationContext applicationContext,
      final Class<? extends BaseDao<?>> daoClass,
      final Class<? extends BaseDO<?>> doClass)
  {
    this.applicationContext = applicationContext;
    this.daoClass = daoClass;
    this.doClass = doClass;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean canConvert(final Class type)
  {
    return doClass.isAssignableFrom(type);
  }

  @Override
  public String toString(final Object obj)
  {
    if (obj == null) {
      return null;
    }
    try {
      return String.valueOf(((BaseDO<?>) obj).getId());
    } catch (final Exception ex) {
      log.warn(ex.getMessage(), ex);
      return "";
    }
  }

  @Override
  public Object fromString(final String str)
  {
    if (StringUtils.isBlank(str) == true) {
      return null;
    }
    final Integer id = Integer.parseInt(str);
    final BaseDao<?> dao = applicationContext.getBean(daoClass);
    if (dao == null) {
      log.error("Could not get dao '" + daoClass + "'. It's not registerd in the Registry.");
      return null;
    } else {
      return dao.getOrLoad(id);
    }
  }
}
