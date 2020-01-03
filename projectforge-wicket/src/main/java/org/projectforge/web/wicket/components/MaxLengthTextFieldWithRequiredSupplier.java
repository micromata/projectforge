/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import java.util.function.BooleanSupplier;

/**
 * This is a MaxLengthTextField where you can set a supplier whose result is returned in isRequired().
 */
public class MaxLengthTextFieldWithRequiredSupplier extends MaxLengthTextField
{
  private static final long serialVersionUID = 1L;

  private BooleanSupplier requiredSupplier;

  public MaxLengthTextFieldWithRequiredSupplier(String id, IModel<String> model)
  {
    super(id, model);
  }

  /**
   * Set a supplier whose result is returned in isRequired().
   */
  public void setRequiredSupplier(BooleanSupplier requiredSupplier)
  {
    this.requiredSupplier = requiredSupplier;
  }

  @Override
  public boolean isRequired()
  {
    return requiredSupplier != null && requiredSupplier.getAsBoolean();
  }
}
