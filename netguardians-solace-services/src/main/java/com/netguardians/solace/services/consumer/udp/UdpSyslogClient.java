package com.netguardians.solace.services.consumer.udp;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.solacesystems.jcsmp.BytesMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import com.netguardians.solace.services.config.NgServiceProperty;
import com.netguardians.syslogappender.Facility;
import com.netguardians.syslogappender.MessageFormat;
import com.netguardians.syslogappender.Severity;
import com.netguardians.syslogappender.SyslogMessage;
import com.netguardians.syslogappender.sender.UdpSyslogMessageSender;

@Component
@Log4j2
public class UdpSyslogClient {

    private Gson gson = new Gson();
    private UdpSyslogMessageSender messageSender;
    private NgServiceProperty ngServiceProperty;
    private DateFormat dateFormatParser;

    public UdpSyslogClient(UdpSyslogMessageSender udpSyslogMessageSender, NgServiceProperty ngServiceProperty) {
        this.messageSender = udpSyslogMessageSender;
        this.ngServiceProperty = ngServiceProperty;
        this.dateFormatParser = new SimpleDateFormat(ngServiceProperty.getInputTimestampFormat());
    }

    public void sendData(String bytesMessage) {
        JsonObject convertedObject = gson.fromJson(bytesMessage, JsonObject.class);
        String message = getMessage(convertedObject);

        SyslogMessage syslogMessage = new SyslogMessage()
                .withMsg(message)
                .withAppName(ngServiceProperty.getServiceName())
                .withHostname(ngServiceProperty.getServiceHost())
                .withFacility(Facility.USER)
                .withSeverity(Severity.INFORMATIONAL);

        try {
            final Date timestamp = dateFormatParser.parse(convertedObject.get(ngServiceProperty.getTimestampBasis()).getAsString());
            syslogMessage.withTimestamp(timestamp);
        } catch (ParseException e) {
            log.warn("Parsing of input timestamp field failed. Current system timestamp will be attached in SyslogMessage");
        }

        try {
            messageSender.sendMessage(syslogMessage);
            log.debug("Sent message to Syslog: " + syslogMessage.toSyslogMessage(MessageFormat.RFC_3164));
        } catch (IOException e) {
            log.error("Message not sent to Syslog due to Syslog Appender failure", e);
        }

    }

    private String getMessage(JsonObject convertedObject) {
        final List<String> bdmFields = Arrays.asList(ngServiceProperty.getBdmFields().split(","));
        final List<String> incomingFields = Arrays.asList(ngServiceProperty.getIncomingFields().split(","));
        final StringBuilder messageBuilder = new StringBuilder();

        for (int i = 0; i < bdmFields.size(); i++) {
            if (convertedObject.get(incomingFields.get(i)) != null) {
                messageBuilder
                        .append(bdmFields.get(i))
                        .append("=")
                        .append(convertedObject.get(incomingFields.get(i)).getAsString())
                        .append("\t");
            }
        }

        return messageBuilder.toString();
    }

}
