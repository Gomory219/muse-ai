package cn.edu.sxu.museai.common;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class MinioUtil {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String baseUrl;

    // 构造器注入MinioClient
    public MinioUtil(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 上传文件到MinIO
     *
     * @param file 上传的文件（MultipartFile）
     * @return 文件在MinIO中的存储路径（可用于下载/访问）
     * @throws Exception 上传异常
     */
    public String uploadFile(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        assert originalFilename != null;
        String objectName = generateObjectName(originalFilename);

        try (InputStream inputStream = file.getInputStream()) {
            uploadStream(objectName, inputStream, file.getSize(), file.getContentType());
        }
        return buildAccessUrl(objectName);
    }

    /**
     * 上传本地文件到MinIO
     *
     * @param localFilePath 本地文件路径
     * @return 文件在MinIO中的存储路径（可用于下载/访问）
     * @throws IOException 文件读取异常
     */
    public String uploadFile(String localFilePath) {
        java.nio.file.Path path = Paths.get(localFilePath);
        String fileName = path.getFileName().toString();
        String objectName = generateObjectName(fileName);

        try (InputStream inputStream = Files.newInputStream(path)) {
            long fileSize = Files.size(path);
            String contentType = Files.probeContentType(path);
            uploadStream(objectName, inputStream, fileSize, contentType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return buildAccessUrl(objectName);
    }

    /**
     * 确保Bucket存在，不存在则创建
     */
    private void ensureBucketExists() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * 生成MinIO对象名（按日期分目录 + UUID + 原始后缀）
     *
     * @param originalFilename 原始文件名
     * @return 对象名，如 2025/03/12/uuid.jpg
     */
    private String generateObjectName(String originalFilename) {
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID() + suffix;
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return datePath + "/" + fileName;
    }

    /**
     * 上传输入流到MinIO
     *
     * @param objectName    对象名
     * @param inputStream   文件流
     * @param fileSize      文件大小
     * @param contentType   文件类型
     */
    private void uploadStream(String objectName, InputStream inputStream, long fileSize, String contentType) throws Exception {
        ensureBucketExists();
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, fileSize, -1)
                        .contentType(contentType)
                        .build()
        );
    }

    /**
     * 构建文件访问URL
     *
     * @param objectName 对象名
     * @return 完整访问URL
     */
    private String buildAccessUrl(String objectName) {
        return baseUrl + "/" + bucketName + "/" + objectName;
    }
}