package com.netguardians.solace.services.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@PropertySource(value = "${ng.service.properties.path}")
public class NgServiceProperty {

    @Value("${ng.service.name}")
    private String serviceName;

    @Value("${ng.service.port}")
    private int servicePort;

    @Value("${ng.service.host}")
    private String serviceHost;

    @Value("${ng.service.ip}")
    private String serviceIp;

    @Value("${ng.service.incomingFields}")
    private String incomingFields;

    @Value("${ng.service.bdmFields}")
    private String bdmFields;

    @Value("${ng.service.timestamp.basis}")
    private String timestampBasis;

    @Value("${ng.service.udp.timestamp.format}")
    private String udpTimestampFormat;

    @Value("${ng.service.input.timestamp.format}")
    private String inputTimestampFormat;

    @Value("${ng.service.syslog.port}")
    private int syslogPort;

    @Value("${ng.service.syslog.cache.path}")
    private String cachePath;

}
