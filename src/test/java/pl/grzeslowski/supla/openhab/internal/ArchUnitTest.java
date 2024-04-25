/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * <p>See the NOTICE file(s) distributed with this work for additional information.
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 */
package pl.grzeslowski.supla.openhab.internal;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** @author Grzeslowski - Initial contribution */
@AnalyzeClasses(
        packages = {"pl.grzeslowski.supla.openhab.."},
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchUnitTest {

    @ArchTest
    public static final ArchRule serverDoesNotAccessCloud = noClasses()
            .that()
            .resideInAPackage("pl.grzeslowski.supla.openhab.internal.server..")
            .should()
            .accessClassesThat()
            .resideInAPackage("pl.grzeslowski.supla.openhab.internal.cloud..");

    @ArchTest
    public static final ArchRule cloudDoesNotAccessServer = noClasses()
            .that()
            .resideInAPackage("pl.grzeslowski.supla.openhab.internal.cloud..")
            .should()
            .accessClassesThat()
            .resideInAPackage("pl.grzeslowski.supla.openhab.internal.server..");

    @ArchTest
    public static final ArchRule cloudDoNotUseProtocolJava = noClasses()
            .that()
            .resideInAPackage("pl.grzeslowski.supla.openhab.internal.cloud..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("pl.grzeslowski.jsupla.protocoljava..");

    @ArchTest
    public static final ArchRule cloudDoNotUseServer = noClasses()
            .that()
            .resideInAPackage("pl.grzeslowski.supla.openhab.internal.cloud..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("pl.grzeslowski.jsupla.server..");

    @ArchTest
    public static final ArchRule cloudDoNotUseNetty = noClasses()
            .that()
            .resideInAPackage("pl.grzeslowski.supla.openhab.internal.cloud..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("pl.grzeslowski.jsupla.netty..");

    @ArchTest
    public static final ArchRule serverDoNotUseApi = noClasses()
            .that()
            .resideInAPackage("pl.grzeslowski.supla.openhab.internal.server..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("pl.grzeslowski.jsupla.api..");

    @ArchTest
    public static final ArchRule serverDoNotUseSwagger = noClasses()
            .that()
            .resideInAPackage("pl.grzeslowski.supla.openhab.internal.server..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.swagger..");
}
