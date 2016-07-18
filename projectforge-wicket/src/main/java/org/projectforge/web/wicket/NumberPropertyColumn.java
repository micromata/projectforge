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

import java.math.BigDecimal;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.framework.utils.NumberHelper;


/**
 * Formats given number (null values as empty strings) by using String.valueOf or NumberFormatter.format(BigDecimal). Sets the html
 * attribute style="text-align: right;".
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class NumberPropertyColumn<T> extends CellItemListenerPropertyColumn<T>
{
  private static final long serialVersionUID = 2718761068078002330L;

  private String textAlign = "right";

  private String suffix;

  private int scale = -1;

  private boolean displayZeroValues = true;

  public NumberPropertyColumn(final String label, final String sortProperty, final String property, final CellItemListener<T> cellItemListener)
  {
    super(new Model<String>(label), sortProperty, property, cellItemListener);
  }

  public NumberPropertyColumn(final String label, final String sortProperty, final String property)
  {
    this(label, sortProperty, property, null);
  }

  @Override
  public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> rowModel)
  {
    final Object obj = getValue(rowModel);
    final String value;
    if (obj == null) {
      value = "";
    } else if (obj instanceof BigDecimal) {
      if (displayZeroValues == true || NumberHelper.isNotZero((BigDecimal) obj) == true) {
        if (this.scale >= 0) {
          value = NumberFormatter.format((BigDecimal) obj, this.scale);
        } else {
          value = NumberFormatter.format((BigDecimal) obj);
        }
      } else {
        value = "";
      }
    } else {
      value = obj.toString();
    }
    if (suffix != null && value.length() > 0) {
      item.add(new Label(componentId, value + suffix));
    } else {
      item.add(new Label(componentId, value));
    }
    if (cellItemListener != null) {
      cellItemListener.populateItem(item, componentId, rowModel);
    }
    if (textAlign != null) {
      item.add(AttributeModifier.append("style", new Model<String>("text-align: " + textAlign + ";")));
    }
  }

  protected Object getValue(final IModel<T> rowModel)
  {
    return BeanHelper.getNestedProperty(rowModel.getObject(), getPropertyExpression());
  }

  /**
   * @param textAlign Default is right. If null then no text-align will be given to style attribute.
   * @return this (fluent)
   */
  public NumberPropertyColumn<T> withTextAlign(final String textAlign)
  {
    this.textAlign = textAlign;
    return this;
  }

  /**
   * @param suffix The suffix to print behind the number for not null values. Default is null (no suffix).
   * @return this (fluent)
   */
  public NumberPropertyColumn<T> withSuffix(final String suffix)
  {
    this.suffix = suffix;
    return this;
  }

  /**
   * @param displayZeroValues If set to false then values equal to zero will not displayed.
   * @return
   */
  public NumberPropertyColumn<T> withDisplayZeroValues(final boolean displayZeroValues)
  {
    this.displayZeroValues = displayZeroValues;
    return this;
  }

  /**
   * @param scale Scale to display.
   * @return
   */
  public NumberPropertyColumn<T> withScale(final int scale)
  {
    this.scale = scale;
    return this;
  }
}
