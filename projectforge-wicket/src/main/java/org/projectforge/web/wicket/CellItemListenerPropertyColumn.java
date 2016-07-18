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

import java.util.Date;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.common.i18n.I18nEnum;
import org.projectforge.common.props.PropUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateTimeFormatter;

/**
 * Supports CellItemListener.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class CellItemListenerPropertyColumn<T> extends PropertyColumn<T, String>
{
  protected CellItemListener<T> cellItemListener;

  /**
   * @param displayModelString For creation of new Model<String>.
   * @param sortProperty
   * @param propertyExpression
   * @param cellItemListener
   */
  public CellItemListenerPropertyColumn(final String displayModelString, final String sortProperty, final String propertyExpression,
      final CellItemListener<T> cellItemListener)
  {
    super(new Model<String>(displayModelString), sortProperty, propertyExpression);
    this.cellItemListener = cellItemListener;
  }

  /**
   * @param displayModelString
   * @param sortProperty
   * @param propertyExpression
   * @see #CellItemListenerPropertyColumn(String, String, String, CellItemListener)
   */
  public CellItemListenerPropertyColumn(final String displayModelString, final String sortProperty, final String propertyExpression)
  {
    this(displayModelString, sortProperty, propertyExpression, null);
  }

  public CellItemListenerPropertyColumn(final IModel<String> displayModel, final String sortProperty, final String propertyExpression,
      final CellItemListener<T> cellItemListener)
  {
    super(displayModel, sortProperty, propertyExpression);
    this.cellItemListener = cellItemListener;
  }

  public CellItemListenerPropertyColumn(final Class< ? > clazz, final String sortProperty, final String propertyExpression,
      final CellItemListener<T> cellItemListener)
  {
    super(new ResourceModel(PropUtils.getI18nKey(clazz, propertyExpression)), sortProperty, propertyExpression);
    this.cellItemListener = cellItemListener;
  }

  public CellItemListenerPropertyColumn(final IModel<String> displayModel, final String sortProperty, final String propertyExpression)
  {
    this(displayModel, sortProperty, propertyExpression, null);
  }

  /**
   * Override this method if you want to have tool-tips.
   * @return
   */
  public String getTooltip(final T object)
  {
    return null;
  }

  /**
   * Call CellItemListener. If a property model object is of type I18nEnum then the translation is automatically used.
   * @see org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
   *      java.lang.String, org.apache.wicket.model.IModel)
   * @see CellItemListener#populateItem(Item, String, IModel)
   */
  @Override
  public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> rowModel)
  {
    final IModel< ? > propertyModel = createLabelModel(rowModel);
    final Object object = propertyModel.getObject();
    if (object == null) {
      item.add(new Label(componentId, propertyModel).setRenderBodyOnly(true));
    } else if (object instanceof I18nEnum) {
      item.add(new Label(componentId, ThreadLocalUserContext.getLocalizedString(((I18nEnum) object).getI18nKey())).setRenderBodyOnly(true));
    } else if (object instanceof java.sql.Date) {
      item.add(new Label(componentId, DateTimeFormatter.instance().getFormattedDate(object)).setRenderBodyOnly(true));
    } else if (object instanceof Date) {
      item.add(new Label(componentId, DateTimeFormatter.instance().getFormattedDateTime((Date) object)).setRenderBodyOnly(true));
    } else {
      item.add(new Label(componentId, propertyModel).setRenderBodyOnly(true));
    }
    final String tooltip = getTooltip(rowModel.getObject());
    if (tooltip != null && tooltip.length() > 0) {
      WicketUtils.addTooltip(item, new Model<String>() {
        @Override
        public String getObject()
        {
          return getTooltip(rowModel.getObject());
        }
      });
    }
    if (cellItemListener != null) {
      cellItemListener.populateItem(item, componentId, rowModel);
    }
  }
}
