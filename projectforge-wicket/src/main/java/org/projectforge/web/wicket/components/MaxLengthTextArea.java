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

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.StringValidator;

public class MaxLengthTextArea extends TextArea<String>
{
  private static final long serialVersionUID = 1507157818607697767L;

  private Integer maxLength;

  /**
   * Tries to get the length definition of the Hibernate configuration. If not available then a warning will be logged. <br/>
   * Example:
   * 
   * @param id
   * @param label needed for validation error messages. Is also used for setting label via wicket id [label]Label.
   * @param model
   * @see org.apache.wicket.Component#Component(String, IModel)
   * @see FormComponent#setLabel(IModel)
   */
  public MaxLengthTextArea(final String id, final IModel<String> model)
  {
    super(id, model);
    final Integer length = MaxLengthTextField.getMaxLength(model);
    init(id, length);
  }

  /**
   * @param id
   * @param label needed for validation error messages.
   * @param model
   * @param maxLength
   * @see org.apache.wicket.Component#Component(String, IModel)
   * @see FormComponent#setLabel(IModel)
   */
  public MaxLengthTextArea(final String id, final IModel<String> model, final int maxLength)
  {
    super(id, model);
    final Integer length = MaxLengthTextField.getMaxLength(model, maxLength);
    init(id, length);
  }

  private void init(final String id, final Integer maxLength)
  {
    if (maxLength != null) {
      add(StringValidator.maximumLength(maxLength));
      // add(AttributeModifier.replace("maxlength", String.valueOf(maxLength))); // Not supported by html textarea!
      this.maxLength = maxLength;
    }
  }

  /**
   * @return the maxLength
   */
  public Integer getMaxLength()
  {
    return maxLength;
  }
}
