package org.cbioportal.staging.services.resource.ftp;

import java.util.stream.Stream;

import com.jcraft.jsch.ChannelSftp.LsEntry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Option;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

@Configuration
@ConditionalOnProperty(value="scan.location.type", havingValue ="sftp")
@IntegrationComponentScan("org.cbioportal.staging.services.resource.ftp")
public class SftpConfig {

    @Value("${ftp.host}")
    private String sftpHost;

    @Value("${ftp.port:22}")
    private int sftpPort;

    @Value("${ftp.user}")
    private String sftpUser;

    @Value("${ftp.password}")
    private String sftpPasword;

    @Value("${sftp.privateKey}")
    private Resource sftpPrivateKey;

    @Value("${sftp.privateKeyPassphrase}")
    private String sftpPrivateKeyPassphrase;

    @Bean
    public SessionFactory<LsEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(sftpHost);
        factory.setPort(sftpPort);
        factory.setUser(sftpUser);
        if (sftpPrivateKey != null) {
            factory.setPrivateKey(sftpPrivateKey);
            factory.setPrivateKeyPassphrase(sftpPrivateKeyPassphrase);
        } else {
            factory.setPassword(sftpPasword);
        }
        factory.setAllowUnknownKeys(true);
        return new CachingSessionFactory<LsEntry>(factory);
    }

    @Bean
    @ServiceActivator(inputChannel = "resource.ls")
    public MessageHandler ftpGatewayLs() {
        return createFtpGateway(AbstractRemoteFileOutboundGateway.Command.LS);
    }

    @Bean
    @ServiceActivator(inputChannel = "resource.ls.dir")
    public MessageHandler ftpGatewayLsDir() {
        return createFtpGateway(
            AbstractRemoteFileOutboundGateway.Command.LS,
            AbstractRemoteFileOutboundGateway.Option.SUBDIRS
        );
    }

    @Bean
    @ServiceActivator(inputChannel = "resource.ls.dir.recur")
    public MessageHandler ftpGatewayLsDirRecur() {
        return createFtpGateway(
            AbstractRemoteFileOutboundGateway.Command.LS,
            AbstractRemoteFileOutboundGateway.Option.SUBDIRS,
            AbstractRemoteFileOutboundGateway.Option.RECURSIVE
        );
    }

    @Bean
    @ServiceActivator(inputChannel = "resource.get.stream")
    public MessageHandler ftpGatewayGetStream() {
        return createFtpGateway(
            AbstractRemoteFileOutboundGateway.Command.GET,
            AbstractRemoteFileOutboundGateway.Option.STREAM
        );
    }

    @Bean
    @ServiceActivator(inputChannel = "resource.put")
    public MessageHandler ftpGatewayPut() {
        SftpOutboundGateway gateway = createFtpGateway(
            AbstractRemoteFileOutboundGateway.Command.PUT
        );
        ExpressionParser spelParser = new SpelExpressionParser();
        // the remote directory will be evaluated from a the
        // path specified with the dest.dir attribute in the header
        gateway.setRemoteDirectoryExpression(spelParser.parseExpression("headers['dest.dir']"));
        gateway.setFileNameGenerator(new FileNameGenerator(){
            @Override
            public String generateFileName(Message<?> message) {
                String name = (String) message.getHeaders().get("filename");
                return name;
            }
        });
        // TODO fix WARNING o.s.i.expression.ExpressionUtils: Creating EvaluationContext with no beanFactory
        gateway.setAutoCreateDirectory(true);
        return gateway;
    }

    @Bean
    @ServiceActivator(inputChannel = "resource.rm")
    public MessageHandler ftpGatewayRm() {
        return createFtpGateway(
            AbstractRemoteFileOutboundGateway.Command.RM
        );
    }

    private SftpOutboundGateway createFtpGateway(Command command, Option ... options) {
        SftpOutboundGateway gateway = new SftpOutboundGateway(sftpSessionFactory(), command.getCommand(), "payload");
        Stream.of(options).forEach(o -> gateway.setOption(o));
        return gateway;
    }

}