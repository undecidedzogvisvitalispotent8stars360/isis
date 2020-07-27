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
package org.apache.isis.viewer.restfulobjects.viewer.resources;

import javax.ws.rs.core.Response;

import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.xactn.TransactionService;
import org.apache.isis.core.metamodel.interactions.managed.ActionInteraction;
import org.apache.isis.core.metamodel.interactions.managed.InteractionVeto;
import org.apache.isis.core.metamodel.interactions.managed.ManagedMember;
import org.apache.isis.core.metamodel.interactions.managed.ManagedParameter2;
import org.apache.isis.core.metamodel.interactions.managed.ParameterNegotiationModel;
import org.apache.isis.core.metamodel.interactions.managed.ActionInteraction.SemanticConstraint;
import org.apache.isis.core.metamodel.interactions.managed.MemberInteraction.AccessIntent;
import org.apache.isis.core.metamodel.spec.ManagedObject;
import org.apache.isis.viewer.restfulobjects.applib.JsonRepresentation;
import org.apache.isis.viewer.restfulobjects.rendering.IResourceContext;
import org.apache.isis.viewer.restfulobjects.rendering.domainobjects.ActionResultReprRenderer;
import org.apache.isis.viewer.restfulobjects.rendering.domainobjects.DomainObjectLinkTo;
import org.apache.isis.viewer.restfulobjects.rendering.domainobjects.DomainServiceLinkTo;
import org.apache.isis.viewer.restfulobjects.rendering.domainobjects.ObjectAdapterLinkTo;
import org.apache.isis.viewer.restfulobjects.rendering.domainobjects.ObjectAndActionInvocation;
import org.apache.isis.viewer.restfulobjects.rendering.service.RepresentationService;
import org.apache.isis.viewer.restfulobjects.viewer.context.ResourceContext;

import lombok.NonNull;
import lombok.val;

class DomainResourceHelper {

    private final IResourceContext resourceContext;
    private final RepresentationService representationService;
    private final TransactionService transactionService;

    public static DomainResourceHelper ofObjectResource(
            IResourceContext resourceContext,
            ManagedObject objectAdapter) {
        return new DomainResourceHelper(resourceContext, objectAdapter, new DomainObjectLinkTo());
    }
    
    public static DomainResourceHelper ofServiceResource(
            IResourceContext resourceContext,
            ManagedObject objectAdapter) {
        return new DomainResourceHelper(resourceContext, objectAdapter, new DomainServiceLinkTo());
    }
    
    private DomainResourceHelper(
            final IResourceContext resourceContext,
            final ManagedObject objectAdapter,
            final ObjectAdapterLinkTo adapterLinkTo) {

        ((ResourceContext)resourceContext).setObjectAdapterLinkTo(adapterLinkTo);
        
        this.resourceContext = resourceContext;
        this.objectAdapter = objectAdapter;

        adapterLinkTo.usingUrlBase(this.resourceContext)
        .with(this.objectAdapter);

        representationService = lookupService(RepresentationService.class);
        transactionService = lookupService(TransactionService.class);
    }

    private final ManagedObject objectAdapter;


    // //////////////////////////////////////
    // Helpers (resource delegate here)
    // //////////////////////////////////////

    /**
     * Simply delegates to the {@link org.apache.isis.viewer.restfulobjects.rendering.service.RepresentationService} to
     * render a representation of the object.
     */
    public Response objectRepresentation() {
        transactionService.flushTransaction();
        return representationService
                .objectRepresentation(resourceContext, objectAdapter);
    }

    /**
     * Obtains the property (checking it is visible) of the object and then delegates to the
     * {@link org.apache.isis.viewer.restfulobjects.rendering.service.RepresentationService} to render a representation
     * of that property.
     */
    public Response propertyDetails(
            final String propertyId,
            final ManagedMember.RepresentationMode representationMode) {

        val property = ObjectAdapterAccessHelper.of(resourceContext, objectAdapter)
                .getPropertyThatIsVisibleForIntent(propertyId, AccessIntent.ACCESS);
        property.setRepresentationMode(representationMode);

        transactionService.flushTransaction();
        return representationService.propertyDetails(resourceContext, property);
    }


    /**
     * Obtains the collection (checking it is visible) of the object and then delegates to the
     * {@link org.apache.isis.viewer.restfulobjects.rendering.service.RepresentationService} to render a representation
     * of that collection.
     */
    public Response collectionDetails(
            final String collectionId,
            final ManagedMember.RepresentationMode representationMode) {

        val collection = ObjectAdapterAccessHelper.of(resourceContext, objectAdapter)
                .getCollectionThatIsVisibleForIntent(collectionId, AccessIntent.ACCESS);
        collection.setRepresentationMode(representationMode);

        transactionService.flushTransaction();
        return representationService.collectionDetails(resourceContext, collection);
    }


    /**
     * Obtains the action details (arguments etc), checking it is visible, of the object and then delegates to the
     * {@link org.apache.isis.viewer.restfulobjects.rendering.service.RepresentationService} to render a representation
     * of that object's action (arguments).
     */
    public Response actionPrompt(final String actionId) {

        val action = ObjectAdapterAccessHelper.of(resourceContext, objectAdapter)
                .getObjectActionThatIsVisibleForIntentAndSemanticConstraint(
                        actionId, AccessIntent.ACCESS, SemanticConstraint.NONE);

        transactionService.flushTransaction();
        return representationService.actionPrompt(resourceContext, action);
    }

    /**
     * Invokes the action for the object  (checking it is visible) and then delegates to the
     * {@link org.apache.isis.viewer.restfulobjects.rendering.service.RepresentationService} to render a representation
     * of the result of that action.
     *
     * <p>
     *     The action must have {@link SemanticsOf#isSafeInNature()} safe/request-cacheable}  semantics
     *     otherwise an error response is thrown.
     * </p>
     */
    public Response invokeActionQueryOnly(final String actionId, final JsonRepresentation arguments) {

        return invokeAction( 
                actionId, AccessIntent.MUTATE, SemanticConstraint.SAFE, 
                arguments, ActionResultReprRenderer.SelfLink.EXCLUDED);
    }

    /**
     * Invokes the action for the object  (checking it is visible) and then delegates to the
     * {@link org.apache.isis.viewer.restfulobjects.rendering.service.RepresentationService} to render a representation
     * of the result of that action.
     *
     * <p>
     *     The action must have {@link SemanticsOf#IDEMPOTENT idempotent}
     *     semantics otherwise an error response is thrown.
     * </p>
     */
    public Response invokeActionIdempotent(final String actionId, final JsonRepresentation arguments) {

        return invokeAction( 
                actionId, AccessIntent.MUTATE, SemanticConstraint.IDEMPOTENT, 
                arguments, ActionResultReprRenderer.SelfLink.EXCLUDED);
    }

    /**
     * Invokes the action for the object  (checking it is visible) and then delegates to the
     * {@link org.apache.isis.viewer.restfulobjects.rendering.service.RepresentationService} to render a representation
     * of the result of that action.
     */
    public Response invokeAction(final String actionId, final JsonRepresentation arguments) {

        return invokeAction( 
                actionId, AccessIntent.MUTATE, SemanticConstraint.NONE, 
                arguments, ActionResultReprRenderer.SelfLink.EXCLUDED);
    }

    private Response invokeAction(
            @NonNull final String actionId, 
            @NonNull final AccessIntent intent,
            @NonNull final SemanticConstraint semanticConstraint,
            @NonNull final JsonRepresentation arguments,
            @NonNull final ActionResultReprRenderer.SelfLink selfLink) {
        
        val where = resourceContext.getWhere();
        
        // lombok issue, needs explicit cast here 
        val actionInteraction = (ActionInteraction) ActionInteraction.start(objectAdapter, actionId)
        .checkVisibility(where)
        .checkUsability(where, intent)
        .checkSemanticConstraint(semanticConstraint);

//TODO can we simplify the API?        
//        actionInteraction.startParameterNegotiation(pendingArgs->{
//            actionInteraction.invokeWith(pendingArgs);
//        });
        
        
        actionInteraction
        .useParameters(action->{
            
            val argAdapters = ObjectActionArgHelper.of(resourceContext, action)
                    .parseAndValidateArguments(arguments);
            
            return argAdapters;
            
        }, 
                (ManagedParameter2 managedParameter, InteractionVeto veto)->{
                    InteractionFailureHandler.onParameterInvalid(managedParameter, veto, arguments);
                }
        );
        
        if(resourceContext.isValidateOnly()) {
            actionInteraction.validateElseThrow(InteractionFailureHandler::onFailure);
            return Response.noContent().build();
        }
        
        val actionInteractionResult = actionInteraction
                .getResultElseThrow(InteractionFailureHandler::onFailure);
        
        val objectAndActionInvocation = ObjectAndActionInvocation.of(actionInteractionResult, arguments, selfLink);

        // response
        transactionService.flushTransaction();
        return representationService.actionResult(resourceContext, objectAndActionInvocation);
    }


    // //////////////////////////////////////
    // dependencies (from context)
    // //////////////////////////////////////

    private <T> T lookupService(Class<T> serviceType) {
        return resourceContext.getServiceRegistry().lookupServiceElseFail(serviceType);
    }

}

