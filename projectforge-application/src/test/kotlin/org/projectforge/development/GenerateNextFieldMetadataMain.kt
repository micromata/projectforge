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

package org.projectforge.development

import jakarta.persistence.Entity
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.utils.SourcesUtils
import org.projectforge.ui.ElementInfo
import org.projectforge.ui.ElementsRegistry
import org.projectforge.ui.UIDataTypeUtils
import org.reflections.Reflections
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Generates the field metadata of projectforge-next from the entity classes, so field lengths, types,
 * mandatory fields and enum value lists aren't maintained twice.
 *
 * The declaration in the backend is the single source of truth: `@PropertyInfo` (i18nKey, required,
 * tooltip) at the property and the JPA `@Column` (length, nullable) at its getter. Both are merged by
 * [ElementsRegistry.getElementInfo] — the very same call the UILayout of `/react` and Wicket's fields
 * go through. This generator derives no rule of its own; it only writes down what that call answers.
 * A changed column length therefore reaches the hand-written next forms instead of leaving them
 * silently wrong.
 *
 * Every `@Entity` class with at least one annotated property gets its own file
 * `lib/metadata/<entity>.generated.ts`, exporting a single `<ENTITY>_METADATA` constant. One file per
 * entity and no barrel: nothing imports what it doesn't name, so the bundle only carries the entities
 * a page actually uses. Nested DOs (e.g. `AuftragsPositionDO`) are covered as well although they have
 * no `AbstractPagesRest` of their own — a "only what has a PagesRest" rule would need a hand-kept
 * exception list, which is exactly the staleness this generator removes.
 *
 * The generated files are never edited by hand — [GenerateNextFieldMetadataTest] fails if they differ
 * from what [generate] produces, which catches both a manual edit and an entity change that was
 * committed without regenerating. Run [DevelopmentMainForRelease] to update them.
 */
object GenerateNextFieldMetadataMain {
  private const val OUT_DIR = "projectforge-next/lib/metadata"
  private const val FILE_SUFFIX = ".generated.ts"
  private val ENCODING = StandardCharsets.UTF_8

  /**
   * Package to scan. Covers the plugins (`org.projectforge.plugins.*`) as well.
   */
  private const val SCAN_PACKAGE = "org.projectforge"

  /**
   * Emergency exit for entities that must not be exported (e.g. because their metadata would leak
   * something). Fully qualified class names. Empty on purpose: nothing has needed it so far.
   */
  private val SKIP = emptySet<String>()

  @JvmStatic
  fun main(args: Array<String>) {
    val rootDir = resolveRootDir()
    generate(rootDir).forEach { (fileName, content) ->
      val outFile = outFile(rootDir, fileName)
      outFile.parentFile.mkdirs()
      outFile.writeText(content, ENCODING)
      println("Wrote ${outFile.path}")
    }
  }

  /**
   * Builds the metadata files without writing them, so [GenerateNextFieldMetadataTest] can compare
   * them against the committed files.
   *
   * @param rootDir unused for the content (everything comes from the classpath), but part of the
   *                signature so the test reads exactly like the i18n one.
   * @return file name (without directory, e.g. `book.generated.ts`) to its full content, sorted.
   */
  @Suppress("UNUSED_PARAMETER")
  internal fun generate(rootDir: File): Map<String, String> {
    val entities = Reflections(SCAN_PACKAGE).getTypesAnnotatedWith(Entity::class.java)
      .filterNot { it.name in SKIP }
      .sortedBy { it.name }
    require(entities.isNotEmpty()) { "No @Entity classes found in '$SCAN_PACKAGE' — is the classpath complete?" }

    val result = sortedMapOf<String, String>()
    val fileOwners = mutableMapOf<String, String>() // file name -> class that claimed it
    val skipped = mutableListOf<String>()
    entities.forEach { clazz ->
      val fields = ElementsRegistry.listProperties(clazz).mapNotNull { property ->
        val elementInfo = ElementsRegistry.getElementInfo(clazz, property)
        if (elementInfo == null) {
          skipped.add("${clazz.simpleName}.$property (no ElementInfo)")
          null
        } else {
          val field = buildField(elementInfo)
          if (field == null) {
            // A collection or a foreign DO: not a value the frontend edits as one field. Its own
            // fields are in its own file.
            skipped.add("${clazz.simpleName}.$property (${elementInfo.propertyClass.simpleName})")
            null
          } else {
            property to field
          }
        }
      }
      if (fields.isEmpty()) {
        return@forEach
      }
      val fileName = "${kebabCase(entityName(clazz))}$FILE_SUFFIX"
      val previousOwner = fileOwners.put(fileName, clazz.name)
      require(previousOwner == null) {
        "Two entities map to '$fileName': $previousOwner and ${clazz.name}. Add one of them to SKIP" +
            " or give the generated files a package-qualified name."
      }
      result[fileName] = render(clazz, fields)
    }
    if (skipped.isNotEmpty()) {
      println("Not exported (${skipped.size} properties): ${skipped.joinToString()}")
    }
    return result
  }

  internal fun outFile(rootDir: File, fileName: String) = File(rootDir, "$OUT_DIR/$fileName")

  internal fun outDir(rootDir: File) = File(rootDir, OUT_DIR)

  /**
   * The repository root. Needed because Gradle runs tests in the module directory while [main] is
   * started from the IDE with the root as working directory.
   */
  internal fun resolveRootDir(): File = SourcesUtils.getBasePath().toFile()

  /**
   * The properties of one field, in the order they are emitted. `null` if the property type has no
   * frontend representation.
   */
  private fun buildField(elementInfo: ElementInfo): List<Pair<String, String>>? {
    val propertyClass = elementInfo.propertyClass
    val isEnum = propertyClass.isEnum
    // Enums travel as their constant name, so the frontend treats them as strings.
    val dataType = if (isEnum) "STRING" else UIDataTypeUtils.getDataType(elementInfo)?.name ?: return null
    val properties = mutableListOf<Pair<String, String>>()
    properties.add("dataType" to quote(dataType))
    elementInfo.i18nKey?.let { properties.add("i18nKey" to quote(it)) }
    properties.add("required" to (elementInfo.required == true).toString())
    if (elementInfo.readOnly) {
      properties.add("readOnly" to "true")
    }
    // Only for strings: @Column.length is preset to 255 even for a number or a date, and the 20 of an
    // enum column is the storage size of the constant name, not a limit the user could hit.
    if (propertyClass == String::class.java) {
      elementInfo.maxLength?.let { properties.add("maxLength" to it.toString()) }
    }
    elementInfo.tooltipI18nKey?.let { properties.add("tooltipI18nKey" to quote(it)) }
    if (isEnum) {
      properties.add("enumValues" to renderEnumValues(propertyClass))
    }
    return properties
  }

  /**
   * The constants of an enum property with the i18n key each of them is labelled with (same source as
   * `UISelect.buildValues`). A plain enum not implementing [I18nEnum] yields the values only; the
   * frontend then has nothing but the constant name to show, which is still better than a free text
   * field.
   */
  private fun renderEnumValues(enumClass: Class<*>): String {
    val sb = StringBuilder("[\n")
    val constants = enumClass.enumConstants ?: emptyArray()
    constants.forEach { constant ->
      val name = (constant as Enum<*>).name
      sb.append("        {\n")
      sb.append("          value: ").append(quote(name)).append(",\n")
      (constant as? I18nEnum)?.i18nKey?.let {
        sb.append("          i18nKey: ").append(quote(it)).append(",\n")
      }
      sb.append("        },\n")
    }
    sb.append("      ]")
    return sb.toString()
  }

  /**
   * Emits prettier-compatible TypeScript by construction: every object expanded with one property per
   * line, two spaces of indentation, double quotes and trailing commas. `lib/metadata` is not in
   * `.prettierignore`, so `format:check` covers these files like any other.
   */
  private fun render(clazz: Class<*>, fields: List<Pair<String, List<Pair<String, String>>>>): String {
    val sb = StringBuilder()
    sb.append("// Generated by GenerateNextFieldMetadataMain (DevelopmentMainForRelease) — do not edit.\n")
    sb.append("// Source of every rule: ").append(clazz.name).append(" (@PropertyInfo + JPA @Column),\n")
    sb.append("// merged by ElementsRegistry.getElementInfo. Change the entity, then regenerate.\n")
    sb.append("\n")
    sb.append("import type { EntityMetadata } from \"./types\";\n")
    sb.append("\n")
    sb.append("export const ").append(constantName(clazz)).append(" = {\n")
    sb.append("  entity: ").append(quote(clazz.simpleName)).append(",\n")
    sb.append("  fields: {\n")
    fields.forEach { (property, properties) ->
      sb.append("    ").append(propertyKey(property)).append(": {\n")
      properties.forEach { (key, value) ->
        sb.append("      ").append(key).append(": ").append(value).append(",\n")
      }
      sb.append("    },\n")
    }
    sb.append("  },\n")
    sb.append("} as const satisfies EntityMetadata;\n")
    return sb.toString()
  }

  /**
   * Nested properties (`task.id`) are no valid identifiers, so they need quoting. Everything else is
   * left bare, as prettier would.
   */
  private fun propertyKey(property: String) =
    if (property.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]*"))) property else quote(property)

  /** `BookDO` -> `Book`, `AuftragsPositionDO` -> `AuftragsPosition`. */
  internal fun entityName(clazz: Class<*>) = clazz.simpleName.removeSuffix("DO")

  /** `AuftragsPosition` -> `auftrags-position`. */
  internal fun kebabCase(name: String) = name
    .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
    .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1-$2")
    .lowercase()

  /** `AuftragsPositionDO` -> `AUFTRAGS_POSITION_METADATA`. */
  private fun constantName(clazz: Class<*>) =
    "${kebabCase(entityName(clazz)).replace('-', '_').uppercase()}_METADATA"

  private fun quote(value: String): String {
    val sb = StringBuilder("\"")
    value.forEach { c ->
      when (c) {
        '"' -> sb.append("\\\"")
        '\\' -> sb.append("\\\\")
        '\n' -> sb.append("\\n")
        '\r' -> sb.append("\\r")
        '\t' -> sb.append("\\t")
        else -> sb.append(c)
      }
    }
    return sb.append('"').toString()
  }
}
