package org.icij.datashare.com.mail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;


import com.sun.mail.smtp.SMTPMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import static java.util.Optional.ofNullable;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;


public class MailSender {
    protected final Log logger = LogFactory.getLog(this.getClass());
    
    public final int port;
    public final String host;
    final String user;
    final String password;

    public MailSender(String host, int port) {
        this(host, port, null, null);
    }

    public MailSender(URI uri) {
        this(
            uri.getHost(),
            uri.getPort(),
            uri.getUserInfo() != null ? uri.getUserInfo().split(":")[0]: null,
            uri.getUserInfo() != null ? uri.getUserInfo().split(":")[1]: null
        );
    }

    public MailSender(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public void send(Mail donneesEmail) throws MailException {
        try {
            Message message = createMessage(donneesEmail);
            Transport.send(message);
        } catch (NullPointerException e) {
            logger.error("If error is about MailcapFile, delete .mailcap in the user's home");
            throw new MailException(e);
        } catch (Throwable t) {
            logger.fatal("Failed to send mail : hostmail=" + host + ", port=" + port + ", Exception=" + t.getMessage());
            throw new MailException(t);
        }
    }

    private Message createMessage(Mail donneesEmail) throws MessagingException, AddressException {
        Message message = new SMTPMessage(getMailSession(host, port, user, password));
        logger.info("MimeMessage: host = " + host + " - port = " + port);

        message.setHeader("X-Mailer", "msgsend");
        message.setSentDate(new java.util.Date());

        message.setFrom(new InternetAddress(donneesEmail.from));
        if (donneesEmail.toRecipientList != null) {
            for (String to : donneesEmail.toRecipientList) {
                message.addRecipient(TO, new InternetAddress(to));
            }
        }
        if (donneesEmail.ccRecipientList != null) {
            for (String cc : donneesEmail.ccRecipientList) {
                message.addRecipient(CC, new InternetAddress(cc));
            }
        }
        message.setSubject(donneesEmail.subject);
        message.setText(donneesEmail.messageBody);
        return message;
    }

    private synchronized static Session getMailSession(String hostTransportMail, int portTransportMail, String user, String password) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.class", "com.sun.mail.smtp.SMTPTransport");
        properties.setProperty("mail.smtp.port", String.valueOf(portTransportMail));
        properties.setProperty("mail.smtp.host", hostTransportMail);
        return user == null ? Session.getDefaultInstance(properties): Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }
}
