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
package org.apache.isis.testdomain.conf;

import javax.inject.Singleton;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import org.apache.isis.core.config.presets.IsisPresets;
import org.apache.isis.core.runtimeservices.IsisModuleCoreRuntimeServices;
import org.apache.isis.extensions.secman.api.SecmanConfiguration;
import org.apache.isis.extensions.secman.api.permission.dom.PermissionsEvaluationService;
import org.apache.isis.extensions.secman.api.permission.dom.PermissionsEvaluationServiceAllowBeatsVeto;
import org.apache.isis.persistence.jdo.datanucleus.IsisModuleJdoDatanucleus;
import org.apache.isis.security.shiro.IsisModuleSecurityShiro;
import org.apache.isis.testdomain.jdo.JdoTestDomainModule;
import org.apache.isis.testdomain.util.kv.KVStoreForTesting;
import org.apache.isis.testing.fixtures.applib.IsisModuleTestingFixturesApplib;

@Configuration
@Import({
    IsisModuleCoreRuntimeServices.class,
    IsisModuleSecurityShiro.class,
    IsisModuleJdoDatanucleus.class,
    IsisModuleTestingFixturesApplib.class,
    KVStoreForTesting.class, // Helper for JUnit Tests
})
@ComponentScan(
        basePackageClasses= {
                JdoTestDomainModule.class
        })
@PropertySources({
    @PropertySource(IsisPresets.NoTranslations),
    @PropertySource(IsisPresets.DatanucleusAutocreateNoValidate),
    @PropertySource(IsisPresets.H2InMemory_withUniqueSchema),
})
public class Configuration_usingJdoAndShiro {

    @Bean @Singleton
    public SecmanConfiguration securityModuleConfigBean() {
        return SecmanConfiguration.builder()
                .build();
    }

    @Bean @Singleton
    public PermissionsEvaluationService permissionsEvaluationService() {
        return new PermissionsEvaluationServiceAllowBeatsVeto();
    }

}
