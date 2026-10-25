package com.oxtore.backend.identity.otp.delivery;

/** The provider rejected or could not receive the message. The message never contains the code. */
public class OtpDeliveryException extends RuntimeException {
    public OtpDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public OtpDeliveryException(String message) {
        super(message);
    }
}
