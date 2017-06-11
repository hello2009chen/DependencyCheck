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
 * Copyright (c) 2017 The OWASP Foundation. All Rights Reserved.
 */
package org.owasp.dependencycheck;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.owasp.dependencycheck.utils.InvalidSettingException;
import org.owasp.dependencycheck.utils.Settings;
import org.owasp.dependencycheck.utils.Settings.KEYS;

/**
 * Tests for the {@link AppTest} class.
 */
public class AppTest {

    /** Test rule for asserting exceptions and their contents. */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Initialize the {@link Settings} singleton.
     */
    @Before
    public void setUp() {
        Settings.initialize();
    }

    /**
     * Clean the {@link Settings} singleton.
     */
    @After
    public void tearDown() {
        Settings.cleanup();
    }

    /**
     * Test of ensureCanonicalPath method, of class App.
     */
    @Test
    public void testEnsureCanonicalPath() {
        String file = "../*.jar";
        App instance = new App();
        String result = instance.ensureCanonicalPath(file);
        assertFalse(result.contains(".."));
        assertTrue(result.endsWith("*.jar"));

        file = "../some/skip/../path/file.txt";
        String expResult = "/some/path/file.txt";
        result = instance.ensureCanonicalPath(file);
        assertTrue("result=" + result, result.endsWith(expResult));
    }

    /**
     * Assert that boolean properties can be set on the CLI and parsed into the {@link Settings} singleton.
     *
     * @throws Exception the unexpected {@link Exception}.
     */
    @Test
    public void testPopulateSettings() throws Exception {
        File prop = new File(this.getClass().getClassLoader().getResource("sample.properties").toURI().getPath());
        String[] args = { "-P", prop.getAbsolutePath() };
        Map<String, Boolean> expected = new HashMap<>();
        expected.put(Settings.KEYS.AUTO_UPDATE, Boolean.FALSE);
        expected.put(Settings.KEYS.ANALYZER_ARCHIVE_ENABLED, Boolean.TRUE);

        assertTrue(testBooleanProperties(args, expected));

        String[] args2 = { "-n" };
        expected.put(Settings.KEYS.AUTO_UPDATE, Boolean.FALSE);
        expected.put(Settings.KEYS.ANALYZER_ARCHIVE_ENABLED, Boolean.TRUE);
        assertTrue(testBooleanProperties(args2, expected));

        String[] args3 = { "-h" };
        expected.put(Settings.KEYS.AUTO_UPDATE, Boolean.TRUE);
        expected.put(Settings.KEYS.ANALYZER_ARCHIVE_ENABLED, Boolean.TRUE);
        assertTrue(testBooleanProperties(args3, expected));

        String[] args4 = { "--disableArchive" };
        expected.put(Settings.KEYS.AUTO_UPDATE, Boolean.TRUE);
        expected.put(Settings.KEYS.ANALYZER_ARCHIVE_ENABLED, Boolean.FALSE);
        assertTrue(testBooleanProperties(args4, expected));

        String[] args5 = { "-P", prop.getAbsolutePath(), "--disableArchive" };
        expected.put(Settings.KEYS.AUTO_UPDATE, Boolean.FALSE);
        expected.put(Settings.KEYS.ANALYZER_ARCHIVE_ENABLED, Boolean.FALSE);
        assertTrue(testBooleanProperties(args5, expected));

        prop = new File(this.getClass().getClassLoader().getResource("sample2.properties").toURI().getPath());
        String[] args6 = { "-P", prop.getAbsolutePath(), "--disableArchive" };
        expected.put(Settings.KEYS.AUTO_UPDATE, Boolean.TRUE);
        expected.put(Settings.KEYS.ANALYZER_ARCHIVE_ENABLED, Boolean.FALSE);
        assertTrue(testBooleanProperties(args6, expected));

        String[] args7 = { "-P", prop.getAbsolutePath(), "--noupdate" };
        expected.put(Settings.KEYS.AUTO_UPDATE, Boolean.FALSE);
        expected.put(Settings.KEYS.ANALYZER_ARCHIVE_ENABLED, Boolean.FALSE);
        assertTrue(testBooleanProperties(args7, expected));

        String[] args8 = { "-P", prop.getAbsolutePath(), "--noupdate", "--disableArchive" };
        expected.put(Settings.KEYS.AUTO_UPDATE, Boolean.FALSE);
        expected.put(Settings.KEYS.ANALYZER_ARCHIVE_ENABLED, Boolean.FALSE);
        assertTrue(testBooleanProperties(args8, expected));
    }

    /**
     * Assert that an {@link UnrecognizedOptionException} is thrown when a property that is not supported is specified on the CLI.
     *
     * @throws Exception the unexpected {@link Exception}.
     */
    @Test
    public void testPopulateSettingsException() throws Exception {
        String[] args = { "-invalidPROPERTY" };

        expectedException.expect(UnrecognizedOptionException.class);
        expectedException.expectMessage("Unrecognized option: -invalidPROPERTY");
        testBooleanProperties(args, null);
    }

    /**
     * Assert that a single suppression file can be set using the CLI.
     *
     * @throws Exception the unexpected {@link Exception}.
     */
    @Test
    public void testPopulatingSuppressionSettings() throws Exception {
        // GIVEN CLI properties with the mandatory arguments
        File prop = new File(this.getClass().getClassLoader().getResource("sample.properties").toURI().getPath());

        // AND a single suppression file
        String[] args = { "-P", prop.getAbsolutePath(), "--suppression", "another-file.xml" };

        // WHEN parsing the CLI arguments
        final CliParser cli = new CliParser();
        cli.parse(args);
        final App classUnderTest = new App();
        classUnderTest.populateSettings(cli);

        // THEN the suppression file is set in the settings singleton for use in the application core
        assertThat("Expected the suppression file to be set in the Settings singleton", Settings.getString(KEYS.SUPPRESSION_FILE), is("another-file.xml"));
    }

    private boolean testBooleanProperties(String[] args, Map<String, Boolean> expected) throws URISyntaxException, FileNotFoundException, ParseException, InvalidSettingException {
        Settings.initialize();
        try {
            final CliParser cli = new CliParser();
            cli.parse(args);
            App instance = new App();
            instance.populateSettings(cli);
            boolean results = true;
            for (Map.Entry<String, Boolean> entry : expected.entrySet()) {
                results &= Settings.getBoolean(entry.getKey()) == entry.getValue();
            }

            return results;
        } finally {
            Settings.cleanup();
        }
    }

}
