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

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.xmlstream.converter.ISODateConverter;
import org.projectforge.business.test.AbstractTestBase;

import java.lang.reflect.Field;
import java.time.Month;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class XmlStreamTest extends AbstractTestBase {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(XmlStreamTest.class);

  @Override
  protected void afterAll() {
    recreateDataBase();
  }

  @Test
  public void testWrite() {
    final XmlObjectWriter writer = new XmlObjectWriter();
    final Document document = DocumentHelper.createDocument();
    final Element root = document.addElement("root");
    TestObject obj = create(null, "", null, null, null, "", Double.NaN, 0.0, XmlConstants.MAGIC_INT_NUMBER,
            XmlConstants.MAGIC_INT_NUMBER);
    Element el = writer.write(root, obj);
    assertEquals("test", el.getName());
    containsAttrs(el, "string2", "d1", "d2", "i1", "i2");
    containsElements(el, "t2");
    containsNotAttrs(el, "color", "s1", "s3", "s4"); // Non serializable fields or fields with default or null value.
    containsNotElements(el, "t1");
    obj = create("ds", XmlConstants.MAGIC_STRING, "Hurzel", "", "Hurzel", "Hurzel", 5.0, 5.0, 0, 42);
    obj.color1 = obj.color2 = TestEnum.BLUE;
    el = writer.write(root, obj);
    containsAttrs(el, "color1", "s1", "string2", "d1", "i1");
    containsElements(el, "t1");
    containsNotAttrs(el, "color2", "s3", "s4", "d2", "i2"); // Non serializable fields or fields with default value.
    containsNotElements(el, "t2");
  }

  @Test
  public void testOmitFields() {
    final XmlObjectWriter writer = new XmlObjectWriter();
    final Document document = DocumentHelper.createDocument();
    final Element root = document.addElement("root");
    final TestObject obj = new TestObject();
    obj.s0 = "s0";
    obj.t0 = "t0";
    obj.color1 = obj.color2 = TestEnum.RED;
    Element el = writer.write(root, obj);
    containsElements(el, "s0");
    containsAttrs(el, "color1", "color2");
    containsNotAttrs(el, "OMIT_STATIC", "omitFinal", "omitTransient", "s0", "t0");
    containsNotElements(el, "t0");
    writer.setOnlyAnnotatedFields(true);
    el = writer.write(root, obj);
    containsNotAttrs(el, "color1", "s0", "t0");
    containsNotElements(el, "s0", "t0");
  }

  @Test
  public void testDefaultBooleanValue() {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(TestObject.class);
    TestObject obj = new TestObject();
    obj.b1 = obj.b2 = obj.b3 = false;
    String xml = XmlObjectWriter.writeAsXml(obj);
    assertTrue(!xml.contains("b1"), "b1 shouldn't be present.");
    assertTrue(xml.contains("b2=\"false\""), "b1 shouldn't be present.");
    assertTrue(!xml.contains("b3"), "b3 shouldn't be present.");
    obj = (TestObject) reader.read(xml);
    assertFalse(obj.b1, "b1");
    assertFalse(obj.b2, "b2");
    assertFalse(obj.b3, "b3");

    obj.b1 = obj.b2 = obj.b3 = true;
    xml = XmlObjectWriter.writeAsXml(obj);
    assertTrue(xml.contains("b1=\"true\""), "b1 should be present.");
    assertTrue(!xml.contains("b2"), "b2 shouldn't be present.");
    assertTrue(xml.contains("b3=\"true\""), "b3 should be present.");
    obj = (TestObject) reader.read(xml);
    assertTrue(obj.b1, "b1");
    assertTrue(obj.b2, "b2");
    assertTrue(obj.b3, "b3");

    obj = (TestObject) reader.read("<test />");
    assertFalse(obj.b1);
    assertTrue(obj.b2);
    assertFalse(obj.b3);
  }

  @Test
  public void testDateValue() {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(TestObject.class);
    TestObject obj = new TestObject();
    String xml = XmlObjectWriter.writeAsXml(obj);
    obj = (TestObject) reader.read(xml);
    assertNull(obj.date, "date should be null.");
    final PFDateTime dateTime = PFDateTime.withDate(2010, Month.AUGUST, 3);
    obj.date = dateTime.getUtilDate();
    xml = XmlObjectWriter.writeAsXml(obj);
    obj = (TestObject) reader.read(xml);
    assertEquals(dateTime.getEpochMilli(), obj.date.getTime(), "date");
  }

  @Test
  public void testWriteComplexTypes() {
    final XmlObjectWriter writer = new XmlObjectWriter();
    final Document document = DocumentHelper.createDocument();
    final Element root = document.addElement("root");
    final TestObject2 testObject2 = new TestObject2();
    Element el = writer.write(root, testObject2);
    assertEquals(TestObject2.class.getName(), el.getName());
    containsNotElements(el, "testObject", "list");

    testObject2.testObject = create("ds", XmlConstants.MAGIC_STRING, "Hurzel", "", "Hurzel", "Hurzel", 5.0, 5.0, 0, 42);
    el = writer.write(root, testObject2);
    containsElements(el, "testObject");
    containsNotElements(el, "list");
    Element el2 = el.element("testObject");
    assertEquals("ds", el2.attribute("s1").getText(), "s1");

    testObject2.list = new ArrayList<>();
    el = writer.write(root, testObject2);
    containsElements(el, "testObject", "list");
    el2 = el.element("list");
    containsNotElements(el2, "test");
    testObject2.list.add(create("1", "", "", "", "", "", 0.0, 0.0, 0, 0));
    testObject2.list.add(create("2", "", "", "", "", "", 0.0, 0.0, 0, 0));
    testObject2.list.add(create("3", "", "", "", "", "", 0.0, 0.0, 0, 0));
    el = writer.write(root, testObject2);
    el2 = el.element("list");
    containsElements(el2, "test");
    final Iterator<?> it = el2.elementIterator("test");
    assertEquals("1", ((Element) it.next()).attribute("s1").getText(), "s1");
    assertEquals("2", ((Element) it.next()).attribute("s1").getText(), "s1");
    assertEquals("3", ((Element) it.next()).attribute("s1").getText(), "s1");
    assertFalse(it.hasNext(), "list should have only 3 elements");
    // log.info(XmlHelper.toString(el, true));
  }

  @Test
  public void testWriteReadProperties() {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(TestObject2.class);
    final XmlObjectWriter writer = new XmlObjectWriter();
    final Document document = DocumentHelper.createDocument();
    final Element root = document.addElement("root");
    TestObject obj = create(null, "", null, null, null, "", Double.NaN, 0.0, XmlConstants.MAGIC_INT_NUMBER,
            XmlConstants.MAGIC_INT_NUMBER);
    obj.color2 = TestEnum.RED;
    obj.s0 = "s0";
    Element el = writer.write(root, obj);
    obj = (TestObject) reader.read(el);
    assertValues(obj, null, "", "Hurzel", "", null, "", Double.NaN, 0.0, XmlConstants.MAGIC_INT_NUMBER,
            XmlConstants.MAGIC_INT_NUMBER);
    assertEquals("s0", "s0", obj.s0);
    assertNull(obj.color1, "color1");
    assertEquals(TestEnum.RED, obj.color2, "color2");
    obj = create("ds", XmlConstants.MAGIC_STRING, "Hurzel", "", "Hurzel", "Hurzel", 5.0, 5.0, 0, 42);
    obj.s0 = "s0";
    obj.color1 = obj.color2 = TestEnum.BLUE;
    el = writer.write(root, obj);
    obj = (TestObject) reader.read(el);
    assertValues(obj, "ds", XmlConstants.MAGIC_STRING, "Hurzel", "", "Hurzel", "Hurzel", 5.0, 5.0, 0, 42);
    assertEquals("s0", "s0", obj.s0);
    assertEquals(TestEnum.BLUE, obj.color1, "color1");
    assertEquals(TestEnum.BLUE, obj.color2, "color2");
  }

  @Test
  public void testReadWriteComplexTypes() {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(TestObject2.class);
    final XmlObjectWriter writer = new XmlObjectWriter();
    final Document document = DocumentHelper.createDocument();
    final Element root = document.addElement("root");
    final TestObject2 testObject2 = new TestObject2();
    Element el = writer.write(root, testObject2);
    TestObject2 o2 = (TestObject2) reader.read(el);
    assertNull(o2.testObject);
    assertNull(o2.list);
    testObject2.testObject = create("ds", XmlConstants.MAGIC_STRING, "Hurzel", "", "Hurzel", "Hurzel", 5.0, 5.0, 0, 42);
    el = writer.write(root, testObject2);
    o2 = (TestObject2) reader.read(el);
    assertEquals("ds", o2.testObject.s1);
    assertNull(o2.list);
    testObject2.list = new ArrayList<>();
    el = writer.write(root, testObject2);
    o2 = (TestObject2) reader.read(el);
    assertEquals("ds", o2.testObject.s1);
    assertEquals(0, o2.list.size());
    testObject2.list.add(create("1", "", "", "", "", "", 0.0, 0.0, 0, 0));
    testObject2.list.add(create("2", "", "", "", "", "", 0.0, 0.0, 0, 0));
    testObject2.list.add(create("3", "", "", "", "", "", 0.0, 0.0, 0, 0));
    el = writer.write(root, testObject2);
    o2 = (TestObject2) reader.read(el);
    assertEquals("ds", o2.testObject.s1);
    assertEquals(3, o2.list.size());
    final Iterator<TestObject> it = o2.list.iterator();
    assertEquals("1", it.next().s1);
    assertEquals("2", it.next().s1);
    assertEquals("3", it.next().s1);
  }

  @Test
  public void testTypeSet() {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(TestObject2.class);
    TestObject2 obj2 = new TestObject2();
    String xml = XmlObjectWriter.writeAsXml(obj2);
    obj2 = (TestObject2) reader.read(xml);
    assertNull(obj2.set);
    assertNull(obj2.intSet);
    obj2.set = new HashSet<>();
    obj2.intSet = new HashSet<>();
    xml = XmlObjectWriter.writeAsXml(obj2);
    obj2 = (TestObject2) reader.read(xml);
    assertEquals(0, obj2.set.size(), "Set should be empty.");
    assertEquals(0, obj2.intSet.size(), "intSet should be empty.");
    TestObject obj = new TestObject();
    obj.s1 = "1";
    obj2.set.add(obj);
    obj = new TestObject();
    obj.s1 = "2";
    obj2.set.add(obj);
    obj = new TestObject();
    obj.s1 = "3";
    obj2.set.add(obj);
    obj2.intSet.add(1);
    obj2.intSet.add(2);
    obj2.intSet.add(4);
    xml = XmlObjectWriter.writeAsXml(obj2);
    obj2 = (TestObject2) reader.read(xml);
    assertEquals(3, obj2.set.size(), "Set should have 3 entries.");
    assertEquals(3, obj2.intSet.size(), "intSet should have 3 entries.");
    int sum = 0;
    for (final Integer value : obj2.intSet) {
      sum += value;
    }
    assertEquals(7, sum, "intSet: sum of all entries should wrong.");
  }

  @Test
  public void testIgnoreEmptyCollections() {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(TestObject2.class);
    TestObject2 obj2 = new TestObject2();
    String xml = XmlObjectWriter.writeAsXml(obj2);
    obj2 = (TestObject2) reader.read(xml);
    assertNull(obj2.set);
    assertNull(obj2.intSet);
    obj2.set = new HashSet<>();
    obj2.intSet = new HashSet<>();
    xml = XmlObjectWriter.writeAsXml(obj2);
    obj2 = (TestObject2) reader.read(xml);
    assertEquals(0, obj2.set.size(), "Set should be empty.");
    assertEquals(0, obj2.intSet.size(), "intSet should be empty.");

    reader.setIgnoreEmptyCollections(true);
    obj2 = (TestObject2) reader.read(xml);
    assertNull(obj2.set, "Set should be null.");
    assertNull(obj2.intSet, "intSet should be null.");
  }

  @Test
  public void testReadWriteXmlString() {
    TestObject obj = new TestObject();
    final String xml = XmlObjectWriter.writeAsXml(obj);
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(TestObject.class);
    obj = (TestObject) reader.read(xml);
  }

  @Test
  public void testAlias() {
    final AliasMap aliasMap = new AliasMap();
    aliasMap.put(TestObject.class, "testAlias1");
    TestObject obj = new TestObject();
    obj.s0 = "s0";
    final String xml = XmlObjectWriter.writeAsXml(obj, aliasMap);
    assertTrue(xml.startsWith("<testAlias1 "), "Should start with '<testAlias1 ': " + xml);
    assertTrue(xml.endsWith("</testAlias1>"), "Should end with '</testAlias1>': " + xml);
    final XmlObjectReader reader = new XmlObjectReader();
    reader.setAliasMap(aliasMap);
    reader.initialize(TestObject.class);
    obj = (TestObject) reader.read(xml);
    assertEquals("s0", "s0", obj.s0);
  }

  @Test
  public void ignoreFields() {
    final XmlObjectWriter writer = new XmlObjectWriter() {
      @Override
      protected boolean ignoreField(final Object obj, final Field field) {
        if (obj instanceof TestObject && field.getName().equals("s1")) {
          return true;
        }
        return super.ignoreField(obj, field);
      }
    };
    TestObject obj = new TestObject();
    obj.s1 = "should be ignored.";
    obj.s2 = "s2";
    final String xml = writer.writeToXml(obj);
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(TestObject.class);
    obj = (TestObject) reader.read(xml);
    assertNull(obj.s1, "s1 should be ignored.");
    assertEquals("s2", obj.s2, "s2 shouldn't be ignored.");
  }

  @Test
  public void implementationMapping() {
    TestObject2 obj = new TestObject2();
    obj.testObjectIFace = new TestObject();
    ((TestObject) obj.testObjectIFace).s1 = "iface";
    final String xml = XmlObjectWriter.writeAsXml(obj);
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(TestObject2.class);
    BeanHelper.enterTestMode();
    obj = (TestObject2) reader.read(xml); // throws java.lang.InstantiationException
    assertNull(obj.testObjectIFace);
    reader.addImplementationMapping(TestObjectIFace.class, TestObject.class);
    obj = (TestObject2) reader.read(xml); // throws java.lang.InstantiationException
    log.info("***** TESTMODE: The last warning message of XmlObjectReader while parsing xml was expected.");
    BeanHelper.exitTestMode();
    assertNotNull(obj.testObjectIFace);
    assertEquals("iface", ((TestObject) obj.testObjectIFace).s1);
  }

  @Test
  public void testRefIds() {
    TestObject obj = new TestObject();
    obj.testObject = obj; // Self reference.
    final String testString = "testRefIds";
    obj.s1 = testString;
    TestObject2 obj2 = new TestObject2();
    obj2.testObject = obj;
    obj2.testObjectIFace = obj;
    obj2.list = new ArrayList<>();
    obj2.list.add(obj);
    obj = new TestObject();
    obj.s1 = "Fin";
    obj2.list.add(obj);
    obj2.list.add(obj);
    final String xml = XmlObjectWriter.writeAsXml(obj2);
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(TestObject2.class);
    obj2 = (TestObject2) reader.read(xml);
    assertEquals(testString, obj2.testObject.s1);
    assertEquals(obj2.testObject, obj2.testObjectIFace);
    assertEquals(obj2.testObject, obj2.testObject.testObject);
    assertEquals(3, obj2.list.size(), "list should contain 3 elements");
    final ArrayList<TestObject> list = (ArrayList<TestObject>) obj2.list;
    assertEquals(obj2.testObject, list.get(0));
    obj = list.get(1);
    assertEquals("Fin", obj.s1);
    assertEquals(list.get(1), list.get(2));
  }

  @XmlObject(alias = "ProjectForge")
  public class MyRootElement extends ProjectForgeRootElement {
    private TestObject testObject;
  }

  @Test
  public void testProjectForgeRoot() {
    final TestObject obj = new TestObject();
    obj.s1 = "hurzel";
    MyRootElement root = new MyRootElement();
    root.testObject = obj;
    final PFDateTime dateTime = PFDateTime.withDate(2010, Month.AUGUST, 30, 9, 18, 57);
    root.setCreated(dateTime.getUtilDate());
    final XmlObjectWriter writer = new XmlObjectWriter();
    final XmlRegistry xmlRegistry = new XmlRegistry();
    xmlRegistry.registerConverter(Date.class, new ISODateConverter());
    writer.setXmlRegistry(xmlRegistry);
    root.setTimeZone(DateHelper.EUROPE_BERLIN);
    final String xml = writer.writeToXml(root);
    assertEquals(
            "<ProjectForge timeZone=\"Europe/Berlin\" created=\"2010-08-30 09:18:57\"><testObject s1=\"hurzel\" d1=\"0.0\" i1=\"0\"/></ProjectForge>",
            xml);
    final XmlObjectReader reader = new XmlObjectReader() {
      @Override
      protected Object newInstance(final Class<?> clazz, final Element el, final String attrName,
                                   final String attrValue) {
        if (MyRootElement.class.isAssignableFrom(clazz)) {
          return new MyRootElement();
        }
        return null;
      }
    };
    reader.initialize(MyRootElement.class);
    root = (MyRootElement) reader.read(xml);
    assertEquals(DateHelper.EUROPE_BERLIN.getID(), root.getTimeZone().getID());
    assertEquals(dateTime.getUtilDate(), root.getCreated());
    assertEquals("hurzel", (root.testObject).s1);
  }

  @Test
  public void testCDATA() {
    final XmlObjectWriter writer = new XmlObjectWriter();
    final TestObject3 obj = new TestObject3();
    obj.s0 = "Hallo\n  Test";
    final String xml = writer.writeToXml(obj);
    assertEquals("<test3><s0><![CDATA[Hallo\n  Test]]></s0></test3>", xml);
  }

  @Test
  public void testEnums() {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(TestObject.class);
    TestObject obj = (TestObject) reader.read("<test color1=\"RED\" />");
    assertEquals(TestEnum.RED, obj.color1);
    obj = (TestObject) reader.read("<test color1=\"BLUE\" />");
    assertEquals(TestEnum.BLUE, obj.color1, "Default color is BLUE.");
    obj = (TestObject) reader.read("<test color1=\"blue\" />");
    assertEquals(TestEnum.BLUE, obj.color1, "Default color is BLUE.");
  }

  private TestObject create(final String s1, final String s2, final String s3, final String s4, final String t1,
                            final String t2,
                            final double d1, final double d2, final int i1, final int i2) {
    final TestObject obj = new TestObject();
    obj.s1 = s1;
    obj.s2 = s2;
    obj.s3 = s3;
    obj.s4 = s4;
    obj.t1 = t1;
    obj.t2 = t2;
    obj.d1 = d1;
    obj.d2 = d2;
    obj.i1 = i1;
    obj.i2 = i2;
    return obj;
  }

  private void assertValues(final TestObject obj, final String s1, final String s2, final String s3, final String s4,
                            final String t1,
                            final String t2, final double d1, final Double d2, final int i1, final int i2) {
    assertEquals(s1, obj.s1, "s1");
    assertEquals(s2, obj.s2, "s2");
    assertEquals(s3, obj.s3, "s3");
    assertEquals(s4, obj.s4, "s4");
    assertEquals(t1, obj.t1, "t1");
    assertEquals(t2, obj.t2, "t2");
    assertEquals(d1, obj.d1, "d1");
    assertEquals((double) d2, obj.d2, "d2");
    //    assertEquals("d1", d1, obj.d1, 0.00001);
    //    assertEquals("d2", d2, obj.d2, 0.00001);
    assertEquals(i1, obj.i1, "i1");
    assertEquals(i2, obj.i2, "i2");
  }

  private void containsAttrs(final Element el, final String... attrNames) {
    for (final String attr : attrNames) {
      assertNotNull(el.attribute(attr), "Element should contain attribute '" + attr + "': " + el);
    }
  }

  private void containsNotAttrs(final Element el, final String... attrNames) {
    for (final String attr : attrNames) {
      assertNull(el.attribute(attr), "Element shouldn't contain attribute '" + attr + "': " + el);
    }
  }

  private void containsElements(final Element el, final String... elementNames) {
    for (final String name : elementNames) {
      assertNotNull(el.element(name), "Element should contain element '" + name + "': " + el);
    }
  }

  private void containsNotElements(final Element el, final String... elementNames) {
    for (final String name : elementNames) {
      assertNull(el.element(name), "Element shouldn't contain element '" + name + "': " + el);
    }
  }
}
