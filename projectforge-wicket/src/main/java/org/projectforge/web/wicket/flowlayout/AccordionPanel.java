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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;

/**
 * Represents one or more accordions.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class AccordionPanel extends Panel
{
  public static final String WICKET_ID = "textarea";

  public static final String CONTENT_WICKET_ID = "content";

  private static final long serialVersionUID = -4126462093466172226L;

  private WebMarkupContainer ul;

  private RepeatingView liRepeater;

  public AccordionPanel(final String id)
  {
    super(id);
    add(this.ul = new WebMarkupContainer("ul"));
    this.ul.add(AttributeModifier.append("class", "no_rearrange"));
    ul.add(liRepeater = new RepeatingView("li"));
  }

  /**
   * @param heading
   * @param body
   * @return this for chaining.
   */
  public AccordionPanel addAccordion(final String heading, final Component body) {
    final WebMarkupContainer accordion = new WebMarkupContainer(liRepeater.newChildId());
    liRepeater.add(accordion);
    accordion.add(new Label("heading", heading));
    accordion.add(body);
    return this;
  }
}
