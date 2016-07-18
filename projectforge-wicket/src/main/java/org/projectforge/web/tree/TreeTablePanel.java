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

package org.projectforge.web.tree;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * The table component which uses the TreeTable implementation should implement this interface for receiving events from the TreeIconsActionPanel.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public interface TreeTablePanel
{
  /**
   * This method will be called by the TreeIconsActionPanel always for nodes which the user has opened, closed or explored. So the parent
   * page can highlight the corresponding row.
   */
  public void setEventNode(Serializable hashId);
  
  /**
   * This method will be called by the TreeIconsActionPanel always for nodes which the user has opened, closed or explored. So the parent
   * page can highlight the corresponding row.
   */
  public void setEvent(AjaxRequestTarget target, TreeTableEvent event, TreeTableNode node);
  
  /**
   * @return the event node which was previously set or null if not exists.
   */
  public Serializable getEventNode();

  /**
   * Used by the TreeIconsActionPanel to get the full path of the icons.
   * @param image filename of the icon to show (without any path information).
   * @return
   */
  public String getImageUrl(String image);
}
