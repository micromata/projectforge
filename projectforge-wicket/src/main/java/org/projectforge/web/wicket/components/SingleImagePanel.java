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

import org.apache.wicket.markup.html.panel.Panel;
import org.projectforge.web.wicket.PresizedImage;

/**
 * Panel containing only one image (ContextImage). Can be used for creating dynamic elements (e. g. inside a repeating view). Please use
 * PresizedImage or TooltipImage.
 * <br/>
 * This component calls setRenderBodyOnly(true). If the outer html element is needed, please call setRenderBodyOnly(false).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class SingleImagePanel extends Panel
{
  private static final long serialVersionUID = 886760607697490355L;

  private SingleImagePanel(final String id)
  {
    super(id);
    setRenderBodyOnly(true);
  }

  public static SingleImagePanel createTooltipImage(final String id, final String imageUrl, final String tooltip)
  {
    final SingleImagePanel panel = new SingleImagePanel(id);
    final PresizedImage image = new TooltipImage("image", imageUrl, tooltip);
    panel.add(image);
    return panel;
  }

  public static SingleImagePanel createPresizedImage(final String id, final String imageUrl)
  {
    final SingleImagePanel panel = new SingleImagePanel(id);
    final PresizedImage image = new PresizedImage("image", imageUrl);
    panel.add(image);
    return panel;
  }
}
