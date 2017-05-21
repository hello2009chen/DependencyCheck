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
 * Copyright (c) 2017 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.data.nsp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.owasp.dependencycheck.utils.Settings;
import org.owasp.dependencycheck.utils.URLConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * Class of methods to search via Node Security Platform.
 *
 * @author Steve Springett
 */
public class NspSearch {

    /**
     * The URL for the public NSP check API.
     */
    private final URL nspCheckUrl;

    /**
     * Whether to use the Proxy when making requests.
     */
    private final boolean useProxy;

    /**
     * Used for logging.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NspSearch.class);

    /**
     * Creates a NspSearch for the given repository URL.
     *
     * @param nspCheckUrl the URL to the public NSP check API
     */
    public NspSearch(URL nspCheckUrl) {
        this.nspCheckUrl = nspCheckUrl;
        if (null != Settings.getString(Settings.KEYS.PROXY_SERVER)) {
            useProxy = true;
            LOGGER.debug("Using proxy");
        } else {
            useProxy = false;
            LOGGER.debug("Not using proxy");
        }
    }

    /**
     * Submits the package.json file to the NSP public /check API and returns
     * a list of zero or more Advisories.
     *
     * @param packageJson the package.json file retrieved from the Dependency
     * @return a List of zero or more Advisory object
     * @throws IOException if it's unable to connect to Node Security Platform
     */
    public List<Advisory> submitPackage(JsonObject packageJson) throws IOException {
        List<Advisory> result = new ArrayList<>();
        byte[] packageDatabytes = packageJson.toString().getBytes(StandardCharsets.UTF_8);

        final HttpURLConnection conn = URLConnectionFactory.createHttpURLConnection(nspCheckUrl, useProxy);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-NSP-VERSION", "2.6.2");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-Length", Integer.toString(packageDatabytes.length));
        conn.connect();

        try (OutputStream os = new BufferedOutputStream(conn.getOutputStream())) {
            os.write(packageDatabytes);
            os.flush();
        }

        if (conn.getResponseCode() == 200) {
            try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
                JsonReader jsonReader = Json.createReader(in);
                JsonArray array = jsonReader.readArray();
                if (array != null) {
                    for (int i=0; i<array.size(); i++) {
                        JsonObject object = array.getJsonObject(i);
                        Advisory advisory = new Advisory();
                        advisory.setId(object.getInt("id"));
                        advisory.setUpdatedAt(object.getString("updated_at", null));
                        advisory.setCreatedAt(object.getString("created_at", null));
                        advisory.setPublishDate(object.getString("publish_date", null));
                        advisory.setOverview(object.getString("overview"));
                        advisory.setRecommendation(object.getString("recommendation", null));
                        advisory.setCvssVector(object.getString("cvss_vector", null));
                        advisory.setCvssScore(Float.parseFloat(object.getJsonNumber("cvss_score").toString()));
                        advisory.setModule(object.getString("module", null));
                        advisory.setVersion(object.getString("version", null));
                        advisory.setVulnerableVersions(object.getString("vulnerable_versions", null));
                        advisory.setPatchedVersions(object.getString("patched_versions", null));
                        advisory.setTitle(object.getString("title", null));
                        advisory.setAdvisory(object.getString("advisory", null));

                        JsonArray jsonPath = object.getJsonArray("path");
                        List<String> stringPath = new ArrayList<>();
                        for (int j=0; j<jsonPath.size(); j++) {
                            stringPath.add(jsonPath.getString(j));
                        }
                        advisory.setPath(stringPath.toArray(new String[stringPath.size()]));

                        result.add(advisory);
                    }
                }
            }
        } else {
            LOGGER.debug("Could not connect to Node Security Platform. Received response code: {} {}",
                    conn.getResponseCode(), conn.getResponseMessage());
            throw new IOException("Could not connect to Node Security Platform");
        }
        return result;
    }
}
