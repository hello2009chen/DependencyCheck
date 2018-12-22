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
 * Copyright (c) 2018 Steve Springett. All Rights Reserved.
 */
package org.owasp.dependencycheck.agent;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.owasp.dependencycheck.BaseTest;
import org.owasp.dependencycheck.dependency.Confidence;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.EvidenceType;
import org.owasp.dependencycheck.dependency.Identifier;
import org.owasp.dependencycheck.reporting.ReportGenerator;
import org.owasp.dependencycheck.utils.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DependencyCheckScanAgentTest extends BaseTest {

    private static final File DATA_DIR = new File("target/test-scan-agent/data");
    private static final File REPORT_DIR = new File("target/test-scan-agent/report");

    @BeforeClass
    public static void beforeClass() {
        if (!DATA_DIR.exists()) {
            DATA_DIR.mkdirs();
        }
        if (!REPORT_DIR.exists()) {
            REPORT_DIR.mkdirs();
        }
    }

    @Test
    public void testComponentMetadata() throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(createDependency("apache", "tomcat", "5.0.5"));
        DependencyCheckScanAgent scanAgent = createScanAgent();
        scanAgent.setDependencies(dependencies);
        scanAgent.execute();

        Dependency tomcat = scanAgent.getDependencies().get(0);
        Assert.assertEquals(1, tomcat.getIdentifiers().size());
        Identifier id = tomcat.getIdentifiers().iterator().next();
        Assert.assertEquals("cpe:/a:apache:tomcat:5.0.5", id.getValue());
        Assert.assertEquals("cpe", id.getType());

        // This will change over time
        Assert.assertTrue(tomcat.getVulnerabilities().size() > 5);
    }

    private DependencyCheckScanAgent createScanAgent() {
        final DependencyCheckScanAgent scanAgent = new DependencyCheckScanAgent();
        scanAgent.setApplicationName("Dependency-Track");
        scanAgent.setDataDirectory(DATA_DIR.getAbsolutePath());
        scanAgent.setReportOutputDirectory(REPORT_DIR.getAbsolutePath());
        scanAgent.setReportFormat(ReportGenerator.Format.XML);
        scanAgent.setAutoUpdate(true);
        scanAgent.setUpdateOnly(false);
        return scanAgent;
    }

    private Dependency createDependency(final String vendor, final String name, final String version) {
        final Dependency dependency = new Dependency(new File(FileUtils.getBitBucket()), true);
        dependency.setName(name);
        dependency.setVersion(version);
        if (vendor != null) {
            dependency.addEvidence(EvidenceType.VENDOR, "dependency-track", "vendor", vendor, Confidence.HIGHEST);
            dependency.addVendorWeighting(vendor);
        }
        if (name != null) {
            dependency.addEvidence(EvidenceType.PRODUCT, "dependency-track", "name", name, Confidence.HIGHEST);
            dependency.addProductWeighting(name);
        }
        if (version != null) {
            dependency.addEvidence(EvidenceType.VERSION, "dependency-track", "version", version, Confidence.HIGHEST);
        }
        return dependency;
    }
}
