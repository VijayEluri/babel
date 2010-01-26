/**
 * This file is licensed to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package babel.content.pages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseText;
import org.apache.nutch.protocol.Content;

import babel.prep.extract.NutchChunk;
import babel.util.xmlpersistence.XMLPersistable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Encapsulates all information extracted per page version by a nutch crawl.
 */
public class PageVersion implements XMLPersistable, Writable
{
  public static final Log LOG = LogFactory.getLog(PageVersion.class);
  
  private static final String XML_TAG_PAGEVERSION = "PageVersion";  
  private static final String XML_TAG_OUT_LINKS = "OutgoingLinks";
  private static final String XML_TAG_LINK = "Link";
  private static final String XML_ATTRIB_ANCHOR = "Anchor";
  private static final String XML_TAG_CONTENT = "ParsedContent";
  
  private static final String PROP_TITLE = "Title";
  private static final String PROP_FETCH_TIME = "Fetched";
  private static final String PROP_MODIFIED_TIME = "Modified";
  private static final String PROP_SEGMENT_ID = "NutchSegment";

  public PageVersion()
  {
    m_verProps = new MetaData("VersionProperties");
    m_contentMeta = new MetaData("ContentMetadata");
    m_parseMeta = new MetaData("ParseMetadata");  
    m_outLinks = null;
    m_content = new String();
  }
  
  /**
   * Used to construct a page version from a set of nutch page related objects.
   * Should only be used by NutchPageExtractor.
   */
  public PageVersion(String segmentId, List<NutchChunk> chunks)
  {
    this();
    
    Writable curVal;
    CrawlDatum curCD;
    //Content curCT;
    ParseData curPD;
    ParseText curPT;

    // Store Segment ID
    m_verProps.set(PROP_SEGMENT_ID, segmentId);
    
    // Unwrap all of the page related information
    for (NutchChunk chunk : chunks) 
    {
      curVal = chunk.get();
      
      if (curVal instanceof CrawlDatum) 
      {
        // Get fetch information
        curCD = (CrawlDatum)curVal;
        
        if (curCD.getStatus() == CrawlDatum.STATUS_FETCH_SUCCESS)
        {
          m_verProps.set(PROP_FETCH_TIME, Long.toString(curCD.getFetchTime()));
          m_verProps.set(PROP_MODIFIED_TIME, Long.toString(curCD.getModifiedTime()));     
        }
      } 
      else if (curVal instanceof Content)
      {
        // Get the original unparsed content; nothing that we need - ignoring
        // We shouldn't get it anyway, since PageExtractor desn't ask for it
        
        // curCT = (Content)curVal;
      } 
      else if (curVal instanceof ParseData)
      {
        // Get data extracted from page content
        curPD = (ParseData)curVal;
        if (curPD.getStatus().isSuccess())
        {
          m_verProps.set(PROP_TITLE, curPD.getTitle());          
          m_parseMeta.setAll(curPD.getParseMeta());
          m_contentMeta.setAll(curPD.getContentMeta());
          m_outLinks = curPD.getOutlinks();
        }
      }
      else if (curVal instanceof ParseText)
      {
        // Get parsed content
        curPT = (ParseText)curVal;
        m_content = setStr(curPT.getText());
      } 
      else if (LOG.isWarnEnabled()) 
      {        
        LOG.warn("Unrecognized type: " + curVal.getClass());
      }
    }
  }
  
  /**
   * Checks if the page version is complete (i.e. contains at least a nutch 
   * segment ID and parsed content).
   */
  public boolean isNutchComplete()
  {
    return ((m_content.length() > 0) && m_verProps.hasKey(PROP_SEGMENT_ID) && m_verProps.hasKey(PROP_FETCH_TIME));
  }
  
  public String getContent()
  {
    return m_content;
  }
  
  public Long getFetchTime()
  {
    String prop = m_verProps.getFirst(PROP_FETCH_TIME);
    return (prop != null ? Long.parseLong(prop) : null);
  }

  public String toString()
  {
    StringBuilder strBld = new StringBuilder();
    SimpleDateFormat dft = new SimpleDateFormat();
    String prop;

    prop = m_verProps.getFirst(PROP_SEGMENT_ID);
    strBld.append("PageVersion from Segment: " + (prop != null ? prop : "-") + "\n");
    prop = m_verProps.getFirst(PROP_TITLE);
    strBld.append("  Title: " + (prop != null ? prop : "-") + "\n");
    prop = m_verProps.getFirst(PROP_FETCH_TIME);    
    strBld.append("  Fetched: " + (prop != null ? dft.format(new Date(Long.parseLong(prop))) : "-") + "\n");        
    strBld.append("  Content Metadata: " + ((m_contentMeta.numKeys() > 0) ? "present" : "absent") + "\n");
    strBld.append("  Parse Metadata: " + ((m_parseMeta.numKeys() > 0) ? "present" : "absent") + "\n");
    strBld.append("  Outgoing Links: " + ((m_outLinks == null) ? "0" : m_outLinks.length) + "\n");
    strBld.append("  Parsed Content: " + ((m_content.length() > 0) ? "present" : "absent") + "\n");
    
    return strBld.toString();
  }

  /**
   * Considers two version equal if content is the same.
   */
  public boolean equals(Object obj)
  {
    return (obj instanceof PageVersion) && (m_content.equals(((PageVersion)obj).m_content));
  }
  
  /**
   * To be called by ObjectWriter only.
   * @throws XMLStreamException 
   */
  public void persist(XMLStreamWriter writer) throws XMLStreamException
  {
    writer.writeStartElement(XML_TAG_PAGEVERSION);

    if (m_verProps.numKeys() > 0)
    { m_verProps.persist(writer);
    }
    
    if (m_parseMeta.numKeys() > 0)
    { m_parseMeta.persist(writer);
    }
        
    if (m_contentMeta.numKeys() > 0)
    { m_contentMeta.persist(writer);
    }

    if (m_outLinks != null)
    {
      String anchor;

      writer.writeStartElement(XML_TAG_OUT_LINKS);

      for (Outlink outlink : m_outLinks)
      {
        writer.writeStartElement(XML_TAG_LINK);
        anchor = outlink.getAnchor();
        
        if ((anchor != null) && (anchor.length() != 0))
        { writer.writeAttribute(XML_ATTRIB_ANCHOR, anchor);
        }

        writer.writeCharacters(outlink.getToUrl());
        writer.writeEndElement();
      }

      writer.writeEndElement();
    }
    
    if (m_content.length() > 0)
    {
      writer.writeStartElement(XML_TAG_CONTENT);
      writer.writeCharacters(new String(Base64.encodeBase64(m_content.getBytes())));
      //writer.writeCharacters(m_content);
      writer.writeEndElement();
    }

    writer.writeEndElement();
  }
  
  public void readFields(DataInput in) throws IOException
  { 
    m_verProps.readFields(in);
    m_contentMeta.readFields(in);
    m_parseMeta.readFields(in);
    
    int numLinks = WritableUtils.readVInt(in);

    m_outLinks = (numLinks == 0) ? null : new Outlink[numLinks];

    for (int i = 0; i < numLinks; i++)
    { (m_outLinks[i] = new Outlink()).readFields(in);
    }

    m_content = Text.readString(in);  
  }

  public void write(DataOutput out) throws IOException
  {
    m_verProps.write(out);
    m_contentMeta.write(out);
    m_parseMeta.write(out);

    int numLinks = (m_outLinks == null) ? 0 : m_outLinks.length;
    
    WritableUtils.writeVInt(out, numLinks);
      
    for (int i = 0; i < numLinks; i++)
    { m_outLinks[i].write(out);
    }

    Text.writeString(out, m_content);  
  }
  
  protected String setStr(String val)
  {
    return (val == null) ? new String() : val;
  }
  
  protected MetaData m_verProps;
  protected MetaData m_contentMeta;
  protected MetaData m_parseMeta;  
  protected Outlink[] m_outLinks; 
  protected String m_content;
}