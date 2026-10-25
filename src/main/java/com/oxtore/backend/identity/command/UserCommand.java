package com.oxtore.backend.identity.command;

public record UserCommand(
        String email,
        String password,
        String firstName,
        String lastName,
        String phoneNumber
) {
    public UserCommand {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password is required");
        if (firstName == null || firstName.isBlank())
            throw new IllegalArgumentException("First name is required");
        // One-word names are allowed (the sign-up form only asks for a full name), so lastName may be empty.
        if (lastName == null)
            throw new IllegalArgumentException("Last name is required");
        if (phoneNumber == null || phoneNumber.isBlank())
            throw new IllegalArgumentException("Phone number is required");
    }

    @Override
    public String toString() {
        return "UserCommand[email=" + email + ", password=***, firstName=" + firstName
                + ", lastName=" + lastName + ", phoneNumber=" + phoneNumber + "]";
    }
}
