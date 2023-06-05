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

package org.projectforge.jcr

import mu.KotlinLogging
import org.apache.commons.codec.digest.DigestUtils
import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.jcr.Jcr
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders
import org.apache.jackrabbit.oak.segment.file.FileStore
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.projectforge.common.CryptStreamUtils
import org.projectforge.common.FormatterUtils
import org.projectforge.common.NumberOfBytes
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.*
import javax.annotation.PreDestroy
import javax.jcr.Binary
import javax.jcr.Node
import javax.jcr.Repository
import javax.jcr.Session
import kotlin.concurrent.thread

private val log = KotlinLogging.logger {}

@Service
open class RepoService {
  internal lateinit var repository: Repository

  internal var fileStore: FileStore? = null

  var fileStoreLocation: File? = null
    internal set

  private var nodeStore: NodeStore? = null

  internal lateinit var mainNodeName: String

  @PreDestroy
  fun shutdown() {
    log.info { "Shutting down jcr repository..." }
    fileStore?.let {
      it.flush()
      it.compactFull()
      it.cleanup()
      log.info { "Jcr stats: ${FileStoreInfo(this)}" }
      it.close()
    }
    nodeStore?.let {
      //log.warn { "Method not yet implemented: ${it.javaClass}.dispose()" }
      /*if (it is DocumentNodeStore) {
        it.dispose()
      }*/
    }
  }

  /**
   * Should only be called by test cases if you need to initialize a repo multiple times.
   */
  fun internalResetForJunitTestCases() {
    nodeStore = null
  }

  /**
   * @param parentNodePath Path, nodes are separated by '/', e. g. "world/germany". The nodes of this path must already exist.
   * For creating top level nodes (direct child of main node), set parentNode to null, empty string or "/".
   * @param relPath Sub node parent node to create if not exists. Null value results in nop.
   */
  open fun ensureNode(parentNodePath: String?, relPath: String? = null): Node? {
    relPath ?: return null
    return runInSession { session ->
      val node = getNode(session, parentNodePath, relPath, true)
      session.save()
      node
    }
  }

  @JvmOverloads
  open fun storeProperty(
    parentNodePath: String?,
    relPath: String?,
    name: String,
    value: String,
    ensureRelNode: Boolean = true
  ) {
    runInSession { session ->
      val node = getNode(session, parentNodePath, relPath, ensureRelNode)
      node.setProperty(name, value)
      session.save()
    }
  }

  open fun retrievePropertyString(parentNodePath: String?, relPath: String?, name: String): String? {
    return runInSession { session ->
      getNodeOrNull(session, parentNodePath, relPath, false)?.getProperty(name)?.string
    }
  }

  /**
   * Content of file should be given as [FileObject.content].
   * @param password Optional password for encryption. The password will not be stored in any kind!
   */
  @JvmOverloads
  open fun storeFile(
    fileObject: FileObject, fileSizeChecker: FileSizeChecker,
    user: String? = null,
    password: String? = null,
  ) {
    val content = fileObject.content ?: ByteArray(0) // Assuming 0 byte file if no content is given.
    return storeFile(fileObject, content.inputStream(), fileSizeChecker, user, password = password)
  }

  /**
   * @param password Optional password for encryption. The password will not be stored in any kind!
   */
  @JvmOverloads
  open fun storeFile(
    fileObject: FileObject,
    content: InputStream,
    fileSizeChecker: FileSizeChecker,
    user: String? = null,
    /**
     * Optional data e. g. for fileSizeChecker of data transfer area size.
     */
    data: Any? = null,
    password: String? = null,
  ) {
    if (fileObject.size != null) { // file size already known:
      fileSizeChecker.checkSize(fileObject, data)
    }
    val parentNodePath = fileObject.parentNodePath
    val relPath = fileObject.relPath
    if (parentNodePath == null || relPath == null) {
      throw IllegalArgumentException("Parent node path and relPath not given. Can't determine location of file to store: $fileObject")
    }
    var lazyCheckSumFileObject: FileObject? = null
    runInSession { session ->
      val node = getNode(session, parentNodePath, relPath, true)
      val filesNode = ensureNode(node, NODENAME_FILES)
      val fileId = fileObject.fileId ?: createRandomId
      fileObject.fileId = fileId
      log.info { "Storing file: $fileObject" }
      val fileNode = filesNode.addNode(fileId)
      val now = Date()
      if (fileObject.created == null) {
        // created should only be preset for test cases. So normally, use current date.
        fileObject.created = now
      }
      fileObject.createdByUser = user
      if (fileObject.lastUpdate == null) {
        // last update should only be preset for test cases. So normally, use current date.
        fileObject.lastUpdate = now
      }
      fileObject.lastUpdate = fileObject.created
      fileObject.lastUpdateByUser = user
      var bin: Binary? = null
      try {
        if (password.isNullOrBlank()) {
          bin = session.valueFactory.createBinary(content)
        } else {
          val inputStream = CryptStreamUtils.pipeToEncryptedInputStream(content, password)
          bin = session.valueFactory.createBinary(inputStream)
          fileObject.aesEncrypted = true
        }
        fileNode.setProperty(PROPERTY_FILECONTENT, bin)
        fileObject.size = bin?.size
        Integer.MAX_VALUE
      } finally {
        bin?.dispose()
      }
      // Check size again for the case, the fileObject didn't contain file size before processing the stream.
      try {
        fileSizeChecker.checkSize(fileObject, data)
      } catch (ex: Exception) {
        fileNode.remove()
        throw ex
      }
      if (fileObject.size ?: 0 > NumberOfBytes.MEGA_BYTES * 50) {
        lazyCheckSumFileObject = fileObject
        fileObject.checksum = "..."
      } else {
        checksum(fileNode, fileObject)
      }
      fileObject.copyTo(fileNode)
      session.save()
    }
    lazyCheckSumFileObject?.let {
      thread {
        checksum(it)
      }
    }
  }

  private fun checksum(fileNode: Node, fileObject: FileObject) {
    // Calculate checksum for files smaller than 50MB (it's fast enough).
    val startTime = System.currentTimeMillis()
    // Calculate checksum
    getFileInputStream(fileNode, fileObject, useEncryptedFile = true).use { istream ->
      fileObject.checksum = checksum(istream)
    }
    FileObject.setChecksum(fileNode, fileObject.checksum)
    log.info {
      "Checksum of '${fileObject.fileName}' of size ${FormatterUtils.formatBytes(fileObject.size)} calculated in ${
        FormatterUtils.format(
          (System.currentTimeMillis() - startTime) / 1000
        )
      }s."
    }
  }

  open fun deleteFile(fileObject: FileObject): Boolean {
    return runInSession { session ->
      val node = getNode(session, fileObject.parentNodePath, fileObject.relPath, false)
      if (!node.hasNode(NODENAME_FILES)) {
        log.error { "Can't delete file, because '$NODENAME_FILES' not found for node '${node.path}': $fileObject" }
        false
      } else {
        val filesNode = node.getNode(NODENAME_FILES)
        val fileNode = findFile(filesNode, fileObject.fileId, fileObject.fileName)
        if (fileNode == null) {
          log.info { "Nothing to delete, file node doesn't exit: $fileObject" }
          false
        } else {
          fileObject.copyFrom(fileNode)
          log.info { "Deleting file: $fileObject" }
          fileNode.remove()
          session.save()
          true
        }
      }
    }
  }

  open fun deleteNode(nodeInfo: NodeInfo): Boolean {
    return runInSession { session ->
      val node = getNode(session, nodeInfo.path, nodeInfo.name, false)
      log.info { "Deleting node: $nodeInfo" }
      node.remove()
      session.save()
      true
    }
  }

  /**
   * @return list of file infos without content.
   */
  @JvmOverloads
  open fun getFileInfos(parentNodePath: String?, relPath: String? = null): List<FileObject>? {
    return runInSession { session ->
      val filesNode = getFilesNode(session, parentNodePath, relPath)
      getFileInfos(filesNode)
    }
  }

  /**
   * @return file info without content.
   */
  @JvmOverloads
  open fun getFileInfo(
    parentNodePath: String?,
    relPath: String? = null,
    fileId: String? = null,
    fileName: String? = null
  ): FileObject? {
    return runInSession { session ->
      val filesNode = getFilesNode(session, parentNodePath, relPath)
      val node = findFile(filesNode, fileId, fileName)
      if (node != null) {
        FileObject(node, parentNodePath, relPath)
      } else {
        null
      }
    }
  }

  /**
   * Change fileName and/or description if given.
   * @param updateLastUpdateInfo If true (default),
   * time stamp of last update and user of this update will be updated. Otherwise time stamp and user info will be left untouched.
   * @return new file info without content.
   */
  @JvmOverloads
  open fun changeFileInfo(
    fileObject: FileObject,
    user: String,
    newFileName: String? = null,
    newDescription: String? = null,
    newZipMode: ZipMode? = null,
    updateLastUpdateInfo: Boolean = true,
  ): FileObject? {
    return runInSession { session ->
      val node = getNode(session, fileObject.parentNodePath, fileObject.relPath, false)
      if (!node.hasNode(NODENAME_FILES)) {
        log.error { "Can't change file info, because '$NODENAME_FILES' not found for node '${node.path}': $fileObject" }
        null
      } else {
        val filesNode = node.getNode(NODENAME_FILES)
        val fileNode = findFile(filesNode, fileObject.fileId, fileObject.fileName)
        if (fileNode == null) {
          log.error { "Can't change file info, file node doesn't exit: $fileObject" }
          null
        } else {
          var modified = false
          if (!newFileName.isNullOrBlank()) {
            log.info { "Changing file name to '$newFileName' for: $fileObject" }
            fileNode.setProperty(PROPERTY_FILENAME, newFileName)
            modified = true
          }
          if (newDescription != null) {
            log.info { "Changing file description to '$newDescription' for: $fileObject" }
            fileNode.setProperty(PROPERTY_FILEDESC, newDescription)
            modified = true
          }
          if (newZipMode != null) {
            log.info { "Changing zip encryption algorithm to '$newZipMode' for: $fileObject" }
            fileNode.setProperty(PROPERTY_ZIP_MODE, newZipMode.name)
            modified = true
          }
          if (modified && updateLastUpdateInfo) {
            fileNode.setProperty(PROPERTY_LAST_UPDATE_BY_USER, user)
            fileNode.setProperty(PROPERTY_LAST_UPDATE, PFJcrUtils.convertToString(Date()) ?: "")
          }
          session.save()
          FileObject(fileNode)
        }
      }
    }
  }

  /**
   * Returns the already calculated checksum or calculates it, if not given.
   * @return new file info including checksum without content.
   */
  open fun checksum(fileObject: FileObject): String? {
    return runInSession { session ->
      val node = getNode(session, fileObject.parentNodePath, fileObject.relPath, false)
      if (!node.hasNode(NODENAME_FILES)) {
        log.error { "Can't change file info, because '$NODENAME_FILES' not found for node '${node.path}': $fileObject" }
        null
      } else {
        val filesNode = node.getNode(NODENAME_FILES)
        val fileNode = findFile(filesNode, fileObject.fileId, fileObject.fileName)
        if (fileNode == null) {
          log.error { "Can't get or calculate file info, file node doesn't exit: $fileObject" }
          null
        } else {
          val storedFileObject = FileObject(fileNode)
          checksum(fileNode, storedFileObject)
          session.save()
          fileObject.checksum = storedFileObject.checksum
          fileObject.checksum
        }
      }
    }
  }

  @JvmOverloads
  open fun getNodeInfo(absPath: String, recursive: Boolean = false): NodeInfo {
    return runInSession { session ->
      log.info { "Getting node info of path '$absPath'..." }
      val node = session.getNode(absPath)
      NodeInfo(node, recursive)
    }
  }

  private fun getFilesNode(
    sessionWrapper: SessionWrapper,
    parentNodePath: String?,
    relPath: String?,
    ensureFilesNode: Boolean = false
  ): Node? {
    val parentNode = getNodeOrNull(sessionWrapper, parentNodePath, relPath, false)
    if (parentNode == null) {
      log.info { "Parent node '${getAbsolutePath(parentNodePath, relPath)}' doesn't exist. No files found (OK)." }
      return null
    }
    return if (ensureFilesNode || parentNode.hasNode(NODENAME_FILES)) {
      ensureNode(parentNode, NODENAME_FILES)
    } else {
      null
    }
  }

  internal fun getFileInfos(
    filesNode: Node?,
    parentNodePath: String? = null,
    relPath: String? = null
  ): List<FileObject>? {
    filesNode ?: return null
    val fileNodes = filesNode.nodes
    if (fileNodes == null || !fileNodes.hasNext()) {
      return null
    }
    val result = mutableListOf<FileObject>()
    while (fileNodes.hasNext()) {
      val node = fileNodes.nextNode()
      if (node.hasProperty(PROPERTY_FILENAME)) {
        result.add(FileObject(node, parentNodePath ?: node.path, relPath))
      }
    }
    return result
  }

  fun getFileInfos(nodeInfo: NodeInfo?): List<FileObject>? {
    nodeInfo ?: return null
    val fileNodes = nodeInfo.children
    if (fileNodes.isNullOrEmpty()) {
      return null
    }
    val result = mutableListOf<FileObject>()
    fileNodes.forEach { node ->
      if (node.hasProperty(PROPERTY_FILENAME)) {
        result.add(FileObject(node))
      }
    }
    return result
  }

  internal fun findFile(filesNode: Node?, fileId: String?, fileName: String? = null): Node? {
    filesNode ?: return null
    if (!filesNode.hasNodes()) {
      return null
    }
    filesNode.nodes?.let {
      while (it.hasNext()) {
        val node = it.nextNode()
        if (node.name == fileId || PFJcrUtils.getProperty(node, PROPERTY_FILENAME)?.string == fileName) {
          return node
        }
      }
    }
    return null
  }

  @JvmOverloads
  open fun retrieveFile(fileObject: FileObject, password: String? = null): Boolean {
    return runInSession { session ->
      val filesNode = getFilesNode(session, fileObject.parentNodePath, fileObject.relPath, false)
      val node = findFile(filesNode, fileObject.fileId, fileObject.fileName)
      if (node == null) {
        log.warn { "File not found in repository: $fileObject" }
        false
      } else {
        fileObject.copyFrom(node)
        fileObject.content = getFileContent(node, fileObject, password)
        true
      }
    }
  }

  open fun retrieveFileInputStream(fileObject: FileObject, password: String? = null): InputStream? {
    return runInSession { session ->
      val filesNode = getFilesNode(session, fileObject.parentNodePath, fileObject.relPath, false)
      val node = findFile(filesNode, fileObject.fileId, fileObject.fileName)
      if (node == null) {
        log.warn { "File not found in repository: $fileObject" }
        null
      } else {
        getFileInputStream(node, fileObject)
      }
    }
  }

  internal fun getFileContent(
    node: Node?, fileObject: FileObject,
    password: String? = null,
    useEncryptedFile: Boolean = false,
  ): ByteArray? {
    return getFileInputStream(node, fileObject, password = password, useEncryptedFile = useEncryptedFile)?.use(
      InputStream::readBytes
    )
  }

  /**
   * @param password Must be given for encrypted file to decrypt (if useEncryptedFile isn't true)
   * @param useEncryptedFile If true, work with encrypted file directly without password and decryption.
   * Used by internal checksum and backup functionality.
   */
  internal fun getFileInputStream(
    node: Node?,
    fileObject: FileObject,
    suppressLogInfo: Boolean = false,
    password: String? = null,
    useEncryptedFile: Boolean = false,
  ): InputStream? {
    node ?: return null
    if (!suppressLogInfo) {
      log.info { "Reading file from repository '${node.path}': $fileObject..." }
    }
    if (!useEncryptedFile && fileObject.aesEncrypted == true && password.isNullOrBlank()) {
      log.error { "File is crypted, but no password given to decrypt in repository '${node.path}': $fileObject" }
      return null
    }
    var binary: Binary? = null
    try {
      binary = node.getProperty(PROPERTY_FILECONTENT)?.binary ?: return null
      return if (useEncryptedFile || password.isNullOrBlank()) {
        binary.stream
      } else {
        try {
          CryptStreamUtils.pipeToDecryptedInputStream(binary.stream, password)
        } catch (ex: Exception) {
          if (CryptStreamUtils.wasWrongPassword(ex)) {
            log.error { "Can't decrypt and retrieve file (wrong password) in repository '${node.path}': $fileObject" }
            null
          } else {
            throw ex
          }
        }
      }
    } finally {
      binary?.dispose()
    }
  }

  internal fun getFileSize(node: Node?, fileObject: FileObject, suppressLogInfo: Boolean = false): Long? {
    node ?: return null
    if (!suppressLogInfo) {
      log.info { "Determing size of file from repository '${node.path}': '${fileObject.fileName}'..." }
    }
    var binary: Binary? = null
    try {
      binary = node.getProperty(PROPERTY_FILECONTENT)?.binary
      return binary?.size
    } finally {
      binary?.dispose()
    }
  }

  internal fun getNode(
    session: SessionWrapper,
    parentNodePath: String?,
    relPath: String? = null,
    ensureRelNode: Boolean = true
  ): Node {
    return getNodeOrNull(session, parentNodePath, relPath, ensureRelNode)
      ?: throw IllegalArgumentException("Can't find node ${getAbsolutePath(parentNodePath, relPath)}.")
  }

  internal fun getNodeOrNull(
    session: SessionWrapper,
    parentNodePath: String?,
    relPath: String? = null,
    ensureRelNode: Boolean = true
  ): Node? {
    val absolutePath = getAbsolutePath(parentNodePath)
    if (!session.nodeExists(absolutePath)) {
      return null
    }
    val parentNode = try {
      session.getNode(absolutePath)
    } catch (ex: Exception) {
      log.error { "Can't get node '$absolutePath'. ${ex::class.java.name}: ${ex.message}." }
      return null
    }
    return when {
      ensureRelNode -> ensureNode(parentNode, relPath)
      parentNode.hasNode(relPath) -> parentNode.getNode(relPath)
      else -> null
    }
  }

  fun getAbsolutePath(nodePath: String?): String {
    val path = nodePath?.removePrefix("/")?.removePrefix(mainNodeName)?.removePrefix("/") ?: ""
    return "/$mainNodeName/$path"
  }

  private fun getAbsolutePath(parentNode: Node, relPath: String?): String? {
    val parentPath = parentNode.path
    return getAbsolutePath(parentPath, relPath)
  }

  fun cleanup() {
    log.info { "Cleaning JCR repository up..." }
    fileStore?.let {
      it.flush()
      it.compactFull()
      it.cleanup()
    }
  }

  internal fun ensureNode(parentNode: Node, relPath: String?): Node {
    relPath ?: return parentNode
    var current: Node = parentNode
    relPath.split("/").forEach {
      current = if (current.hasNode(it)) {
        current.getNode(it)
      } else {
        log.info { "Creating node ${getAbsolutePath(current, it)}." }
        current.addNode(it)
      }
    }
    return current
  }

  private val createRandomId: String
    get() {
      val random = SecureRandom()
      val bytes = ByteArray(PROPERTY_RANDOM_ID_LENGTH)
      random.nextBytes(bytes)
      val sb = StringBuilder()
      for (i in 0 until PROPERTY_RANDOM_ID_LENGTH) {
        sb.append(ALPHA_CHARSET[(bytes[i].toInt() and 0xFF) % PROPERTY_RANDOM_ID_LENGTH])
      }
      return sb.toString()
    }

  internal fun <T> runInSession(method: (sessionWrapper: SessionWrapper) -> T): T {
    val session = SessionWrapper(this)
    try {
      return method(session)
    } finally {
      session.logout()
    }
  }

  /**
   * @param mainNodeName All activities (working with nodes) will done under topNode. TopNode should be given for backing up and
   * restoring. By default "ProjectForge" is used.
   */
  @JvmOverloads
  fun init(repositoryDir: File, mainNodeName: String = "ProjectForge") {
    synchronized(this) {
      if (nodeStore != null) {
        throw IllegalArgumentException("Can't initialize repo twice! repo=$this")
      }
      if (mainNodeName.isBlank()) {
        throw IllegalArgumentException("Top node shouldn't be empty!")
      }
      if (log.isDebugEnabled) {
        log.debug { "Setting system property: derby.stream.error.field=${DerbyUtil::class.java.name}.DEV_NULL" }
      }
      System.setProperty("derby.stream.error.field", "${DerbyUtil::class.java.name}.DEV_NULL")
      log.info { "Initializing JCR repository with main node '$mainNodeName' in: ${repositoryDir.absolutePath}" }
      this.mainNodeName = mainNodeName

      FileStoreBuilder.fileStoreBuilder(repositoryDir).build().let { fileStore ->
        this.fileStore = fileStore
        this.fileStoreLocation = repositoryDir
        nodeStore = SegmentNodeStoreBuilders.builder(fileStore).build()
        repository = Jcr(Oak(nodeStore)).createRepository()
      }

      runInSession { session ->
        if (!session.rootNode.hasNode(mainNodeName)) {
          log.info { "Creating top level node '$mainNodeName'." }
          session.rootNode.addNode(mainNodeName)
        }
        session.save()
      }
    }
  }

  internal fun close(session: Session) {
    session.save()
    fileStore?.close()
  }

  companion object {
    const val NODENAME_FILES = "__FILES"
    internal const val PROPERTY_FILENAME = "fileName"
    internal const val PROPERTY_FILESIZE = "size"
    internal const val PROPERTY_FILECONTENT = "content"
    internal const val PROPERTY_CREATED = "created"
    internal const val PROPERTY_CREATED_BY_USER = "createdByUser"
    internal const val PROPERTY_FILEDESC = "fileDescription"
    internal const val PROPERTY_LAST_UPDATE = "lastUpdate"
    internal const val PROPERTY_LAST_UPDATE_BY_USER = "lastUpdateByUser"
    internal const val PROPERTY_CHECKSUM = "checksum"
    internal const val PROPERTY_AES_ENCRYPTED = "aesEncrypted"
    internal const val PROPERTY_ZIP_MODE = "zipMode"
    private const val PROPERTY_RANDOM_ID_LENGTH = 20
    private val ALPHA_CHARSET: Array<Char> = ('a'..'z').toList().toTypedArray()

    internal fun checksum(istream: InputStream?): String {
      istream ?: return ""
      return "SHA256: ${DigestUtils.sha256Hex(istream)}"
    }

    internal fun getAbsolutePath(parentPath: String?, relPath: String?): String? {
      if (parentPath == null && relPath == null) {
        return null
      }
      parentPath ?: return relPath
      relPath ?: return parentPath
      return if (parentPath.endsWith("/")) "$parentPath$relPath" else "$parentPath/$relPath"
    }
  }

  // https://stackoverflow.com/questions/1004327/getting-rid-of-derby-log
  object DerbyUtil {
    @JvmField
    val DEV_NULL: OutputStream = object : OutputStream() {
      override fun write(b: Int) {}
    }
  }
}
