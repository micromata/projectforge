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

import org.apache.wicket.model.IModel;

public class RequiredMinMaxNumberField<Z extends Comparable<Z> & Serializable> extends MinMaxNumberField<Z>
{
  private static final long serialVersionUID = 7967450478496603051L;

  /**
   * @param id
   * @param model
   * @see org.apache.wicket.Component#Component(String, IModel)
   */
  public RequiredMinMaxNumberField(final String id, final IModel<Z> model, final Z minimum, final Z maximum)
  {
    super(id, model, minimum, maximum);
    setRequired(true);
  }
}
