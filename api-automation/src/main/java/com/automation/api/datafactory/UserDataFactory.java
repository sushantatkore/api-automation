package com.automation.api.datafactory;

import com.automation.api.models.User;
import com.automation.api.utils.JsonUtils;
import com.github.javafaker.Faker;

import java.util.List;
import java.util.Locale;

/**
 * Generates realistic, randomized User payloads. Tests should never construct User objects manually.
 */
public final class UserDataFactory {

    private static final Faker FAKER = new Faker(Locale.ENGLISH);

    private UserDataFactory() { }

    public static User randomUser() {
        String firstName = FAKER.name().firstName();
        String lastName = FAKER.name().lastName();
        String username = (firstName + "." + lastName + "." + FAKER.number().digits(4)).toLowerCase();
        return User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(username + "@" + FAKER.internet().domainName())
                .username(username)
                .phone(FAKER.phoneNumber().cellPhone())
                .jobTitle(FAKER.job().title())
                .department(FAKER.commerce().department())
                .status("ACTIVE")
                .build();
    }

    public static User randomUserWithDomain(String domain) {
        User base = randomUser();
        return base.toBuilder()
                .email(base.getUsername() + "@" + domain)
                .build();
    }

    public static User invalidUser() {
        return User.builder()
                .firstName("")
                .lastName("")
                .email("not-an-email")
                .build();
    }

    public static List<User> fromClasspathJson(String resourcePath) {
        return JsonUtils.readResourceAsList(resourcePath, User.class);
    }
}
