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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class MyListDataProvider<T extends Serializable> implements IDataProvider<T>
{
  private static final long serialVersionUID = 756862441195280278L;

  /** reference to the list used as dataprovider for the dataview */
  protected List<T> list;

  public MyListDataProvider()
  {
  }

  /**
   * @see IDataProvider#iterator(int, int)
   */
  public Iterator< ? extends T> iterator(final int first, final int count)
  {
    int toIndex = first + count;
    if (toIndex > getList().size()) {
      toIndex = list.size();
    }
    return list.subList(first, toIndex).listIterator();
  }

  /**
   * @see IDataProvider#size()
   */
  public long size()
  {
    return getList().size();
  }

  /**
   * @see IDataProvider#model(Object)
   */
  public IModel<T> model(final T object)
  {
    return new Model<T>(object);
  }

  /**
   * @see org.apache.wicket.model.IDetachable#detach()
   */
  public void detach()
  {
    this.list = null;
  }

  protected abstract List<T> loadList();

  protected List<T> getList()
  {
    if (list == null) {
      list = loadList();
    }
    return list;
  }
}
