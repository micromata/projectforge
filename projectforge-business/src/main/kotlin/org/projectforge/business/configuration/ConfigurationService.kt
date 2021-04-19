/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.configuration

import de.micromata.genome.util.runtime.config.MailSessionLocalSettingsConfigModel
import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.projectforge.ProjectForgeApp
import org.projectforge.business.meb.MebMailClient
import org.projectforge.business.orga.ContractType
import org.projectforge.framework.configuration.*
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.time.TimeNotation
import org.projectforge.framework.utils.FileHelper
import org.projectforge.mail.SendMailConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.*
import java.nio.file.Paths
import java.security.KeyStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.util.*
import javax.annotation.PostConstruct
import javax.net.ssl.*

private val log = KotlinLogging.logger {}

@Service
open class ConfigurationService {
  private lateinit var configXml: ConfigXml

  @Autowired
  private lateinit var configDao: ConfigurationDao

  @Value("\${projectforge.base.dir}")
  open var applicationHomeDir: String? = null

  /**
   * Resource directory relative to application's home (default 'resources').
   */
  @Value("\${projectforge.resourcesDirectory}")
  var resourceDirName: String? = null
    private set
  var usersSSLSocketFactory: SSLSocketFactory? = null
    private set

  /**
   * For additional certificates you can set the file name of the jssecert file in your ProjectForge home (config)
   * directory (path of your confix.xml). <br></br>
   * If given then the key-store file is used.
   */
  @Value("\${projectforge.keystoreFile}")
  private var keystoreFileName: String? = null

  @Value("\${projectforge.keystorePassphrase}")
  private var keystorePassphrase: String? = null

  @Autowired
  private lateinit var sendMailConfiguration: SendMailConfig

  @Autowired
  private lateinit var mebMailClient: MebMailClient

  /**
   * Default value: "resources/fonts" (absolute path).
   *
   * @return the fontsDirectory
   */
  @Value("\${projectforge.fontsDirectory}")
  var fontsDir: String? = null
    private set

  /**
   * Format http://asterisk.acme.com/originatecall.php?source=#source&amp;target=#target<br></br>
   * #source will be replaced by the current user's phone and #target by the chosen phone number to call.
   */
  @Value("\${projectforge.telephoneSystemUrl}")
  var telephoneSystemUrl: String? = null
    private set


  /**
   * For direct calls all numbers beginning with the this number will be stripped, e. g. for 0561316793: 056131679323 ->
   * 23. So internal calls are supported.
   */
  @Value("\${projectforge.telephoneSystemNumber}")
  var telephoneSystemNumber: String? = null
    private set

  /**
   * The SMS receiver verifies this key given as get parameter to the servlet call. <br></br>
   * The key should be an alpha numeric random value with at least 6 characters for security reasons.
   */
  @Value("\${projectforge.receiveSmsKey}")
  var receiveSmsKey: String? = null
    private set

  /**
   * The reverse phone lookup service verifies the key given as parameter to the servlet call against this key. The key
   * should be an alpha numeric random value with at least 6 characters for security reasons.
   *
   * @return the receivePhoneLookupKey
   */
  @Value("\${projectforge.phoneLookupKey}")
  var phoneLookupKey: String? = null
    private set

  /**
   * @return the securityConfig
   */
  @Autowired
  lateinit var securityConfig: SecurityConfig
    private set

  /**
   * If configured then this logo file is used for displaying at the top of the navigation menu.
   *
   * @return The path of the configured logo (relative to the image dir of the application's resource path, at default:
   * '&lt;app-home&gt;/resources/images').
   */
  @Value("\${projectforge.logoFile}")
  var logoFileName: String? = null
    private set

  /**
   * Default is €
   */
  @Value("\${projectforge.currencySymbol}")
  var currencySymbol: String? = null

  @Value("\${projectforge.defaultLocale}")
  var defaultLocale: Locale? = null

  @Value("\${projectforge.defaultTimeNotation}")
  var defaultTimeNotation: TimeNotation? = null
    private set

  @Value("\${projectforge.defaultFirstDayOfWeek}")
  var defaultFirstDayOfWeek: DayOfWeek? = null

  @Value("\${projectforge.minimalDaysInFirstWeek}")
  var minimalDaysInFirstWeek: Int? = null

  @Value("\${projectforge.excelPaperSize}")
  var excelPaperSize: String? = null
    private set

  @Value("\${projectforge.wicket.developmentMode}")
  private var developmentMode = false

  @Value("\${projectforge.support.mail}")
  var pfSupportMailAddress: String? = null
    private set

  @Value("\${mail.session.pfmailsession.emailEnabled}")
  private var pfmailsessionEmailEnabled: String? = null

  @Value("\${mail.session.pfmailsession.name}")
  private var pfmailsessionName: String? = null

  @Value("\${mail.session.pfmailsession.standardEmailSender}")
  private var pfmailsessionStandardEmailSender: String? = null

  @Value("\${mail.session.pfmailsession.encryption}")
  private var pfmailsessionEncryption: String? = null

  @Value("\${mail.session.pfmailsession.smtp.host}")
  private var pfmailsessionHost: String? = null

  @Value("\${mail.session.pfmailsession.smtp.port}")
  private var pfmailsessionPort: String? = null

  @Value("\${mail.session.pfmailsession.smtp.auth}")
  private var pfmailsessionAuth = false

  @Value("¢{mail.session.pfmailsession.smtp.user}")
  private var pfmailsessionUser: String? = null

  @Value("¢{mail.session.pfmailsession.smtp.password}")
  private var pfmailsessionPassword: String? = null

  @Value("\${pf.config.security.sqlConsoleAvailable:false}")
  var isSqlConsoleAvailable = false
    private set

  @Value("\${pf.config.security.teamCalCryptPassword}")
  var teamCalCryptPassword: String? = null
    private set

  @Value("\${pf.config.compileCss:true}")
  var compileCss = false
    private set

  @Value("\${projectforge.login.handlerClass}")
  var loginHandlerClass: String? = null
    private set

  @Value("\${projectforge.max-file-size.datev}")
  var maxFileSizeDatev: String? = null
    private set

  @Value("\${projectforge.max-file-size.xml-dump-import}")
  open var maxFileSizeXmlDumpImport: String? = null

  var isDAVServicesAvailable = false

  @PostConstruct
  fun init() {
    ConfigurationServiceAccessor.setConfigurationService(this)
    staticApplicationHomeDir = applicationHomeDir
    configXml = ConfigXml(applicationHomeDir)
    if (StringUtils.isBlank(resourceDirName)) {
      resourceDirName = DEFAULT_RESOURCES_DIR
    }
    if (!System.getProperty(ProjectForgeApp.DOCKER_MODE).isNullOrBlank()) {
      // Check environment.sh script
      val workingDir = File(applicationHomeDir)
      val environmentFile = File(workingDir, ENVIRONMENT_FILE)
      if (!environmentFile.exists()) {
        log.info { "Creating environment file for java options (docker): ${environmentFile.absolutePath}" }
        environmentFile.writeText(ENVIRONMENT_FILE_INITIAL_CONTENT)
      }
    }
    resourceDirName = FileHelper.getAbsolutePath(applicationHomeDir, resourceDirName)
    ensureDir(File(resourceDirName))
    if (StringUtils.isBlank(fontsDir)) {
      fontsDir = DEFAULT_FONTS_DIR
    }
    fontsDir = FileHelper.getAbsolutePath(applicationHomeDir, fontsDir)
    ensureDir(File(fontsDir))
    val pluginsDir = FileHelper.getAbsolutePath(applicationHomeDir, "plugins")
    ensureDir(File(pluginsDir))
    setupKeyStores()
    Configuration(this)
  }

  /**
   * Tries to get the given filename from the application's resource dir (file system). If not exist, the content will
   * be taken as resource input stream. Calls getInputStream(filename) and converts input stream to String.
   *
   * @param filename Filename (can include relative path settings): "test.xsl", "fo-styles/doit.xsl".
   * @return Object[2]: First value is the content as string and second value is the url in external form.
   * @see .getResourceAsInputStream
   */
  fun getResourceContentAsString(filename: String): Array<Any?> {
    val result = getResourceAsInputStream(filename)
    (result[0] as InputStream?).use {
      try {
        result[0] = IOUtils.toString(it, "UTF-8")
      } catch (ex: IOException) {
        log.error(ex.message, ex)
      }
    }
    return result
  }

  /**
   * PLEASE NOTE: Don't forget to close the returned InputStream for avoiding leaked resources!!!<br></br>
   * Tries to get the given filename from the application's resource dir (file system). If not exist, the input stream
   * will be taken as resource input stream.
   *
   * @param filename Filename (can include relative path settings): "test.xsl", "fo-styles/doit.xsl".
   * @return Object[2]: First value is the InputStream and second value is the url in external form.
   */
  fun getResourceAsInputStream(filename: String): Array<Any?> {
    var inputStream: InputStream? = null
    var path: String? = null
    val base = File(resourceDirName)
    if (base.isDirectory) {
      val file = File(base, filename)
      if (!file.exists()) {
        showNonExistingMessage(file, false)
      } else {
        try {
          inputStream = FileInputStream(file)
          path = file.toURI().toString()
        } catch (ex: FileNotFoundException) {
          log.error(file.absoluteFile.toString() + ": " + ex.message, ex) // Should not occur.
          inputStream = null
        }
        showExistingMessage(file, false)
      }
    }
    if (inputStream == null) {
      val cLoader = javaClass.classLoader
      val url = cLoader.getResource(filename)
      if (url != null) {
        path = url.toExternalForm()
      }
      inputStream = cLoader.getResourceAsStream(filename)
    }
    if (inputStream == null) {
      log.error(
        "File '" + filename + "' not found (wether in file system under '" + base.absolutePath
            + "' nor in resource!)"
      )
    }
    val result = arrayOfNulls<Any>(2)
    result[0] = inputStream
    result[1] = path
    return result
  }

  /**
   * @return true if at least a send mail host is given, otherwise false.
   */
  val isSendMailConfigured: Boolean
    get() = sendMailConfiguration.isMailSendConfigOk

  open fun getSendMailConfiguration(): SendMailConfig? {
    return if (isSendMailConfigured) {
      sendMailConfiguration
    } else null
  }

  val isTelephoneSystemUrlConfigured: Boolean
    get() = StringUtils.isNotEmpty(telephoneSystemUrl)
  val contractTypes: List<ContractType>
    get() = configXml.contractTypes
  val isSecurityConfigured: Boolean
    get() = StringUtils.isNotBlank(securityConfig.passwordPepper)

  /**
   * @return true if meb mail account with hostname is configured, otherwise false.
   */
  val isMebMailAccountConfigured: Boolean
    get() = mebMailClient.isMailAccountAvailable

  private fun setupKeyStores() {
    if (!StringUtils.isBlank(keystoreFileName)) {
      try {
        var keystoreFile = File(keystoreFileName)
        if (!keystoreFile.canRead()) {
          keystoreFile = File(applicationHomeDir, keystoreFileName)
        }
        if (!keystoreFile.canRead()) {
          log.warn("Can't read keystore file: $keystoreFile")
          return
        }
        val inputStream: InputStream = FileInputStream(keystoreFile)
        usersSSLSocketFactory = createSSLSocketFactory(inputStream, keystorePassphrase ?: "")
        log.info("Keystore successfully read from file: " + keystoreFile.absolutePath)
      } catch (ex: Throwable) {
        log.error("Could not initialize your key store (see error message below)!")
        log.error(ex.message, ex)
      }
    }
  }

  @Throws(Exception::class)
  private fun createSSLSocketFactory(inputStream: InputStream, passphrase: String): SSLSocketFactory {
    val ks = KeyStore.getInstance(KeyStore.getDefaultType())
    ks.load(inputStream, passphrase.toCharArray())
    inputStream.close()
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    tmf.init(ks)
    val defaultTrustManager = tmf.trustManagers[0] as X509TrustManager
    val context = SSLContext.getInstance("TLS")
    context.init(null, arrayOf<TrustManager>(defaultTrustManager), null)
    return context.socketFactory
  }

  private fun ensureDir(dir: File): Boolean {
    if (!dir.exists()) {
      log.info("Creating directory $dir")
      dir.mkdir()
    }
    if (!dir.canRead()) {
      log.error("Can't create directory: $dir")
      return false
    }
    return true
  }

  fun getDaoValue(parameter: IConfigurationParam?, configurationDO: ConfigurationDO?): Any? {
    return configDao.getValue(parameter, configurationDO)
  }

  fun daoInternalLoadAll(): List<ConfigurationDO> {
    return configDao.internalLoadAll()
  }

  val timezone: TimeZone?
    get() {
      val configurationDO = configDao.getEntry(ConfigurationParam.DEFAULT_TIMEZONE)
      return if (configurationDO != null) {
        configurationDO.timeZone
      } else {
        log.error("No timezone configured in db configuration. Return default timezone.")
        TimeZone.getDefault()
      }
    }

  open fun createMailSessionLocalSettingsConfigModel(): MailSessionLocalSettingsConfigModel {
    return MailSessionLocalSettingsConfigModel()
      .setEmailEnabled(pfmailsessionEmailEnabled)
      .setName(pfmailsessionName)
      .setEmailHost(pfmailsessionHost)
      .setEmailPort(pfmailsessionPort.toString())
      .setStandardEmailSender(pfmailsessionStandardEmailSender)
      .setEmailAuthEnabled(pfmailsessionAuth.toString())
      .setEmailAuthUser(pfmailsessionUser)
      .setEmailAuthPass(pfmailsessionPassword)
      .setEncryption(pfmailsessionEncryption)
  }

  /**
   * 31.03. of the given year, if not configured different. This date determine when vacation days of an employee
   * from the last year will be invalid, if not used.
   */
  fun getEndOfCarryVacationOfPreviousYear(year: Int): LocalDate {
    var day = 31
    var month = 3 // March, 1 based, 1-January, ..., 12-December.
    val configDO = configDao.getEntry(ConfigurationParam.END_DATE_VACATION_LAST_YEAR)
    if (configDO != null) {
      val dayMonthString = configDO.stringValue
      val dayMonthParts = dayMonthString!!.split("\\.".toRegex()).toTypedArray()
      try {
        month = dayMonthParts[1].toInt()
        day = dayMonthParts[0].toInt()
      } catch (e: NumberFormatException) {
        log.error("Error while parsing ConfigurationParam.END_DATE_VACATION_LASTR_YEAR: $dayMonthString")
        day = 31
        month = 3 // March
      }
    }
    return LocalDate.of(year, Month.JANUARY, 1).withMonth(month).withDayOfMonth(day)
  }

  val hREmailadress: String?
    get() {
      val hrMailaddress = configDao.getEntry(ConfigurationParam.HR_MAILADDRESS)
      return hrMailaddress?.stringValue
    }

  // this could happen if the database is not initialized (during projectforge initial setup)
  val minPasswordLength: Int
    get() {
      try {
        val minPwLenEntry = configDao.getEntry(ConfigurationParam.MIN_PASSWORD_LENGTH)
        if (minPwLenEntry != null) {
          val minPwLenValue = minPwLenEntry.intValue
          if (minPwLenValue != null) {
            return minPwLenValue
          }
        }
      } catch (e: RuntimeException) {
        // this could happen if the database is not initialized (during projectforge initial setup)
        log.warn("Exception while getting the min password length configuration.", e)
      }
      return ConfigurationParam.MIN_PASSWORD_LENGTH.defaultIntValue
    }

  // this could happen if the database is not initialized (during projectforge initial setup)
  val flagCheckPasswordChange: Boolean
    get() {
      try {
        val flagCheckPwChangeConf = configDao.getEntry(ConfigurationParam.PASSWORD_FLAG_CHECK_CHANGE)
        if (flagCheckPwChangeConf != null) {
          val flagCheckPwChange = flagCheckPwChangeConf.booleanValue
          if (flagCheckPwChange != null) {
            return flagCheckPwChange
          }
        }
      } catch (e: RuntimeException) {
        // this could happen if the database is not initialized (during projectforge initial setup)
        log.warn("Exception while getting configuration flag - password change requirement.", e)
      }
      return ConfigurationParam.PASSWORD_FLAG_CHECK_CHANGE.defaultBooleanValue
    }
  val isLogoFileValid: Boolean
    get() {
      val logoFile = logoFileObject
      return logoFile != null && logoFile.canRead() && logoFile.isFile
    }
  val syntheticLogoName: String?
    get() {
      val logoFile = logoFileName
      if (StringUtils.isBlank(logoFile)) {
        return null
      }
      if (logoFile!!.endsWith(".png")) {
        return "logo.png"
      }
      return if (logoFile.endsWith(".jpg") || logoFile.endsWith(".jpeg")) {
        "logo.jpg"
      } else "logo.gif"
    }
  var logoFileObject: File? = null
    get() {
      if (field != null) {
        return field
      }
      logoFileName?.let {
        if (it.isNotBlank()) {
          var file = File(it)
          if (!file.isAbsolute) {
            file = Paths.get(resourceDirName, "images", it).toFile()
          }
          field = file
          return file
        }
      }
      return null
    }
    private set

  companion object {
    @Transient
    private val nonExistingResources: MutableSet<String> = HashSet()

    @Transient
    private val existingResources: MutableSet<String> = HashSet()

    /**
     * Available early in the spring start up phase. Usable by Flyway.
     */
    @JvmStatic
    var staticApplicationHomeDir: String? = null
      private set
    private const val DEFAULT_RESOURCES_DIR = "resources"
    private val DEFAULT_FONTS_DIR = DEFAULT_RESOURCES_DIR + File.separator + "fonts"
    private fun showNonExistingMessage(file: File, directory: Boolean) {
      // Synchronized not needed, for concurrent calls, output entries exist twice in the worst case.
      if (!nonExistingResources.contains(file.absolutePath)) {
        nonExistingResources.add(file.absolutePath)
        existingResources.remove(file.absolutePath) // If changed by administrator during application running.
        val type = if (directory) "directory" else "file"
        log.info(
          "Using default " + type + " of ProjectForge, because " + type + "'" + file.absolutePath
              + "' does not exist (OK)"
        )
      }
    }

    private fun showExistingMessage(file: File, directory: Boolean) {
      // Synchronized not needed, for concurrent calls, output entries exist twice in the worst case.
      if (!existingResources.contains(file.absolutePath)) {
        existingResources.add(file.absolutePath)
        nonExistingResources.remove(file.absolutePath) // If changed by administrator during application running.
        val type = if (directory) "directory" else "file"
        log.info("Using existing " + type + ":" + file.absolutePath)
      }
    }

    private const val ENVIRONMENT_FILE = "environment.sh"
    private const val ENVIRONMENT_FILE_INITIAL_CONTENT = "#!/bin/bash\n\n" +
        "# Set the java options and arguments here for Your docker installation only.\n\n" +
        "# Increase ProjectForge's memory setting:\n" +
        "#export JAVA_OPTS=\"-DXmx4g\"\n" +
        "export JAVA_OPTS=\n\n" +
        "# For license file of Milton (CardDAV/CalDAV):\n" +
        "#export JAVA_OPTS=\"\$JAVA_OPTS -Dloader.path=\${HOME}/ProjectForge/resources/milton\"\n\n" +
        "# Define your optional program arguments here\n" +
        "export JAVA_ARGS=\n"
  }
}
