package io.github.coldworld22.eservices.user;

public enum Role {
    CITIZEN,
    EMPLOYEE,
    SUPERVISOR,
    ADMIN;

    public boolean isStaff() {
        return this != CITIZEN;
    }
}
