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

package org.projectforge.web.wicket.autocompletion;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.projectforge.web.wicket.components.MaxLengthTextField;

public abstract class PFAutoCompleteMaxLengthTextField extends PFAutoCompleteTextField<String>
{
  private static final long serialVersionUID = -1269934405480896598L;

  /**
   * 
   * @param id
   * @param model
   */
  public PFAutoCompleteMaxLengthTextField(final String id, final PropertyModel<String> model)
  {
    this(id, model, PFAutoCompleteRenderer.INSTANCE, new PFAutoCompleteSettings());// , type, StringAutoCompleteRenderer.INSTANCE,
    // settings);
  }

  /**
   * @param id
   * @param model
   * @param maxLength
   */
  public PFAutoCompleteMaxLengthTextField(final String id, final PropertyModel<String> model, final int maxLength)
  {
    this(id, model, PFAutoCompleteRenderer.INSTANCE, new PFAutoCompleteSettings(), maxLength);// , type,
    // StringAutoCompleteRenderer.INSTANCE,
    // settings);
  }

  /**
   * @param id
   * @param model
   * @param renderer
   * @param settings
   */
  public PFAutoCompleteMaxLengthTextField(final String id, final PropertyModel<String> model, final IAutoCompleteRenderer<String> renderer,
      final PFAutoCompleteSettings settings)
  {
    this(id, model, renderer, settings, null);
  }

  /**
   * @param id
   * @param model
   * @param renderer
   * @param settings
   * @param maxLength
   */
  public PFAutoCompleteMaxLengthTextField(final String id, final PropertyModel<String> model, final IAutoCompleteRenderer<String> renderer,
      final PFAutoCompleteSettings settings, final Integer maxLength)
  {
    super(id, model, renderer, settings);
    final Integer length = MaxLengthTextField.getMaxLength(model, maxLength);
    init(length);
  }

  private void init(final Integer maxLength)
  {
    if (maxLength != null) {
      add(StringValidator.maximumLength(maxLength));
      //add(AttributeModifier.replace("maxlength", String.valueOf(maxLength))); // Done by StringValidator
    }
  }
}
