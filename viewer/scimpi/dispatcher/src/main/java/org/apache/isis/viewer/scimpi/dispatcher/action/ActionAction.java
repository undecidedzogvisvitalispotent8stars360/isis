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


package org.apache.isis.viewer.scimpi.dispatcher.action;

import java.io.IOException;
import java.util.List;

import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.commons.debug.DebugBuilder;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.consent.Consent;
import org.apache.isis.core.metamodel.consent.Veto;
import org.apache.isis.core.metamodel.facets.object.parseable.ParseableFacet;
import org.apache.isis.core.metamodel.facets.object.parseable.TextEntryParseException;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;
import org.apache.isis.runtimes.dflt.runtime.context.IsisContext;
import org.apache.isis.runtimes.dflt.runtime.persistence.ConcurrencyException;
import org.apache.isis.runtimes.dflt.runtime.transaction.messagebroker.MessageBroker;
import org.apache.isis.viewer.scimpi.dispatcher.Action;
import org.apache.isis.viewer.scimpi.dispatcher.Dispatcher;
import org.apache.isis.viewer.scimpi.dispatcher.NotLoggedInException;
import org.apache.isis.viewer.scimpi.dispatcher.UserlessSession;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestContext;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestContext.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.edit.FieldEditState;
import org.apache.isis.viewer.scimpi.dispatcher.edit.FormState;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;


public class ActionAction implements Action {

    public static final String ACTION = "action";

    @Override
    public String getName() {
        return ACTION;
    }

    /**
     * REVIEW - this and EditAction are very similar - refactor out common code.
     */
    @Override
    public void process(RequestContext context) throws IOException {
        String objectId = context.getParameter(OBJECT);
        String version = context.getParameter(VERSION);
        String methodName = context.getParameter(METHOD);
        String override = context.getParameter(RESULT_OVERRIDE);
        String resultName = context.getParameter(RESULT_NAME);
        String message = context.getParameter(MESSAGE);
        resultName = resultName == null ? RequestContext.RESULT : resultName;
        
        FormState entryState = null;
        try {
            ObjectAdapter object = MethodsUtils.findObject(context, objectId);
            // FIXME need to find method based on the set of parameters. otherwise overloaded method may be incorrectly selected.
            ObjectAction action = MethodsUtils.findAction(object, methodName);
            entryState = validateParameters(context, action, object);

            AuthenticationSession session = context.getSession();
            if (session == null && action.isUsable(new UserlessSession(), object).isVetoed()) {
                throw new NotLoggedInException();
            }
            
            object.checkLock(context.getVersion(version));
       /*     
            Version adapterVersion = object.getVersion();
            if (adapterVersion.different(context.getVersion(version))) {
                
                IsisContext.getMessageBroker().addMessage("The " + object.getSpecification().getSingularName() + " was edited " +
                        "by another user (" + adapterVersion.getUser() +  "). Please  make your changes based on their changes.");

                entryState.setForm(objectId + ":" + methodName);
                context.addVariable(ENTRY_FIELDS, entryState, Scope.REQUEST);
                context.addVariable(resultName, objectId, Scope.REQUEST);
                if (override != null) {
                    context.addVariable(resultName, override, Scope.REQUEST);
                }   
                final String error = entryState.getError();
                if (error != null) {
                    context.addVariable(RequestContext.ERROR, error, Scope.REQUEST);
                }
                
                String view = context.getParameter(ERRORS);
                context.setRequestPath(view, Dispatcher.ACTION);

            } else    */         
            if (entryState.isValid()) {
                boolean hasResult = invokeMethod(context, resultName, object, action, entryState);
                String view = context.getParameter(hasResult ? VIEW : VOID);
                
             //   context.clearVariables(Scope.REQUEST);

                int questionMark = view == null ? -1 : view.indexOf("?");
                if (questionMark > -1) {
                    String params[] = view.substring(questionMark + 1).split("&"); 
                    for (String param : params) { 
                        int equals = param.indexOf("="); 
                        context.addVariable(param.substring(0, equals), param.substring(equals + 1), Scope.REQUEST); 
                        view = view.substring(0, questionMark); 
                    }
                }
                context.setRequestPath(view);
                if (message != null) {
                    MessageBroker messageBroker = IsisContext.getMessageBroker();
                    messageBroker.addMessage(message);
                }
                if (override != null) {
                    context.addVariable(resultName, override, Scope.REQUEST);
                }                
                if (context.getVariable(resultName) == null) {
                    context.addVariable(resultName, objectId, Scope.REQUEST);
                }                
            } else {
                entryState.setForm(objectId + ":" + methodName);
                context.addVariable(ENTRY_FIELDS, entryState, Scope.REQUEST);
                context.addVariable(resultName, objectId, Scope.REQUEST);
                if (override != null) {
                    context.addVariable(resultName, override, Scope.REQUEST);
                }   
                final String error = entryState.getError();
                /*
                if (error != null) {
                    context.addVariable(RequestContext.ERROR, error, Scope.REQUEST);
                }
                */
                
                String view = context.getParameter(ERRORS);
                context.setRequestPath(view, Dispatcher.ACTION);
                
                MessageBroker messageBroker = IsisContext.getMessageBroker();
                messageBroker.addWarning(error);
            }

        } catch (ConcurrencyException e) {
            
            IsisContext.getMessageBroker().addMessage(e.getMessage());

            entryState.setForm(objectId + ":" + methodName);
            context.addVariable(ENTRY_FIELDS, entryState, Scope.REQUEST);
            context.addVariable(resultName, objectId, Scope.REQUEST);
            if (override != null) {
                context.addVariable(resultName, override, Scope.REQUEST);
            }   
            final String error = entryState.getError();
            if (error != null) {
                context.addVariable(RequestContext.ERROR, error, Scope.REQUEST);
            }
            
            String view = context.getParameter(ERRORS);
            context.setRequestPath(view, Dispatcher.ACTION);
            
            
        } catch (RuntimeException e) {
            IsisContext.getMessageBroker().getMessages();
            IsisContext.getMessageBroker().getWarnings();
            IsisContext.getUpdateNotifier().clear();
            IsisContext.getUpdateNotifier().clear();
            throw e;
        }
    }

    private boolean invokeMethod(
            RequestContext context,
            String variable,
            ObjectAdapter object,
            ObjectAction action,
            FormState entryState) {

        ObjectAdapter[] parameters = getParameters(action, entryState);
        String scopeName = context.getParameter(SCOPE);
        Scope scope = RequestContext.scope(scopeName, Scope.REQUEST);
        return MethodsUtils.runMethod(context, action, object, parameters, variable, scope);
    }

    private ObjectAdapter[] getParameters(ObjectAction action, FormState entryState) {
        int parameterCount = action.getParameterCount();
        ObjectAdapter[] parameters = new ObjectAdapter[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            parameters[i] = entryState.getField(parameterName(i)).getValue();
        }
        return parameters;
    }

    private FormState validateParameters(RequestContext context, ObjectAction action, ObjectAdapter object) {
        FormState formState = new FormState();
        List<ObjectActionParameter> parameters2 = action.getParameters();
        int parameterCount = action.getParameterCount();
        for (int i = 0; i < parameterCount; i++) {
            String fieldName = parameterName(i);
            String newEntry = context.getParameter(fieldName);
            
            if(newEntry != null && newEntry.equals("-OTHER-")) {
                newEntry = context.getParameter(fieldName + "-other");
            }
            
            if (newEntry == null) {
                // TODO figure out a better way to determine if boolean or a password
                ObjectSpecification spec = parameters2.get(i).getSpecification();
                if (spec.isOfType(IsisContext.getSpecificationLoader().loadSpecification(boolean.class))
                        || spec.isOfType(IsisContext.getSpecificationLoader().loadSpecification(Boolean.class))) {
                    newEntry = FALSE;
                } else {
                    newEntry = "";
                }
            }
            FieldEditState fieldState = formState.createField(fieldName, newEntry);
            Consent consent = null;

            
            if (!parameters2.get(i).isOptional() && newEntry.equals("")) {
                consent = new Veto(parameters2.get(i).getName() + " required");
                formState.setError("Not all fields have been set");

            } else  if (parameters2.get(i).getSpecification().getFacet(ParseableFacet.class) != null) {
                try {
                    ParseableFacet facet = parameters2.get(i).getSpecification().getFacet(ParseableFacet.class);
                    String message = parameters2.get(i).isValid(object, newEntry); 
                    if (message != null) { 
                        consent = new Veto(message); 
                        formState.setError("Not all fields are valid");
                    } 
                    ObjectAdapter entry = facet.parseTextEntry(null, newEntry);
                    fieldState.setValue(entry);
                } catch (TextEntryParseException e) {
                    consent = new Veto(e.getMessage());
                    formState.setError("Not all fields are valid");
                }
            } else {
                fieldState.setValue(newEntry == null ? null : context.getMappedObject(newEntry));
            }
            if (consent != null && consent.isVetoed()) {
                fieldState.setError(consent.getReason());
            }
        }
        
        if (formState.isValid()) {
            ObjectAdapter[] parameters = getParameters(action, formState);
            Consent consent = action.isProposedArgumentSetValid(object, parameters);
            if (consent != null && consent.isVetoed()) {
                formState.setError(consent.getReason());
            }
        }
        
        return formState;
    }

    public static String parameterName(int index) {
        return PARAMETER + (index + 1);
    }

    @Override
    public void init() {}

    @Override
    public void debug(DebugBuilder debug) {}
}

