package com.kfd.healthmenu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kfd.healthmenu.entity.CustomerMenu;
import com.kfd.healthmenu.entity.CustomerMenuMeal;
import com.kfd.healthmenu.entity.CustomerMenuMealItem;
import com.kfd.healthmenu.mapper.CustomerMenuMealItemMapper;
import com.kfd.healthmenu.mapper.CustomerMenuMealMapper;
import com.kfd.healthmenu.service.ExportService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final CustomerMenuMealMapper mealMapper;
    private final CustomerMenuMealItemMapper mealItemMapper;

    @Override
    public String exportMenuExcel(CustomerMenu menu, HttpServletResponse response) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("餐单");
            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.createCell(0).setCellValue(menu.getTitle());

            Row dateRow = sheet.createRow(rowIndex++);
            dateRow.createCell(0).setCellValue("日期");
            dateRow.createCell(1).setCellValue(String.valueOf(menu.getMenuDate()));

            List<CustomerMenuMeal> meals = mealMapper.selectList(new LambdaQueryWrapper<CustomerMenuMeal>()
                    .eq(CustomerMenuMeal::getCustomerMenuId, menu.getId())
                    .eq(CustomerMenuMeal::getDeleted, 0)
                    .orderByAsc(CustomerMenuMeal::getSortOrder));

            for (CustomerMenuMeal meal : meals) {
                Row mealRow = sheet.createRow(rowIndex++);
                mealRow.createCell(0).setCellValue(meal.getMealName());
                mealRow.createCell(1).setCellValue(meal.getMealTime());
                List<CustomerMenuMealItem> items = mealItemMapper.selectList(new LambdaQueryWrapper<CustomerMenuMealItem>()
                        .eq(CustomerMenuMealItem::getCustomerMenuMealId, meal.getId())
                        .eq(CustomerMenuMealItem::getDeleted, 0)
                        .orderByAsc(CustomerMenuMealItem::getSortOrder));
                for (CustomerMenuMealItem item : items) {
                    Row itemRow = sheet.createRow(rowIndex++);
                    itemRow.createCell(1).setCellValue(item.getItemName());
                    itemRow.createCell(2).setCellValue(item.getItemValue());
                    itemRow.createCell(3).setCellValue(item.getImagePath());
                }
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String rawFileName = (StringUtils.hasText(menu.getTitle()) ? menu.getTitle() : "餐单") + ".xlsx";
            String encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            outputStream.flush();
            return rawFileName;
        } catch (Exception ex) {
            throw new IllegalStateException("导出 Excel 失败", ex);
        }
    }
}
