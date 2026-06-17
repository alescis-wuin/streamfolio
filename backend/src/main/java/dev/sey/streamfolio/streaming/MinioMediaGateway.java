package dev.sey.streamfolio.streaming;

import dev.sey.streamfolio.common.BadRequestException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.UploadObjectArgs;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "streamfolio.minio", name = "enabled", havingValue = "true")
public class MinioMediaGateway {
    private final MinioClient client;
    private final String bucket;
    private final boolean autoCreateBucket;

    public MinioMediaGateway(@Value("${streamfolio.minio.endpoint:http://localhost:9000}") String endpoint,
                             @Value("${streamfolio.minio.access-key:streamfolio}") String accessKey,
                             @Value("${streamfolio.minio.secret-key:streamfolio-minio-secret}") String secretKey,
                             @Value("${streamfolio.minio.bucket:streamfolio-media}") String bucket,
                             @Value("${streamfolio.minio.auto-create-bucket:true}") boolean autoCreateBucket) {
        this.client = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
        this.bucket = bucket;
        this.autoCreateBucket = autoCreateBucket;
        ensureBucket();
    }

    public Resource resource(String objectName) {
        try {
            InputStream input = client.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build());
            return new InputStreamResource(input);
        } catch (Exception exception) {
            throw new BadRequestException("Objet MinIO indisponible: " + objectName + ".");
        }
    }

    public boolean exists(String objectName) {
        try {
            client.statObject(StatObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public void upload(Path source, String objectName, String contentType) {
        if (!Files.isRegularFile(source)) {
            return;
        }
        try {
            client.uploadObject(UploadObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .filename(source.toString())
                .contentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType)
                .build());
        } catch (Exception exception) {
            throw new BadRequestException("Upload MinIO impossible: " + objectName + ".");
        }
    }

    public void uploadTree(Path directory, String objectPrefix) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                String relative = directory.relativize(file).toString().replace('\\', '/');
                upload(file, objectPrefix + "/" + relative, contentType(file));
            }
        } catch (Exception exception) {
            throw new BadRequestException("Upload MinIO du dossier impossible: " + objectPrefix + ".");
        }
    }

    public String objectName(String directory, String filename) {
        return directory + "/" + filename;
    }

    public String videoObjectPrefix(Long videoId, String directory) {
        return directory + "/" + videoId;
    }

    private void ensureBucket() {
        if (!autoCreateBucket) {
            return;
        }
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception exception) {
            throw new BadRequestException("Bucket MinIO indisponible: " + bucket + ".");
        }
    }

    private String contentType(Path file) {
        try {
            String detected = Files.probeContentType(file);
            return detected == null || detected.isBlank() ? "application/octet-stream" : detected;
        } catch (Exception exception) {
            return "application/octet-stream";
        }
    }
}
