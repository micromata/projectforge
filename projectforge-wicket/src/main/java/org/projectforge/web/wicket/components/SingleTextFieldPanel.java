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

import java.math.BigDecimal;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;

/**
 * Panel containing only one text input field. Can be used for creating dynamic elements (e. g. inside a repeating view). <br/>
 * This component calls setRenderBodyOnly(true). If the outer html element is needed, please call setRenderBodyOnly(false).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class SingleTextFieldPanel extends Panel
{
  private static final long serialVersionUID = 6436118474627248790L;

  private TextField< ? > textField;

  private SingleTextFieldPanel(final String id)
  {
    super(id);
    setRenderBodyOnly(true);
  }

  private void setTextField(final TextField< ? > textField)
  {
    this.textField = textField;
    this.add(textField);
  }

  public SingleTextFieldPanel setAttributeModifier(final String attrName, final String value)
  {
    textField.add(new AttributeModifier(attrName, true, new Model<String>(value)));
    return this;
  }

  public static SingleTextFieldPanel createMinMaxNumberField(final String id, final IModel<Integer> model, final Integer minimum,
      final Integer maximum)
  {
    return createMinMaxNumberField(id, model, minimum, maximum, null);
  }

  @SuppressWarnings("serial")
  public static SingleTextFieldPanel createMinMaxNumberField(final String id, final IModel<Integer> model, final Integer minimum,
      final Integer maximum, final IConverter converter)
  {
    final SingleTextFieldPanel panel = new SingleTextFieldPanel(id);
    final MinMaxNumberField<Integer> textField;
    if (converter != null) {
      textField = new MinMaxNumberField<Integer>("text", model, minimum, maximum) {
        /**
         * @see org.projectforge.web.wicket.components.MinMaxNumberField#getConverter(java.lang.Class)
         */
        @Override
        public <C> IConverter<C> getConverter(final Class<C> type)
        {
          return converter;
        }
      };
    } else {
      textField = new MinMaxNumberField<Integer>("text", model, minimum, maximum);
    }
    panel.setTextField(textField);
    return panel;
  }

  public static SingleTextFieldPanel createMinMaxNumberField(final String id, final IModel<BigDecimal> model, final BigDecimal minimum,
      final BigDecimal maximum)
  {
    final MinMaxNumberField<BigDecimal> textField = new MinMaxNumberField<BigDecimal>("text", model, minimum, maximum);
    final SingleTextFieldPanel panel = new SingleTextFieldPanel(id);
    panel.setTextField(textField);
    return panel;
  }
}
