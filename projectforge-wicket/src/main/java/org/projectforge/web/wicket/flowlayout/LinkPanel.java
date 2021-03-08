/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.framework.utils.ReflectionHelper;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredPage;

/**
 * Created by vit on 20.12.16.
 */
public class LinkPanel extends Panel
{

  private final Link link;

  public LinkPanel(final String id, final String linkName, final Class<? extends WebPage> editClass,
      final WebPage returnToPage)
  {
    this(id, linkName, editClass, returnToPage, new PageParameters());
  }

  public LinkPanel(final String id, final String linkName, final Class<? extends WebPage> editClass,
      final WebPage returnToPage, final PageParameters pageParameters)
  {
    super(id);

    link = new Link<String>("link")
    {
      @Override
      public void onClick()
      {
        LinkPanel.this.onClick();
        final AbstractSecuredPage editPage = (AbstractSecuredPage) ReflectionHelper.newInstance(editClass, PageParameters.class,
            pageParameters);
        if (editPage instanceof AbstractEditPage) {
          ((AbstractEditPage<?, ?, ?>) editPage).setReturnToPage(returnToPage);
        }
        setResponsePage(editPage);
      }
    };
    add(link);

    link.add(new Label("label", linkName));
  }

  public LinkPanel(final String id, final String linkName, final Class<? extends WebPage> editClass,
                   final Class<? extends IRequestablePage> returnToPage, final PageParameters pageParameters)
  {
    super(id);

    link = new Link<String>("link")
    {
      @Override
      public void onClick()
      {
        LinkPanel.this.onClick();
        final AbstractSecuredPage editPage = (AbstractSecuredPage) ReflectionHelper.newInstance(editClass, PageParameters.class,
                pageParameters);
        if (editPage instanceof AbstractEditPage) {
          ((AbstractEditPage<?, ?, ?>) editPage).setReturnToPage(returnToPage, pageParameters);
        }
        setResponsePage(editPage);
      }
    };
    add(link);

    link.add(new Label("label", linkName));
  }

  public void onClick() {
  }

  public void addLinkAttribute(String attribute, String value)
  {
    link.add(new AttributeAppender(attribute, value));
  }
}
