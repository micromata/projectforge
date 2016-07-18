package org.projectforge.tools.schemaexp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;

public class XStreamTest
{
  public static class Parent implements Serializable
  {
    Map<String, Child> childres = new HashMap<>();
  }

  public static class Child implements Serializable
  {
    Parent parentRef;
  }

  @Test
  public void testXStream()
  {
    Parent parent = new Parent();
    Child child = new Child();
    child.parentRef = parent;
    parent.childres.put("asdf", child);
    XStream xstream = new XStream();
    xstream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());
    String res = xstream.toXML(parent);
    System.out.println("marshaled: " + res);
    xstream = new XStream();
    xstream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());
    Object object = xstream.fromXML(res);
    Parent rparent = (Parent) object;
  }
}
