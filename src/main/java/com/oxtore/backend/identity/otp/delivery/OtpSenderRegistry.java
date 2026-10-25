package com.oxtore.backend.identity.otp.delivery;

import com.oxtore.backend.identity.exceptions.OtpException;
import com.oxtore.backend.identity.otp.OtpChannel;
import com.oxtore.backend.identity.otp.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Picks the sender for a channel: the configured provider, else the dev log sender (dev profile only), else 503. */
@Slf4j
@Component
public class OtpSenderRegistry {

    private final Map<OtpChannel, OtpSender> senders = new EnumMap<>(OtpChannel.class);
    private final DevLoggingOtpSender devSender;

    public OtpSenderRegistry(List<OtpSender> senders, ObjectProvider<DevLoggingOtpSender> devSender) {
        senders.forEach(s -> this.senders.put(s.channel(), s));
        this.devSender = devSender.getIfAvailable();
        for (OtpChannel channel : OtpChannel.values()) {
            OtpSender sender = this.senders.get(channel);
            boolean configured = sender != null && sender.isConfigured();
            log.info("OTP delivery {}: {}", channel,
                    configured ? "configured" : (this.devSender != null ? "dev log sender" : "NOT configured"));
        }
    }

    /**
     * @throws OtpException OTP_CHANNEL_UNAVAILABLE when nothing can deliver on this channel
     * @throws OtpDeliveryException when the provider failed
     */
    public void deliver(OtpChannel channel, String destination, String code, Duration validity, OtpPurpose purpose) {
        OtpSender sender = senders.get(channel);
        if (sender != null && sender.isConfigured()) {
            sender.send(destination, code, validity, purpose);
            return;
        }
        if (devSender != null) {
            devSender.send(channel, destination, code, purpose);
            return;
        }
        throw OtpException.channelUnavailable(channel);
    }
}
