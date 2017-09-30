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
import java.io.FilenameFilter;
import javax.annotation.concurrent.ThreadSafe;

import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.exception.AnalysisException;
import org.owasp.dependencycheck.dependency.Dependency;

/**
 * This analyzer accepts the fully resolved .gemspec created by the Ruby bundler
 * (http://bundler.io) for better evidence results. It also tries to resolve the
 * dependency packagePath to where the gem is actually installed. Then during
 * the {@link org.owasp.dependencycheck.analyzer.AnalysisPhase#PRE_FINDING_ANALYSIS}
 * {@link DependencyMergingAnalyzer} will merge two .gemspec dependencies
 * together if <code>Dependency.getPackagePath()</code> are the same.
 *
 * Ruby bundler creates new .gemspec files under a folder called
 * "specifications" at deploy time, in addition to the original .gemspec files
 * from source. The bundler generated .gemspec files always contain fully
 * resolved attributes thus provide more accurate evidences, whereas the
 * original .gemspec from source often contain variables for attributes that
 * can't be used for evidences.
 *
 * Note this analyzer share the same
 * {@link org.owasp.dependencycheck.utils.Settings.KEYS#ANALYZER_RUBY_GEMSPEC_ENABLED}
 * as {@link RubyGemspecAnalyzer}, so it will enabled/disabled with
 * {@link RubyGemspecAnalyzer}.
 *
 * @author Bianca Jiang (https://twitter.com/biancajiang)
 */
@Experimental
@ThreadSafe
public class RubyBundlerAnalyzer extends RubyGemspecAnalyzer {

    /**
     * The name of the analyzer.
     */
    private static final String ANALYZER_NAME = "Ruby Bundler Analyzer";

    /**
     * Folder name that contains .gemspec files created by "bundle install"
     */
    private static final String SPECIFICATIONS = "specifications";

    /**
     * Folder name that contains the gems by "bundle install"
     */
    private static final String GEMS = "gems";

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
     * Only accept *.gemspec files generated by "bundle install --deployment"
     * under "specifications" folder.
     *
     * @param pathname the path name to test
     * @return true if the analyzer can process the given file; otherwise false
     */
    @Override
    public boolean accept(File pathname) {

        boolean accepted = super.accept(pathname);
        if (accepted) {
            final File parentDir = pathname.getParentFile();
            accepted = parentDir != null && parentDir.getName().equals(SPECIFICATIONS);
        }

        return accepted;
    }

    @Override
    protected void analyzeDependency(Dependency dependency, Engine engine)
            throws AnalysisException {
        super.analyzeDependency(dependency, engine);

        //find the corresponding gem folder for this .gemspec stub by "bundle install --deployment"
        final File gemspecFile = dependency.getActualFile();
        final String gemFileName = gemspecFile.getName();
        final String gemName = gemFileName.substring(0, gemFileName.lastIndexOf(".gemspec"));
        final File specificationsDir = gemspecFile.getParentFile();
        if (specificationsDir != null && specificationsDir.getName().equals(SPECIFICATIONS) && specificationsDir.exists()) {
            final File parentDir = specificationsDir.getParentFile();
            if (parentDir != null && parentDir.exists()) {
                final File gemsDir = new File(parentDir, GEMS);
                if (gemsDir.exists()) {
                    final File[] matchingFiles = gemsDir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.equals(gemName);
                        }
                    });

                    if (matchingFiles != null && matchingFiles.length > 0) {
                        final String gemPath = matchingFiles[0].getAbsolutePath();
                        if (dependency.getActualFilePath().equals(dependency.getFilePath())) {
                            if (gemPath != null) {
                                dependency.setPackagePath(gemPath);
                            }
                        } else {
                            //.gemspec's actualFilePath and filePath are different when it's from a compressed file
                            //in which case actualFilePath is the temp directory used by decompression.
                            //packagePath should use the filePath of the identified gem file in "gems" folder
                            final File gemspecStub = new File(dependency.getFilePath());
                            final File specDir = gemspecStub.getParentFile();
                            if (specDir != null && specDir.getName().equals(SPECIFICATIONS)) {
                                final File gemsDir2 = new File(specDir.getParentFile(), GEMS);
                                final File packageDir = new File(gemsDir2, gemName);
                                dependency.setPackagePath(packageDir.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }
    }
}
