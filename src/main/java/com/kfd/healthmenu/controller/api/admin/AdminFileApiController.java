package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.dto.api.ApiResponse;
import com.kfd.healthmenu.dto.api.FileUploadResponse;
import com.kfd.healthmenu.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
public class AdminFileApiController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ApiResponse<FileUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("FILE_EMPTY", "请先选择图片");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new BizException("FILE_TYPE_INVALID", "仅支持上传图片文件");
        }
        return ApiResponse.success("图片上传成功", new FileUploadResponse(fileStorageService.store(file)));
    }
}
