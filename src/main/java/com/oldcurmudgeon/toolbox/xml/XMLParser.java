/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oldcurmudgeon.toolbox.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author caswellp
 */
public class XMLParser extends DefaultHandler {
  // Current level - 0 = bottom level.
  int level = 0;
  // The parsed xml.
  Parsed parsed = new Parsed(null);
  // The one we are reading into at present.
  Parsed parsing = parsed;
  // The parser.
  protected SAXParser sax;
  // My factory.
  static final SAXParserFactory factory = SAXParserFactory.newInstance();

  public XMLParser() throws ParserConfigurationException, SAXException{
    synchronized (factory) {
      // Build my parser
      sax = factory.newSAXParser();
    }
  }

  public Parsed parse(InputSource source) throws SAXException, IOException{
    // Start the parse.
    sax.parse(source, this);
    return parsed;
  }

  public Parsed parse(File f) throws InterruptedException, FileNotFoundException, SAXException, IOException {
    // Start the parse.
    return parse(new InputSource(new FileReader(f)));
  }

  public Parsed parse(InputStream is) throws InterruptedException, FileNotFoundException, SAXException, IOException {
    // Start the parse.
    return parse(new InputSource(is));
  }

  public Parsed parse(String xml) throws InterruptedException, FileNotFoundException, SAXException, IOException {
    // The source.
    return parse(new InputSource(new StringReader(xml)));
  }

  public Parsed getParsed() {
    return parsed;
  }

  /*
   * Called when the starting of the Element is reached. For Example if we have Tag
   * called <Title> ... </Title>, then this method is called when <Title> tag is
   * Encountered while parsing the Current XML File. The AttributeList Parameter has
   * the list of all Attributes declared for the Current Element in the XML File.
   */
  @Override
  public void startElement(String uri, String localName, String name, Attributes atrbts) throws SAXException {
    if (level > 0) {
      // Create new one.
      parsing = new Parsed(parsing);
      // Connect it up.
      parsing.getParent().addContents(parsing);
    }
    parsing.setTag(name);
    for (int i = 0; i < atrbts.getLength(); i++) {
      parsing.addAttribute(atrbts.getQName(i), XML.unHashAll(atrbts.getValue(i)));
    }
    level += 1;
  }

  /*
   * Called when the Ending of the current Element is reached. For example in the
   * above explanation, this method is called when </Title> tag is reached
   */
  @Override
  public void endElement(String uri, String localName, String name) throws SAXException {
    //log.info("Ending: "+name);
    if (level > 0) {
      parsing = parsing.getParent();
    }
    level -= 1;
  }

  /*
   * While Parsing the XML file, if extra characters like space or enter Character
   * are encountered then this method is called. If you don't want to do anything
   * special with these characters, then you can normally leave this method blank.
   */
  @Override
  public void characters(char buf[], int offset, int len) throws SAXException {
    String chs = new String(buf, offset, len).trim();
    parsing.addData(XML.unHashAll(chs));
  }

  public static class Parsed {
    // My parent - or null if I'm the daddy.
    protected final Parsed parent;
    // The tag.
    protected String tag;
    // The attributes.
    protected final Map<String, String> attributes = new HashMap<String, String>();
    // The sub-parts.
    protected final List<Parsed> contents = new LinkedList<Parsed>();
    // The data.
    protected final StringBuilder data = new StringBuilder();

    // Growing a new child.
    public Parsed(Parsed parent) {
      this.parent = parent;
    }

    // Growing a new parent.
    public Parsed(Parsed parent, Parsed child) {
      this.parent = parent;
      contents.add(child);
    }

    public void setTag(String newTag) {
      this.tag = newTag.trim();
    }

    public void addAttribute(String name, String value) {
      this.attributes.put(name.trim(), value.trim());
    }

    public void addContents(Parsed newContents) {
      if (newContents != null) {
        contents.add(newContents);
      }
    }

    public void addData(String newData) {
      data.append(newData);
    }

    public String getTag() {
      return tag;
    }

    public String getAttribute(String id) {
      return attributes.get(id);
    }

    public List<Parsed> getContents() {
      return contents;
    }

    public String getData() {
      return data.toString();
    }

    public Parsed getParent() {
      return parent;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      // Tag
      sb.append("<").append(tag);
      // Attributes.
      for (String att : new TreeSet<String>(attributes.keySet())) {
        sb.append(" ").append(att).append("=\"").append(attributes.get(att)).append("\"");
      }
      if (data.length() > 0 || !contents.isEmpty()) {
        // Close the tag.
        sb.append(">");
        // Data
        sb.append(data.toString());
        // Sub parts.
        for (Parsed p : contents) {
          sb.append(p.toString());
        }
        // End tag.
        sb.append("</").append(tag).append(">");
      } else {
        // Empty.
        sb.append("/>");
      }
      return sb.toString();
    }
  }

  public static void main(String[] args) {
    try {
      String xml = "<product id=\"kt00016\" name=\"Tongue &amp; Groove Corner Shed 7ft x 7ft\"><productinformation><sku>kt00016</sku><name>Tongue &amp;amp; Groove Corner Shed 7ft x 7ft</name><producttype>gardensheds</producttype><brand/></productinformation><pricing><sellingprice>420.00</sellingprice><msrpprice>999.99</msrpprice><wasprice/><startqty>1</startqty><endqty>1000000</endqty></pricing><stock><releasedate>2010-02-26</releasedate><stocketadate/><stocketadaysleft/><onhand>200</onhand><attributesinstock>0</attributesinstock><preorder/><published>1</published></stock><images><thumb>http://wilko.uat.venda.com/content/ebiz/wilkinsonplus/invt/kt00016/kt00016_t.jpg</thumb><small>http://wilko.uat.venda.com/content/ebiz/wilkinsonplus/invt/kt00016/kt00016_s.jpg</small><xsmall/><medium>http://wilko.uat.venda.com/content/ebiz/wilkinsonplus/invt/kt00016/kt00016_m.jpg</medium><large>http://wilko.uat.venda.com/content/ebiz/wilkinsonplus/invt/kt00016/kt00016_l.jpg</large></images><alternateimages><xsmall></xsmall><medium></medium><large></large></alternateimages><descriptions><description1>Opening: W116cm H164cm &amp;lt;br&amp;gt; Windows: 4 &amp;lt;br&amp;gt; Window Size: 56cm x 61cm &amp;lt;br&amp;gt; Glazing Material: Styrene &amp;lt;br&amp;gt;&amp;lt;br&amp;gt; Floor Joists &amp;lt;br&amp;gt; Each building comes complete with a sufficient number of factory treated floor joists which act as a strengthening tool that provides ventilation and prevents the damp and cold penetrating the floor.  &amp;lt;br&amp;gt;&amp;lt;br&amp;gt; Home Assembly &amp;lt;br&amp;gt; To aid in the assembly some of the work has already been carried out for you. Each building comes with fully assembled panels and we supply a fixing kit with sufficient nails and felt to complete the roof assembly. &amp;lt;br&amp;gt;&amp;lt;br&amp;gt; Pre-Treated &amp;lt;br&amp;gt; This shed is delivered pre-treated with our very own anti-fungal wood preservative. The treatment will protect your building for approximately 6-8 weeks giving you plenty of time to choose your plot, prepare your base and erect your building. &amp;lt;br&amp;gt;&amp;lt;br&amp;gt; What's Included With Your Shed &amp;lt;br&amp;gt; Side, Roof, Door, Back and Floor Panels (quantity depends on size) Assembly Instructions, Fixtures and Fittings, Roofing Felt, Styrene Window Glazing. &amp;lt;br&amp;gt;&amp;lt;br&amp;gt;&amp;lt;b&amp;gt; Delivery will be to the nearest kerbside or hardstanding. &amp;lt;/b&amp;gt;  For more Garden Sheds &amp; Storage please check out the rest of our Brilliant Ranges.</description1><description2></description2><description3></description3><description4>Garden Shed, Shed, Wooden Shed, Sheds</description4></descriptions><deliveryweightinformation><weight>0</weight><weightunits/><additionaldeliverycost>0</additionaldeliverycost><volume>0</volume></deliveryweightinformation><addtobasketparams><bsref>wilkinsonplus</bsref><log>22</log><mode>add</mode><curpage/><next/><layout/><ex>co_disp-shopc</ex><buy>kt00016</buy><invt>kt00016</invt><htxt>lgZi%2F7li8EMWdJqcaWb5%2F7H8WOs38pmmD0clUUaMAkf7DR6aOMwZx0SyPLUyItaEk86%2Fpx%2BASOAI%0AxLoRoVJmbg%3D%3D</htxt><qty>1</qty></addtobasketparams><buynowparams><bsref>wilkinsonplus</bsref><log>22</log><mode>add</mode><curpage/><next/><layout/><ex>co_wizr-shopcart</ex><ivref>kt00016</ivref><htxt>lgZi%2F7li8EMWdJqcaWb5%2F7H8WOs38pmmD0clUUaMAkf7DR6aOMwZx0SyPLUyItaEk86%2Fpx%2BASOAI%0AxLoRoVJmbg%3D%3D</htxt><qty>1</qty></buynowparams></product>";
      XMLParser parser = new XMLParser();
      parser.parse(xml);
      System.out.println(parser.toString());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
