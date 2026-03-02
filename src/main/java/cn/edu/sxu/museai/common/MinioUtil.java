package cn.edu.sxu.museai.common;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Component
public class MinioUtil {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    // 构造器注入MinioClient
    public MinioUtil(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 上传文件到MinIO
     * @param file 上传的文件（MultipartFile）
     * @return 文件在MinIO中的存储路径（可用于下载/访问）
     * @throws Exception 上传异常
     */
    public String uploadFile(MultipartFile file) throws Exception {
        // 1. 检查Bucket是否存在，不存在则创建（可选，推荐提前创建）
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // 2. 生成唯一文件名（避免重复，格式：UUID+原文件名后缀）
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID() + suffix;

        // 3. 构建文件存储路径（可选：按日期分目录，如2024/03/02/xxx.jpg）
        // String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // String objectName = datePath + "/" + fileName;
        String objectName = fileName; // 简单存储，直接用唯一文件名

        // 4. 上传文件到MinIO
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName) // MinIO中的文件路径/名称
                            .stream(inputStream, file.getSize(), -1) // -1表示自动识别文件大小
                            .contentType(file.getContentType()) // 文件类型（如image/jpeg）
                            .build()
            );
        }

        // 5. 返回文件访问路径（MinIO默认访问地址：endpoint/bucketName/objectName）
        return bucketName + "/" + objectName;
    }
}