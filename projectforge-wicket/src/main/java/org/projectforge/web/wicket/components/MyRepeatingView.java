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

import org.apache.wicket.markup.repeater.RepeatingView;

/**
 * This repeating view is only visible if any child was added (detected by calling newChildId()).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class MyRepeatingView extends RepeatingView
{
  private static final long serialVersionUID = 1534625043282794990L;

  protected boolean hasChilds = false;

  public MyRepeatingView(final String id)
  {
    super(id);
  }

  @Override
  public String newChildId()
  {
    hasChilds = true;
    return super.newChildId();
  }

  @Override
  public boolean isVisible()
  {
    return hasChilds;
  }
}
