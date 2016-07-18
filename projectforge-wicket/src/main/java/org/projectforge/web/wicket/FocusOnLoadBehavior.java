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

package org.projectforge.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;

public class FocusOnLoadBehavior extends Behavior
{
  private static final long serialVersionUID = 2698344455870507074L;

  public FocusOnLoadBehavior()
  {
    super();
  }

  @Override
  public void bind(final Component component)
  {
    component.setOutputMarkupId(true);
  }

  /**
   * @see org.apache.wicket.behavior.Behavior#renderHead(org.apache.wicket.Component, org.apache.wicket.markup.html.IHeaderResponse)
   */
  @Override
  public void renderHead(final Component component, final IHeaderResponse response)
  {
    super.renderHead(component, response);
    response.render(OnLoadHeaderItem.forScript("document.getElementById('" + component.getMarkupId() + "').focus()"));
  }
}
