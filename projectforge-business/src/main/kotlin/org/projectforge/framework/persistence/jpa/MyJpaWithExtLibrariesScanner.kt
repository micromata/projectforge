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

package org.projectforge.framework.persistence.jpa

import mu.KotlinLogging
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory
import org.hibernate.boot.archive.scan.internal.ScanResultCollector
import org.hibernate.boot.archive.scan.spi.*
import org.hibernate.boot.archive.scan.spi.Scanner
import org.hibernate.boot.archive.spi.*
import java.util.*


/**
 * Plugins to load, if ProjectForge is started from IDE. If not started from IDE, all loaded jars will be scanned automatically.
 */
private val embeddedPlugins4IDEStart = arrayOf(
    "banking",
    "datatransfer",
    "extendedemployeedata",
    "ihk",
    "licensemanagement",
    "liquidityplanning",
    "marketing",
    "memo",
    "merlin",
    "skillmatrix",
    "todo"
)

private val log = KotlinLogging.logger {}

/**
 * Scanner, which looks into class path, and optionally additionally libraries.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 * @author Florian Blumenstein
 * @author Kai Reinhard
 */
class MyJpaWithExtLibrariesScanner @JvmOverloads constructor(private val archiveDescriptorFactory: ArchiveDescriptorFactory = StandardArchiveDescriptorFactory.INSTANCE) :
    Scanner {
    //private val archiveDescriptorCache: MutableMap<String, ArchiveDescriptorInfo> = HashMap()

    override fun scan(environment: ScanEnvironment, options: ScanOptions, parameters: ScanParameters): ScanResult {
        log.debug { "Method scan (1)." }
        scanned = true
        pluginEntitiesForTestCases.forEach {
            log.debug { "environment.explicitlyListedClassNames.add('$it')" }
            environment.explicitlyListedClassNames.add(it)
        }
        val collector = ScanResultCollector(environment, options, parameters)
        /*
            if (environment.nonRootUrls != null) {
              val context: ArchiveContext = JpaWithExtLibrariesScanner.ArchiveContextImpl(false, collector)
              for (url in environment.nonRootUrls) {
                val descriptor = buildArchiveDescriptor(url, false)
                log.debug { "Method scan (2): call ArchiveDescriptor.visitArchive for url $url" }
                descriptor.visitArchive(context)
              }
            }
            val loadedUrls = mutableSetOf<URL>()
            val rootUrl = environment.rootUrl
            if (rootUrl != null) {
              log.debug { "Method scan (3): call visitUrl for rootUrl $rootUrl" }
              visitUrl(rootUrl, collector, CommonMatchers.always())
            }
            visitExternUrls(environment, collector, loadedUrls)*/
        return collector.toScanResult()
    }
    /*
      private fun visitUrl(url: URL, collector: ScanResultCollector, urlMatcher: Matcher<String?>) {
        log.debug { "Method visitUrl (1) for url $url" }
        if (!urlMatcher.match(url.toString())) {
          log.debug { "Method visitUrl (2): url doesn't match: '$url'" }
          return
        }
        val context: ArchiveContext = JpaWithExtLibrariesScanner.ArchiveContextImpl(true, collector)
        val surl = url.toString()
        log.info("Scanning module '$surl'...")
        if (surl.contains("!")) {
          var customUrlStr = url.toString()
          if (!surl.startsWith("jar:")) {
            customUrlStr = "jar:$customUrlStr"
          }
          // Remove the trail after the second '!', otherwise a file not found exception will be thrown.
          // e. g.: jar:file:/...myjar.jar!/BOOT-INF/lib/org.projectforge.plugins.memo-7.0-SNAPSHOT.jar!/
          // This workaround is needed by ProjectForge. In former version it works, because another exception
          // was thrown by Spring while building ArchiveDescriptor ;-)
          if (customUrlStr.lastIndexOf('!') > customUrlStr.indexOf('!')) {
            customUrlStr = customUrlStr.substring(0, customUrlStr.lastIndexOf('!'))
          }
          log.debug("Method visitUrl (3): custom url: $customUrlStr")
          try {
            val customUrl = URL(customUrlStr)
            val descriptor = buildArchiveDescriptor(customUrl, true)
            descriptor.visitArchive(context)
          } catch (e: MalformedURLException) {
            log.error("Error while getting custom URL: $customUrlStr")
          }
        } else {
          val descriptor = buildArchiveDescriptor(url, true)
          log.debug { "Method visitUrl (4): calling ArchiveDescriptor.visitArchive for url: '$url'" }
          descriptor.visitArchive(context)
          handleClassManifestClassPath(url, collector, urlMatcher)
        }
      }
    */
    /*
    private fun fixUrlToOpen(url: URL): URL {
      var surl = url.toString()
      val orgurl = surl
      if (surl.endsWith("!/")) {
        surl = surl.substring(0, surl.length - 2)
      }
      if (StringUtils.startsWith(surl, "jar:jar:file:")) {
        surl = surl.substring("jar:jar:".length)
      }
      if (StringUtils.startsWith(surl, "jar:file:")) {
        surl = surl.substring("jar:".length)
      }
      return try {
        val ret = URL(surl)
        log.debug { "fixUrlToOpen: url from $orgurl patched to $surl" }
        ret
      } catch (ex: MalformedURLException) {
        log.warn("Cannot parse patched url: " + surl + "; " + ex.message)
        url
      }
    }*/

    /**
     * A jar may have also declared more deps in manifest (like surefire).
     *
     * @param url the url
     * @param collector the collector to use
     */
    /*
    private fun handleClassManifestClassPath(url: URL, collector: ScanResultCollector, urlMatcher: Matcher<String?>) {
      val urlToOpen = fixUrlToOpen(url)
      val urls = urlToOpen.toString()
      log.debug { "Method handleClassManifestClassPath (1) for url '$url'" }
      if (!urls.endsWith(".jar")) {
        log.debug { "Method handleClassManifestClassPath (2) aborted, no jar: '$url'" }
        return
      }
      try {
        urlToOpen.openStream().use { inputStream ->
          JarInputStream(inputStream).use { jarStream ->
            val manifest = jarStream.manifest ?: return
            val attr = manifest.mainAttributes
            val `val` = attr.getValue("Class-Path")
            if (StringUtils.isBlank(`val`)) {
              return
            }
            val entries = StringUtils.split(`val`, " \t\n")
            for (entry in entries) {
              val surl = URL(entry)
              log.debug { "Method handleClassManifestClassPath (3), calling visitUrl for url '$surl'" }
              visitUrl(surl, collector, urlMatcher)
            }
          }
        }
      } catch (ex: IOException) {
        log.warn("JpaScan; Cannot open jar: " + url + ": " + ex.message)
      }
    }*/
    /*
      private fun visitExternUrls(
        environment: ScanEnvironment,
        collector: ScanResultCollector,
        loadedUrls: MutableSet<URL>
      ) {
        log.debug { "Method visitExternUrls (1)" }
        val matcherexppr = getPersistenceProperties(environment).getProperty(EXTLIBURLMATCHER)
        var urlmatcher = CommonMatchers.always<String?>()
        if (StringUtils.isNotBlank(matcherexppr)) {
          urlmatcher = BooleanListRulesFactory<String?>().createMatcher(matcherexppr)
          if (!externalUrlMatcherLogged) {
            log.info("Using url matcher '$matcherexppr' for external urls.")
            externalUrlMatcherLogged = true // Don't log mutliple times.
          }
        }
        val prov = loadJpaExtScannerUrlProvider(environment)
        val urls = prov!!.scannUrls
        log.debug { "Scanner urls: ${urls?.joinToString { it.toString() }}" }
        for (url in urls) {
          if (loadedUrls.contains(url)) {
            log.debug { "Method visitExternUrls (2), skipping url which was already visited: '$url'" }
            continue
          }
          log.debug { "Method visitExternUrls (3), call visitUrl for url '$url'" }
          try {
            visitUrl(url, collector, urlmatcher)
            loadedUrls.add(url)
          } catch (ex: Exception) {
            log.warn("Cannot scan " + url + "; " + ex.message)
          }
        }
        log.debug { "loadedUrls: ${loadedUrls.joinToString { it.toExternalForm() }}" }
        if (!INTERNAL_TEST_MODE) {
          workarroundForIDEStart(environment, collector, urlmatcher, loadedUrls)
          scanPlugins(environment, collector, loadedUrls)
        }
      }*/
    /*
      private fun getPersistenceProperties(environment: ScanEnvironment): Properties {
        if (environment !is StandardJpaScanEnvironmentImpl) {
          log.warn("environment is not StandardJpaScanEnvironmentImpl: " + environment.javaClass)
          return Properties()
        }
        val pud = PrivateBeanUtils.readField(
          environment,
          "persistenceUnitDescriptor"
        ) as PersistenceUnitDescriptor
        return pud.properties
      }*/
    /*
      private fun loadJpaExtScannerUrlProvider(environment: ScanEnvironment): JpaExtScannerUrlProvider? {
        val properties = getPersistenceProperties(environment)
        val provider = properties.getProperty(EXTLIBURLPROVIDER)
        return if (StringUtils.isBlank(provider)) {
          null
        } else try {
          val clazz = Class.forName(provider)
          clazz.getDeclaredConstructor().newInstance() as JpaExtScannerUrlProvider
        } catch (ex: Exception) {
          log.error("Cannot create JpaExtScannerUrlProvider: " + ex.message, ex)
          null
        }
      }*/
    /*
      private fun buildArchiveDescriptor(url: URL, isRootUrl: Boolean): ArchiveDescriptor {
        val descriptor: ArchiveDescriptor
        val descriptorInfo = archiveDescriptorCache[url.toString()]
        if (descriptorInfo == null) {
          descriptor = archiveDescriptorFactory.buildArchiveDescriptor(url)
          archiveDescriptorCache[url.toString()] = ArchiveDescriptorInfo(descriptor, isRootUrl)
        } else {
          validateReuse()
          descriptor = descriptorInfo.archiveDescriptor
        }
        return descriptor
      }*/
    /*
      // This needs to be protected and attributes/constructor visible in case
      // a custom scanner needs to override validateReuse.
      private class ArchiveDescriptorInfo(val archiveDescriptor: ArchiveDescriptor, val isRoot: Boolean)

      private fun validateReuse() {
        // is it really reasonable that a single url be processed multiple times?
        // for now, throw an exception, mainly because I am interested in situations where this might happen
        throw IllegalStateException("ArchiveDescriptor reused; can URLs be processed multiple times?")
      }
    */
    /*
    class ArchiveContextImpl(private val isRootUrl: Boolean, scanResultCollector: ScanResultCollector?) : ArchiveContext {
      private val classEntryHandler = ClassFileArchiveEntryHandler(scanResultCollector)
      private val packageEntryHandler = PackageInfoArchiveEntryHandler(scanResultCollector)
      private val fileEntryHandler = NonClassFileArchiveEntryHandler(scanResultCollector)
      override fun isRootUrl(): Boolean {
        return isRootUrl
      }

      override fun obtainArchiveEntryHandler(entry: ArchiveEntry): ArchiveEntryHandler {
        val nameWithinArchive = entry.nameWithinArchive
        return when {
          nameWithinArchive.endsWith("package-info.class") -> {
            packageEntryHandler
          }
          nameWithinArchive.endsWith(".class") -> {
            classEntryHandler
          }
          else -> {
            fileEntryHandler
          }
        }
      }
    }
  */
    /*
    fun scanPlugins(environment: ScanEnvironment, collector: ScanResultCollector, loadedUrls: MutableSet<URL>) {
      // pluginAdminService.activePlugins isn't yet initialized!
      val rootUrl = File(environment.rootUrl.toURI())
      val rootDir = if (rootUrl.extension == "jar") {
        rootUrl.parentFile
      } else {
        rootUrl
      }
      log.debug { "Root dir: $rootDir" }
      rootDir.listFiles { _: File?, name: String? -> name?.startsWith("org.projectforge.plugins.") == true }
        ?.forEach { jarFile ->
          scanPlugin(environment, collector, jarFile, loadedUrls)
        }
      val pluginsPath = System.getProperty(ProjectForgeApp.CONFIG_PLUGINS_DIR)
      if (pluginsPath.isNullOrBlank()) {
        return
      }
      val pluginsDir = File(pluginsPath)
      if (!pluginsDir.exists()) {
        log.info { "Plugins dir '${pluginsDir.absolutePath}' doesn't exist (OK). No external plugins found." }
        return
      }
      pluginsDir.listFiles { dir: File?, name: String? -> name?.endsWith(".jar") == true }
        ?.forEach { jarFile ->
          scanPlugin(environment, collector, jarFile, loadedUrls)
        }
    }*/
    /*
      fun scanPlugin(
        environment: ScanEnvironment,
        collector: ScanResultCollector,
        jarFile: File,
        loadedUrls: MutableSet<URL>
      ) {
        log.info { "Scanning plugin file '${jarFile.absolutePath}'.'" }
        if (jarFile.extension == "jar") {
          val url = jarFile.toURI().toURL()
          visitUrl(url, collector, CommonMatchers.always())
          loadedUrls.add(url)
        } else {
          log.warn { "Can't scan plugin '${jarFile.absolutePath}, not a jar file." }
        }
      }*/

    /**
     * Workarround, because if started in Intellij, the entities of the plugins are not scanned for Hibernate.
     */
    /*
    private fun workarroundForIDEStart(
      environment: ScanEnvironment,
      collector: ScanResultCollector,
      urlmatcher: Matcher<String?>,
      loadedUrls: MutableSet<URL>
    ) {
      if (!loadedUrls.isNullOrEmpty() && loadedUrls.any { it.toExternalForm().contains("org.projectforge.plugins.") }) {
        log.debug { "Method workarroundForIDEStart (1) aborted, no matching urls found." }
        return
      }
      val rootUrl = environment.rootUrl ?: return
      if (!rootUrl.toExternalForm().matches("file:.*target.classes.*".toRegex())) {
        log.debug { "Method workarroundForIDEStart (2) aborted, skipping target/classes: $rootUrl" }
        return
      }
      log.info("*********** ProjectForge seems to be started from inside an IDE (entities of plugins have to be added now. This is OK, if started from IDE).")
      var path: Path? = Paths.get(rootUrl.toURI())
      while (path != null) {
        if (path.fileName.toString() == "projectforge-business") {
          path = path.parent
          break
        }
        path = path.parent
      }
      if (path == null) {
        return
      }
      val pluginsPath = path.resolve("plugins")
      try {
        Files.newDirectoryStream(pluginsPath).use { directoryStream ->
          directoryStream.forEach { p ->
            val dirString = p.toString()
            if (File(dirString).isDirectory && dirString.contains("org.projectforge.plugins") && embeddedPlugins4IDEStart.any {
                dirString.contains(
                  it
                )
              }) {
              val url = p.resolve(Paths.get("target", "classes")).toUri().toURL()
              try {
                log.debug { "Method workarroundForIDEStart (3), calling visitUrl for url: $url" }
                visitUrl(url, collector, urlmatcher)
                loadedUrls.add(url)
              } catch (ex: Exception) {
                log.warn("Cannot scan " + url + "; " + ex.message)
              }
            }
          }
        }
      } catch (ex: IOException) {
      }
    }*/

    companion object {
        /**
         * Class name which has to implement JpaExtScannerUrlProvider.
         */
        const val EXTLIBURLPROVIDER = "de.micromata.genome.jpa.extlibrary.urlprovider"

        /**
         * Url matcher expression
         */
        const val EXTLIBURLMATCHER = "de.micromata.genome.jpa.extlibrary.urlmatcher"

        private var INTERNAL_TEST_MODE = false

        private var externalUrlMatcherLogged = false

        @JvmStatic
        fun setInternalSetUnitTestMode() {
            INTERNAL_TEST_MODE = true
        }

        private var pluginEntitiesForTestCases = mutableListOf<String>()

        private var scanned = false

        @JvmStatic
        fun addPluginEntitiesForTestMode(vararg entities: String) {
            entities.forEach { entity ->
                if (!pluginEntitiesForTestCases.contains(entity)) {
                    if (scanned) {
                        throw IllegalArgumentException("Scan is already done. Adding plugin entities has now effect anymore! This may occur in different run orders of tests, if the first test running in your package hasn't added this entity.")
                    }
                    log.info { "Adding '$entity' as plugin-entity for test cases." }
                    pluginEntitiesForTestCases.add(entity)
                }
            }
        }
    }
}
