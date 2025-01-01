/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.xmlstream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.projectforge.common.BeanHelper;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.xmlstream.converter.IConverter;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Parses objects serialized by the XmlObjectWriter. Uses the dom4j.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class XmlObjectReader
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(XmlObjectReader.class);

  private static final int ABBREVIATE_WARNING = 500;

  private AliasMap aliasMap;

  private Map<Class< ? >, Class< ? >> implementationMapping;

  private XmlRegistry xmlRegistry = XmlRegistry.baseRegistry();

  private String warnings;

  /**
   * This set is used for detecting ignored elements.
   */
  private Set<Element> processedElements;

  /**
   * The values of the enclosed set are the name of the processed attributes. This map is used for detecting ignored attributes.
   */
  private Map<Element, Set<String>> processedAttributes;

  /**
   * Key is the object id (o-id) and value is the already deserialized object.
   */
  private final Map<String, Object> referenceObjects = new HashMap<>();

  private boolean ignoreEmptyCollections;

  /**
   * For customization, the base xml registry of XmlRegistry is used at default.
   * @param xmlRegistry
   */
  public void setXmlRegistry(final XmlRegistry xmlRegistry)
  {
    this.xmlRegistry = xmlRegistry;
  }

  public XmlObjectReader setAliasMap(final AliasMap aliasMap)
  {
    this.aliasMap = aliasMap;
    return this;
  }

  public XmlObjectReader addImplementationMapping(final Class< ? > clazz, final Class< ? > implementationClass)
  {
    if (this.implementationMapping == null) {
      this.implementationMapping = new HashMap<>();
    }
    this.implementationMapping.put(clazz, implementationClass);
    return this;
  }

  public Class< ? > getImplemenationMapping(final Class< ? > clazz)
  {
    if (this.implementationMapping == null) {
      return null;
    } else {
      return this.implementationMapping.get(clazz);
    }
  }

  /**
   * If true then empty collections of the xml will not be initialized. The corresponding field is left null instead of creating a empty
   * collection.
   * @param ignoreEmptyCollections
   */
  public void setIgnoreEmptyCollections(final boolean ignoreEmptyCollections)
  {
    this.ignoreEmptyCollections = ignoreEmptyCollections;
  }

  private AliasMap getAliasMap()
  {
    if (this.aliasMap == null) {
      this.aliasMap = new AliasMap();
    }
    return this.aliasMap;
  }

  /**
   * Could be called multiple times. Parses the given object and all field recursively for annotations from type {@link XmlObject}.
   */
  public void initialize(final Class< ? > clazz)
  {
    initialize(new HashSet<>(), clazz);
  }

  private void initialize(final Set<Class< ? >> processed, final Class< ? > clazz)
  {
    if (processed.contains(clazz)) {
      // Already processed, avoid endless loops:
      return;
    }
    processed.add(clazz);
    if (log.isDebugEnabled()) {
      log.debug("Initializing class '" + clazz.getName() + "'...");
    }
    if (clazz.isAnnotationPresent(XmlObject.class)) {
      final XmlObject xmlObject = clazz.getAnnotation(XmlObject.class);
      if (StringUtils.isNotEmpty(xmlObject.alias())) {
        getAliasMap().put(clazz, xmlObject.alias());
      }
    }
    for (final Field field : BeanHelper.getAllDeclaredFields(clazz)) {
      if (!field.getType().isPrimitive() && !XmlObjectWriter.ignoreField(field)) {
        initialize(processed, field.getType());
      }
    }
  }

  /**
   * @return Any warning messages produced during the xml reading.
   */
  public String getWarnings()
  {
    return warnings;
  }

  public Object read(final String xml)
  {
    final Element element = XmlHelper.fromString(xml);
    processedElements = new HashSet<>();
    processedAttributes = new HashMap<>();
    warnings = null;
    final Object obj = read(element);
    warnings = checkForIgnoredElements(element);
    if (warnings != null) {
      if (warnings.length() > ABBREVIATE_WARNING) {
        log.warn("Warnings while parsing xml:\n" + StringUtils.abbreviate(warnings, ABBREVIATE_WARNING) + " (message abbreviated).");
      } else {
        log.warn("Warnings while parsing xml:\n" + warnings);
      }
    }
    return obj;
  }

  public Object read(final Element el)
  {
    if (el == null) {
      return null;
    }
    final Class< ? > clazz = getClass(el.getName());
    if (clazz != null) {
      final Object obj = read(clazz, el, null, null);
      return obj;
    } else {
      return null;
    }
  }

  /**
   * @param el
   * @return Warning messages if exists, otherwise null.
   */
  public String checkForIgnoredElements(final Element el)
  {
    if (el == null) {
      return null;
    }
    if (processedElements == null || processedAttributes == null) {
      return "No information available";
    }
    final StringBuilder buf = new StringBuilder();
    checkForIgnoredElements(buf, el);
    final String warnings = buf.toString();
    if (StringUtils.isEmpty(warnings)) {
      return null;
    }
    return warnings;
  }

  private void checkForIgnoredElements(final StringBuilder buf, final Element el)
  {
    if (!processedElements.contains(el)) {
      buf.append("Ignored xml element: ").append(el.getPath()).append("\n");
    }
    @SuppressWarnings("rawtypes")
    final List attributes = el.attributes();
    if (attributes != null) {
      final Set<String> attributeSet = processedAttributes.get(el);
      for (final Object attr : attributes) {
        if (attributeSet == null || !attributeSet.contains(((Attribute) attr).getName())) {
          buf.append("Ignored xml attribute: ").append(((Attribute) attr).getPath()).append("\n");
        }
      }
    }
    @SuppressWarnings("rawtypes")
    final List children = el.elements();
    if (children != null) {
      for (final Object child : children) {
        checkForIgnoredElements(buf, (Element) child);
      }
    }
  }

  protected Class< ? > getClass(final String elelementName)
  {
    Class< ? > clazz = null;
    if (getAliasMap().containsAlias(elelementName)) {
      clazz = aliasMap.getClassForAlias(elelementName);
    } else {
      clazz = xmlRegistry.getClassForAlias(elelementName);
    }
    if (clazz == null) {
      try {
        clazz = Class.forName(elelementName);
        final Class< ? > mappingClass = getImplemenationMapping(clazz);
        if (mappingClass != null) {
          clazz = mappingClass;
        }
      } catch (final ClassNotFoundException ex) {
        log.error("Class '" + elelementName + "' not found: " + ex.getMessage());
      }
    }
    return clazz;
  }

  /**
   * Overload this method for manipulating the object creation for new objects. If null is returned, the object is instantiated
   * automatically by class type or alias. If this method returns {@link Status#IGNORE} then this object will be ignored from
   * deserialization.
   * @param clazz
   * @param el
   * @param attrValue
   * @return Always null.
   */
  protected Object newInstance(final Class< ? > clazz, final Element el, final String attrName, final String attrValue)
  {
    return null;
  }

  protected boolean addCollectionEntry(final Collection< ? > col, final Object obj, final Element el)
  {
    return false;
  }

  protected Object fromString(final IConverter< ? > converter, final Element element, final String attrName, final String attrValue)
  {
    final Object obj = converter.fromString(attrValue != null ? attrValue : element.getText());
    if (attrName != null) {
      putProcessedAttribute(element, attrName);
    } else {
      putProcessedElement(element);
    }
    return obj;
  }

  @SuppressWarnings({ "unchecked", "rawtypes"})
  protected Object enumFromString(final Class< ? > clazz, final Element element, final String attrName, final String attrValue)
  {
    final String val = attrValue != null ? attrValue : element.getText();
    Enum enumValue;
    if (StringUtils.isBlank(val) || "null".equals(val)) {
      enumValue = null;
    } else {
      try {
        enumValue = Enum.valueOf((Class) clazz, val);
      } catch (final IllegalArgumentException ex) {
        // Try toUpperCase:
        enumValue = Enum.valueOf((Class) clazz, val.toUpperCase());
      }
    }
    if (attrName != null) {
      putProcessedAttribute(element, attrName);
    } else {
      putProcessedElement(element);
    }
    return enumValue;
  }

  private Object read(final Class< ? > clazz, final Element el, final String attrName, final String attrValue)
  {
    final Attribute refIdAttr = el.attribute(XmlObjectWriter.ATTR_REF_ID);
    if (refIdAttr != null) {
      final String refId = refIdAttr.getText();
      if (StringUtils.isEmpty(refId)) {
        log.error("Invalid ref-id for element '" + el.getName() + "': " + refId);
        return null;
      }
      final Object obj = referenceObjects.get(refId);
      if (obj == null) {
        log.error("Oups, can't find referenced object for element '" + el.getName() + "': " + refId);
      }
      putProcessedElement(el);
      putProcessedAttribute(el, refIdAttr.getName());
      return obj;
    }
    Object value = newInstance(clazz, el, attrName, attrValue);
    if (value == Status.IGNORE) {
      return null;
    }
    final IConverter< ? > converter = xmlRegistry.getConverter(clazz);
    if (converter != null) {
      if (value == null) {
        value = fromString(converter, el, attrName, attrValue);
      }
    } else if (Enum.class.isAssignableFrom(clazz)) {
      if (value == null) {
        value = enumFromString(clazz, el, attrName, attrValue);
      }
    } else if (Collection.class.isAssignableFrom(clazz)) {
      Collection<Object> col = null;
      if (value != null) {
        @SuppressWarnings("unchecked")
        final Collection<Object> c = (Collection<Object>) value;
        col = c;
      } else if (SortedSet.class.isAssignableFrom(clazz)) {
        col = new TreeSet<>();
      } else if (Set.class.isAssignableFrom(clazz)) {
        col = new HashSet<>();
      } else {
        col = new ArrayList<>();
      }
      putProcessedElement(el);
      for (final Object listObject : el.elements()) {
        final Element childElement = (Element) listObject;
        final Object child = read(childElement);
        if (child != null) {
          if (!addCollectionEntry(col, child, childElement)) {
            col.add(child);
          }
        }
      }
      if (!ignoreEmptyCollections || CollectionUtils.isNotEmpty(col)) {
        value = col;
      }
    } else {
      if (value == null) {
        final Class< ? > mappingClass = getImplemenationMapping(clazz);
        if (mappingClass != null) {
          value = BeanHelper.newInstance(mappingClass);
        } else {
          value = BeanHelper.newInstance(clazz);
        }
      }
      if (value != null) {
        final Attribute idAttr = el.attribute(XmlObjectWriter.ATTR_ID);
        if (idAttr != null) {
          final String id = idAttr.getText();
          if (StringUtils.isEmpty(id)) {
            log.error("Invalid id for element '" + el.getName() + "': " + id);
          } else {
            this.referenceObjects.put(id, value);
          }
          putProcessedAttribute(el, idAttr.getName());
        }
        read(value, el);
      }
    }
    return value;
  }

  /**
   * Please note: Does not set the default values, this should be done by the class itself (when declaring the fields or in the
   * constructors).
   * @param obj The object to assign the parameters parsed from the given xml element.
   * @param el
   */
  public void read(final Object obj, final Element el)
  {
    if (el == null) {
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug("Reading object of class '" + obj.getClass().getName() + "'...");
    }
    final Field[] fields = BeanHelper.getAllDeclaredFields(obj.getClass());
    AccessibleObject.setAccessible(fields, true);
    for (final Object listObject : el.attributes()) {
      final Attribute attr = (Attribute) listObject;
      final String key = attr.getName();
      if (StringHelper.isIn(key, XmlObjectWriter.ATTR_ID, XmlObjectWriter.ATTR_REF_ID)) {
        // Do not try to find fields for o-id and ref-id.
        continue;
      }
      final String value = attr.getText();
      proceedElement(obj, fields, el, key, value, true);
    }
    for (final Object listObject : el.elements()) {
      final Element childElement = (Element) listObject;
      final String key = childElement.getName();
      proceedElement(obj, fields, childElement, key, null, false);
    }
    putProcessedElement(el);
  }

  protected void setField(final Field field, final Object obj, final Object value, final Element element, final String key,
      final String attrValue)
  {
    if (value != null) {
      setField(field, obj, value);
    }
  }

  protected void setField(final Field field, final Object obj, final Object value)
  {
    try {
      if (log.isDebugEnabled()) {
        log.debug("Setting field value: " + field.getDeclaringClass().getName() + "." + field.getName() + "=" + value);
      }
      field.set(obj, value);
    } catch (final IllegalArgumentException | IllegalAccessException ex) {
      log.error("Exception encountered "
          + ex
          + ". Ignoring field '"
          + field.getName()
          + "' with value '"
          + value
          + "' in deserialization of class '"
          + obj.getClass()
          + "'.", ex);
    }
  }

  private void proceedElement(final Object obj, final Field[] fields, final Element el, final String key, final String attrValue,
      final boolean isAttribute)
  {
    Field foundField = null;
    if (log.isDebugEnabled()) {
      log.debug("proceedElement '" + obj.getClass() + "'....");
    }
    for (final Field field : fields) {
      if (log.isDebugEnabled()) {
        log.info("proceedElement: field '" + field.getDeclaringClass().getName() + "." + field.getName() + "'.");
      }
      if (XmlObjectWriter.ignoreField(field)) {
        continue;
      }
      final XmlField ann = field.isAnnotationPresent(XmlField.class) ? field.getAnnotation(XmlField.class) : null;
      if (!key.equals(field.getName()) && (ann == null || !key.equals(ann.alias()))) {
        continue;
      }
      foundField = field;
      break;
    }
    if (foundField != null) {
      // Field found:
      final Class< ? > type = foundField.getType();
      final Object value = read(type, el, key, attrValue);
      setField(foundField, obj, value, el, key, attrValue);
      if (isAttribute) {
        putProcessedAttribute(el, key);
      } else {
        putProcessedElement(el);
      }
    } else {
      log.warn("Field '" + key + "' not found.");
    }
  }

  private void putProcessedElement(final Element el)
  {
    if (processedElements != null) {
      processedElements.add(el);
    }
  }

  private void putProcessedAttribute(final Element el, final String attrName)
  {
    if (processedAttributes != null) {
      Set<String> attributeSet = processedAttributes.get(el);
      if (attributeSet == null) {
        attributeSet = new HashSet<>();
        processedAttributes.put(el, attributeSet);
      }
      attributeSet.add(attrName);
    }
  }
}
