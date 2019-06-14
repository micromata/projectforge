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

package org.projectforge.export;

import java.lang.reflect.Field;

import org.projectforge.business.excel.PropertyMapping;
import org.projectforge.common.BeanHelper;

/**
 * Created by blumenstein on 24.11.16.
 */
public class DOGetterListExcelExporter extends DOListExcelExporter
{
  public DOGetterListExcelExporter(final String filenameIdentifier)
  {
    super(filenameIdentifier);
  }

  @Override
  public void addMapping(final PropertyMapping mapping, final Object entry, final Field field)
  {
    if (BeanHelper.getFieldValue(entry, field) != null) {
      mapping.add(field.getName(), BeanHelper.getFieldValue(entry, field));
    } else {
      Object result = BeanHelper.invoke(entry, BeanHelper.determineGetter(entry.getClass(), field.getName()));
      if (result != null) {
        mapping.add(field.getName(), result.toString());
      }

    }
  }

}
