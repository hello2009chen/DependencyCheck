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
package org.owasp.dependencycheck.data.central;

import javax.annotation.concurrent.ThreadSafe;

/**
 * An exception used when Central has blocked the requests due to too many requests.
 *
 * @author Jeremy Long
 */
@ThreadSafe
public class TooManyRequestsException extends Exception {

    /**
     * The serial version UID for serialization.
     */
    private static final long serialVersionUID = 4044042874643986535L;

    /**
     * Creates a new TooManyRequestsException.
     */
    public TooManyRequestsException() {
        super();
    }

    /**
     * Creates a new TooManyRequestsException.
     *
     * @param msg a message for the exception.
     */
    public TooManyRequestsException(String msg) {
        super(msg);
    }

    /**
     * Creates a new TooManyRequestsException.
     *
     * @param ex the cause of the exception.
     */
    public TooManyRequestsException(Throwable ex) {
        super(ex);
    }

    /**
     * Creates a new TooManyRequestsException.
     *
     * @param msg a message for the exception.
     * @param ex the cause of the exception.
     */
    public TooManyRequestsException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
