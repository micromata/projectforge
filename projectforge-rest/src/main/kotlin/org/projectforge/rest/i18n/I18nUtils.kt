package org.projectforge.rest.i18n

import org.projectforge.common.FormatterUtils
import org.projectforge.framework.i18n.translateMsg

object I18nUtils {
  fun translateMaxSizeExceeded(fileName: String?, fileSize: Long, maxFileSize: Long): String {
    return translateMsg(
      "file.upload.maxSizeExceeded",
      fileName ?: "<unknown>",
      FormatterUtils.formatBytes(fileSize),
      FormatterUtils.formatBytes(maxFileSize)
    )
  }
}
