package com.kfd.healthmenu.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file);

    String downloadToLocal(String fileUrl, String extensionHint);
}
