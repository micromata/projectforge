/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * MaxLengthTextArea with required-validation.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class RequiredMaxLengthTextArea extends MaxLengthTextArea
{
  private static final long serialVersionUID = -6828091890941024406L;

  /**
   * @see MaxLengthTextArea#MaxLengthTextArea(String, PropertyModel)
   */
  public RequiredMaxLengthTextArea(final String id, final IModel<String> model)
  {
    super(id, model);
    setRequired(true);
  }

  /**
   * @see MaxLengthTextArea#MaxLengthTextArea(String,PropertyModel, int)
   */
  public RequiredMaxLengthTextArea(final String id, final IModel<String> model, final int maxLength)
  {
    super(id, model, maxLength);
    setRequired(true);
  }
}
