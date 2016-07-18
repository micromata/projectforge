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

package org.projectforge.web.wicket;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.projectforge.framework.utils.MyBeanComparator;

public abstract class MySortableDataProvider<O> extends SortableDataProvider<O, String>
{
  private static final long serialVersionUID = -3144475230940819703L;

  /**
   * @param property If null then no sort will be supported.
   * @param ascending
   */
  public MySortableDataProvider(final String property, final SortOrder sortOrder)
  {
    // set default sort
    if (property != null) {
      setSort(property, sortOrder);
    } else {
      setSort("NOSORT", sortOrder);
    }
  }

  public abstract List<O> getList();

  protected abstract IModel<O> getModel(O object);

  /**
   * @see org.apache.wicket.markup.repeater.data.IDataProvider#iterator(int, int)
   */
  @Override
  public Iterator<? extends O> iterator(final long first, final long count)
  {
    final SortParam<String> sp = getSort();
    final List<O> list = getList();
    if (list == null) {
      return null;
    }
    if (sp != null && "NOSORT".equals(sp.getProperty()) == false) {
      final Comparator<O> comp = getComparator(sp.getProperty().toString(), sp.isAscending());
      Collections.sort(list, comp);
    }
    return list.subList((int)first, (int)(first + count)).iterator();
  }

  protected Comparator<O> getComparator(final String sortProperty, final boolean ascending)
  {
    return new MyBeanComparator<O>(sortProperty, ascending);
  }

  /**
   * @see org.apache.wicket.markup.repeater.data.IDataProvider#size()
   */
  public long size()
  {
    return getList() != null ? getList().size() : 0;
  }

  /**
   * @see org.apache.wicket.markup.repeater.data.IDataProvider#model(java.lang.Object)
   */
  public IModel<O> model(final O object)
  {
    return getModel(object);
  }
}
