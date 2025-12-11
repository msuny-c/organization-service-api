package ru.itmo.organization.service;

public record UserContext(String username, boolean admin) {

    public static UserContext regular(String username) {
        return new UserContext(username, false);
    }

    public static UserContext admin(String username) {
        return new UserContext(username, true);
    }
}
