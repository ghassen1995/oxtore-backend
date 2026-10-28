package com.oxtore.backend.order.DTO;

/** Store action on an order: status = confirmed | shipped | delivered | rejected; reason only for rejected. */
public record UpdateOrderStatusRequest(String status, String reason) {}
