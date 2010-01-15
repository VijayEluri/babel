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

package babel.prep.corpus;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.lib.MultipleSequenceFileOutputFormat;

import babel.content.pages.Page;

public class MultipleLangFileOutputFormat extends MultipleSequenceFileOutputFormat<Text, Page>
{  
  protected String generateFileNameForKeyValue(Text key, Page page, String name)
  {
    String lang = page.pageProperties().getFirst(Page.PROP_LANG);
    lang = (lang == null || lang.length() == 0) ? "none" : lang;
    
    CorpusGenerator.Stats.incLangPageCount(lang);
    
    return lang + "." + super.generateFileNameForKeyValue(key, page, name);
  }
}