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

package org.projectforge.start;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * For reading a system console input (with timeout).
 */
public class ConsoleTimeoutReader {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConsoleTimeoutReader.class);

  private static final int CONSOLE_INPUT_TIMEOUT = 120;

  private int timeOutSeconds = CONSOLE_INPUT_TIMEOUT;

  private String question;

  private String defaultAnswer;

  private InputStreamReader streamReader = new InputStreamReader(System.in);
  private BufferedReader bufferedReader = new BufferedReader(streamReader);

  public ConsoleTimeoutReader(String question) {
    this(question, null);
  }

  /**
   * @param question
   * @param defaultAnswer The answer if the user hits simply return.
   */
  public ConsoleTimeoutReader(String question, String defaultAnswer) {
    this.question = question;
    this.defaultAnswer = defaultAnswer;
  }

  /**
   * @return this for chaining.
   */
  public ConsoleTimeoutReader setTimeOutSeconds(int timeOutSeconds) {
    this.timeOutSeconds = timeOutSeconds;
    return this;
  }

  public String ask() {
    log.info("ProjectForge is waiting " + timeOutSeconds + " seconds for your input on console (if running without console, ProjectForge isn't able to proceed): " + question);
    System.out.println();
    System.out.println(StringUtils.center(" QUESTION ", 120, "?"));
    System.out.println();
    String answer = null;
    do {
      answer = readConsoleAnswerWithTimeout();
      if (answer == null)
        return null;
      String result;
      if (defaultAnswer != null && answer.length() == 0) {
        result = defaultAnswer;
      } else {
        result = answer.trim().toLowerCase();
      }
      if (answerValid(result))
        return result;
    } while (true);
  }

  /**
   * If the user hits simply return, the parameter answer will be the configured default answer or an empty string,
   * if no default value is defined.
   *
   * @param answer Answer given from console input ("" or input to lower case).
   * @return true if answer starts with 'y' or 'n', otherwise false.
   */
  protected boolean answerValid(String answer) {
    return answer.startsWith("y") || answer.startsWith("n");
  }

  public String getDefaultAnswer() {
    return defaultAnswer;
  }

  private String readConsoleAnswerWithTimeout() {

    final Duration timeout = Duration.ofSeconds(timeOutSeconds);
    ExecutorService executor = Executors.newSingleThreadExecutor();

    final Future<String> handler = executor.submit(new Callable<String>() {
      @Override
      public String call() throws Exception {
        return readConsoleAnswer(question);
      }
    });
    String answer = null;
    try {
      answer = handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException | InterruptedException | ExecutionException ex) {
      log.info("Timeout of console input exceed (>" + timeOutSeconds + "s). Aborting.");
      handler.cancel(true);
    }
    executor.shutdownNow();
    return answer;
  }


  private String readConsoleAnswer(String question) throws IOException {
    System.out.println(question);
    String answer = null;
    answer = bufferedReader.readLine();
    return answer != null ? answer : "";
  }

  public static void main(String[] args) {
    System.out.println("The answer is: " + new ConsoleTimeoutReader("Question").setTimeOutSeconds(5).ask());
    System.exit(0);
  }
}
