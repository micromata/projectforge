/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.jcr.Jcr
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders
import org.apache.jackrabbit.oak.segment.file.FileStore
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
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


private val log = KotlinLogging.logger {}

@Service
open class RepoService {
    internal lateinit var repository: Repository

    internal var fileStore: FileStore? = null

    internal var fileStoreLocation: File? = null

    private var nodeStore: NodeStore? = null

    internal lateinit var mainNodeName: String

    @PreDestroy
    fun shutdown() {
        log.info { "Shutting down jcr repository." }
        fileStore?.let {
            it.flush()
            it.gc()
            log.info { "Jcr stats: ${FileStoreInfo(this)}" }
            it.close()
        }
        nodeStore?.let {
            if (it is DocumentNodeStore) {
                it.dispose()
            }
        }
    }

    /**
     * @param parentNodePath Path, nodes are separated by '/', e. g. "world/germany". The nodes of this path must already exist.
     * For creating top level nodes (direct child of main node), set parentNode to null, empty string or "/".
     * @param relPath Sub node parent node to create if not exists. Null value results in nop.
     */
    open fun ensureNode(parentNodePath: String?, relPath: String?): String? {
        relPath ?: return parentNodePath
        return runInSession<String> { session ->
            val node = getNode(session, parentNodePath, relPath, true)
            val path = node.path
            session.save()
            path
        }
    }

    @JvmOverloads
    open fun storeProperty(parentNodePath: String?, relPath: String?, name: String, value: String, ensureRelNode: Boolean = true) {
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
     */
    @JvmOverloads
    open fun storeFile(fileObject: FileObject, user: String? = null) {
        val content = fileObject.content ?: ByteArray(0) // Assuming 0 byte file if no content is given.
        val inputStream = ByteArrayInputStream(content)
        return storeFile(fileObject, inputStream, user)
    }

    @JvmOverloads
    open fun storeFile(fileObject: FileObject, content: InputStream, user: String? = null) {
        val parentNodePath = fileObject.parentNodePath
        val relPath = fileObject.relPath
        if (parentNodePath == null || relPath == null) {
            throw IllegalArgumentException("Parent node path and relPath not given. Can't determine location of file to store: $fileObject")
        }
        runInSession { session ->
            val node = getNode(session, parentNodePath, relPath, true)
            val filesNode = ensureNode(node, NODENAME_FILES)
            val fileId = fileObject.fileId ?: createRandomId
            fileObject.fileId = fileId
            log.info { "Storing file: $fileObject" }
            val fileNode = filesNode.addNode(fileId)
            fileObject.created = Date()
            fileObject.createdByUser = user
            fileObject.lastUpdate = fileObject.created
            fileObject.lastUpdateByUser = user
            var bin: Binary? = null
            try {
                bin = session.valueFactory.createBinary(content)
                fileNode.setProperty(PROPERTY_FILECONTENT, bin)
                fileObject.size = bin?.size?.toInt()
            } finally {
                bin?.dispose()
            }
            fileObject.copyTo(fileNode)
            session.save()
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
                    log.info { "Deleting file: $fileObject" }
                    fileNode.remove()
                    session.save()
                    true
                }
            }
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
    open fun getFileInfo(parentNodePath: String?, relPath: String? = null, fileId: String? = null, fileName: String? = null): FileObject? {
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
     * @return new file info without content.
     */
    @JvmOverloads
    open fun changeFileInfo(fileObject: FileObject, newFileName: String? = null, newDescription: String?): FileObject? {
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
                    if (!newFileName.isNullOrBlank()) {
                        log.info{"Changing file name to '$newFileName' for: $fileObject"}
                        fileNode.setProperty(PROPERTY_FILENAME, newFileName)
                    }
                    if (newDescription != null) {
                        log.info{"Changing file description to '$newDescription' for: $fileObject"}
                        fileNode.setProperty(PROPERTY_FILEDESC, newDescription)
                    }
                    session.save()
                    FileObject(fileNode)
                }
            }
        }
    }

    open fun getNodeInfo(absPath: String, recursive: Boolean = false): NodeInfo {
        return runInSession { session ->
            log.info { "Getting node info of path '$absPath'..." }
            val node = session.getNode(absPath)
            NodeInfo(node, recursive)
        }
    }

    private fun getFilesNode(sessionWrapper: SessionWrapper, parentNodePath: String?, relPath: String?, ensureFilesNode: Boolean = false): Node? {
        val parentNode = getNodeOrNull(sessionWrapper, parentNodePath, relPath, false)
        if (parentNode == null) {
            log.warn { "Can't get files of not existing parent node '${getAbsolutePath(parentNode, relPath)}." }
            return null
        }
        return if (ensureFilesNode || parentNode.hasNode(NODENAME_FILES)) {
            ensureNode(parentNode, NODENAME_FILES)
        } else {
            null
        }
    }

    internal fun getFileInfos(filesNode: Node?, parentNodePath: String? = null, relPath: String? = null): List<FileObject>? {
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

    internal fun findFile(filesNode: Node?, fileId: String?, fileName: String? = null): Node? {
        filesNode ?: return null
        if (!filesNode.hasNodes()) {
            return null
        }
        filesNode.nodes?.let {
            while (it.hasNext()) {
                val node = it.nextNode()
                if (node.name == fileId || node.getProperty(PROPERTY_FILENAME).string == fileName) {
                    return node
                }
            }
        }
        return null
    }

    open fun retrieveFile(fileObject: FileObject): Boolean {
        return runInSession { session ->
            val filesNode = getFilesNode(session, fileObject.parentNodePath, fileObject.relPath, false)
            val node = findFile(filesNode, fileObject.fileId, fileObject.fileName)
            if (node == null) {
                log.warn { "File not found in repository: $fileObject" }
                false
            } else {
                fileObject.copyFrom(node)
                fileObject.content = getFileContent(node, fileObject)
                true
            }
        }
    }

    open fun retrieveFileInputStream(fileObject: FileObject): InputStream? {
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

    internal fun getFileContent(node: Node?, fileObject: FileObject): ByteArray? {
        val content = getFileInputStream(node, fileObject)?.use {
            it.readBytes()
        }
        return content
    }

    private fun getFileInputStream(node: Node?, fileObject: FileObject): InputStream? {
        node ?: return null
        log.info { "Reading file from repository '${node.path}': '${fileObject.fileName}'..." }
        var binary: Binary? = null
        try {
            binary = node.getProperty(PROPERTY_FILECONTENT)?.binary
            return binary?.stream
        } finally {
            binary?.dispose()
        }
    }

    internal fun getNode(session: SessionWrapper, parentNodePath: String?, relPath: String? = null, ensureRelNode: Boolean = true): Node {
        return getNodeOrNull(session, parentNodePath, relPath, ensureRelNode)
                ?: throw IllegalArgumentException("Can't find node ${getAbsolutePath(parentNodePath, relPath)}.")
    }

    internal fun getNodeOrNull(session: SessionWrapper, parentNodePath: String?, relPath: String? = null, ensureRelNode: Boolean = true): Node? {
        val absolutePath = getAbsolutePath(parentNodePath)
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

    private fun getAbsolutePath(nodePath: String?): String {
        val path = nodePath?.removePrefix("/")?.removePrefix(mainNodeName)?.removePrefix("/") ?: ""
        return "/$mainNodeName/$path"
    }

    private fun getAbsolutePath(parentNode: Node, relPath: String?): String? {
        val parentPath = parentNode.path
        return getAbsolutePath(parentPath, relPath)
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

    private fun <T> runInSession(method: (sessionWrapper: SessionWrapper) -> T): T {
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
        internal const val NODENAME_FILES = "__FILES"
        internal const val PROPERTY_FILENAME = "fileName"
        internal const val PROPERTY_FILESIZE = "size"
        internal const val PROPERTY_FILECONTENT = "content"
        internal const val PROPERTY_CREATED = "created"
        internal const val PROPERTY_CREATED_BY_USER = "createdByUser"
        internal const val PROPERTY_FILEDESC = "fileDescription"
        internal const val PROPERTY_LAST_UPDATE = "lastUpdate"
        internal const val PROPERTY_LAST_UPDATE_BY_USER = "lastUpdateByUser"
        private const val PROPERTY_RANDOM_ID_LENGTH = 20
        private val ALPHA_CHARSET: Array<Char> = ('a'..'z').toList().toTypedArray()

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
