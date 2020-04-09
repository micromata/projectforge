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
import org.apache.jackrabbit.commons.JcrUtils
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.jcr.*


private val log = KotlinLogging.logger {}

@Service
open class RepoService {
    /**
     * @param parentNodePath Path, nodes are separated by '/', e. g. "world/germany". The nodes of this path must already exist.
     * For creating top level nodes, set parentNode to null, empty string or "/".
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

    open fun storeFile(fileObject: FileObject) {
        val parentNodePath = fileObject.parentNodePath
        val relPath = fileObject.relPath
        if (parentNodePath == null || relPath == null) {
            throw IllegalArgumentException("Parent node path and/or relPath not given. Can't determine location of file to store: $fileObject")
        }
        val content = fileObject.content ?: ByteArray(0) // Assuming 0 byte file if no content is given.
        runInSession { session ->
            val node = getNode(session, parentNodePath, relPath, false)
            val filesNode = ensureNode(node, NODENAME_FILES)
            val id = fileObject.id ?: createRandomId
            fileObject.id = id
            log.info { "Storing file: $fileObject" }
            val fileNode = filesNode.addNode(id)
            fileNode.setProperty(PROPERTY_FILENAME, fileObject.fileName)
            fileObject.size?.let { fileNode.setProperty(PROPERTY_FILESIZE, it.toLong()) }
            val inputStream = ByteArrayInputStream(content)
            val bin: Binary = session.valueFactory.createBinary(inputStream)
            fileNode.setProperty(PROPERTY_FILECONTENT, session.valueFactory.createValue(bin))
            session.save()
        }
    }

    open fun getFiles(parentNodePath: String?, relPath: String?): List<FileObject> {
        return runInSession { session ->
            val filesNode = getFilesNode(session, parentNodePath, relPath)
            getFiles(filesNode)
        }
    }


    open fun getNodeInfo(absPath: String, recursive: Boolean = false): NodeInfo {
        return runInSession { session ->
            log.info { "Getting node info of path '$absPath'..." }
            val node = session.getNode(absPath)
            NodeInfo(node, recursive)
        }
    }

    private fun getFilesNode(session: Session, parentNodePath: String?, relPath: String?, ensureFilesNode: Boolean = false): Node? {
        val parentNode = getNodeOrNull(session, parentNodePath, relPath)
        if (parentNode == null) {
            log.warn { "Can't get files of not existing parent node '${getFullPath(parentNode, relPath)}." }
            return null
        }
        return if (ensureFilesNode || parentNode.hasNode(NODENAME_FILES)) {
            ensureNode(parentNode, NODENAME_FILES)
        } else {
            null
        }
    }

    internal fun getFiles(filesNode: Node?): List<FileObject> {
        filesNode ?: return emptyList()
        val fileNodes = filesNode.nodes
        if (fileNodes == null || !fileNodes.hasNext()) {
            return emptyList()
        }
        val result = mutableListOf<FileObject>()
        while (fileNodes.hasNext()) {
            val node = fileNodes.nextNode()
            if (node.hasProperty(PROPERTY_FILENAME)) {
                result.add(FileObject(node))
            }
        }
        return result
    }

    internal fun findFile(filesNode: Node?, id: String?, fileName: String? = null): Node? {
        filesNode ?: return null
        if (!filesNode.hasNodes()) {
            return null
        }
        filesNode.nodes?.let {
            while (it.hasNext()) {
                val node = it.nextNode()
                if (node.name == id || node.getProperty(PROPERTY_FILENAME).string == fileName) {
                    return node
                }
            }
        }
        return null
    }

    open fun retrieveFile(file: FileObject): Boolean {
        return runInSession { session ->
            val filesNode = getFilesNode(session, file.parentNodePath, file.relPath, false)
            val node = findFile(filesNode, file.id, file.fileName)
            if (node == null) {
                log.warn { "File not found in repository: $file" }
                false
            } else {
                file.fileName = node.getProperty(PROPERTY_FILENAME).string
                file.id = node.name
                file.content = getFileContent(node)
                true
            }
        }
    }

    internal fun getFileContent(node: Node?): ByteArray? {
        node ?: return null
        log.info { "Reading file from repository: ${node.path}..." }
        var binary: Binary? = null
        var content: ByteArray?
        try {
            binary = node.getProperty(PROPERTY_FILECONTENT)?.binary
            content = binary?.stream?.readBytes()
        } finally {
            binary?.dispose()
        }
        if (content != null) {
            log.info { "Got file from repository: ${node.path}..." }
        }
        return content
    }

    internal fun getNode(session: Session, parentNodePath: String?, relPath: String? = null, ensureRelNode: Boolean = true): Node {
        return getNodeOrNull(session, parentNodePath, relPath, ensureRelNode)
                ?: throw IllegalArgumentException("Can't find node ${getFullPath(parentNodePath, relPath)}.")
    }

    internal fun getNodeOrNull(session: Session, parentNodePath: String?, relPath: String? = null, ensureRelNode: Boolean = true): Node? {
        val parentNode = if (parentNodePath.isNullOrBlank() || parentNodePath == "/") {
            session.rootNode
        } else if (isAbsolute(parentNodePath)) {
            session.getNode(parentNodePath)
        } else {
            session.rootNode.getNode(parentNodePath)
        }
        return when {
            ensureRelNode -> ensureNode(parentNode, relPath)
            parentNode.hasNode(relPath) -> parentNode.getNode(relPath)
            else -> null
        }
    }

    private fun ensureNode(parentNode: Node, relPath: String?): Node {
        relPath ?: return parentNode
        var current: Node = parentNode
        relPath.split("/").forEach {
            current = if (current.hasNode(it)) {
                current.getNode(it)
            } else {
                log.info { "Creating node ${getFullPath(parentNode, it)}." }
                current.addNode(it)
            }
        }
        return current
    }

    /**
     * return true if given path starts with '/'-
     */
    private fun isAbsolute(path: String): Boolean {
        return path.startsWith("/")
    }

    private fun getFullPath(parentNode: Node, relPath: String?): String? {
        val parentPath = parentNode.path
        return getFullPath(parentPath, relPath)
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

    private fun <T> runInSession(method: (session: Session) -> T): T {
        val session: Session = login()
        try {
            return method(session)
        } finally {
            session.logout()
        }
    }

    /*
    private fun decoder(base64: ByteArray?): ByteArray? {
        base64 ?: return null
        return decoder(base64.toString(StandardCharsets.UTF_8))
    }

    private fun decoder(base64Str: String): ByteArray? {
        return Base64.getDecoder().decode(base64Str)
    }
    */

    private lateinit var repository: Repository

    private val credentials = SimpleCredentials("admin", "admin".toCharArray())

    private var initialized = false

    internal fun login(): Session {
        return repository.login(credentials)
    }

    fun init(parameters: Map<String, String>) {
        synchronized(this) {
            if (initialized) {
                throw IllegalArgumentException("Can't initialize repo twice! repo=$this")
            }
            initialized = true
            if (log.isDebugEnabled) {
                log.debug { "Setting system property: derby.stream.error.field=${DerbyUtil::class.java.name}.DEV_NULL" }
            }
            System.setProperty("derby.stream.error.field", "${DerbyUtil::class.java.name}.DEV_NULL")
            log.info { "Initializing Jcr repository: ${parameters.entries.joinToString { "${it.key}='${it.value}'" }}" }
            repository = JcrUtils.getRepository(parameters)
        }
    }

    companion object {
        internal const val NODENAME_FILES = "__FILES"
        internal const val PROPERTY_FILENAME = "fileName"
        internal const val PROPERTY_FILESIZE = "size"
        internal const val PROPERTY_FILECONTENT = "content"
        private const val PROPERTY_RANDOM_ID_LENGTH = 20
        private val ALPHA_CHARSET: Array<Char> = ('a'..'z').toList().toTypedArray()

        internal fun getFullPath(parentPath: String?, relPath: String?): String? {
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
