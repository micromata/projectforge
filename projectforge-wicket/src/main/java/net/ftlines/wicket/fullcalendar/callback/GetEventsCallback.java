/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.ftlines.wicket.fullcalendar.callback;

import net.ftlines.wicket.fullcalendar.EventProvider;
import net.ftlines.wicket.fullcalendar.EventSource;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.collections.MicroMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class GetEventsCallback extends AbstractCallback {
	private static final String SOURCE_ID = "sid";

	public String getUrl(EventSource source) {
		return getUrl(new MicroMap<String, Object>(SOURCE_ID, source.getUuid()));
	}

	@Override
	protected void respond() {
		Request r = getCalendar().getRequest();

		String sid = r.getRequestParameters().getParameterValue(SOURCE_ID).toString();
		DateTime start = new DateTime(r.getRequestParameters().getParameterValue("start").toLong());
		DateTime end = new DateTime(r.getRequestParameters().getParameterValue("end").toLong());

		if (getCalendar().getConfig().isIgnoreTimezone()) {
			// Convert to same DateTime in local time zone.
			int remoteOffset = -r.getRequestParameters().getParameterValue("timezoneOffset").toInt();
			int localOffset = DateTimeZone.getDefault().getOffset(null) / 60000;
			int minutesAdjustment = remoteOffset - localOffset;
			start = start.plusMinutes(minutesAdjustment);
			end = end.plusMinutes(minutesAdjustment);
		}
		EventSource source = getCalendar().getEventManager().getEventSource(sid);
		EventProvider provider = source.getEventProvider();
		String response = getCalendar().toJson(provider.getEvents(start, end));

		getCalendar().getRequestCycle().scheduleRequestHandlerAfterCurrent(
			new TextRequestHandler("application/json", "UTF-8", response));

	}
}
