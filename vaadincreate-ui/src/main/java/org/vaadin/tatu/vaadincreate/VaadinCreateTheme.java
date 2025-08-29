package org.vaadin.tatu.vaadincreate;

import org.jspecify.annotations.NullMarked;

/**
 * The VaadinCreateTheme class defines the CSS class names used in the Vaadin
 * Create application's theme. These class names are used to style various
 * components and elements in the application's user interface. The class is
 * final and has a private constructor to prevent instantiation.
 *
 * See: vaadincreate.scss for the implementations.
 */
@NullMarked
public final class VaadinCreateTheme {

    private VaadinCreateTheme() {
        // private constructor to hide the implicit public one
    }

    public static final String WHITESPACE_PRE = "whitespace-pre";

    public static final String LOGINVIEW_INFORMATION = "loginview-information";
    public static final String LOGINVIEW_FORM = "loginview-form";
    public static final String LOGINVIEW_CENTER = "loginview-center";
    public static final String LOGINVIEW_FORGOTBUTTON = "loginview-forgotbutton";
    public static final String LOGINVIEW = "loginview";

    public static final String ABOUTVIEW_ADMINSCONTENT = "aboutview-adminscontent";
    public static final String ABOUT_VIEW = "aboutview";
    public static final String ABOUTVIEW_ABOUTCONTENT = "aboutview-aboutcontent";
    public static final String ABOUTVIEW_ABOUTLABEL = "aboutview-aboutlabel";

    public static final String ADMINVIEW = "adminview";
    public static final String ADMINVIEW_CATEGORY_GRID = "adminview-categorygrid";
    public static final String ADMINVIEW_USERFORM = "adminview-userform";
    public static final String ADMINVIEW_USERFORM_CHANGES = "adminview-userform-changes";
    public static final String ADMINVIEW_USERVIEW = "adminview-userview";

    public static final String BOOKVIEW_GRID = "bookview-grid";
    public static final String BOOKVIEW_TOOLBAR = "bookview-toolbar";
    public static final String BOOKVIEW_FILTER = "bookview-filter";
    public static final String BOOKVIEW = "bookview";

    public static final String FAKEGRID_HEADER = "fakegrid-header";
    public static final String FAKEGRID_SPINNERWRAPPER = "fakegrid-spinnerwrapper";
    public static final String FAKEGRID = "fakegrid";

    public static final String BOOKVIEW_GRID_ALIGNRIGHT = "bookview-grid-alignright";
    public static final String BOOKVIEW_GRID_EDITED = "bookview-grid-edited";
    public static final String BOOKVIEW_GRID_LOCKED = "bookview-grid-locked";
    public static final String BOOKVIEW_GRID_DESCRIPTIONCAPTION = "bookview-grid-descriptioncaption";
    public static final String BOOKVIEW_AVAILABILITYLABEL = "bookview-availabilitylabel";
    public static final String BOOKVIEW_GRIDWRAPPER = "bookview-gridwrapper";
    public static final String BOOKVIEW_NOMATCHES = "bookview-nomatches";

    public static final String BOOKFORM_FORM = "bookform-form";
    public static final String BOOKFORM_WRAPPER = "bookform-wrapper";
    public static final String BOOKFORM = "bookform";
    public static final String BOOKFORM_WRAPPER_VISIBLE = "bookform-wrapper-visible";
    public static final String BOOKFORM_FIELD_DIRTY = "bookform-field-dirty";

    public static final String STATSVIEW = "statsview";
    public static final String DASHBOARD = "dashboard";
    public static final String DASHBOARD_CHART_WIDE = "dashboard-chart-wide";
    public static final String DASHBOARD_CHART = "dashboard-chart";
    public static final String DASHBOARD_CHART_FOCUSRING = "dashboard-chart-focusring";

    public static final String COLOR_AVAILABLE = "var(--color-available, green)";
    public static final String COLOR_COMING = "var(--color-coming, orange)";
    public static final String COLOR_DISCONTINUED = "var(--color-discontinued, red)";

    public static final String GRID_NO_STRIPES = "no-stripes";
    public static final String GRID_NO_BORDERS = "no-borders";
    public static final String GRID_NO_CELL_FOCUS = "no-cell-focus";
    public static final String GRID_ROW_FOCUS = "row-focus";

    public static final String CHECKBOXGROUP_SCROLL = "scrollable";

    public static final String BUTTON_CANCEL = "cancel";

}
