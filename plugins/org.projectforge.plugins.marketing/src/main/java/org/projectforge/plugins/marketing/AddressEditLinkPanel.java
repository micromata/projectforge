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

package org.projectforge.plugins.marketing;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.address.AddressDO;
import org.projectforge.web.address.AddressEditPage;
import org.projectforge.web.wicket.AbstractEditPage;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class AddressEditLinkPanel extends Panel
{
  private static final long serialVersionUID = -5300335230191404832L;

  @SuppressWarnings("serial")
  public AddressEditLinkPanel(final String id, final WebPage returnToPage, final AddressDO address,
      final String addressText)
  {
    super(id);
    add(new Link<Object>("link")
    {
      @Override
      public void onClick()
      {
        final PageParameters parameters = new PageParameters();
        parameters.add(AbstractEditPage.PARAMETER_KEY_ID, address.getId());
        final AddressEditPage editPage = new AddressEditPage(parameters);
        editPage.setReturnToPage(returnToPage);
        setResponsePage(editPage);
      }
    }.add(new Label("label", addressText).setRenderBodyOnly(true)));
  }
}
