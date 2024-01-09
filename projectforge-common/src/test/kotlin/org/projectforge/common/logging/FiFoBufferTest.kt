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

package org.projectforge.common.logging

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.InterruptedException
import java.util.ArrayList

class FiFoBufferTest {
  private var fiFoBuffer: FiFoBuffer<Long>? = null
  private var counter = 0L
  private val threads = ArrayList<Thread>()
  @Test
  fun test() {
    fiFoBuffer = FiFoBuffer(1000)
    for (i in 0..9) {
      startProducerThread()
      startConsumerThread(i % 2 == 0)
    }
    for (thread in threads) {
      try {
        thread.join()
      } catch (ex: InterruptedException) {
      }
    }
    Assertions.assertEquals(100000, counter)
  }

  private fun startProducerThread() {
    val thread: Thread = object : Thread() {
      override fun run() {
        for (i in 0..9999) {
          var value: Long
          synchronized(threads) { value = ++counter }
          fiFoBuffer!!.add(value)
        }
      }
    }
    thread.start()
    threads.add(thread)
  }

  private fun startConsumerThread(ascending: Boolean) {
    val thread: Thread = object : Thread() {
      override fun run() {
        for (i in 0..999) {
          if (ascending) {
            for (j in 0 until fiFoBuffer!!.size) {
              fiFoBuffer!![j]
            }
          } else {
            for (j in fiFoBuffer!!.size downTo 0) {
              fiFoBuffer!![j]
            }
          }
        }
      }
    }
    thread.start()
    threads.add(thread)
  }
}
