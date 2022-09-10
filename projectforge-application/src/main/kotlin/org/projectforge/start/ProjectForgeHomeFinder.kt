/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.start

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.SystemUtils
import org.projectforge.ProjectForgeApp
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.setup.ProjectForgeInitializer.initialize
import org.projectforge.setup.SetupContext
import org.projectforge.setup.SetupData
import org.projectforge.setup.wizard.lanterna.LantSetupWizard.Companion.run
import org.projectforge.setup.wizard.swing.SwingSetupWizard.Companion.run
import java.awt.GraphicsEnvironment
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.prefs.Preferences

private val log = KotlinLogging.logger {}

/**
 * Helper for finding ProjectForge's home directory:<br></br>
 *
 *  1. Create ProjectForge as a top level directory of your home directory: '$HOME/ProjectForge', or
 *  1. create a directory and put the jar file somewhere inside this directory. ProjectForge detects the folder relative to the executed jar, or
 *  1. create a directory and define it as command line parameter: java -Dhome.dir=yourdirectory -jar ..., or
 *  1. create a directory and define it as system environment variable $PROJECTFORGE_HOME.
 *
 */
class ProjectForgeHomeFinder {
  private var userAcceptsGraphicalTerminal: WizardMode? = null

  private enum class WizardMode { DESKTOP, CONSOLE, NONE }

  /**
   * Tries to find ProjectForge's home dir. If not found or isn't initialized, a setup wizard is started.
   *
   * @return The home dir. If not found, a System.exit() is done and user information are shown on how to proceed.
   */
  fun findAndEnsureAppHomeDir(): File? {
    // Try directory defined through command line: -Dhome.dir:
    var appHomeDir = proceedForced(
      System.getProperty(COMMAND_LINE_VAR_HOME_DIR),
      "ProjectForge's home dir is defined as command line param, but isn't yet initialized: -D"
          + COMMAND_LINE_VAR_HOME_DIR + "=\$APP_HOME_DIR"
    )
    if (appHomeDir != null) return appHomeDir

    // Try directory defined through command line: -Dprojectforge.base.dir:
    appHomeDir = proceedForced(
      System.getProperty(ProjectForgeApp.CONFIG_PARAM_BASE_DIR),
      "ProjectForge's home dir is defined as command line param, but isn't yet initialized: -D"
          + ProjectForgeApp.CONFIG_PARAM_BASE_DIR + "=\$APP_HOME_DIR"
    )
    if (appHomeDir != null) return appHomeDir

    // Try directory defined through environment variable:
    appHomeDir = proceedForced(
      System.getenv(ENV_PROJECTFORGE_HOME),
      "ProjectForge's home dir is defined as system environment variable $" + ENV_PROJECTFORGE_HOME + ": \$APP_HOME_DIR"
    )
    if (appHomeDir != null) return appHomeDir

    // Try directory defined through user preferences variable:
    val prefHomeDir = userPrefHomeDir
    if (prefHomeDir != null) {
      appHomeDir = proceed(
        File(prefHomeDir),
        "ProjectForge's home dir is defined as java user preference " + ENV_PROJECTFORGE_HOME + ": \$APP_HOME_DIR",
        false
      )
      if (appHomeDir != null) return appHomeDir
    }

    // Try directory where the executable jar resides:
    appHomeDir = searchAndProceed(
      File(System.getProperty("user.home")),
      "ProjectForge's home directory found in the user's home dir: \$APP_HOME_DIR",
      false
    )
    if (appHomeDir != null) return appHomeDir

    // Try directory where the executable jar resides:
    appHomeDir = searchAndProceed(
      getExecutableDir(true),
      "ProjectForge's home directory found relative to executable jar: \$APP_HOME_DIR",
      false
    )
    if (appHomeDir != null) return appHomeDir

    // Try current directory (launch wizard, because it's the last chance to do it):
    appHomeDir = searchAndProceed(
      File("."),
      """ProjectForge's home directory not found, searched for:
  ${StringUtils.join(suggestedDirectories, "\n  ")}""",
      true
    )
    if (appHomeDir != null) return appHomeDir
    ProjectForgeApplication.giveUpAndSystemExit("No home directory of ProjectForge found or configured, giving up :-(")
    return null // unreachable, because SystemExit() was called before.
  }

  /**
   *
   *  1.
   * If the given appHomeDir is null or blank, null is returned.
   *
   *  1.
   * Checks if the given appHomeDir is already given. If so, the appHomeDir is returned as File object and ProjectForge
   * should continue the start-up phase with this directory.
   *
   *  1.
   * If given appHomeDir doesn't exist or isn't already initialized, the setup wizard and installation is started.
   * If aborted by the user or any failure occurs, System.exit(1) is called, otherwise the file with the new installed
   * home directory is returned.
   *
   *
   */
  private fun proceedForced(appHomeDir: String?, logMessage: String): File? {
    return if (StringUtils.isNotBlank(appHomeDir)) {
      proceed(File(appHomeDir), logMessage, true)
    } else null
  }

  private fun searchAndProceed(parentDir: File?, logMessage: String, forceDirectory: Boolean): File? {
    var appHomeDir = findBaseDirAndAncestors(parentDir)
    if (appHomeDir == null) {
      appHomeDir = File(parentDir, "ProjectForge")
    }
    return proceed(appHomeDir, logMessage, forceDirectory)
  }

  private fun proceed(paramAppHomeDir: File, logMessage: String, forceDirectory: Boolean): File? {
    var appHomeDir: File? = paramAppHomeDir
    if (appHomeDir != null) {
      if (isProjectForgeConfigured(appHomeDir)) {
        log.info(logMessage.replace("\$APP_HOME_DIR", appHomeDir.path))
        saveUserPrefHomeDir(appHomeDir)
        return appHomeDir
      }
      if (!forceDirectory) {
        return null
      }
      EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.NORMAL, EmphasizedLogSupport.Alignment.LEFT)
        .log(logMessage.replace("\$APP_HOME_DIR", appHomeDir.path))
        .logEnd()

      val setupContext = SetupContext()
      if (setupContext.runAsDockerContainer) {
        userAcceptsGraphicalTerminal = WizardMode.CONSOLE
      } else if (userAcceptsGraphicalTerminal == null) {
        val answer = if (!setupContext.graphicModeSupported || GraphicsEnvironment.isHeadless()) {
          ConsoleTimeoutReader("Do you want to enter the setup wizard (Y/n)?", "y")
            .ask()
        } else {
          object : ConsoleTimeoutReader(
            "Do you want to enter the setup wizard? (1), 2, 3\n *(1) Graphical wizard (default)\n  (2) Console based wizard\n  (3) Abort",
            "1"
          ) {
            override fun answerValid(answer: String): Boolean {
              return StringUtils.equalsAny(answer, "1", "2", "3")
            }
          }.ask()
        }
        userAcceptsGraphicalTerminal =
          if (StringUtils.startsWithIgnoreCase(answer, "y") || StringUtils.equals(answer, "2")) {
            WizardMode.CONSOLE
          } else if (StringUtils.equals(answer, "1")) {
            WizardMode.DESKTOP
          } else {
            WizardMode.NONE
          }
      }
      if (userAcceptsGraphicalTerminal != WizardMode.NONE) {
        try {
          val setupData: SetupData? = if (userAcceptsGraphicalTerminal == WizardMode.CONSOLE) {
            run(appHomeDir, setupContext)
          } else {
            run(appHomeDir)
          }
          appHomeDir = initialize(setupData)
          saveUserPrefHomeDir(appHomeDir!!)
          return appHomeDir
        } catch (ex: Exception) {
          log.error("Error while initializing new ProjectForge home: " + ex.message, ex)
          ProjectForgeApplication.giveUpAndSystemExit(
            "Error while initializing new ProjectForge home: " + CanonicalFileUtils.absolutePath(
              appHomeDir
            )
          )
        }
      } else {
        ProjectForgeApplication.giveUpAndSystemExit(
          "Can't start ProjectForge in specified directory '" + CanonicalFileUtils.absolutePath(
            appHomeDir
          ) + "'."
        )
      }
    }
    return null
  }

  private val userPrefHomeDir: String?
    private get() {
      val preferences = Preferences.userNodeForPackage(this.javaClass)
      return preferences[ENV_PROJECTFORGE_HOME, null]
    }

  private fun saveUserPrefHomeDir(homeDir: File) {
    val preferences = Preferences.userNodeForPackage(this.javaClass)
    val savedHomeDir = preferences[ENV_PROJECTFORGE_HOME, null]
    val absolutePath = homeDir.absolutePath
    if (absolutePath == savedHomeDir) {
      // Value is unchanged, not needed to save.
      return
    }
    log.info("Saving ProjectForge's home dir as user preferences (for next start): $absolutePath")
    preferences.put(ENV_PROJECTFORGE_HOME, absolutePath)
  }

  companion object {
    const val ENV_PROJECTFORGE_HOME = "PROJECTFORGE_HOME"
    const val COMMAND_LINE_VAR_HOME_DIR = "home.dir"
    private val DIR_NAMES = arrayOf("ProjectForge", "Projectforge", "projectforge")

    /**
     * @return %PROJECTFORGE_HOME for Windows, otherwise $PPROJECTFORGE_HOME
     */
    @JvmStatic
    val homeEnvironmentVariableDefinition: String
      get() = (if (SystemUtils.IS_OS_WINDOWS) "%" else "$") + ENV_PROJECTFORGE_HOME
    val suggestedDirectories: Array<File>
      get() {
        val files: MutableList<File> = ArrayList()
        checkAndAdd(files, System.getProperty("user.home"))
        checkAndAdd(files, File(getExecutableDir(true), "ProjectForge"))
        checkAndAdd(files, File("ProjectForge"))
        return files.toTypedArray()
      }

    private fun checkAndAdd(files: MutableList<File>, path: String) {
      checkAndAdd(files, CanonicalFileUtils.absolute(File(path, "ProjectForge")))
    }

    private fun checkAndAdd(files: MutableList<File>, dir: File) {
      if (isProjectForgeSourceCodeRepository(dir)) return
      val canonicalDir = CanonicalFileUtils.absolute(dir)
      if (!files.contains(canonicalDir)) files.add(canonicalDir)
    }

    private fun getExecutableDir(includingSourceCodeRepository: Boolean): File? {
      return try {
        val locationUrl = ProjectForgeHomeFinder::class.java.protectionDomain.codeSource.location
        var location = locationUrl.toExternalForm()
        if (location.startsWith("jar:")) {
          location = location.substring(4)
        } else if (!includingSourceCodeRepository) {
          // Development source code, don't use the ProjectForge source code repository as working directory directly:
          return null
        }
        if (location.indexOf('!') > 0) {
          location = location.substring(0, location.indexOf('!'))
        }
        var file: File? = File(URI(location))
        for (i in 0..99) { // Paranoi counter for endless loops (circular file system links)
          if (file == null) {
            return null
          }
          if (file.exists() && file.isDirectory) {
            return file
          }
          file = file.parentFile
        }
        null
      } catch (ex: URISyntaxException) {
        log.error("Internal error while trying to get the location of ProjectForge's running code: " + ex.message, ex)
        null
      }
    }

    @JvmStatic
    fun findBaseDirAndAncestors(baseDir: File?): File? {
      if (baseDir == null) return null
      // Need absolute directory to check parent directories.
      var currentDir = CanonicalFileUtils.absolute(baseDir) // absolute needed for getting the parent file.
      var recursiveCounter = 100 // Soft links may result in endless loops.
      do {
        val dir = findBaseDirOnly(currentDir)
        if (dir != null) {
          return dir
        }
        currentDir = currentDir!!.parentFile
      } while (currentDir != null && --recursiveCounter > 0)
      return null
    }

    private fun findBaseDirOnly(baseDir: File?): File? {
      if (!baseDir!!.exists() || !baseDir.isDirectory) {
        return null
      }
      for (path in DIR_NAMES) {
        val dir = File(baseDir, path)
        if (checkDirectory(dir, false)) return dir
      }
      return null
    }

    /**
     * @param baseDir
     * @param logWarning
     * @return true, if the given baseDir exists and is a directory, but not the source code repository root dir.
     */
    fun checkDirectory(baseDir: File, logWarning: Boolean): Boolean {
      if (!baseDir.exists()) {
        if (logWarning) log.warn("Configured base dir '" + CanonicalFileUtils.absolutePath(baseDir) + "' doesn't exist. Ignoring it.")
        return false
      }
      if (!baseDir.isDirectory) {
        if (logWarning) log.warn("Configured base dir '" + CanonicalFileUtils.absolutePath(baseDir) + "' is not a directory. Ignoring it.") else log.warn(
          "'" + CanonicalFileUtils.absolutePath(baseDir) + "' found, but isn't a directory, ignoring..."
        )
        return false
      }
      // Check for ProjectForge as source code repository:
      if (isProjectForgeSourceCodeRepository(baseDir)) {
        if (logWarning) {
          log.warn("Configured base dir '" + CanonicalFileUtils.absolutePath(baseDir) + "' seems to be the source code repository and shouldn't be used. Ignoring it.")
        }
        return false
      }
      return true
    }

    /**
     * @return true only and only if the given dir represents the directory $HOME/ProjectPorge or $HOME/projectforge (case insensitive).
     */
    fun isStandardProjectForgeUserDir(dir: File): Boolean {
      val parent = dir.parentFile ?: return false
      try {
        if (parent.canonicalPath != File(System.getProperty("user.home")).canonicalPath) {
          return false
        }
      } catch (ex: IOException) {
        return false
      }
      return "projectforge" == dir.name.lowercase()
    }

    /**
     * @return true, if the given dir is the root directory of the source code repository, false otherwise, if the given
     * dir is a sub directory of the source code repository or any other directory.
     */
    fun isProjectForgeSourceCodeRepository(dir: File): Boolean {
      //int recursiveCounter = 100; // Soft links may result in endless loops.
      //do {
      return (dir.exists()
          && File(dir, "projectforge-application").exists()
          && File(dir, "projectforge-business").exists()
          && File(dir, "projectforge-common").exists())
      //   current = current.getParentFile();
      // } while (current != null && --recursiveCounter > 0);
    }

    /**
     * @return true, if the directory exists and contains projectforge.properties.
     */
    fun isProjectForgeConfigured(dir: String?): Boolean {
      return StringUtils.isNotBlank(dir) && isProjectForgeConfigured(File(dir))
    }

    /**
     * @return true, if the directory exists and contains projectforge.properties.
     */
    fun isProjectForgeConfigured(dir: File?): Boolean {
      return dir != null && dir.exists() && File(dir, "projectforge.properties").exists()
    }
  }
}
