package babel.content.eqclasses.properties;

import java.io.BufferedReader;
import java.util.HashMap;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import babel.content.corpora.accessors.CorpusAccessor;
import babel.content.eqclasses.EquivalenceClass;
import babel.content.eqclasses.SimpleEquivalenceClass;

/**
 * Collects contextual equivalence class.  Does not collect across sentence 
 * boundaries. TODO: does primitive sentence segmentation - re-implement.
 */
public class ContextCollector extends PropertyCollector
{
  public static final Log LOG = LogFactory.getLog(ContextCollector.class);

  public ContextCollector(boolean caseSensitive, int leftSize, int rightSize, Set<EquivalenceClass> contextEqs) throws Exception
  {
    m_caseSensitive = caseSensitive;
    m_leftSize = leftSize;
    m_rightSize = rightSize;
    m_allContextEqsMap = new HashMap<String, SimpleEquivalenceClass>(contextEqs.size());
    SimpleEquivalenceClass seq;
    
    for (EquivalenceClass eq : contextEqs)
    {
      seq = (SimpleEquivalenceClass)eq; // TODO: not pretty
      m_allContextEqsMap.put(seq.getWord(), seq);
    }
  }
  
  public void collectProperty(CorpusAccessor corpusAccess, Set<EquivalenceClass> eqs) throws Exception
  {
    BufferedReader reader = new BufferedReader(corpusAccess.getCorpusReader());
    String curLine;
    String[] curSents;
    String[] curSentTokens;
    EquivalenceClass foundEq;
    //EquivalenceClass cntEq;
    //CoOccurrers cntCoOcc;
    Context fountEqContext;
    int min, max;
      
    // TODO: Very inefficient - think of something better
    HashMap<String, EquivalenceClass> eqsMap = new HashMap<String, EquivalenceClass>(eqs.size());
 
    for (EquivalenceClass eq : eqs)
    {
      for (String word : eq.getAllWords())
      { 
        assert eqsMap.get(word) == null;
        eqsMap.put(word, eq);
      }
    }
     
    while ((curLine = reader.readLine()) != null)
    {
      // Split into likely sentences
      curSents = curLine.split(SENT_DELIM_REGEX);
        
      // Within each sentence, split into words
      for (int numSent = 0; numSent < curSents.length; numSent++ )
      {
        curSentTokens = curSents[numSent].split(WORD_DELIM_REGEX);
   
        for (int numToken = 0; numToken < curSentTokens.length; numToken++)
        {         
          // Look for the word's equivalence class
          if (null != (foundEq = eqsMap.get(EquivalenceClass.getWordOfAppropriateForm(curSentTokens[numToken], m_caseSensitive))))               
          {        
            // Get/set its context prop
            if ((fountEqContext = (Context)foundEq.getProperty(Context.class.getName())) == null)
            { foundEq.setProperty(fountEqContext = new Context(foundEq));
            }
   
            // A window around the word
            min = Math.max(0, numToken - m_leftSize);
            max = Math.min(numToken + m_rightSize + 1, curSentTokens.length);
            
            // Add all words in the contextual window (except for the word itself).
            for (int contextIdx = min; contextIdx < max; contextIdx++)
            {
              if (contextIdx != numToken)
              { 
                // Add current word to the current equivalence class context
                //cntEq = 
                  fountEqContext.addContextWord(m_caseSensitive, m_allContextEqsMap, curSentTokens[contextIdx]);
                  
               // TODO: Temp
               // if (cntEq != null)
               // {
               //   // Get/set and update the co-occurrers prop
               //   if ((cntCoOcc = (CoOccurrers)cntEq.getProperty(CoOccurrers.class.getName())) == null)
               //   { cntEq.setProperty(cntCoOcc = new CoOccurrers());
               //   }
                    
               //   cntCoOcc.addCoOccurrer(tmpEq);    
               // }
              }
            }
          }
        }
      }
    }

    reader.close();
  }
 
  /** All equivalence classes which from which to construct context. */
  protected HashMap<String, SimpleEquivalenceClass> m_allContextEqsMap;
  protected boolean m_caseSensitive;
  protected int m_leftSize;
  protected int m_rightSize;
}