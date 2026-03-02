package cn.edu.sxu.museai.controller;

import cn.edu.sxu.museai.common.BaseResponse;
import cn.edu.sxu.museai.common.MinioUtil;
import cn.edu.sxu.museai.common.ResultUtils;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
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
     *
     * @param file 上传的文件
     * @return 上传结果（成功/失败+文件路径）
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.PARAMS_ERROR,"上传文件不能为空");

        try {
            String filePath = minioUtil.uploadFile(file);
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"文件上传错误");
        }
    }
}