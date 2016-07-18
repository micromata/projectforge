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

package org.projectforge.web.teamcal.event.importics;

import java.io.StringReader;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.projectforge.web.wicket.components.DropFileContainer;

/**
 * Adaption of {@link DropFileContainer} for dropped {@link Calendar} files.
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * 
 */
public abstract class DropIcsPanel extends DropFileContainer
{
  private static final long serialVersionUID = 1094945928912822172L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DropIcsPanel.class);

  /**
   * @param id
   */
  public DropIcsPanel(final String id)
  {
    super(id, "text/calendar");
  }

  /**
   * @see org.projectforge.web.wicket.components.DropFileContainer#onStringImport(org.apache.wicket.ajax.AjaxRequestTarget,
   *      java.lang.String, java.lang.String)
   */
  @Override
  protected void onStringImport(final AjaxRequestTarget target, final String fileName, final String content)
  {

    try {
      final CalendarBuilder builder = new CalendarBuilder();
      final Calendar calendar = builder.build(new StringReader(content));
      onIcsImport(target, calendar);
    } catch (final Exception ex) {
      // TODO ju: handle exception
      log.fatal("unable to import dropped calendar", ex);
    }
  }

  protected abstract void onIcsImport(final AjaxRequestTarget target, final Calendar calendar);
}
