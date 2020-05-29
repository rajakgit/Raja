package com.netguardians.solace.services.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@PropertySource(value = {"${ng.solace.properties.path}"})
public class NgSolaceProperty {

    @Value("${solace.host}")
    private String host;

    @Value("${solace.userName}")
    private String userName;

    @Value("${solace.password}")
    private String password;

    @Value("${solace.vpn}")
    private String vpn;

    @Value("${solace.topicQueue}")
    private String topicQueue;

    @Value("${solace.topic}")
    private String topic;
    
    @Value("${solace.permDelete}")
    private int permDelete;

    @Value("${solace.accessType}")
    private int accessType;
    
    @Value("${solace.quota}")
    private int quota;
}
