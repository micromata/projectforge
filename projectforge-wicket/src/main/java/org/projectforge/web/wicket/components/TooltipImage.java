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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.ImageDef;
import org.projectforge.web.wicket.PresizedImage;

public class TooltipImage extends PresizedImage
{
  private static final long serialVersionUID = 1333929048394636569L;

  public TooltipImage(final String id, final String relativePath, final String tooltip)
  {
    super(id, relativePath);
    WicketUtils.addTooltip(this, tooltip).add(AttributeModifier.replace("border", "0"));
  }

  public TooltipImage(final String id, final ImageDef imageDef, final String tooltip)
  {
    super(id, imageDef);
    WicketUtils.addTooltip(this, tooltip).add(AttributeModifier.replace("border", "0"));
  }

  public TooltipImage(final String id, final String relativePath, final IModel<String> tooltip)
  {
    super(id, relativePath);
    WicketUtils.addTooltip(this, tooltip).add(AttributeModifier.replace("border", "0"));
  }

  public TooltipImage(final String id, final ImageDef imageDef, final IModel<String> tooltip)
  {
    super(id, imageDef);
    WicketUtils.addTooltip(this, tooltip).add(AttributeModifier.replace("border", "0"));
  }
}
