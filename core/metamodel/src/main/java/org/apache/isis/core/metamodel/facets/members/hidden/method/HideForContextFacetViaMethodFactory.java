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

package org.apache.isis.core.metamodel.facets.members.hidden.method;

import java.lang.reflect.Method;

import org.apache.isis.core.commons.collections.Can;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facetapi.FacetUtil;
import org.apache.isis.core.metamodel.facetapi.FeatureType;
import org.apache.isis.core.metamodel.facets.MethodFinder2;
import org.apache.isis.core.metamodel.facets.MethodLiteralConstants;
import org.apache.isis.core.metamodel.facets.MethodPrefixBasedFacetFactoryAbstract;

import lombok.val;

public class HideForContextFacetViaMethodFactory 
extends MethodPrefixBasedFacetFactoryAbstract {

    private static final String PREFIX = MethodLiteralConstants.HIDE_PREFIX;

    public HideForContextFacetViaMethodFactory() {
        super(FeatureType.MEMBERS, OrphanValidation.VALIDATE, Can.ofSingleton(PREFIX));
    }

    @Override
    public void process(final ProcessMethodContext processMethodContext) {
        attachHideFacetIfHideMethodIsFound(processMethodContext);
    }

    private void attachHideFacetIfHideMethodIsFound(final ProcessMethodContext processMethodContext) {

        final Method actionOrGetter = processMethodContext.getMethod();
        
        val namingConvention = PREFIX_BASED_NAMING.map(naming->naming.providerForMember(actionOrGetter, PREFIX));
        
        val cls = processMethodContext.getCls();
        Method hideMethod = MethodFinder2.findMethod(
                cls, 
                namingConvention.map(x->x.get()), 
                boolean.class, 
                NO_ARG);
        if (hideMethod == null) {

            boolean noParamsOnly = getConfiguration().getCore().getMetaModel().getValidator().isNoParamsOnly();
            boolean searchExactMatch = !noParamsOnly;
            if(searchExactMatch) {
                hideMethod = MethodFinder2.findMethod(
                        cls, 
                        namingConvention.map(x->x.get()), 
                        boolean.class, 
                        actionOrGetter.getParameterTypes());
            }
        }

        if (hideMethod == null) {
            return;
        }

        processMethodContext.removeMethod(hideMethod);

        final FacetHolder facetedMethod = processMethodContext.getFacetHolder();
        FacetUtil.addFacet(new HideForContextFacetViaMethod(hideMethod, facetedMethod));
    }

}
