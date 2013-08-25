/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oldcurmudgeon.toolbox.xml;

import nu.xom.Element;

/**
 *
 * @author caswellp
 */
public class XMLBuilder {
  // Make a new element with the specified tag and value.
  public static Element xml(String tag, String value) {
    Element e = new Element(tag);
    e.appendChild(value);
    return e;
  }

  // Shortcut for when the value is an int or a long.
  public static Element xml(String tag, long value) {
    return xml(tag, Long.toString(value));
  }

  // Shortcut for when the value is a float or a double.
  public static Element xml(String tag, double value) {
    return xml(tag, Double.toString(value));
  }

  // Allow as many as you like, they all (well almost all) get added.
  public static Element xml(String tag, Element... values) {
    Element e = new Element(tag);
    for (Element v : values) {
      if (v != NoElement) {
        e.appendChild(v);
      }
    }
    return e;
  }

  // Use when, for example, something is either there or not such as include?xml("Thing","Contents of Thing"):NoElement;
  public static final Element NoElement = new Element("NoElement");

  public static void main(String[] args) throws InterruptedException {
    // Sample manually created Element.
    Element e = xml("kml",
                    xml("Document",
                        xml("Placemark",
                            xml("name", "New York City"),
                            xml("description", "New York City"),
                            xml("Point",
                                xml("Coordinate", "-74.006393,40.714172,0")))));
    System.out.println(e.toXML());
  }
}
