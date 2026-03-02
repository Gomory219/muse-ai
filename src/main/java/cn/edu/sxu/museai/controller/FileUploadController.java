package cn.edu.sxu.museai.controller;

import cn.edu.sxu.museai.common.MinioUtil;
import cn.edu.sxu.museai.common.ResultUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
public class FileUploadController {

    private final MinioUtil minioUtil;

    // 构造器注入MinioUtil
    public FileUploadController(MinioUtil minioUtil) {
        this.minioUtil = minioUtil;
    }

    /**
     * 文件上传接口
     * @param file 上传的文件
     * @return 上传结果（成功/失败+文件路径）
     */
    @PostMapping("/upload")
        public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "上传文件不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            // 2. 调用工具类上传文件到MinIO
            String filePath = minioUtil.uploadFile(file);

            // 3. 返回成功结果
            result.put("code", 200);
            result.put("msg", "文件上传成功");
            result.put("data", filePath); // 返回MinIO中的文件路径
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // 4. 异常处理
            result.put("code", 500);
            result.put("msg", "文件上传失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}