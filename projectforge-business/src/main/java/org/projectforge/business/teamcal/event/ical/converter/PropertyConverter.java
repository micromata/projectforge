/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal.event.ical.converter;

import java.net.URISyntaxException;

import org.projectforge.business.teamcal.event.ical.VEventComponentConverter;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.ParameterFactoryImpl;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;

public abstract class PropertyConverter implements VEventComponentConverter
{
  public Property toVEvent(final TeamEventDO event)
  {
    return null;
  }

  @Override
  public boolean toVEvent(TeamEventDO event, VEvent vEvent)
  {
    Property property = this.toVEvent(event);

    if (property == null) {
      return false;
    }

    vEvent.getProperties().add(property);

    return true;
  }

  protected boolean isAllDay(final VEvent vEvent)
  {
    final DtStart dtStart = vEvent.getStartDate();
    return dtStart != null && dtStart.getDate() instanceof net.fortuna.ical4j.model.DateTime == false;
  }

  protected void parseAdditionalParameters(final ParameterList list, final String additonalParams)
  {
    if (list == null || additonalParams == null) {
      return;
    }

    ParameterFactoryImpl parameterFactory = ParameterFactoryImpl.getInstance();
    StringBuilder sb = new StringBuilder();
    boolean escaped = false;
    char[] chars = additonalParams.toCharArray();
    String name = null;

    for (char c : chars) {
      switch (c) {
        case ';':
          if (escaped == false && name != null && sb.length() > 0) {
            try {
              list.add(parameterFactory.createParameter(name, sb.toString().replaceAll("\"", "")));
            } catch (URISyntaxException e) {
              // TODO
              e.printStackTrace();
            }
            name = null;
            sb.setLength(0);
          }
          break;
        case '"':
          escaped = (escaped == false);
          break;
        case '=':
          if (escaped == false && sb.length() > 0) {
            name = sb.toString();
            sb.setLength(0);
          }
          break;
        default:
          sb.append(c);
          break;
      }
    }

    if (escaped == false && name != null && sb.length() > 0) {
      try {
        list.add(parameterFactory.createParameter(name, sb.toString().replaceAll("\"", "")));
      } catch (URISyntaxException e) {
        // TODO
        e.printStackTrace();
      }
    }
  }
}
