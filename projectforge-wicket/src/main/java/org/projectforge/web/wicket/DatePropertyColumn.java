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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.time.DateFormatType;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateTimeFormatter;


/**
 * Table view property columns representing date or time stamps.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class DatePropertyColumn<T> extends CellItemListenerPropertyColumn<T>
{
  private static final long serialVersionUID = 4759972917073202845L;
  
  private DateTimeFormatter dateTimeFormatter;
  
  private String datePattern = DateFormats.getFormatString(DateFormatType.DATE_SHORT);

  public DatePropertyColumn(final DateTimeFormatter dateTimeFormatter, final String label, final String sortProperty, final String property, CellItemListener<T> cellItemListener)
  {
    super(new Model<String>(label), sortProperty, property, cellItemListener);
    this.dateTimeFormatter = dateTimeFormatter;
  }

  public DatePropertyColumn(final DateTimeFormatter dateTimeFormatter, final String label, final String sortProperty, final String property)
  {
    this(dateTimeFormatter, label, sortProperty, property, null);
  }
  
  /**
   * Default is SHORT_DATE_FORMAT.
   * @param datePattern Date pattern to set. Please use DateTimeFormatter constants.
   * @see DateTimeFormatter#I18N_KEY_SHORT_DATE_FORMAT
   */
  public DatePropertyColumn<T> setDatePattern(final String datePattern) {
    this.datePattern = datePattern;
    return this;
  }

  @Override
  public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> rowModel)
  {
    final Date value = (Date) BeanHelper.getNestedProperty(rowModel.getObject(), getPropertyExpression());
    final Label label = new Label(componentId, dateTimeFormatter.getFormattedDate(value, datePattern));
    item.add(label);
    if (cellItemListener != null) {
      cellItemListener.populateItem(item, componentId, rowModel);
    }
  }
}
