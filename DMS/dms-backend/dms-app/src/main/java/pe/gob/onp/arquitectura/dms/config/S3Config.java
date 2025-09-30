package pe.gob.onp.arquitectura.dms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.s3.endpoint-url:}")
    private String endpointUrl;

    @Value("${aws.s3.use-path-style:true}")
    private boolean usePathStyle;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());

        // Para MinIO local
        if (!endpointUrl.isEmpty()) {
            builder.endpointOverride(URI.create(endpointUrl))
                    .forcePathStyle(usePathStyle);
        }

        return builder.build();
    }

}
