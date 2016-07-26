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

package org.projectforge.web.user;

import org.apache.commons.lang.Validate;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;

public class UserPropertyColumn<T> extends CellItemListenerPropertyColumn<T>
{
  private static final long serialVersionUID = -26352961662061891L;

  private UserFormatter userFormatter;

  private transient UserGroupCache userGroupCache;

  /**
   * @param clazz
   * @param sortProperty
   * @param propertyExpression
   * @param cellItemListener
   */
  public UserPropertyColumn(UserGroupCache userGroupCache, final Class<?> clazz, final String sortProperty,
      final String propertyExpression,
      final CellItemListener<T> cellItemListener)
  {
    super(clazz, sortProperty, propertyExpression, cellItemListener);
    this.userGroupCache = userGroupCache;
  }

  /**
   * @param userFormatter
   * @param label
   * @param sortProperty
   * @param property Should be from type PFUserDO or Integer for user id.
   * @param cellItemListener
   */
  public UserPropertyColumn(UserGroupCache userGroupCache, final String label, final String sortProperty,
      final String property,
      final CellItemListener<T> cellItemListener)
  {
    super(new Model<String>(label), sortProperty, property, cellItemListener);
    this.userGroupCache = userGroupCache;
  }

  /**
   * @param userFormatter
   * @param label
   * @param sortProperty
   * @param property Should be from type PFUserDO or Integer for user id.
   */
  public UserPropertyColumn(UserGroupCache userGroupCache, final String label, final String sortProperty,
      final String property)
  {
    this(userGroupCache, label, sortProperty, property, null);
  }

  @Override
  public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> rowModel)
  {
    final Label label = new Label(componentId, new Model<String>(getLabelString(rowModel)));
    item.add(label);
    if (cellItemListener != null)
      cellItemListener.populateItem(item, componentId, rowModel);
  }

  protected String getLabelString(final IModel<T> rowModel)
  {
    final Object obj = BeanHelper.getNestedProperty(rowModel.getObject(), getPropertyExpression());
    PFUserDO user = null;
    if (obj != null) {
      if (obj instanceof PFUserDO) {
        user = (PFUserDO) obj;
      } else if (obj instanceof Integer) {
        final Integer userId = (Integer) obj;
        user = getUserGroupCache().getUser(userId);
      } else {
        throw new IllegalStateException("Unsupported column type: " + obj);
      }
    }
    String result;
    if (user != null) {
      Validate.notNull(userFormatter);
      result = userFormatter.formatUser(user);
    } else {
      result = "";
    }
    return result;
  }

  /**
   * Fluent pattern
   * 
   * @param userFormatter
   */
  public UserPropertyColumn<T> withUserFormatter(final UserFormatter userFormatter)
  {
    this.userFormatter = userFormatter;
    return this;
  }

  private UserGroupCache getUserGroupCache()
  {
    return userGroupCache;
  }
}
