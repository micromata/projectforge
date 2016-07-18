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

import java.io.Serializable;

import org.apache.commons.lang.ClassUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.validator.RangeValidator;

public class MinMaxNumberField<Z extends Comparable<Z> & Serializable> extends TextField<Z>
{
  private static final long serialVersionUID = 7967450478496603051L;

  private IConverter converter;

  /**
   * @param id
   * @param model
   * @see org.apache.wicket.Component#Component(String, IModel)
   */
  public MinMaxNumberField(final String id, final IModel<Z> model, final Z minimum, final Z maximum)
  {
    super(id, model);
    if (minimum.compareTo(maximum) <= 0) {
      add(new RangeValidator<Z>(minimum, maximum));
    } else {
      add(new RangeValidator<Z>(maximum, minimum));

    }
    if (ClassUtils.isAssignable(minimum.getClass(), Integer.class) == true) {
      setMaxLength(Math.max(String.valueOf(minimum).length(), String.valueOf(maximum).length()));
    }
  }

  /**
   * Results in attribute maxlength in HTML output of input field. For type integer the maxLength will be calculated automatically.
   * @param maxLength
   */
  public MinMaxNumberField<Z> setMaxLength(final int maxLength)
  {
    add(AttributeModifier.replace("maxlength", String.valueOf(maxLength)));
    return this;
  }

  /**
   * Results in attribute maxlength in HTML output of input field. For type integer the maxLength will be calculated automatically.
   * @param maxLength
   */
  public MinMaxNumberField<Z> setSize(final int size)
  {
    add(AttributeModifier.replace("size", String.valueOf(size)));
    return this;
  }

  /**
   * @see org.apache.wicket.Component#getConverter(java.lang.Class)
   */
  @Override
  public <C> IConverter<C> getConverter(final Class<C> type)
  {
    if (converter != null) {
      return converter;
    } else {
      return super.getConverter(type);
    }
  }

  /**
   * Setting a converter is more convenient instead of overriding method getConverter(Class).
   * @param converter
   * @return This for chaining.
   */
  public MinMaxNumberField<Z> setConverter(final IConverter converter)
  {
    this.converter = converter;
    return this;
  }
}
