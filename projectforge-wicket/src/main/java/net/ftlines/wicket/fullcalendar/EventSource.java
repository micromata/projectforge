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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import net.ftlines.wicket.fullcalendar.selector.EventSourceSelector;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EventSource implements Serializable {

  private static final long serialVersionUID = 8332913729042907959L;
  private String color;
  private String backgroundColor;
  private String borderColor;
  private String textColor;
  private String className;
  private Boolean editable;
  private Boolean allDayDefault;
  private Boolean ignoreTimezone;
  private String error;
  private final Map<String, Object> data = new HashMap<String, Object>();
  private IModel<String> events;

  private EventProvider eventProvider;
  private Boolean enableInSelector = true;
  private Boolean includeInSelector = true;

  public String getColor() {
    return color;
  }

  public EventSource setColor(final String color) {
    this.color = color;
    return this;
  }

  public String getBackgroundColor() {
    return backgroundColor;
  }

  public EventSource setBackgroundColor(final String backgroundColor) {
    this.backgroundColor = backgroundColor;
    return this;
  }

  public String getBorderColor() {
    return borderColor;
  }

  public EventSource setBorderColor(final String borderColor) {
    this.borderColor = borderColor;
    return this;
  }

  public String getTextColor() {
    return textColor;
  }

  public EventSource setTextColor(final String textColor) {
    this.textColor = textColor;
    return this;
  }

  public String getClassName() {
    return className;
  }

  public EventSource setClassName(final String className) {
    this.className = className;
    return this;
  }

  public Boolean isEditable() {
    return editable;
  }

  public EventSource setEditable(final Boolean editable) {
    this.editable = editable;
    return this;
  }

  public Boolean isAllDayDefault() {
    return allDayDefault;
  }

  public EventSource setAllDayDefault(final Boolean allDayDefault) {
    this.allDayDefault = allDayDefault;
    return this;
  }

  public Boolean isIgnoreTimezone() {
    return ignoreTimezone;
  }

  public EventSource setIgnoreTimezone(final Boolean ignoreTimezone) {
    this.ignoreTimezone = ignoreTimezone;
    return this;
  }

  public String getError() {
    return error;
  }

  public EventSource setError(final String error) {
    this.error = error;
    return this;
  }

  @JsonIgnore
  public EventProvider getEventProvider() {
    return eventProvider;
  }

  public EventSource setEventsProvider(final EventProvider eventsProvider) {
    this.eventProvider = eventsProvider;
    return this;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public EventSource setTitle(final String title) {
    data.put(Const.TITLE, title);
    return this;
  }

  @JsonIgnore
  public String getTitle() {
    return (String) data.get(Const.TITLE);
  }

  @JsonIgnore
  public String getUuid() {
    return (String) data.get(Const.UUID);
  }

  public EventSource setUuid(final String uuid) {
    data.put(Const.UUID, uuid);
    return this;
  }

  @JsonRawValue
  public String getEvents() {
    return events == null ? "" : events.getObject();
  }

  void setEventsModel(final IModel<String> events) {
    this.events = events;
  }

  public static class Const {
    public static final String TITLE = "fcxTitle";
    public static final String UUID = "fcxUuid";
  }

  /**
   * If <var>enableInSelector</var> is {@code true} then the check box for this EventSource, if included in a
   * {@link EventSourceSelector} to begin with, will be enabled. If {@code false} then the check box will not be
   * enabled. Default is {@code true}.
   *
   * @param includeInSelector
   *            if {@code true} then the check box for this EventSource will be enabled, otherwise it won't
   */
  public void setEnableInSelector(final boolean enableInSelector) {
    this.enableInSelector = enableInSelector;
  }

  /**
   * Returns {@code true} if this EventSource will be included in a {@link EventSourceSelector}, if one is included
   * for the {@link FullCalendar} containing this EventSource. Returns {@code false} if this EventSource will not be
   * included. Default is {@code true}.
   *
   * @return if {@code true} then this event source will be included in a {@link EventSourceSelector}, otherwise it
   *         won't
   */
  public Boolean getEnableInSelector() {
    return enableInSelector;
  }

  /**
   * If <var>includeInSelector</var> is {@code true} then this EventSource will be included in a
   * {@link EventSourceSelector}, if one exists for the {@link FullCalendar} containing this EventSource. If
   * {@code false} then this EventSource will not be included. Default is {@code true}.
   *
   * @param includeInSelector
   *            if {@code true} then this event source will be included in a {@link EventSourceSelector}, otherwise it
   *            won't
   */
  public void setIncludeInSelector(final boolean includeInSelector) {
    this.includeInSelector = includeInSelector;
  }

  /**
   * Returns {@code true} if this EventSource will be included in a {@link EventSourceSelector}, if one exists for the
   * {@link FullCalendar} containing this EventSource. Returns {@code false} if this EventSource will not be included.
   * Default is {@code true}.
   *
   * @return if {@code true} then this event source will be included in a {@link EventSourceSelector}, otherwise it
   *         won't
   */
  public Boolean getIncludeInSelector() {
    return includeInSelector;
  }

}
