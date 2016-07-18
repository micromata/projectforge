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

package org.projectforge.web.wicket.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.projectforge.common.i18n.I18nEnum;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.utils.ILabelValueBean;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LabelValueChoiceRenderer<T> implements IChoiceRenderer<T>
{
  private static final long serialVersionUID = -5832080496659840226L;

  private List<T> values;

  private Map<T, String> displayValues;

  /**
   */
  public LabelValueChoiceRenderer()
  {
    this.values = new ArrayList<T>();
    this.displayValues = new HashMap<T, String>();
  }

  /**
   * Creates already entries from the given enum. Works only if T is from type I18nEnum.
   * @param parent Only needed for internationalization.
   * @param i18nEnum if not enum and not from type T a class cast exception will be thrown.
   * @see Component#getString(String)
   */
  @SuppressWarnings("unchecked")
  public LabelValueChoiceRenderer(final Component parent, final I18nEnum[] values)
  {
    this();
    for (final I18nEnum value : values) {
      addValue((T) value, parent.getString(value.getI18nKey()));
    }
  }

  /**
   * Works only if T is from type String.
   * @param values if the elements are not from type ILabelValueBean<String, T> a class cast exception will be thrown.
   * @see Component#getString(String)
   */
  @SuppressWarnings("unchecked")
  public LabelValueChoiceRenderer(final String... values)
  {
    this();
    for (final String value : values) {
      addValue((T) value, value);
    }
  }

  /**
   * Creates already entries from the given enum.
   * @param values if the elements are not from type ILabelValueBean<String, T> a class cast exception will be thrown.
   * @see Component#getString(String)
   */
  public LabelValueChoiceRenderer(final List<T> values)
  {
    this();
    if (values == null) {
      return;
    }
    for (final Object value : values) {
      @SuppressWarnings("unchecked")
      final ILabelValueBean<String, T> labelValue = (ILabelValueBean<String, T>) value;
      addValue(labelValue.getValue(), labelValue.getLabel());
    }
  }

  /**
   * @param values if the elements are not from type ILabelValueBean<String, T> a class cast exception will be thrown.
   * @return This for chaining.
   */
  public LabelValueChoiceRenderer<T> setValues(final T... values)
  {
    for (final Object value : values) {
      @SuppressWarnings("unchecked")
      final ILabelValueBean<String, T> labelValue = (ILabelValueBean<String, T>) value;
      addValue(labelValue.getValue(), labelValue.getLabel());
    }
    return this;
  }

  /**
   * @return This for chaining.
   */
  @SuppressWarnings("unchecked")
  public LabelValueChoiceRenderer<T> setValueArray(final String[] values)
  {
    for (final String value : values) {
      addValue((T) value, value);
    }
    return this;
  }

  /**
   * @return This for chaining.
   */
  public LabelValueChoiceRenderer<T> clear() {
    this.values.clear();
    this.displayValues.clear();
    return this;
  }

  public LabelValueChoiceRenderer<T> addValue(final T value, final String displayValue)
  {
    this.values.add(value);
    this.displayValues.put(value, displayValue);
    return this;
  }

  public LabelValueChoiceRenderer<T> addValue(final int index, final T value, final String displayValue)
  {
    this.values.add(index, value);
    this.displayValues.put(value, displayValue);
    return this;
  }

  /**
   * Sort the entries by label.
   * @return This for chaining.
   */
  public LabelValueChoiceRenderer<T> sortLabels()
  {
    Collections.sort(values, new Comparator<T>() {
      @Override
      public int compare(final T value1, final T value2)
      {
        final String label1 = displayValues.get(value1).toLowerCase();
        final String label2 = displayValues.get(value2).toLowerCase();
        return label1.compareTo(label2);
      }

    });
    return this;
  }

  public List<T> getValues()
  {
    return values;
  }

  /**
   * Please note: This method does not check wether the given object is an entry of the year list or not.
   * @return given integer as String or "[minYear]-[maxYear]" if value is -1.
   * @see org.apache.wicket.markup.html.form.IChoiceRenderer#getDisplayValue(java.lang.Object)
   */
  public Object getDisplayValue(final T object)
  {
    return this.displayValues.get(object);
  }

  public String getIdValue(final T object, final int index)
  {
    if (object == null) {
      return "";
    }
    if (object instanceof BaseDO) {
      return String.valueOf(((BaseDO<?>)object).getId());
    }
    return object.toString();
  }

}
