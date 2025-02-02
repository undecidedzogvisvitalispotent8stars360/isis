/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.core.metamodel.facets.object.paged;

import org.springframework.lang.Nullable;

import org.apache.isis.core.metamodel.facetapi.Facet;

/**
 * Mechanism for obtaining the page sizes for tables showing instances of a class.
 */
public interface PagedFacet extends Facet {

    int value();

    /**
     * Returns the page-size as held by given {@code pagedFacet} if present, otherwise
     * falls back to {@code defaultPageSize}.
     * @param pagedFacet - null-able
     * @param defaultPageSize
     */
    static int pageSizeOrDefault(
            final @Nullable PagedFacet pagedFacet,
            final int defaultPageSize) {
        return pagedFacet != null
                ? pagedFacet.value()
                : defaultPageSize;
    }

}
