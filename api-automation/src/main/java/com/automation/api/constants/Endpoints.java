package com.automation.api.constants;

/**
 * Central registry of API endpoint paths. All path fragments live here —
 * services reference them.
 */
public final class Endpoints {

    private Endpoints() {
    }

    public static final class Auth {
        public static final String LOGIN = "/auth/login";
        public static final String REFRESH = "/auth/refresh";

        private Auth() {
        }
    }

    public static final class Users {
        public static final String BASE = "/users";
        public static final String LOGIN = "user-api/auth/login"; // adjust to your API
        public static final String BY_ID = "/users/{id}";

        public static String byId(Object id) {
            return BASE + "/" + id;
        }

        private Users() {
        }
    }
}
