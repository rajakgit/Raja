package com.netguardians.solace.services.consumer;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.Message;

import com.solacesystems.jcsmp.Browser;
import com.solacesystems.jcsmp.BrowserProperties;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.Consumer;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.impl.BytesMessageImpl;
import com.solacesystems.jms.message.SolBytesMessage;
import com.solacesystems.jms.SolJmsUtility;
import com.welab.LoginEvent;

import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.springframework.stereotype.Component;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.netguardians.solace.services.config.NgSolaceProperty;
import com.netguardians.solace.services.consumer.udp.UdpSyslogClient;

@Component
@Log4j2
public class SolaceReceiver {

    private NgSolaceProperty ngSolaceProperty;
    private SolaceSession solaceSession;
    private UdpSyslogClient udpClient;

    public SolaceReceiver(NgSolaceProperty ngSolaceProperty, SolaceSession solaceSession, UdpSyslogClient udpClient) {
        this.ngSolaceProperty = ngSolaceProperty;
        this.solaceSession = solaceSession;
        this.udpClient = udpClient;
    }

    @PostConstruct
    private void init() throws JCSMPException {
        Queue queue = JCSMPFactory.onlyInstance().createQueue(ngSolaceProperty.getTopicQueue());

        /*
         * Create a Flow to consume messages on the Queue.
         */
        final ConsumerFlowProperties flowProp = new ConsumerFlowProperties();
        flowProp.setEndpoint(queue);
        final Consumer consumer = solaceSession.getTopicQueueSession().createFlow(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                if (msg instanceof BytesMessage) {
                    log.debug("BytesMessage received: " + new String(((BytesMessage) msg).getData()));                    
                    try 
                    {
                    Parser parser = new Parser();
                    Schema mSchema = parser.parse(this.getClass().getResourceAsStream("/LoginEvent.avsc"));
                    log.debug(" mSchema value " + mSchema);
                    byte[] data = new byte[((BytesMessage) msg).getAttachmentContentLength()];
                    msg.readBytes(data);
                    log.debug(" Bytes in array " + msg.getBytes());
                    log.debug(" data in array " + data);

                    final LoginEvent loginEvent = LoginEvent
                            .fromByteBuffer(ByteBuffer.wrap(((BytesMessageImpl) msg).getData()));
                    log.debug(" Bytes in array " + loginEvent.toString());
                    udpClient.sendData(loginEvent.toString());
                	BrowserProperties br_prop = new BrowserProperties();
        			br_prop.setEndpoint(queue);
        			br_prop.setTransportWindowSize(1);
        			br_prop.setWaitTimeout(1000);
        			Browser myBrowser = solaceSession.getTopicQueueSession().createBrowser(br_prop);
        			BytesXMLMessage rx_msg = msg;
        			int rx_msg_count = 0;
        			do {
        				rx_msg = myBrowser.getNext();
        				if (rx_msg != null) {
        					rx_msg_count++;
        					log.debug("Browser got message... dumping:");
        					log.debug(rx_msg.dump());
        					log.debug("Removing message from queue...");
        					myBrowser.remove(rx_msg);
        					log.debug("OK");
        				}
        			} while (rx_msg != null);
        			myBrowser.close();
                    
                    log.debug("Finished browsing.");
                    
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                      }
                    catch (JCSMPException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    	
                } else {
                    log.debug("Message received.");
                }
                log.debug("Message Dump: " + msg.dump());

            }

            @Override
            public void onException(JCSMPException e) {
                log.error("Consumer received exception: " + e.getMessage());
            }

        }, flowProp);
        consumer.start();
    }
    
    public static String avroToJson(byte[] avro, Schema mschema) throws IOException {
        boolean pretty = false;
        GenericDatumReader<GenericRecord> reader = null;
        JsonEncoder encoder = null;
        ByteArrayOutputStream output = null;
        try {
            //Schema schema = new Schema.Parser().parse(schemaStr);
            reader = new GenericDatumReader<GenericRecord>(mschema);
            InputStream input = new ByteArrayInputStream(avro);
            output = new ByteArrayOutputStream();
            DatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>(mschema);
            encoder = EncoderFactory.get().jsonEncoder(mschema, output, pretty);
            Decoder decoder = DecoderFactory.get().binaryDecoder(input, null);
            GenericRecord datum;
            while (true) {
                try {
                    datum = reader.read(null, decoder);
                    log.debug(" datnum " + datum);
                } catch (EOFException eofe) {
                    break;
                }
                writer.write(datum, encoder);
            }
            encoder.flush();
            output.flush();
            log.debug(" output " + output);
            return new String(output.toByteArray());
        } finally {
            try { if (output != null) output.close(); } catch (Exception e) { }
        }
    }
/*    public String avroToJson(Schema schema, byte[] avroBinary) throws IOException {
        // byte to datum
        DatumReader<Object> datumReader = new GenericDatumReader<>(schema);
        Decoder decoder = DecoderFactory.get().binaryDecoder(avroBinary, null);
        Object avroDatum = datumReader.read(null, decoder);

        // datum to json
        String json = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
          DatumWriter<Object> writer = new GenericDatumWriter<>(schema);
          JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, baos, false);
          writer.write(avroDatum, encoder);
          encoder.flush();
          baos.flush();
          return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
      }*/
   /* public String avroToJson(Schema schema, byte[] avroBinary) throws IOException {
        // byte to datum
        DatumReader<Object> datumReader = new GenericDatumReader<>(schema);
        Decoder decoder = DecoderFactory.get().binaryDecoder(avroBinary, null);
        Object avroDatum = datumReader.read(null, decoder);

        // datum to json
        String json = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
          DatumWriter<Object> writer = new GenericDatumWriter<>(schema);
          JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, baos, false);
          writer.write(avroDatum, encoder);
          encoder.flush();
          baos.flush();
          return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
      }*/

}
