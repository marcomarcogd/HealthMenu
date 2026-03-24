package com.kfd.healthmenu.service.impl;

import com.kfd.healthmenu.dto.AiImportResultDto;
import com.kfd.healthmenu.dto.CozeWorkflowResponse;
import com.kfd.healthmenu.service.AiImportService;
import com.kfd.healthmenu.service.CozeWorkflowService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AiImportServiceImplTest {

    @Autowired
    private AiImportService aiImportService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @MockBean
    private CozeWorkflowService cozeWorkflowService;

    @Test
    void parseMenuText_shouldNotInjectSampleMealsWhenFallbacking() {
        CozeWorkflowResponse response = new CozeWorkflowResponse();
        response.setSuccess(false);
        response.setRawResponse("mock fallback");
        when(cozeWorkflowService.execute(any())).thenReturn(response);

        AiImportResultDto result = aiImportService.parseMenuText("这周注意清淡饮食");

        assertThat(result.getParseMode()).isIn("HEURISTIC", "FALLBACK");
        assertThat(result.getMeals()).isEmpty();
        assertThat(result.getParseMessage()).doesNotContain("默认结构");
    }

    @Test
    void parseMenuText_shouldKeepHeuristicMealsWhenTextContainsMealLines() {
        CozeWorkflowResponse response = new CozeWorkflowResponse();
        response.setSuccess(false);
        response.setRawResponse("mock heuristic");
        when(cozeWorkflowService.execute(any())).thenReturn(response);

        AiImportResultDto result = aiImportService.parseMenuText("""
                标题：减脂计划
                第3周
                早餐：燕麦 40g；蛋白：鸡蛋 2 个
                晚餐：玉米 1 根
                """);

        assertThat(result.getParseMode()).isEqualTo("HEURISTIC");
        assertThat(result.getMeals()).hasSize(2);
        assertThat(result.getMeals().get(0).getMealCode()).isEqualTo("breakfast");
        assertThat(result.getMeals().get(0).getItems()).isNotEmpty();
    }

    @Test
    void parseMenuText_shouldExposeCozeFailureReasonInFallbackMessage() {
        CozeWorkflowResponse response = new CozeWorkflowResponse();
        response.setSuccess(false);
        response.setErrorMessage("Access token is invalid");
        when(cozeWorkflowService.execute(any())).thenReturn(response);

        AiImportResultDto result = aiImportService.parseMenuText("这周注意清淡饮食");

        assertThat(result.getParseMode()).isIn("HEURISTIC", "FALLBACK");
        assertThat(result.getParseMessage()).contains("Access token is invalid");
    }

    @Test
    void generateImage_shouldDownloadRemoteImageToLocal() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] jpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
        server.createContext("/generated.jpeg", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, jpegBytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(jpegBytes);
            }
        });
        server.start();

        String imageUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/generated.jpeg";
        CozeWorkflowResponse response = new CozeWorkflowResponse();
        response.setSuccess(true);
        response.setImageUrl(imageUrl);
        when(cozeWorkflowService.execute(any())).thenReturn(response);

        try {
            String storedPath = aiImportService.generateImage("鸡胸肉减脂餐");

            assertThat(storedPath).startsWith("/uploads/");
            assertThat(storedPath).endsWith(".jpeg");
            Path savedFile = Path.of(uploadDir).resolve(storedPath.replaceFirst("^/uploads/", ""));
            assertThat(Files.exists(savedFile)).isTrue();
            assertThat(Files.size(savedFile)).isEqualTo(jpegBytes.length);
            Files.deleteIfExists(savedFile);
        } finally {
            server.stop(0);
        }
    }
}
