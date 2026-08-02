package com.opportunityboard.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.s3.enabled", havingValue = "s3")
    public S3Client s3Client(S3Properties props) {
        requireConfigured(props);
        return S3Client.builder()
                .region(Region.of(props.region().trim()))
                .credentialsProvider(creds(props))
                .build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.s3.enabled", havingValue = "s3")
    public S3Presigner s3Presigner(S3Properties props) {
        requireConfigured(props);
        return S3Presigner.builder()
                .region(Region.of(props.region().trim()))
                .credentialsProvider(creds(props))
                .build();
    }

    private static StaticCredentialsProvider creds(S3Properties props) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey().trim(), props.secretKey().trim()));
    }

    private static void requireConfigured(S3Properties props) {
        if (!props.isConfigured()) {
            throw new IllegalStateException(
                    "UPLOAD_STORAGE=s3 nhưng thiếu S3_BUCKET / S3_REGION / AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY");
        }
    }
}
