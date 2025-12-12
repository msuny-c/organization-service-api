package ru.itmo.organization.service;

public record UserContext(String username, boolean admin) {
    public static UserContext of(String username, boolean admin) {
        return new UserContext(username, admin);
    }
}
