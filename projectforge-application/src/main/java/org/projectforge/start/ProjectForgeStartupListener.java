/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.ProjectForgeApp;
import org.projectforge.ProjectForgeVersion;
import org.projectforge.common.CanonicalFileUtils;
import org.projectforge.common.EmphasizedLogSupport;
import org.projectforge.common.FormatterUtils;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

/**
 * Only for logging some information on a very early start-up phase (logging is initialized).
 */
public class ProjectForgeStartupListener implements ApplicationListener<ApplicationPreparedEvent> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectForgeStartupListener.class);

  private File baseDir;

  ProjectForgeStartupListener(File baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  public void onApplicationEvent(ApplicationPreparedEvent applicationPreparedEvent) {
    log.info("Starting " + ProjectForgeVersion.APP_ID + " " + ProjectForgeVersion.VERSION_NUMBER + ": build date="
        + ProjectForgeVersion.BUILD_TIMESTAMP + ", " + ProjectForgeVersion.SCM + "=" + ProjectForgeVersion.SCM_ID
        + " (" + ProjectForgeVersion.SCM_ID_FULL + ")");

    new EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.NORMAL)
        .log("Using ProjectForge directory: " + CanonicalFileUtils.absolutePath(baseDir))
        .logEnd();

    log.info("Using Java version: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
    log.info("Using Java home   : " + System.getProperty("java.home"));
    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    List<String> arguments = runtimeMxBean.getInputArguments();
    log.info("Using JVM opts    : " + StringUtils.join(arguments, " "));
    log.info("Using classpath   : " + runtimeMxBean.getClassPath());

    checkResource("static/index.html");
    checkResource("static/react-app.html");
    checkResource("static/favicon.ico");
  }

  private void checkResource(final String resourcePath) {
    try (InputStream resourceStream = ProjectForgeApp.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (resourceStream == null) {
        log.error("Oups, can't find " + resourcePath + " in class path. React-Frontend not available?");
      } else {
        byte[] ba = IOUtils.toByteArray(resourceStream);
        log.info(resourcePath + ": " + FormatterUtils.formatBytes(ba.length));
      }
    } catch (IOException ex) {
      log.error("Oups, can't find " + resourcePath + " in class path. React-Frontend not available?");
    }
  }

}
