package com.kfd.healthmenu.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kfd.healthmenu.dto.CozeWorkflowRequest;
import com.kfd.healthmenu.dto.CozeWorkflowResponse;
import com.kfd.healthmenu.service.CozeWorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class CozeWorkflowServiceImpl implements CozeWorkflowService {

    @Value("${app.coze.enabled:false}")
    private boolean enabled;

    @Value("${app.coze.text-import-url:}")
    private String textImportUrl;

    @Value("${app.coze.text-import-token:}")
    private String textImportToken;

    @Value("${app.coze.image-url:}")
    private String imageUrl;

    @Value("${app.coze.image-token:}")
    private String imageToken;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public CozeWorkflowResponse execute(CozeWorkflowRequest request) {
        CozeWorkflowResponse response = new CozeWorkflowResponse();
        response.setSuccess(false);
        if (!enabled) {
            response.setRawResponse("Coze 未启用，请先配置工作流与鉴权信息");
            response.setParsedText(request.getSourceText());
            return response;
        }

        String url = resolveUrl(request.getWorkflowCode());
        String token = resolveToken(request.getWorkflowCode());
        if (!StringUtils.hasText(url) || !StringUtils.hasText(token)) {
            response.setRawResponse("缺少 Coze URL 或 Token 配置");
            return response;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildPayload(request), headers);
            ResponseEntity<String> httpResponse = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String body = httpResponse.getBody();
            response.setRawResponse(body);
            response.setSuccess(httpResponse.getStatusCode().is2xxSuccessful());
            parseResponse(body, response);
            return response;
        } catch (Exception ex) {
            log.error("调用 Coze 工作流失败", ex);
            response.setRawResponse("调用 Coze 工作流失败：" + ex.getMessage());
            return response;
        }
    }

    private String resolveUrl(String workflowCode) {
        if ("image".equalsIgnoreCase(workflowCode)) {
            return imageUrl;
        }
        return textImportUrl;
    }

    private String resolveToken(String workflowCode) {
        if ("image".equalsIgnoreCase(workflowCode)) {
            return imageToken;
        }
        return textImportToken;
    }

    private Map<String, Object> buildPayload(CozeWorkflowRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if ("image".equalsIgnoreCase(request.getWorkflowCode())) {
            payload.put("prompt", defaultObject(request.getPrompt()));
            payload.put("sceneType", defaultObject(request.getSceneType()));
            payload.put("styleHint", defaultObject(request.getStyleHint()));
            return payload;
        }
        payload.put("sourceText", StringUtils.hasText(request.getSourceText()) ? request.getSourceText() : "");
        payload.put("sourceImageUrl", defaultObject(request.getSourceImageUrl()));
        payload.put("templateHint", defaultObject(request.getTemplateHint()));
        payload.put("customerName", defaultObject(request.getCustomerName()));
        return payload;
    }

    private Object defaultObject(String value) {
        return StringUtils.hasText(value) ? value : new LinkedHashMap<>();
    }

    private void parseResponse(String body, CozeWorkflowResponse response) {
        if (!StringUtils.hasText(body) || !JSONUtil.isTypeJSON(body)) {
            response.setParsedText(body);
            return;
        }
        JSONObject jsonObject = JSONUtil.parseObj(body);
        response.setParsedText(extractMenuJson(jsonObject));
        response.setImageUrl(extractField(jsonObject, "imageUrl", "image_url", "url"));
        response.setTaskId(extractField(jsonObject, "taskId", "task_id"));
        response.setPrompt(extractField(jsonObject, "prompt"));
        if (!StringUtils.hasText(response.getParsedText()) && !StringUtils.hasText(response.getImageUrl())) {
            response.setParsedText(body);
        }
    }

    private String extractMenuJson(JSONObject jsonObject) {
        if (jsonObject.containsKey("menuJson")) {
            return normalizeJsonText(jsonObject.getStr("menuJson"));
        }
        if (jsonObject.containsKey("title") || jsonObject.containsKey("meals")) {
            return jsonObject.toString();
        }
        for (String key : new String[]{"data", "output", "result", "content"}) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject nested) {
                String nestedMenuJson = extractMenuJson(nested);
                if (StringUtils.hasText(nestedMenuJson)) {
                    return nestedMenuJson;
                }
            } else if (value instanceof String text) {
                if (JSONUtil.isTypeJSON(text)) {
                    JSONObject parsed = JSONUtil.parseObj(text);
                    String nestedMenuJson = extractMenuJson(parsed);
                    if (StringUtils.hasText(nestedMenuJson)) {
                        return nestedMenuJson;
                    }
                }
                String normalized = normalizeJsonText(text);
                if (StringUtils.hasText(normalized) && JSONUtil.isTypeJSON(normalized)) {
                    JSONObject parsed = JSONUtil.parseObj(normalized);
                    if (parsed.containsKey("title") || parsed.containsKey("meals")) {
                        return parsed.toString();
                    }
                }
            }
        }
        return null;
    }

    private String normalizeJsonText(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String normalized = text.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\"")) || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.replace("\\\"", "\"");
        return normalized;
    }

    private String extractField(JSONObject jsonObject, String... keys) {
        for (String key : keys) {
            String direct = jsonObject.getStr(key);
            if (StringUtils.hasText(direct)) {
                return direct;
            }
        }
        for (String key : new String[]{"data", "output", "result", "content"}) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject nested) {
                String nestedValue = extractField(nested, keys);
                if (StringUtils.hasText(nestedValue)) {
                    return nestedValue;
                }
            }
        }
        return null;
    }
}
