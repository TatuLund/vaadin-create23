package org.vaadin.tatu.vaadincreate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;

import com.tngtech.archunit.lang.ArchRule;

import org.junit.runner.RunWith;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;

/**
 * Architecture tests for Vaadin Create.
 * 
 * Summary of the rules: Services are called only by presenters, UI and auth
 * classes. EventBus is only used by presenters, UI and AboutView. DAOs are only
 * used by services. Backend should not use Vaadin classes. Service should be
 * used only via its interface outside of the backend. Presenters should not use
 * Vaadin classes. AppLayout should be used only by VaadinCreateUI. LoginView
 * should be used only by VaadinCreateUI. ErrorView should be used only by
 * AppLayout. AccessControl should be used only via its interface outside of the
 * auth package. EventBus should be used only by its interface outside of the
 * eventbus package. LockedObjects should be used only via its interface outside
 * of the lockedobjects package.
 */
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = { "org.vaadin.tatu.vaadincreate", "com.vaadin" })
public class ArchitectureTest {

    @ArchTest
    public static final ArchRule servicesAreCalledOnlyByPresetersUIandAuth = classes()
            .that().resideInAPackage("..backend..").and()
            .haveSimpleNameEndingWith("Service").should().onlyBeAccessed()
            .byClassesThat(simpleNameEndingWith("Presenter")
                    .or(simpleNameEndingWith("Test"))
                    .or(simpleName("VaadinCreateUI"))
                    .or(simpleName("AboutView"))
                    .or(resideInAPackage("..auth.."))
                    .or(resideInAPackage("..backend..")));

    @ArchTest
    public static final ArchRule eventBusIsOnlyUsedByPresentersAndUI = classes()
            .that().resideInAPackage("..vaadincreate.eventbus..").should()
            .onlyBeAccessed()
            .byClassesThat(simpleNameEndingWith("Presenter")
                    .or(simpleNameEndingWith("Test"))
                    .or(simpleName("VaadinCreateUI"))
                    .or(simpleName("AboutView"))
                    .or(resideInAPackage("..eventbus.."))
                    .or(resideInAPackage("..locking.."))
                    .or(resideInAPackage("..auth.."))
                    .or(resideInAPackage("..backend..")));

    @ArchTest
    public static final ArchRule daosAreOnlyUsedByServices = classes().that()
            .haveSimpleNameEndingWith("Dao").should().onlyBeAccessed()
            .byClassesThat(resideInAPackage("..service..")
                    .or(resideInAPackage("..dao..")));

    @ArchTest
    public static final ArchRule backendShouldNotUseVaadin = classes().that()
            .resideInAPackage("com.vaadin..").should().onlyAccessClassesThat()
            .resideOutsideOfPackage("..backend..");

    @ArchTest
    public static final ArchRule serviceImplShouldBeUsedOnlyByBackend = classes()
            .that().resideInAPackage("..backend..").and()
            .haveSimpleNameEndingWith("ServiceImpl").should().onlyBeAccessed()
            .byClassesThat().resideInAPackage("..backend..");

    @ArchTest
    public static final ArchRule presentersShouldNotUseVaadin = classes().that()
            .haveSimpleNameEndingWith("Presenter").should()
            .onlyAccessClassesThat().resideOutsideOfPackage("com.vaadin..");

    @ArchTest
    public static final ArchRule appLayoutShouldBeUsedOnlyByVaadinCreateUI = classes()
            .that().haveSimpleName("AppLayout").should().onlyBeAccessed()
            .byClassesThat(simpleNameEndingWith("Test")
                    .or(simpleName("VaadinCreateUI")).or(simpleName("AppLayout")
                            .or(belongTo(simpleName("AppLayout")))));

    @ArchTest
    public static final ArchRule loginViewShouldBeUsedOnlyByVaadinCreateUI = classes()
            .that().haveSimpleName("LoginView").should().onlyBeAccessed()
            .byClassesThat(simpleNameEndingWith("Test")
                    .or(simpleName("VaadinCreateUI"))
                    .or(simpleName("LoginView")));

    @ArchTest
    public static final ArchRule errorViewShouldBeUsedOnlyByAppLayout = classes()
            .that().haveSimpleName("ErrorView").should().onlyBeAccessed()
            .byClassesThat(simpleName("AppLayout").or(simpleName("ErrorView")));

    @ArchTest
    public static final ArchRule basicAccessControlShouldBeUsedOnlyByAuth = classes()
            .that().haveSimpleName("BasicAccessControl").should()
            .onlyBeAccessed().byClassesThat(
                    simpleNameEndingWith("Test").or(simpleName("AccessControl"))
                            .or(simpleName("VaadinCreateUI"))
                            .or(simpleName("BasicAccessControl")));

    @ArchTest
    public static final ArchRule eventBusImplShouldBeUsedOnlyByEventBus = classes()
            .that().haveSimpleName("EventBusImpl").should().onlyBeAccessed()
            .byClassesThat(
                    simpleName("EventBus").or(simpleName("EventBusImpl")));

    @ArchTest
    public static final ArchRule lockedObjectsImplShouldBeUsedOnlyByLockedObjects = classes()
            .that().haveSimpleName("LockedObjectsImpl").should()
            .onlyBeAccessed().byClassesThat(simpleName("LockedObjects")
                    .or(simpleName("LockedObjectsImpl")));
}
