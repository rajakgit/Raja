package com.netguardians.solace.services.consumer;

import java.util.Base64;

import javax.annotation.PostConstruct;

import com.solacesystems.jcsmp.CapabilityType;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.Topic;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import com.netguardians.solace.services.config.NgSolaceProperty;

@Component
@Getter
@Setter
@Log4j2
public class SolaceSession {

    private NgSolaceProperty ngSolaceProperty;

    private JCSMPSession topicQueueSession;

    public SolaceSession(NgSolaceProperty ngSolaceProperty) {
        this.ngSolaceProperty = ngSolaceProperty;
    }

    @PostConstruct
    private void init() throws Exception {

        final JCSMPProperties properties = getJcsmpProperties();
        final JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();
        validateSession(session);

        final Queue queue = JCSMPFactory.onlyInstance().createQueue(ngSolaceProperty.getTopicQueue());

        /*
         * Provision a new queue on the appliance, ignoring if it already exists. Set
         * permissions, access type, quota (100MB), and provisioning flags.
         */
        final EndpointProperties endpointProvisionProperties = getEndpointProperties();
        session.provision(queue, endpointProvisionProperties, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);

        // Add the Topic Subscription to the Queue.
        final Topic tutorialTopic = JCSMPFactory.onlyInstance().createTopic(ngSolaceProperty.getTopic());
        session.addSubscription(queue, tutorialTopic, JCSMPSession.WAIT_FOR_CONFIRM);

        topicQueueSession = session;
    }

    private JCSMPProperties getJcsmpProperties() {
        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, ngSolaceProperty.getHost());
        properties.setProperty(JCSMPProperties.USERNAME, ngSolaceProperty.getUserName());
        properties.setProperty(JCSMPProperties.VPN_NAME, ngSolaceProperty.getVpn());
        //properties.setProperty(JCSMPProperties.PASSWORD, ngSolaceProperty.getPassword());

        String decryptedString = AES.decrypt(ngSolaceProperty.getPassword(), "netguardians");
        properties.setProperty(JCSMPProperties.PASSWORD, decryptedString);
        
        // Make sure that the session is tolerant of the subscription already existing
        // on the queue.
        properties.setProperty(JCSMPProperties.IGNORE_DUPLICATE_SUBSCRIPTION_ERROR, true);
        return properties;
    }

    private void validateSession(JCSMPSession session) {
        // Confirm the current session supports the capabilities required.
        if (session.isCapable(CapabilityType.PUB_GUARANTEED) && session.isCapable(CapabilityType.SUB_FLOW_GUARANTEED)
                && session.isCapable(CapabilityType.ENDPOINT_MANAGEMENT)
                && session.isCapable(CapabilityType.QUEUE_SUBSCRIPTIONS))
                //&& (session.isCapable(CapabilityType.BROWSER
                {
            log.debug("All required capabilities supported!");
        } else {
            log.debug("Missing required capability.");
            log.debug("Capability - PUB_GUARANTEED: " + session.isCapable(CapabilityType.PUB_GUARANTEED));
            log.debug("Capability - SUB_FLOW_GUARANTEED: " + session.isCapable(CapabilityType.SUB_FLOW_GUARANTEED));
            log.debug("Capability - ENDPOINT_MANAGEMENT: " + session.isCapable(CapabilityType.ENDPOINT_MANAGEMENT));
            log.debug("Capability - QUEUE_SUBSCRIPTIONS: " + session.isCapable(CapabilityType.QUEUE_SUBSCRIPTIONS));
        }
    }

    private EndpointProperties getEndpointProperties() {
        EndpointProperties endpointProvisionProperties = new EndpointProperties();
        endpointProvisionProperties.setPermission(ngSolaceProperty.getPermDelete());
        endpointProvisionProperties.setAccessType(ngSolaceProperty.getAccessType());
        endpointProvisionProperties.setQuota(ngSolaceProperty.getQuota());
        return endpointProvisionProperties;
    }

}
