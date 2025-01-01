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

package org.projectforge.business.teamcal.event.ical.converter;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterBuilder;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import org.projectforge.business.teamcal.event.ical.VEventComponentConverter;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

public abstract class PropertyConverter implements VEventComponentConverter {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PropertyConverter.class);

    public Property toVEvent(final TeamEventDO event) {
        return null;
    }

    @Override
    public boolean toVEvent(TeamEventDO event, VEvent vEvent) {
        Property property = this.toVEvent(event);

        if (property == null) {
            return false;
        }

        vEvent.getProperties().add(property);

        return true;
    }

    protected boolean isAllDay(final VEvent vEvent) {
        if (vEvent.getDateTimeStart().isEmpty()) {
            return false;
        }
        DtStart<?> dtStart = vEvent.getDateTimeStart().get();
        return dtStart.toString().contains("VALUE=DATE");
    }

    protected void parseAdditionalParameters(final ParameterList list, final String additonalParams) {
        if (list == null || additonalParams == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        char[] chars = additonalParams.toCharArray();
        String name = null;

        for (char c : chars) {
            switch (c) {
                case ';':
                    if (!escaped && name != null && sb.length() > 0) {
                        try {
                            Parameter parameter = new ParameterBuilder().name(name).value(sb.toString().replaceAll("\"", "")).build();
                            list.add(parameter);
                        } catch (Exception e) {
                            log.error("Error while parsing additional parameters: " + e.getMessage(), e);
                        }
                        name = null;
                        sb.setLength(0);
                    }
                    break;
                case '"':
                    escaped = (!escaped);
                    break;
                case '=':
                    if (!escaped && sb.length() > 0) {
                        name = sb.toString();
                        sb.setLength(0);
                    }
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }

        if (!escaped && name != null && sb.length() > 0) {
            try {
                Parameter parameter = new ParameterBuilder().name(name).value(sb.toString().replaceAll("\"", "")).build();
                list.add(parameter);
            } catch (Exception e) {
                log.error("Error while parsing additional parameters: " + e.getMessage(), e);
            }
        }
    }
}
