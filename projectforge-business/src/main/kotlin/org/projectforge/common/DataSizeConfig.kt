package org.projectforge.common

import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit

object DataSizeConfig {
  @JvmStatic
  fun init(value: String?, dataUnit: DataUnit? = null): DataSize {
    return if (value != null) {
      DataSize.parse(value, dataUnit)
    } else {
      DataSize.ofMegabytes(1)
    }
  }
}
