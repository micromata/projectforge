/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityStatus
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookStatus
import org.projectforge.business.test.TestSetup
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.ui.ValidationError
import java.time.LocalDate

/**
 * The rules under test are declared exactly once, in [BookDO]: `@PropertyInfo(required = true)` on
 * `title` and `status`, `@Column(length = 255)` on `title`. This test asserts that
 * [ValidationUtils.validateFields] finds them without any layout having been built before — which is
 * the situation of a hand-built projectforge-next page, and which the previous implementation (reading
 * the [org.projectforge.ui.ElementsRegistry] cache) got wrong.
 */
class ValidationUtilsTest {
  companion object {
    private val contextUser = TestSetup.init()
  }

  @Test
  fun `required fields are found without a layout having been built`() {
    // Nothing asked the ElementsRegistry for BookDO in this JVM, so its cache is empty for it.
    val errors = ValidationUtils.validateFields(BookDO())
    assertTrue(
      errors.any { it.fieldId == "status" },
      "status is @PropertyInfo(required = true) and unset, expected an error. Got: ${errors.describe()}"
    )
    assertTrue(
      errors.any { it.fieldId == "title" },
      "title is required and unset, expected an error. Got: ${errors.describe()}"
    )
  }

  @Test
  fun `a blank string is as missing as null`() {
    val book = validBook()
    book.title = "   "
    val errors = ValidationUtils.validateFields(book)
    assertEquals(1, errors.size, errors.describe())
    assertEquals("title", errors[0].fieldId)
  }

  @Test
  fun `a valid book yields no error`() {
    assertEquals(0, ValidationUtils.validateFields(validBook()).size)
  }

  @Test
  fun `a string longer than its column is refused, its exact length is not`() {
    val book = validBook()
    book.title = "x".repeat(255) // @Column(length = 255) of BookDO.title.
    assertEquals(0, ValidationUtils.validateFields(book).size, "255 characters fit into the column.")

    book.title = "x".repeat(256)
    val errors = ValidationUtils.validateFields(book)
    assertEquals(1, errors.size, errors.describe())
    assertEquals("title", errors[0].fieldId)
    assertTrue(
      errors[0].message?.contains("255") == true,
      "The message should name the limit, was '${errors[0].message}'."
    )
  }

  /**
   * `authors` is `@Column(length = 1000)` while `title` is 255: the limit must come from the property,
   * not from a default applied to all strings.
   */
  @Test
  fun `the limit is the one of the property`() {
    val book = validBook()
    book.authors = "x".repeat(1000)
    assertEquals(0, ValidationUtils.validateFields(book).size, "authors holds 1000 characters.")

    book.authors = "x".repeat(1001)
    assertEquals(1, ValidationUtils.validateFields(book).size)
  }

  /**
   * The substitutes of an availability entry are references (many-to-many), posted by their ids only: not rows of the
   * form to validate, whose required user name would be missing.
   */
  @Test
  fun `references of a many-to-many collection are no rows to validate`() {
    val entry = AvailabilityDO().also {
      it.user = PFUserDO().also { user -> user.id = 1 }
      it.startDate = LocalDate.of(2030, 1, 1)
      it.endDate = LocalDate.of(2030, 1, 2)
      it.status = AvailabilityStatus.NOT_AVAILABLE
      it.substitutes = mutableSetOf(PFUserDO().also { user -> user.id = 2 })
    }
    assertEquals(0, ValidationUtils.validateFields(entry).size, ValidationUtils.validateFields(entry).describe())
  }

  private fun validBook() = BookDO().also {
    it.title = "ProjectForge"
    it.status = BookStatus.PRESENT
  }

  private fun List<ValidationError>.describe() =
    joinToString { "${it.fieldId}: ${it.message}" }.ifEmpty { "no errors" }
}
