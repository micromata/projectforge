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

package org.projectforge.web.wicket.flowlayout;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class AjaxIconLinkPanel extends IconLinkPanel
{
  private static final long serialVersionUID = 1366456680979183965L;

  /**
   * @param id
   * @param type
   */
  public AjaxIconLinkPanel(final String id, final IconType type)
  {
    this(id, type, null);
  }

  /**
   * @param id
   * @param type
   * @param tooltip
   */
  @SuppressWarnings("serial")
  public AjaxIconLinkPanel(final String id, final IconType type, final IModel<String> tooltip)
  {
    super(id, type, tooltip);
    setLink(new AjaxLink<Void>(IconLinkPanel.LINK_ID) {
      /**
       * @see org.apache.wicket.ajax.markup.html.AjaxLink#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      public void onClick(final AjaxRequestTarget target)
      {
        AjaxIconLinkPanel.this.onClick(target);
      }
    });
  }

  protected void onClick(final AjaxRequestTarget target)
  {

  }

}
