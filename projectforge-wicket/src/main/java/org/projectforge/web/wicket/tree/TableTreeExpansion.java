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

package org.projectforge.web.wicket.tree;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.projectforge.framework.persistence.api.IdObject;

/**
 * Memorizes the open state of the tree nodes.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TableTreeExpansion<I extends Serializable, T extends IdObject<I>> implements Set<T>, Serializable
{
  private static final long serialVersionUID = 2780714125411794367L;

  private Set<I> ids = new HashSet<I>();

  private boolean inverse;

  /**
   * @param ids the ids to set
   */
  public void setIds(final Set<I> ids)
  {
    this.ids = ids;
  }

  /**
   * @return the ids
   */
  public Set<I> getIds()
  {
    return ids;
  }

  public void expandAll()
  {
    ids.clear();
    inverse = true;
  }

  public void collapseAll()
  {
    ids.clear();
    inverse = false;
  }

  @Override
  public boolean add(final T obj)
  {
    if (inverse) {
      return ids.remove(obj.getId());
    } else {
      return ids.add(obj.getId());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean remove(final Object o)
  {
    final T foo = (T) o;

    if (inverse) {
      return ids.add(foo.getId());
    } else {
      return ids.remove(foo.getId());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean contains(final Object o)
  {
    final T foo = (T) o;

    if (inverse) {
      return !ids.contains(foo.getId());
    } else {
      return ids.contains(foo.getId());
    }
  }

  @Override
  public void clear()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmpty()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public <A> A[] toArray(final A[] a)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<T> iterator()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object[] toArray()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(final Collection< ? > c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(final Collection< ? extends T> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(final Collection< ? > c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(final Collection< ? > c)
  {
    throw new UnsupportedOperationException();
  }
}
