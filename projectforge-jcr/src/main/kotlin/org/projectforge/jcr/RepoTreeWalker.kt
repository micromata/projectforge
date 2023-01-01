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

import javax.jcr.Node

open class RepoTreeWalker(
  val repoService: RepoService,
  val absPath: String = "/${repoService.mainNodeName}"
) {
  var numberOfVisitedFiles = 0
    private set

  var numberOfVisitedNodes = 0
    private set

  fun walk() {
    repoService.runInSession { session ->
      walk(repoService.getNode(session, absPath, null), true)
    }
  }

  private fun walk(node: Node, isRootNode: Boolean = false) {
    visit(node, isRootNode)
    ++numberOfVisitedNodes
    val fileList = repoService.getFileInfos(node)
    if (!fileList.isNullOrEmpty()) {
      fileList.forEach {
        repoService.findFile(node, it.fileId, null)?.let { fileNode ->
          val fileObject = FileObject(fileNode)
          visitFile(fileNode, fileObject)
          ++numberOfVisitedFiles
        }
      }
    }
    node.nodes?.let {
      while (it.hasNext()) {
        walk(it.nextNode())
      }
    }
  }

  open fun visit(node: Node, isRootNode: Boolean) {}

  /**
   *  val content = repoService.getFileContent(fileNode, fileObject)
   */
  open fun visitFile(fileNode: Node, fileObject: FileObject) {}
}
