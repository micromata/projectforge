/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package net.ftlines.wicket.fullcalendar;

import java.io.Serializable;

public class ButtonText implements Serializable {
	private String prev, next, prevYear, nextYear, today, month, week, day;

	public String getPrev() {
		return prev;
	}

	public ButtonText setPrev(String prev) {
		this.prev = prev;
		return this;
	}

	public String getNext() {
		return next;
	}

	public ButtonText setNext(String next) {
		this.next = next;
		return this;
	}

	public String getPrevYear() {
		return prevYear;
	}

	public ButtonText setPrevYear(String prevYear) {
		this.prevYear = prevYear;
		return this;
	}

	public String getNextYear() {
		return nextYear;
	}

	public ButtonText setNextYear(String nextYear) {
		this.nextYear = nextYear;
		return this;
	}

	public String getToday() {
		return today;
	}

	public ButtonText setToday(String today) {
		this.today = today;
		return this;
	}

	public String getMonth() {
		return month;
	}

	public ButtonText setMonth(String month) {
		this.month = month;
		return this;
	}

	public String getWeek() {
		return week;
	}

	public ButtonText setWeek(String week) {
		this.week = week;
		return this;
	}

	public String getDay() {
		return day;
	}

	public ButtonText setDay(String day) {
		this.day = day;
		return this;
	}

}
