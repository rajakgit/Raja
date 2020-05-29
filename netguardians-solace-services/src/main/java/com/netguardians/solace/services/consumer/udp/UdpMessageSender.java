package com.netguardians.solace.services.consumer.udp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netguardians.solace.services.config.NgServiceProperty;
import com.netguardians.syslogappender.sender.UdpSyslogMessageSender;

@Configuration
class UdpMessageSender {

    @Bean
    public UdpSyslogMessageSender createUpdSyslogMessageSender(NgServiceProperty ngServiceProperty) {
        final UdpSyslogMessageSender messageSender = new UdpSyslogMessageSender();
        messageSender.setDefaultAppName(ngServiceProperty.getServiceName());
        messageSender.setDefaultMessageHostname(ngServiceProperty.getServiceHost());
        messageSender.setSyslogServerHostname(ngServiceProperty.getServiceIp());
        messageSender.setSyslogServerPort(ngServiceProperty.getSyslogPort());

        return messageSender;
    }
}
