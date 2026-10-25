package com.oxtore.backend.identity.otp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Delivery channel, sent by the app as "email" or "whatsapp". */
public enum OtpChannel {
    EMAIL("email"),
    WHATSAPP("whatsapp");

    private final String json;

    OtpChannel(String json) {
        this.json = json;
    }

    @JsonValue
    public String json() {
        return json;
    }

    @JsonCreator
    public static OtpChannel fromJson(String raw) {
        for (OtpChannel c : values()) {
            if (c.json.equalsIgnoreCase(raw)) return c;
        }
        throw new IllegalArgumentException("method must be \"email\" or \"whatsapp\"");
    }
}
