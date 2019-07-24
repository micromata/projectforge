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

package org.projectforge.common;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper for logging very important information and warnings.
 */
public class LoggerSupport {
  private org.slf4j.Logger log;
  private Alignment alignment;
  private int number;

  public LoggerSupport(org.slf4j.Logger log) {
    this(log, Priority.IMPORTANT);
  }

  public LoggerSupport(org.slf4j.Logger log, Priority priority) {
    this(log, priority, Alignment.CENTER);
  }

  public LoggerSupport(org.slf4j.Logger log, Alignment alignment) {
    this(log, Priority.IMPORTANT, alignment);
  }

  public LoggerSupport(org.slf4j.Logger log, Priority priority, Alignment alignment) {
    this.log = log;
    this.alignment = alignment;
    switch (priority) {
      case HIGH:
        this.number = 1;
        break;
      case VERY_IMPORTANT:
        this.number = 5;
        break;
      default:
        this.number = 2;
    }
    logStartSeparator();
  }


  public enum Priority {HIGH, IMPORTANT, VERY_IMPORTANT}

  public enum Alignment {CENTER, LEFT}

  private static final int CONSOLE_LENGTH = 80;

  /**
   * @return this for chaining.
   */
  private LoggerSupport logStartSeparator() {
    for (int i = 0; i < number; i++) {
      log.info(StringUtils.rightPad("", CONSOLE_LENGTH, "*") + asterisks(number * 2 + 2));
    }
    return log("");
  }

  /**
   * @return this for chaining.
   */
  public LoggerSupport logEnd() {
    log("");
    for (int i = 0; i < number; i++) {
      log.info(StringUtils.rightPad("", CONSOLE_LENGTH, "*") + asterisks(number * 2 + 2));
    }
    return this;
  }

  public LoggerSupport log(String text) {
    String padText = alignment == Alignment.LEFT ? StringUtils.rightPad(text, CONSOLE_LENGTH)
            : StringUtils.center(text, CONSOLE_LENGTH);
    log.info(asterisks(number) + " " + padText + " " + asterisks(number));
    return this;
  }

  private static String asterisks(int number) {
    return StringUtils.rightPad("*", number, '*');
  }
}
