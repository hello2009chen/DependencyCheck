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
 * Copyright (c) 2016 IBM Corporation. All Rights Reserved.
 */
package org.owasp.dependencycheck.analyzer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.exception.AnalysisException;
import org.owasp.dependencycheck.dependency.Confidence;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.EvidenceCollection;
import org.owasp.dependencycheck.utils.FileFilterBuilder;
import org.owasp.dependencycheck.utils.Settings;

/**
 * This analyzer is used to analyze the SWIFT Package Manager
 * (https://swift.org/package-manager/). It collects information about a package
 * from Package.swift files.
 *
 * @author Bianca Jiang (https://twitter.com/biancajiang)
 */
@Experimental
public class SwiftPackageManagerAnalyzer extends AbstractFileTypeAnalyzer {

    /**
     * A descriptor for the type of dependencies processed or added by this analyzer
     */
    public static final String DEPENDENCY_ECOSYSTEM = "Swift.PM";
    
    /**
     * The name of the analyzer.
     */
    private static final String ANALYZER_NAME = "SWIFT Package Manager Analyzer";

    /**
     * The phase that this analyzer is intended to run in.
     */
    private static final AnalysisPhase ANALYSIS_PHASE = AnalysisPhase.INFORMATION_COLLECTION;

    /**
     * The file name to scan.
     */
    public static final String SPM_FILE_NAME = "Package.swift";

    /**
     * Filter that detects files named "package.json".
     */
    private static final FileFilter SPM_FILE_FILTER = FileFilterBuilder.newInstance().addFilenames(SPM_FILE_NAME).build();

    /**
     * The capture group #1 is the block variable. e.g. "import
     * PackageDescription let package = Package( name: "Gloss" )"
     */
    private static final Pattern SPM_BLOCK_PATTERN = Pattern.compile("let[^=]+=\\s*Package\\s*\\(\\s*([^)]*)\\s*\\)", Pattern.DOTALL);

    /**
     * Returns the FileFilter
     *
     * @return the FileFilter
     */
    @Override
    protected FileFilter getFileFilter() {
        return SPM_FILE_FILTER;
    }

    @Override
    protected void initializeFileTypeAnalyzer() {
        // NO-OP
    }

    /**
     * Returns the name of the analyzer.
     *
     * @return the name of the analyzer.
     */
    @Override
    public String getName() {
        return ANALYZER_NAME;
    }

    /**
     * Returns the phase that the analyzer is intended to run in.
     *
     * @return the phase that the analyzer is intended to run in.
     */
    @Override
    public AnalysisPhase getAnalysisPhase() {
        return ANALYSIS_PHASE;
    }

    /**
     * Returns the key used in the properties file to reference the analyzer's
     * enabled property.
     *
     * @return the analyzer's enabled property setting key
     */
    @Override
    protected String getAnalyzerEnabledSettingKey() {
        return Settings.KEYS.ANALYZER_SWIFT_PACKAGE_MANAGER_ENABLED;
    }

    @Override
    protected void analyzeDependency(Dependency dependency, Engine engine)
            throws AnalysisException {

    	    dependency.setEcosystem(DEPENDENCY_ECOSYSTEM);   
    	
        String contents;
        try {
            contents = FileUtils.readFileToString(dependency.getActualFile(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new AnalysisException(
                    "Problem occurred while reading dependency file.", e);
        }
        final Matcher matcher = SPM_BLOCK_PATTERN.matcher(contents);
        if (matcher.find()) {
            final String packageDescription = matcher.group(1);
            if (packageDescription.isEmpty()) {
                return;
            }

            final EvidenceCollection product = dependency.getProductEvidence();
            final EvidenceCollection vendor = dependency.getVendorEvidence();

            //SPM is currently under development for SWIFT 3. Its current metadata includes package name and dependencies.
            //Future interesting metadata: version, license, homepage, author, summary, etc.
            final String name = addStringEvidence(product, packageDescription, "name", "name", Confidence.HIGHEST);
            if (name != null && !name.isEmpty()) {
                vendor.addEvidence(SPM_FILE_NAME, "name_project", name, Confidence.HIGHEST);
                dependency.setName(name);
            }
            else
            {
            	    //if we can't get the name from the meta, then assume the name is the name of the parent folder containing the package.swift file.
            		dependency.setName(dependency.getActualFile().getParentFile().getName());
            }
        }
        setPackagePath(dependency);
    }

    /**
     * Extracts evidence from the package description and adds it to the given
     * evidence collection.
     *
     * @param evidences the evidence collection to update
     * @param packageDescription the text to extract evidence from
     * @param field the name of the field being searched for
     * @param fieldPattern the field pattern within the contents to search for
     * @param confidence the confidence level of the evidence if found
     * @return the string that was added as evidence
     */
    private String addStringEvidence(EvidenceCollection evidences,
            String packageDescription, String field, String fieldPattern, Confidence confidence) {
        String value = "";

        final Matcher matcher = Pattern.compile(
                String.format("%s *:\\s*\"([^\"]*)", fieldPattern), Pattern.DOTALL).matcher(packageDescription);
        if (matcher.find()) {
            value = matcher.group(1);
        }

        if (value != null) {
            value = value.trim();
            if (value.length() > 0) {
                evidences.addEvidence(SPM_FILE_NAME, field, value, confidence);
            }
        }

        return value;
    }

    /**
     * Sets the package path on the given dependency.
     *
     * @param dep the dependency to update
     */
    private void setPackagePath(Dependency dep) {
        final File file = new File(dep.getFilePath());
        final String parent = file.getParent();
        if (parent != null) {
            dep.setPackagePath(parent);
        }
    }
}
