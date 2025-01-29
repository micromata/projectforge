/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.apache.wicket.behavior.Behavior;

/**
 * Styles as behavior for TextPanels.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class TextStyle
{
  public static final Behavior RED = AttributeModifier.append("style", "color: #DA202C;");

  public static final Behavior BLUE = AttributeModifier.append("style", "color: blue;");

  public static final Behavior PURPLE = AttributeModifier.append("style", "color: purple;");

  public static final Behavior FORM_TEXT = AttributeModifier.append("class", "text");
}
