/*
 * This file is part of dependency-check-core.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2013 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.data.lucene;

import java.io.IOException;
import java.io.StringReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import static org.apache.lucene.analysis.BaseTokenStreamTestCase.assertAnalyzesTo;
import static org.apache.lucene.analysis.BaseTokenStreamTestCase.checkOneTerm;
import static org.apache.lucene.analysis.BaseTokenStreamTestCase.checkRandomData;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.util.AttributeFactory;
import static org.apache.lucene.util.LuceneTestCase.RANDOM_MULTIPLIER;
import static org.apache.lucene.util.LuceneTestCase.random;
import org.junit.After;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Jeremy Long
 */
public class TokenPairConcatenatingFilterTest extends BaseTokenStreamTestCase {

        private final Analyzer analyzer;

    public TokenPairConcatenatingFilterTest() {
        analyzer = new Analyzer() {
            @Override
            protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new MockTokenizer(MockTokenizer.WHITESPACE, false);
                return new Analyzer.TokenStreamComponents(source, new TokenPairConcatenatingFilter(source));
            }
        };
    }
    /**
     * Test of incrementToken method, of class TokenPairConcatenatingFilter.
     */
    @Test
    public void testIncrementToken() throws Exception {
        String[] expected = new String[5];
        expected[0] = "red";
        expected[1] = "redblue";
        expected[2] = "blue";
        expected[3] = "bluegreen";
        expected[4] = "green";
        assertAnalyzesTo(analyzer, "red blue green", expected);
    }
    
    
        /**
     * copied from
     * http://svn.apache.org/repos/asf/lucene/dev/trunk/lucene/analysis/common/src/test/org/apache/lucene/analysis/en/TestEnglishMinimalStemFilter.java
     * blast some random strings through the analyzer
     */
    public void testRandomStrings() {
        try {
            checkRandomData(random(), analyzer, 1000 * RANDOM_MULTIPLIER);
        } catch (IOException ex) {
            fail("Failed test random strings: " + ex.getMessage());
        }
    }

    /**
     * copied from
     * http://svn.apache.org/repos/asf/lucene/dev/trunk/lucene/analysis/common/src/test/org/apache/lucene/analysis/en/TestEnglishMinimalStemFilter.java
     *
     * @throws IOException
     */
    public void testEmptyTerm() throws IOException {
        Analyzer a = new Analyzer() {
            @Override
            protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
                Tokenizer tokenizer = new KeywordTokenizer();
                return new Analyzer.TokenStreamComponents(tokenizer, new TokenPairConcatenatingFilter(tokenizer));
            }
        };
        checkOneTerm(a, "", "");
    }
     
}
