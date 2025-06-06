/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.IdObject;
import org.projectforge.framework.utils.MyBeanComparator;

import java.io.Serializable;
import java.util.*;

/**
 * Stores list of result sets (id's) for pagination and provides iterator of data-base objects on demand.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MyListPageSortableDataProvider<T extends IdObject<?>> extends SortableDataProvider<T, String>
{
  private static final long serialVersionUID = 1517715565769991765L;

  /**
   * Complete list is needed every time the sort parameters or filter settings were changed.
   */
  private List<T> completeList;

  /**
   * Stores only the id's of the result set.
   */
  private List<Serializable> idList;

  private Long first, count;

  private SortParam<String> sortParam;

  private SortParam<String> secondSortParam;

  private final AbstractListPage<?, ?, T> listPage;

  public MyListPageSortableDataProvider(final SortParam<String> sortParam, final SortParam<String> secondSortParam,
      final AbstractListPage<?, ?, T> listPage)
  {
    this.listPage = listPage;
    // set default sort
    if (sortParam != null) {
      setSort(sortParam);
    } else {
      setSort("NOSORT", SortOrder.ASCENDING);
    }
  }

  public MyListPageSortableDataProvider<T> setCompleteList(final List<T> completeList)
  {
    this.completeList = completeList;
    this.idList = new LinkedList<Serializable>();
    if (this.completeList != null) {
      sortList(this.completeList);
      for (final T entry : completeList) {
        this.idList.add(entry.getId());
      }
    }
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterator<T> iterator(final long first, final long count)
  {
    if ((this.first != null && this.first != first) || (this.count != null && this.count != count)) {
      this.completeList = null; // Force to load all elements from data-base (avoid lazy initialization exceptions).
    }
    final SortParam<String> sp = getSort();
    if (Objects.equals(sortParam, sp) == false) {
      // The sort parameters were changed, force reload from data-base:
      reloadList();
    }
    this.first = first;
    this.count = count;
    if (idList == null) {
      return null;
    }
    List<T> result;
    int fromIndex = (int) first;
    if (fromIndex < 0) {
      fromIndex = 0;
    }
    int toIndex = (int) (first + count);
    if (this.completeList != null) {
      // The completeList is already load, don't need to load objects from data-base:
      result = completeList;
      if (toIndex > idList.size()) {
        toIndex = idList.size();
      }
      result = new LinkedList<T>();
      for (final T entry : completeList.subList(fromIndex, toIndex)) {
        result.add(entry);
      }
      this.completeList = null; // Don't store the complete list on the server anymore.
      return result.iterator();
    } else {
      if (toIndex > idList.size()) {
        toIndex = idList.size();
      }
      final Object baseDao = listPage.getBaseDao();
      if (baseDao instanceof BaseDao) {
        final List<T> list = (List<T>) ((BaseDao<?>) baseDao).select(idList.subList(fromIndex, toIndex), true);
        sortList(list);
        return list.iterator();
      } else {
        // Can't load objects from BaseDao, have to load complete list from scratch:
        listPage.setRefreshResultList();
        reloadList();
        result = completeList;
        if (toIndex > idList.size()) {
          toIndex = idList.size();
        }
        return result.subList(fromIndex, toIndex).iterator();
      }
    }
  }

  protected Comparator<T> getComparator(final SortParam<String> sortParam, final SortParam<String> secondSortParam)
  {
    final String sortProperty = sortParam != null ? sortParam.getProperty() : null;
    final boolean ascending = sortParam != null ? sortParam.isAscending() : true;

    final String secondSortProperty = secondSortParam != null ? secondSortParam.getProperty() : null;
    final boolean secondAscending = secondSortParam != null ? secondSortParam.isAscending() : true;
    return new MyBeanComparator<T>(sortProperty, ascending, secondSortProperty, secondAscending);
  }

  /**
   * @see org.apache.wicket.markup.repeater.data.IDataProvider#size()
   */
  @Override
  public long size()
  {
    if (idList == null) {
      reloadList();
    }
    return this.idList != null ? this.idList.size() : 0;
  }

  private void reloadList()
  {
    final List<T> list = listPage.getList();
    setCompleteList(list);
  }

  private void sortList(final List<T> list)
  {
    final SortParam<String> sp = getSort();
    if (sp != null && "NOSORT".equals(sp.getProperty()) == false) {
      if (this.sortParam != null && StringUtils.equals(this.sortParam.getProperty(), sp.getProperty()) == false) {
        this.secondSortParam = this.sortParam;
      }
      final Comparator<T> comp = getComparator(sp, secondSortParam);
      Collections.sort(list, comp);
    }
    this.sortParam = sp;
  }

  /**
   * @see org.apache.wicket.markup.repeater.data.IDataProvider#model(java.lang.Object)
   */
  @Override
  public IModel<T> model(final T object)
  {
    return new Model<T>(object);
  }

  /**
   * @see ISortableDataProvider#detach()
   */
  @Override
  public void detach()
  {
    this.completeList = null;
  }
}
