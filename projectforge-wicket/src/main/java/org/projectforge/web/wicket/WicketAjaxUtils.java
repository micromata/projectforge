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

package org.projectforge.web.wicket;

public class WicketAjaxUtils
{
  public static String insertBefore(final String containerMarkupId, final String insertBeforeElementMarkupId, final String htmlTag,
      final String newElementMarkupId)
  {
    return String.format("var item=document.createElement('%s');item.id='%s'; Wicket.$('%s').insertBefore(item, Wicket.$('%s'));", htmlTag,
        newElementMarkupId, containerMarkupId, insertBeforeElementMarkupId);
  }

  public static String appendChild(final String containerMarkupId, final String htmlTag, final String newElementMarkupId)
  {
    return String.format("var item=document.createElement('%s');item.id='%s'; Wicket.$('%s').appendChild(item);", htmlTag,
        newElementMarkupId, containerMarkupId);
  }

  public static String replaceChild(final String containerMarkupId, final String elementMarkupId, final String htmlTag,
      final String newElementMarkupId)
  {
    return String.format("var item=document.createElement('%s');item.id='%s'; Wicket.$('%s').replaceChild(item, Wicket.$('%s'));", htmlTag,
        newElementMarkupId, containerMarkupId, elementMarkupId);
  }

  /**
   * @param containerMarkupId The container which contains the element to remove.
   * @param markupId The id of the element to remove.
   * @return
   */
  public static String removeChild(final String containerMarkupId, final String markupId)
  {
    return String.format("var item=document.getElementById('%s');Wicket.$('%s').removeChild(item);", markupId, containerMarkupId);
  }
}
