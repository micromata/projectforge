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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * MaxLengthTextField with required-validation.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class RequiredMaxLengthTextField extends MaxLengthTextField
{
  private static final long serialVersionUID = 7610655476354134456L;

  /**
   * @see MaxLengthTextField#MaxLengthTextField(WebMarkupContainer, String,PropertyModel)
   */
  public RequiredMaxLengthTextField(final String id, final IModel<String> model)
  {
    super(id, model);
    setRequired(true);
  }

  /**
   * @see MaxLengthTextField#MaxLengthTextField(WebMarkupContainer, String,PropertyModel)
   */
  public RequiredMaxLengthTextField(final String id, final IModel<String> model, final int maxLength)
  {
    super(id, model, maxLength);
    setRequired(true);
  }
}
