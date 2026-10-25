package com.oxtore.backend.staff.DTOs;

public record AddEmployeeRequest(String userPublicId, String fullName, String email, String phone, String role) {}