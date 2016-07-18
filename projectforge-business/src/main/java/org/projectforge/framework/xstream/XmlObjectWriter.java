/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.framework.xstream;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.tree.DefaultCDATA;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.xstream.converter.IConverter;

/**
 * Serializes objects to xml. A simple solution for streaming xml objects and to prevent default values from the xml output (because this
 * feature isn't yet available in XStream). It's only fit the ProjectForge requirements and is not very useful as generic xml streaming
 * package.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class XmlObjectWriter
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(XmlObjectWriter.class);

  public static final String ATTR_ID = "o-id";

  public static final String ATTR_REF_ID = "ref-id";

  private boolean onlyAnnotatedFields;

  private AliasMap aliasMap;

  private int refIdCounter = 0;

  private XmlRegistry xmlRegistry = XmlRegistry.baseRegistry();

  /**
   * Key is the class name with the hashCode of the object, e. g.: org.projectforge.xml.stream.TestObject:987345678854 and the value is the
   * element where the object was written to.
   */
  private Map<String, Element> writtenObjects = new HashMap<String, Element>();

  /**
   * For customization, the base xml registry of XmlRegistry is used at default.
   * @param xmlRegistry
   */
  public XmlObjectWriter setXmlRegistry(final XmlRegistry xmlRegistry)
  {
    this.xmlRegistry = xmlRegistry;
    return this;
  }

  public XmlObjectWriter setAliasMap(final AliasMap aliasMap)
  {
    this.aliasMap = aliasMap;
    return this;
  }

  private AliasMap getAliasMap()
  {
    if (this.aliasMap == null) {
      this.aliasMap = new AliasMap();
    }
    return this.aliasMap;
  }

  /**
   * Writes the given object as xml.
   * @param obj
   */
  public static String writeAsXml(final Object obj, final boolean prettyFormat)
  {
    return writeAsXml(obj, null, prettyFormat);
  }

  /**
   * Writes the given object as xml.
   * @param obj
   */
  public static String writeAsXml(final Object obj)
  {
    return writeAsXml(obj, null, false);
  }

  /**
   * Writes the given object as xml.
   * @param obj
   * @param aliasMap
   */
  public static String writeAsXml(final Object obj, final AliasMap aliasMap)
  {
    return writeAsXml(obj, aliasMap, false);
  }

  /**
   * Writes the given object as xml.
   * @param obj
   * @param aliasMap
   * @param prettyFormat
   */
  public static String writeAsXml(final Object obj, final AliasMap aliasMap, final boolean prettyFormat)
  {
    final XmlObjectWriter xmlWriter = new XmlObjectWriter();
    if (aliasMap != null) {
      xmlWriter.setAliasMap(aliasMap);
    }
    return xmlWriter.writeToXml(obj, prettyFormat);
  }

  /**
   * Reader and Writer will ignore static and final fields.
   * @return true if the field has to be ignored by the writer.
   */
  public static boolean ignoreField(final Field field)
  {
    final int modifiers = field.getModifiers();
    return Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) == true || Modifier.isTransient(modifiers);
  }

  public XmlObjectWriter setOnlyAnnotatedFields(boolean onlyAnnotatedFields)
  {
    this.onlyAnnotatedFields = onlyAnnotatedFields;
    return this;
  }

  public String writeToXml(final Object obj)
  {
    return writeToXml(obj, false);
  }

  public String writeToXml(final Object obj, final boolean prettyFormat)
  {
    final Document document = DocumentHelper.createDocument();
    final Element element = write(document, obj);
    return XmlHelper.toString(element, prettyFormat);
  }

  public Element write(final Branch parent, final Object obj)
  {
    reset();
    return write(parent, obj, null, false, false);
  }

  private void reset()
  {
    this.writtenObjects.clear();
  }

  private Element write(final Branch parent, final Object obj, String name, final boolean asAttribute, final boolean asCDATA)
  {
    if (obj == null) {
      return null;
    }
    final Class< ? > type = obj.getClass();
    if (name == null) {
      name = getAliasMap().getAliasForClass(type);
    }
    if (name == null && obj.getClass().isAnnotationPresent(XmlObject.class) == true) {
      final XmlObject xmlObject = obj.getClass().getAnnotation(XmlObject.class);
      if (StringUtils.isNotEmpty(xmlObject.alias()) == true) {
        name = xmlObject.alias();
      }
    }
    if (name == null) {
      name = xmlRegistry.getAliasForClass(type);
    }
    if (name == null) {
      name = obj.getClass().getName();
    }
    if (isRegistered(obj) == true) {
      final Element el = getRegisteredElement(obj);
      Integer refId;
      final Attribute attr = el.attribute(ATTR_ID);
      if (attr == null) {
        // Id attribute not yet written. So add this attribute:
        refId = refIdCounter++;
        el.addAttribute(ATTR_ID, String.valueOf(refId));
      } else {
        refId = NumberHelper.parseInteger(attr.getText());
      }
      if (refId == null) {
        log.error("Can't parse ref id: " + attr.getText());
      } else {
        final Element element = parent.addElement(name);
        element.addAttribute(ATTR_REF_ID, String.valueOf(refId));
        return element;
      }
    }
    IConverter< ? > converter = xmlRegistry.getConverter(type);
    if (converter != null) {
      final String sValue = converter.toString(obj);
      writeValue(parent, obj, name, sValue, asAttribute, asCDATA);
      return (Element) parent;
    } else if (Enum.class.isAssignableFrom(type) == true) {
      final String sValue = ((Enum< ? >) obj).name();
      writeValue(parent, obj, name, sValue, asAttribute, asCDATA);
      return (Element) parent;
    } else if (obj instanceof Collection< ? >) {
      final Element listElement = parent.addElement(name);
      final Iterator< ? > it = ((Collection< ? >) obj).iterator();
      while (it.hasNext() == true) {
        write(listElement, it.next(), null, false, false);
      }
      return listElement;
    }
    final Element element = parent.addElement(name);
    registerElement(obj, element);
    final Field[] fields = BeanHelper.getAllDeclaredFields(obj.getClass());
    AccessibleObject.setAccessible(fields, true);
    for (final Field field : fields) {
      if (field.isAnnotationPresent(XmlOmitField.class) == true || ignoreField(obj, field) == true) {
        continue;
      }
      final XmlField ann = field.isAnnotationPresent(XmlField.class) == true ? field.getAnnotation(XmlField.class) : null;
      if (onlyAnnotatedFields == true && field.isAnnotationPresent(XmlField.class) == false) {
        continue;
      }
      final Object value = BeanHelper.getFieldValue(obj, field);
      writeField(field, obj, value, ann, element);
    }
    return element;
  }

  protected void writeField(final Field field, final Object obj, final Object fieldValue, final XmlField annotation, final Element element)
  {
    if (fieldValue == null || isDefaultType(annotation, fieldValue) == true) {
      return;
    }
    boolean childAsAttribute = false;
    if (annotation != null) {
      if (annotation.asElement() == false && (asAttributeAsDefault(field.getType()) == true || annotation.asAttribute() == true)) {
        childAsAttribute = true;
      }
    } else if (asAttributeAsDefault(field.getType()) == true) {
      childAsAttribute = true;
    }
    final boolean childAsCDATA = (annotation != null && annotation.asCDATA() == true);
    final String childName;
    if (annotation != null && StringUtils.isNotEmpty(annotation.alias()) == true) {
      childName = annotation.alias();
    } else {
      childName = field.getName();
    }
    write(element, fieldValue, childName, childAsAttribute, childAsCDATA);
  }

  protected void addAttribute(final Element element, final Object obj, final String name, final String value)
  {
    element.addAttribute(name, value);
  }

  private void writeValue(final Branch branch, final Object obj, final String key, final String sValue, final boolean asAttribute,
      final boolean asCDATA)
  {
    if (sValue == null) {
      return;
    }
    if (asAttribute == true) {
      addAttribute((Element) branch, obj, key, sValue);
    } else if (asCDATA == true) {
      branch.addElement(key).add(new DefaultCDATA(sValue));
    } else {
      branch.addElement(key).setText(sValue);
    }
  }

  private void registerElement(final Object obj, final Element element)
  {
    writtenObjects.put(obj.getClass().getName() + ":" + obj.hashCode(), element);
  }

  private Element getRegisteredElement(final Object obj)
  {
    return writtenObjects.get(obj.getClass().getName() + ":" + obj.hashCode());
  }

  private boolean isRegistered(final Object obj)
  {
    return writtenObjects.containsKey(obj.getClass().getName() + ":" + obj.hashCode());
  }

  /**
   * Overload this method for further control. Don't forget to call super!
   * @param obj
   * @param field
   * @return true if the field has to be ignored by the writer.
   * @see #ignoreField(Field)
   */
  protected boolean ignoreField(final Object obj, final Field field)
  {
    return ignoreField(field);
  }

  /**
   * For some types (primitive types as double and int) serialization as attributes instead of elements is used.
   * @param type
   * @return true if for the given type a serialization as attribute should be used at default.
   */
  public boolean asAttributeAsDefault(final Class< ? > type)
  {
    return xmlRegistry.asAttributeAsDefault(type) || Enum.class.isAssignableFrom(type) == true;
  }

  private static boolean isDefaultType(final XmlField ann, final Object value)
  {
    if (hasDefaultType(ann, value) == false) {
      // No default value given in the annotation.
      return false;
    }
    if (value instanceof Boolean) {
      if (ann != null) {
        return ann.defaultBooleanValue() == (Boolean) value;
      } else {
        return (Boolean) value == false;
      }
    } else if (value instanceof Double) {
      return ((Double) value).compareTo(ann.defaultDoubleValue()) == 0;
    } else if (value instanceof Integer) {
      return ((Integer) value).compareTo(ann.defaultIntValue()) == 0;
    } else if (value instanceof String) {
      return ann.defaultStringValue().equals((String) value) == true;
    } else if (Enum.class.isAssignableFrom(value.getClass()) == true) {
      return ann.defaultStringValue().equals(((Enum< ? >) value).name()) == true;
    }
    return false;
  }

  /**
   * @param ann
   * @param value
   * @return True if the annotation has the default type (meaning that no value is set in the annotation as default type).
   */
  private static boolean hasDefaultType(final XmlField ann, final Object value)
  {
    if (value instanceof Boolean) {
      // Boolean values have already default type false, if nothing else declared.
      return true;
    } else if (ann == null) {
      return false;
    } else if (value instanceof Double) {
      return Double.isNaN(ann.defaultDoubleValue()) == false;
    } else if (value instanceof Integer) {
      return ann.defaultIntValue() != XmlConstants.MAGIC_INT_NUMBER;
    } else if (value instanceof String || Enum.class.isAssignableFrom(value.getClass()) == true) {
      return XmlConstants.MAGIC_STRING.equals(ann.defaultStringValue()) == false;
    }
    return false;
  }
}
