package org.projectforge.web.common;

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

import org.apache.wicket.request.cycle.RequestCycle;
import org.projectforge.business.utils.HtmlTagBuilder;
import org.projectforge.web.HtmlAlignment;
import org.projectforge.web.wicket.WicketUtils;

public class WicketHtmlHelper
{
  public static void appendImageTag(final RequestCycle requestCycle, final StringBuffer buf, final String src, final String tooltip)
  {
    appendImageTag(requestCycle, buf, src, null, null, tooltip, null);
  }

  /**
   * For the source the URL will be build via buildUrl();
   *
   * @param buf
   * @param src
   * @param width   If less than zero, than this attribute will be ignored.
   * @param height  If less than zero, than this attribute will be ignored.
   * @param tooltip If null, than this attribute will be ignored.
   * @param align   If null, than this attribute will be ignored.
   */
  public static void appendImageTag(final RequestCycle requestCycle, final StringBuffer buf, final String src, final String width, final String height,
      final String tooltip, final HtmlAlignment align)
  {

    final HtmlTagBuilder tag = new HtmlTagBuilder(buf, "img");
    tag.addAttribute("src", WicketUtils.getImageUrl(requestCycle, src));
    addTooltip(tag, tooltip);
    tag.addAttribute("width", width);
    tag.addAttribute("height", height);
    tag.addAttribute("border", "0");
    if (align != null) {
      tag.addAttribute("align", align.getString());
    }
    tag.finishEmptyTag();
  }

  private static void addTooltip(final HtmlTagBuilder tag, final String tooltip)
  {
    tag.addAttribute("rel", "mytooltip");
    tag.addAttribute("data-original-title", tooltip);
  }

}
