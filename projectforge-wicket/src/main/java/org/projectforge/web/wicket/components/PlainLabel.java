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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * Renders only the body of the Html markup component. For example for &lt;span wicket:id="label"&gt;...&lt;/span&gt; the output of the span
 * tag is suppressed.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see org.apache.wicket.Component#setRenderBodyOnly(boolean)
 */
public class PlainLabel extends Label
{
  private static final long serialVersionUID = -1364572197718474263L;

  public PlainLabel(final String id, IModel< ? > model)
  {
    super(id, model);
    setRenderBodyOnly(true);
  }

  public PlainLabel(String id, String label)
  {
    super(id, label);
    setRenderBodyOnly(true);
  }

}
