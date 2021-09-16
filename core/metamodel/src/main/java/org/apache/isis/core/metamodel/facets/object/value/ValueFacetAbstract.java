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
package org.apache.isis.core.metamodel.facets.object.value;

import org.apache.isis.applib.adapters.ValueSemanticsProvider;
import org.apache.isis.commons.collections.Can;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetAbstract;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;

public abstract class ValueFacetAbstract
extends FacetAbstract
implements ValueFacet {

    private static final Class<? extends Facet> type() {
        return ValueFacet.class;
    }

    private final Can<ValueSemanticsProvider<?>> semanticsProviders;

    protected ValueFacetAbstract(
            final Can<ValueSemanticsProvider<?>> semanticsProviders,
            final FacetHolder holder,
            final Facet.Precedence precedence) {

        super(type(), holder, precedence);

        this.semanticsProviders = semanticsProviders;

        // we now figure add all the facets supported. Note that we do not use
        // FacetUtil.addFacet,
        // because we need to add them explicitly to our delegate facetholder
        // but have the
        // facets themselves reference this value's holder.

        super.getFacetHolder().addFacet(this); // add just ValueFacet.class

    }

    protected boolean hasSemanticsProvider() {
        return !this.semanticsProviders.isEmpty();
    }

}
