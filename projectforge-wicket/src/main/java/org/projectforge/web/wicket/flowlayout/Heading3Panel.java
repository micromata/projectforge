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

import org.apache.wicket.model.IModel;


/**
 * Represents &lt;h1&gt;heading&lt;/h1&gt;.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class Heading3Panel extends HeadingPanel
{
  public Heading3Panel(final String id, final String heading)
  {
    super(id, heading);
  }

  public Heading3Panel(final String id, final IModel<String> heading)
  {
    super(id, heading);
  }
}
