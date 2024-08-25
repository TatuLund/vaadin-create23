package org.vaadin.tatu.vaadincreate.i18n;

/**
 * This class represents the internationalization (i18n) keys used in the
 * application. It provides static final String constants for various keys used
 * in different parts of the application. The keys are organized into nested
 * classes based on their usage. Each nested class represents a specific area or
 * feature of the application. The keys are used to retrieve localized strings
 * from resource bundles or translation files.
 */
@SuppressWarnings("serial")
public final class I18n {

    private I18n() {
        // private constructor to hide the implicit public one
    }

    public static final String AVAILABILITY = "availability";
    public static final String IN_STOCK = "in-stock";
    public static final String PRICE = "price";
    public static final String PRODUCT_NAME = "product-name";
    public static final String DELETE = "delete";
    public static final String WILL_DELETE = "will-delete";
    public static final String CATEGORIES = "categories";
    public static final String CANCEL = "cancel";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String SAVE = "save";
    public static final String SAVE_CONFLICT = "save-conflict";

    public static final class Books {
        public static final String UPDATED = "updated";
        public static final String REMOVED = "removed";
        public static final String CONFIRM = "confirm";
        public static final String NEW_PRODUCT = "new-product";
        public static final String FILTER = "filter";
        public static final String NOT_VALID_PID = "not-valid-pid";
        public static final String PRODUCT_LOCKED = "product-locked";
        public static final String PRODUCT_DELETED = "product-deleted";
        public static final String UNSAVED_CHANGES = "unsaved-changes";
        public static final String CATEGORIES_DELETED = "categories-deleted";
        public static final String INTERNAL_ERROR = "internal-error";

        private Books() {
            // private constructor to hide the implicit public one
        }
    }

    public static final class Form {
        public static final String AVAILABILITY_MISMATCH = "availability-mismatch";
        public static final String CANNOT_CONVERT = "cannot-convert";
        public static final String DISCARD = "discard";

        private Form() {
            // private constructor to hide the implicit public one
        }
    }

    public static final class Grid {
        public static final String CANNOT_CONVERT = "cannot-convert";
        public static final String EDITED_BY = "edited-by";

        private Grid() {
            // private constructor to hide the implicit public one
        }
    }

    public static final class App {
        public static final String LOGOUT = "logout";
        public static final String MENU = "menu";

        private App() {
            // private constructor to hide the implicit public one
        }
    }

    public static final class Error {
        public static final String VIEW_NOT_FOUND = "view-not-found";
        public static final String NOT_FOUND_DESC = "not-found-desc";

        private Error() {
            // private constructor to hide the implicit public one
        }
    }

    public static final class Select {
        public static final String LANGUAGE = "language";

        private Select() {
            // private constructor to hide the implicit public one
        }
    }

    public static final class Login {
        public static final String LOGIN_FAILED = "login-failed";
        public static final String LOGIN_FAILED_DESC = "login-failed-desc";
        public static final String LOGIN_INFO = "login-info";
        public static final String LOGIN_INFO_TEXT = "login-info-text";
        public static final String LOGIN_BUTTON = "login-button";
        public static final String FORGOT_PASSWORD = "forgot-password";
        public static final String HINT = "hint";
        public static final String LANGUAGE = "language";
        public static final String CAPSLOCK = "capslock";

        private Login() {
            // private constructor to hide the implicit public one
        }
    }

    public static final class Stats {
        public static final String NO_DATA = "no-data";
        public static final String AVAILABILITIES = "availabilities";
        public static final String CATEGORIES = "categories";
        public static final String PRICES = "prices";
        public static final String COUNT = "count";

        private Stats() {
            // private constructor to hide the implicit public one
        }
    }

    public static final class About {
        public static final String EDIT_NOTE = "edit-note";
        public static final String VAADIN = "vaadin";

        private About() {
            // private constructor to hide the implicit public one
        }
    }

    public static final class User {
        public static final String NOT_MATCHING = "not-matching";
        public static final String USERNAME = "username";
        public static final String PASSWD_REPEAT = "password-repeat";
        public static final String ROLE = "role";
        public static final String SEARCH = "search";
        public static final String USER_DELETED = "user-deleted";
        public static final String USER_SAVED = "user-saved";
        public static final String USER_IS_DUPLICATE = "user-is-duplicate";
        public static final String NEW_USER = "new-user";
        public static final String EDIT_USERS = "edit-users";

        private User() {
            // private constructor to hide the implicit public one
        }
    }

    public static final class Category {
        public static final String CATEGORY_DELETED = "category-deleted";
        public static final String CATEGORY_SAVED = "category-saved";
        public static final String ADD_NEW_CATEGORY = "add-new-category";
        public static final String EDIT_CATEGORIES = "edit-categories";

        private Category() {
            // private constructor to hide the implicit public one
        }
    }

}
