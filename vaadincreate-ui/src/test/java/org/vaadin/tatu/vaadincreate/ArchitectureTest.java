package org.vaadin.tatu.vaadincreate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import static com.tngtech.archunit.lang.conditions.ArchPredicates.*;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;

import com.tngtech.archunit.lang.ArchRule;
import com.vaadin.navigator.View;

import org.hibernate.SessionFactory;
import org.junit.runner.RunWith;
import org.vaadin.tatu.vaadincreate.auth.AllPermitted;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.dao.HibernateUtil;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.tngtech.archunit.core.importer.ImportOption;
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
@AnalyzeClasses(packages = { "org.vaadin.tatu.vaadincreate",
        "com.vaadin" }, importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    public static final ArchRule Services_are_called_only_by_the_Preseters_UI_and_Auth = classes()
            .that().resideInAPackage("..backend..").and()
            .haveSimpleNameEndingWith("Service").should().onlyBeAccessed()
            .byClassesThat(have(simpleNameEndingWith("Presenter"))
                    .or(have(simpleName("VaadinCreateUI")))
                    .or(have(simpleName("AboutView")))
                    .or(resideInAPackage("..auth.."))
                    .or(resideInAPackage("..backend..")))
            .as("UI implementing code should not depend on backend");

    @ArchTest
    public static final ArchRule EventBus_is_only_used_by_Presenters_and_UI = classes()
            .that().resideInAPackage("..vaadincreate.eventbus..").should()
            .onlyBeAccessed()
            .byClassesThat(have(simpleNameEndingWith("Presenter"))
                    .or(have(simpleName("VaadinCreateUI")))
                    .or(have(simpleName("AboutView")))
                    .or(resideInAPackage("..eventbus.."))
                    .or(resideInAPackage("..locking.."))
                    .or(resideInAPackage("..auth.."))
                    .or(resideInAPackage("..backend..")));

    @ArchTest
    public static final ArchRule only_the_Services_may_use_DAOs = classes()
            .that().haveSimpleNameEndingWith("Dao").should().onlyBeAccessed()
            .byClassesThat(resideInAPackage("..service..")
                    .or(resideInAPackage("..dao..")))
            .as("DAOs should be used only by services");

    @ArchTest
    // Only the HibernateUtil may use SessionFactory
    public static final ArchRule only_DAOs_may_use_the_SessionFactory = noClasses()
            .that().resideOutsideOfPackage("..dao..").should()
            .accessClassesThat().areAssignableTo(SessionFactory.class)
            .as("DAOs may use the " + SessionFactory.class.getSimpleName()
                    + " only via HibernateUtil");

    @ArchTest
    public static final ArchRule only_DAOs_may_use_the_HibernateUtil = noClasses()
            .that().resideOutsideOfPackage("..dao..").should()
            .accessClassesThat().areAssignableTo(HibernateUtil.class)
            .as("Only DAOs may use the " + HibernateUtil.class.getSimpleName());

    @ArchTest
    public static final ArchRule backend_should_not_use_Vaadin = classes()
            .that().resideInAPackage("com.vaadin..").should()
            .onlyAccessClassesThat().resideOutsideOfPackage("..backend..")
            .as("backend should not depend on Vaadin");

    @ArchTest
    public static final ArchRule ServiceImpl_should_be_used_only_by_backend = classes()
            .that().resideInAPackage("..backend..").and()
            .haveSimpleNameEndingWith("ServiceImpl").should().onlyBeAccessed()
            .byClassesThat().resideInAPackage("..backend..")
            .as("Service implementations should be used only via their interfaces");

    @ArchTest
    public static final ArchRule Presenters_should_not_use_Vaadin = classes()
            .that().haveSimpleNameEndingWith("Presenter").should()
            .onlyAccessClassesThat().resideOutsideOfPackage("com.vaadin..")
            .as("Presenters should not have UI implementing code");

    @ArchTest
    public static final ArchRule AppLayout_should_be_used_only_by_VaadinCreateUI = classes()
            .that().haveSimpleName("AppLayout").should().onlyBeAccessed()
            .byClassesThat(have(simpleName("VaadinCreateUI"))
                    .or(have(simpleName("AppLayout"))
                            .or(belongTo(simpleName("AppLayout")))));

    @ArchTest
    public static final ArchRule LoginView_should_be_used_only_by_VaadinCreateUI = classes()
            .that().haveSimpleName("LoginView").should().onlyBeAccessed()
            .byClassesThat(have(simpleName("VaadinCreateUI"))
                    .or(have(simpleName("LoginView"))));

    @ArchTest
    public static final ArchRule ErrorView_should_be_used_only_by_AppLayout = classes()
            .that().haveSimpleName("ErrorView").should().onlyBeAccessed()
            .byClassesThat(simpleName("AppLayout").or(simpleName("ErrorView")));

    @ArchTest
    public static final ArchRule BasicAccessControl_should_be_used_only_by_auth = classes()
            .that().haveSimpleName("BasicAccessControl").should()
            .onlyBeAccessed()
            .byClassesThat(have(simpleName("AccessControl"))
                    .or(have(simpleName("VaadinCreateUI")))
                    .or(have(simpleName("BasicAccessControl"))));

    @ArchTest
    public static final ArchRule EventBusImpl_should_be_used_only_by_EventBus = classes()
            .that().haveSimpleName("EventBusImpl").should().onlyBeAccessed()
            .byClassesThat(have(simpleName("EventBus"))
                    .or(have(simpleName("EventBusImpl"))))
            .as("EventBus should be used only view interface");

    @ArchTest
    public static final ArchRule LockedObjectsImpl_should_be_used_only_by_LockedObjects = classes()
            .that().haveSimpleName("LockedObjectsImpl").should()
            .onlyBeAccessed()
            .byClassesThat(have(simpleName("LockedObjects"))
                    .or(have(simpleName("LockedObjectsImpl"))))
            .as("LockedObjects should be used only view interface");

    @ArchTest
    public static final ArchRule views_should_have_security_annotation = classes()
            .that().resideInAPackage("org.vaadin.tatu.vaadincreate").and()
            .haveSimpleNameEndingWith("View").and().implement(View.class)
            .should().beAnnotatedWith(AllPermitted.class).orShould()
            .beAnnotatedWith(RolesPermitted.class)
            .as("Views should have access control annotation");

    @ArchTest
    public static final ArchRule I18ns_fields_should_be_constants = fields()
            .that().areDeclaredIn(I18n.class).or()
            .areDeclaredInClassesThat(belongTo(simpleName("I18n"))).should()
            .beStatic().andShould().beFinal();

    @ArchTest
    public static final ArchRule VaadinCreateThemes_fields_should_be_constants = fields()
            .that().areDeclaredIn(VaadinCreateTheme.class).should().beStatic()
            .andShould().beFinal();

    @ArchTest
    public static final ArchRule utility_methods_should_be_static = methods()
            .that()
            .areDeclaredInClassesThat(
                    resideInAPackage("org.vaadin.tatu.vaadincreate.util"))
            .should().beStatic();

    @ArchTest
    public static final ArchRule views_should_implement_View_or_Tabview = classes()
            .that().areNotInterfaces().and()
            .resideInAPackage("org.vaadin.tatu.vaadincreate").and()
            .haveSimpleNameEndingWith("View").should()
            .implement(simpleName("View").or(simpleName("TabView")));

    @ArchTest
    public static final ArchRule views_should_implement_HasI18n = classes()
            .that().areNotInterfaces().and()
            .resideInAPackage("org.vaadin.tatu.vaadincreate").and()
            .haveSimpleNameEndingWith("View").should().implement(HasI18N.class);

}
