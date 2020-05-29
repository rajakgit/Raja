package com.netguardians.solace.services.consumer;


import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessageProducer;
import com.welab.avro.User;

import java.io.IOException;

public class Producer {

  private static JCSMPProperties properties;

  static {
    properties = new JCSMPProperties();
    String host = "tcp://mr1qvxdm3zqyo3.messaging.solace.cloud:21088";
    String vpnName = "msgvpn-1u6o37qngorj";
    String username = "solace-cloud-client";
    String password = "ph2nt6i1c66pab6ukojobqfchb";
    properties.setProperty(JCSMPProperties.HOST, host);
    properties.setProperty(JCSMPProperties.USERNAME, username);
    properties.setProperty(JCSMPProperties.VPN_NAME, vpnName);
    properties.setProperty(JCSMPProperties.PASSWORD, password);
  }

  public static void main(String[] args) throws JCSMPException, IOException {

    final JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties);

    session.connect();

    String queueName = "Topic/CIAMQueue";
    System.out.printf("Attempting to provision the queue '%s' on the appliance.%n", queueName);
    final EndpointProperties endpointProps = new EndpointProperties();
    // set queue permissions to "consume" and access-type to "exclusive"
    endpointProps.setPermission(EndpointProperties.PERMISSION_DELETE);
    endpointProps.setAccessType(EndpointProperties.ACCESSTYPE_EXCLUSIVE);
    // create the queue object locally
    final Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
    // Actually provision it, and do not fail if it already exists
    session.provision(queue, endpointProps, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);

    final XMLMessageProducer prod = session.getMessageProducer(
        new JCSMPStreamingPublishEventHandler() {
          @Override
          public void responseReceived(String messageId) {
            System.out.printf("Producer received response for msg ID #%s%n", messageId);
          }

          @Override
          public void handleError(String messageId, JCSMPException e, long timestamp) {
            System.out.printf("Producer received error for msg ID %s @ %s - %s%n",
                messageId, timestamp, e);
          }
        });

    TextMessage msg = (TextMessage) JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
    msg.setDeliveryMode(DeliveryMode.PERSISTENT);
    final byte[] tests = User.newBuilder().setName("test")
        .setFavoriteColor("red")
        .setFavoriteNumber(1)
        .build().toByteBuffer().array();
    msg.setUserData(tests);

    // Send message directly to the queue
    prod.send(msg, queue);
    // Delivery not yet confirmed. See ConfirmedPublish.java
    System.out.println("Message sent. Exiting.");

    // Close session
    session.closeSession();
  }

}


