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
  private int innerLength;
  private LogLevel logLevel = LogLevel.INFO;
  private boolean started = false;

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
        this.logLevel = LogLevel.WARN;
        break;
      default:
        this.number = 2;
    }
    innerLength = CONSOLE_LENGTH - 2 * number;
  }

  public enum Priority {HIGH, IMPORTANT, VERY_IMPORTANT}

  public enum Alignment {CENTER, LEFT}

  public enum LogLevel {ERROR, WARN, INFO}

  private static final int CONSOLE_LENGTH = 120;

  private void ensureStart() {
    if (!started) {
      started = true;
      logStartSeparator();
    }
  }

  /**
   * @return this for chaining.
   */
  public LoggerSupport setLogLevel(LogLevel logLevel) {
    this.logLevel = logLevel;
    return this;
  }

  /**
   * @return this for chaining.
   */
  private LoggerSupport logStartSeparator() {
    for (int i = 0; i < number; i++) {
      logSeparatorLine();
    }
    return log("");
  }

  /**
   * @return this for chaining.
   */
  public LoggerSupport logEnd() {
    ensureStart();
    log("");
    for (int i = 0; i < number; i++) {
      logSeparatorLine();
    }
    return this;
  }

  private void logSeparatorLine() {
    logLine(StringUtils.rightPad("", innerLength, "*") + asterisks(number * 2 + 2));
  }

  public LoggerSupport log(String text) {
    ensureStart();
    if (StringUtils.contains(text, "\n")) {
      for (String line : StringUtils.splitPreserveAllTokens(text, '\n')) {
        logLineText(line);
      }
    } else {
      logLineText(text);
    }
    return this;
  }

  private void logLineText(String line) {
    String padText = alignment == Alignment.LEFT ? StringUtils.rightPad(line, innerLength)
            : StringUtils.center(line, innerLength);
    logLine(asterisks(number) + " " + padText + " " + asterisks(number));
  }

  private void logLine(String msg) {
    if (logLevel == LogLevel.ERROR)
      log.error(msg);
    else if (logLevel == LogLevel.WARN)
      log.warn(msg);
    else
      log.info(msg);
  }

  private static String asterisks(int number) {
    return StringUtils.rightPad("*", number, '*');
  }
}
