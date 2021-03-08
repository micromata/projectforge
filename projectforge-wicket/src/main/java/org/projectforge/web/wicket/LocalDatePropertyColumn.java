/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.common.BeanHelper;
import org.projectforge.common.DateFormatType;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.PFDay;

import java.time.LocalDate;

/**
 * Table view property columns representing LocalDate.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LocalDatePropertyColumn<T> extends CellItemListenerPropertyColumn<T> {
  public LocalDatePropertyColumn(final String label, final String sortProperty, final String property,
                                 CellItemListener<T> cellItemListener) {
    super(new Model<String>(label), sortProperty, property, cellItemListener);
  }

  public LocalDatePropertyColumn(final String label, final String sortProperty, final String property) {
    this(label, sortProperty, property, null);
  }

  @Override
  public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> rowModel) {
    final LocalDate date = (LocalDate) BeanHelper.getNestedProperty(rowModel.getObject(), getPropertyExpression());
    String formattedDate = "";
    if (date != null) {
      formattedDate = PFDay.from(date).format(DateFormats.getDateTimeFormatter(DateFormatType.DATE_SHORT));
    }
    final Label label = new Label(componentId, formattedDate);
    item.add(label);
    if (cellItemListener != null) {
      cellItemListener.populateItem(item, componentId, rowModel);
    }
  }
}
