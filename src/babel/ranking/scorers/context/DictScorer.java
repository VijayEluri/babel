package babel.ranking.scorers.context;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import babel.content.eqclasses.EquivalenceClass;
import babel.content.eqclasses.properties.context.Context;
import babel.content.eqclasses.properties.context.Context.ContextualItem;
import babel.content.eqclasses.properties.type.Type;
import babel.content.eqclasses.properties.type.Type.EqType;
import babel.ranking.scorers.Scorer;
import babel.util.dict.Dictionary;

public abstract class DictScorer extends Scorer
{
  protected static final Log LOG = LogFactory.getLog(DictScorer.class);

  public DictScorer(Dictionary dict)
  {
    m_dict = dict;
  }
  
  public double score(EquivalenceClass srcEq, EquivalenceClass trgEq)
  {
    Context smContext = (Context)srcEq.getProperty(Context.class.getName());     
    Context lgContext = (Context)trgEq.getProperty(Context.class.getName()); 

    if (smContext == null || lgContext == null || !smContext.areContItemsScored() || !lgContext.areContItemsScored())
    { throw new IllegalArgumentException("At least one of the classes has no or unscored context.");
    }

    if (lgContext.size() < smContext.size()) {
      Context tmpContext = smContext;
      smContext = lgContext;
      lgContext = tmpContext;
    }
    
    double score = 0, score1 = 0, score2 = 0;
    double w2, w1;
    
    ContextualItem lgCi;
    
    for (ContextualItem smCi : smContext.getContextualItems())
    {
      w1 = smCi.getScore();
      w2 = 0;
      
      if (null != (lgCi = lgContext.getContextualItem(smCi.getContextEqId())))
      {
        w2 = lgCi.getScore();
      }
      
      score1 += w2 * w2;
      score2 += w1 * w1;
      score += w2 * w1;
    }
    
    return ((score1 * score2) == 0) ? 0 : score / Math.sqrt(score1 * score2);        
  }
  
  public boolean smallerScoresAreBetter()
  { return false;
  }
  
  protected abstract double scoreContItem(ContextualItem contItem, EqType type);
  
  /** Projects and pre-computes feature scores. */
  public void prepare(EquivalenceClass eq)
  {
    EqType type = ((Type)eq.getProperty(Type.class.getName())).getType();
    Context context = ((Context)eq.getProperty(Context.class.getName()));

    if (context == null)
    { 
      if (LOG.isWarnEnabled())
      {// LOG.warn("Equivalence Class <" + eq.toString() + "> has no context property, adding one.");
      }
      
      eq.setProperty(context = new Context(eq));
      //throw new IllegalArgumentException("Class has no context property.");
    }
    else if (type == null || EqType.NONE.equals(type))
    { throw new IllegalArgumentException("Class is of unknown type, cannot compute scores.");
    }

    // If src, project before scoring
    Collection<ContextualItem> cis = EqType.SOURCE.equals(type) ?  m_dict.translateContext(eq) : context.getContextualItems();
    context.clear();
    double score;
    ContextualItem curCi;
    
    for (ContextualItem ci : cis)
    {
      score = scoreContItem(ci, type);
      ci.setScore(score);

      // Add a contextual item or if we happen to have more than one translation in target context, pick the best scoring one
      if ((null == (curCi = context.getContextualItem(ci.getContextEqId()))) || (curCi != null && curCi.getScore() < score))
      {
        context.setContextualItem(ci);
      }
    }
    
    context.contItemsScored();
  }
  
  protected Dictionary m_dict;
}
