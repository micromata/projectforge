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

package org.projectforge.web.wicket.components;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * A simple &lt;a href="..."&gt;label&lt;a&gt;.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class ExternalLinkPanel extends Panel
{
  private static final long serialVersionUID = -7477514367748357957L;

  private ExternalLink link;

  public ExternalLinkPanel(final String id, final String href, final String label)
  {
    super(id);
    add(link = new ExternalLink("link", href, label));
  }

  public ExternalLinkPanel(final String id, final String href, final String label, final String target)
  {
    super(id);
    add(link = new ExternalLink("link", href, label) {
      @Override
      protected void onComponentTag(ComponentTag tag)
      {
        super.onComponentTag(tag);
        tag.put("target", target);
      }
    });
  }

  public ExternalLink getLink()
  {
    return link;
  }
}
