package com.kfd.healthmenu.service.impl;

import com.kfd.healthmenu.dto.AiImportResultDto;
import com.kfd.healthmenu.dto.CustomerMenuForm;
import com.kfd.healthmenu.dto.CustomerMenuMealForm;
import com.kfd.healthmenu.dto.CustomerMenuMealItemForm;
import com.kfd.healthmenu.dto.CustomerMenuSectionForm;
import com.kfd.healthmenu.entity.Customer;
import com.kfd.healthmenu.entity.CustomerMenu;
import com.kfd.healthmenu.entity.MenuPublishRecord;
import com.kfd.healthmenu.entity.MenuTemplate;
import com.kfd.healthmenu.mapper.CustomerMapper;
import com.kfd.healthmenu.mapper.CustomerMenuMealMapper;
import com.kfd.healthmenu.mapper.CustomerMenuSectionContentMapper;
import com.kfd.healthmenu.mapper.MenuPublishRecordMapper;
import com.kfd.healthmenu.mapper.MenuTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerMenuServiceImplTest {

    @Autowired
    private CustomerMenuServiceImpl customerMenuService;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private MenuTemplateMapper menuTemplateMapper;

    @Autowired
    private MenuPublishRecordMapper menuPublishRecordMapper;

    @Autowired
    private CustomerMenuMealMapper customerMenuMealMapper;

    @Autowired
    private CustomerMenuSectionContentMapper customerMenuSectionContentMapper;

    @BeforeEach
    void setUp() {
        if (customerMapper.selectById(2001L) == null) {
            Customer customer = new Customer();
            customer.setId(2001L);
            customer.setName("张三");
            customer.setExclusiveTitle("张女士的营养调理餐单");
            customer.setStatus(1);
            customerMapper.insert(customer);
        }

        if (menuTemplateMapper.selectById(1001L) == null) {
            MenuTemplate template = new MenuTemplate();
            template.setId(1001L);
            template.setName("标准模板");
            template.setThemeCode("standard");
            template.setStatus(1);
            template.setIsDefault(1);
            menuTemplateMapper.insert(template);
        }
    }

    @Test
    void saveMenu_shouldPersistSnapshotAndReadBack() {
        CustomerMenuForm form = new CustomerMenuForm();
        form.setCustomerId(2001L);
        form.setTemplateId(1001L);
        form.setMenuDate(LocalDate.of(2026, 3, 22));
        form.setWeekIndex(3);
        form.setTitle("张女士第三周餐单");
        form.setShowWeeklyTip(true);
        form.setShowSwapGuide(false);
        form.setStatus("PUBLISHED");

        CustomerMenuSectionForm section = new CustomerMenuSectionForm();
        section.setSectionType("WEEKLY_TIP");
        section.setTitle("每周提示");
        section.setContent("多喝水，注意睡眠");
        section.setSortOrder(1);
        section.setBold(true);
        section.setColor("#123456");
        form.setSections(List.of(section));

        CustomerMenuMealItemForm item = new CustomerMenuMealItemForm();
        item.setItemCode("protein");
        item.setItemName("蛋白");
        item.setItemValue("鸡胸肉 120g");
        item.setSortOrder(1);
        item.setBold(false);
        item.setColor("#654321");

        CustomerMenuMealForm meal = new CustomerMenuMealForm();
        meal.setMealCode("lunch");
        meal.setMealName("午餐");
        meal.setTimeLabel("午餐时间");
        meal.setMealTime("12:10");
        meal.setSortOrder(1);
        meal.setItems(List.of(item));
        form.setMeals(List.of(meal));

        Long id = customerMenuService.saveMenu(form);

        CustomerMenu saved = customerMenuService.getById(id);
        assertThat(saved.getThemeCode()).isEqualTo("standard");
        assertThat(saved.getShareToken()).isNotBlank();

        CustomerMenuForm reloaded = customerMenuService.getFormById(id);
        assertThat(reloaded.getTitle()).isEqualTo("张女士第三周餐单");
        assertThat(reloaded.getShowWeeklyTip()).isTrue();
        assertThat(reloaded.getShowSwapGuide()).isFalse();
        assertThat(reloaded.getPublishCount()).isEqualTo(1);
        assertThat(reloaded.getLastPublishedAt()).isNotNull();
        assertThat(reloaded.getSections()).singleElement().satisfies(savedSection -> {
            assertThat(savedSection.getContent()).isEqualTo("多喝水，注意睡眠");
            assertThat(savedSection.getBold()).isTrue();
            assertThat(savedSection.getColor()).isEqualTo("#123456");
            assertThat(savedSection.getAllowImage()).isTrue();
        });
        assertThat(reloaded.getMeals()).singleElement().satisfies(savedMeal -> {
            assertThat(savedMeal.getMealTime()).isEqualTo("12:10");
            assertThat(savedMeal.getItems()).singleElement().satisfies(savedItem -> {
                assertThat(savedItem.getItemValue()).isEqualTo("鸡胸肉 120g");
                assertThat(savedItem.getColor()).isEqualTo("#654321");
                assertThat(savedItem.getAllowImage()).isTrue();
            });
        });
        assertThat(menuPublishRecordMapper.selectList(null))
                .filteredOn(itemRecord -> id.equals(itemRecord.getCustomerMenuId()))
                .filteredOn(itemRecord -> "SHARE_LINK".equals(itemRecord.getExportType()))
                .hasSize(1);
    }

    @Test
    void saveMenu_shouldNotDuplicateShareRecordForPublishedMenu() {
        CustomerMenuForm form = new CustomerMenuForm();
        form.setCustomerId(2001L);
        form.setTemplateId(1001L);
        form.setMenuDate(LocalDate.of(2026, 3, 24));
        form.setTitle("published-menu-save-once");
        form.setStatus("PUBLISHED");

        Long id = customerMenuService.saveMenu(form);

        CustomerMenuForm updateForm = customerMenuService.getFormById(id);
        updateForm.setTitle("published-menu-save-twice");
        customerMenuService.saveMenu(updateForm);

        assertThat(menuPublishRecordMapper.selectList(null))
                .filteredOn(itemRecord -> id.equals(itemRecord.getCustomerMenuId()))
                .filteredOn(itemRecord -> "SHARE_LINK".equals(itemRecord.getExportType()))
                .hasSize(1);
    }

    @Test
    void buildCreateFormFromAi_shouldMergeTemplateAndAiPayload() {
        AiImportResultDto ai = new AiImportResultDto();
        ai.setTitle("AI 识别餐单标题");
        ai.setWeekIndex(5);
        ai.setWeeklyTip("本周请规律进餐");
        ai.setSwapGuide("主食可用玉米替换");

        CustomerMenuMealItemForm breakfastProtein = new CustomerMenuMealItemForm();
        breakfastProtein.setItemCode("protein");
        breakfastProtein.setItemName("优质蛋白");
        breakfastProtein.setItemValue("鸡蛋 2 个");

        CustomerMenuMealForm breakfast = new CustomerMenuMealForm();
        breakfast.setMealCode("breakfast");
        breakfast.setMealName("早餐");
        breakfast.setMealTime("07:30");
        breakfast.setItems(List.of(breakfastProtein));

        CustomerMenuMealItemForm extraItem = new CustomerMenuMealItemForm();
        extraItem.setItemCode("fruit");
        extraItem.setItemName("水果");
        extraItem.setItemValue("蓝莓一小碗");

        CustomerMenuMealForm extraMeal = new CustomerMenuMealForm();
        extraMeal.setMealCode("late_snack");
        extraMeal.setMealName("夜间加餐");
        extraMeal.setMealTime("21:30");
        extraMeal.setItems(List.of(extraItem));

        ai.setMeals(List.of(breakfast, extraMeal));

        CustomerMenuForm form = customerMenuService.buildCreateFormFromAi(2001L, 1001L, ai);

        assertThat(form.getTitle()).isEqualTo("AI 识别餐单标题");
        assertThat(form.getWeekIndex()).isEqualTo(5);
        assertThat(form.getSections())
                .extracting(CustomerMenuSectionForm::getSectionType)
                .contains("EXCLUSIVE_TITLE", "WEEKLY_TIP", "SWAP_GUIDE");
        assertThat(form.getSections().stream()
                .filter(section -> !"DAILY_MENU".equals(section.getSectionType()))
                .toList()).allSatisfy(section -> assertThat(section.getAllowImage()).isTrue());
        assertThat(form.getSections().stream()
                .filter(section -> "DAILY_MENU".equals(section.getSectionType()))
                .findFirst()
                .orElseThrow()
                .getAllowImage()).isFalse();
        assertThat(form.getSections().stream()
                .filter(item -> "WEEKLY_TIP".equals(item.getSectionType()))
                .findFirst()
                .orElseThrow()
                .getContent()).isEqualTo("本周请规律进餐");
        assertThat(form.getSections().stream()
                .filter(item -> "SWAP_GUIDE".equals(item.getSectionType()))
                .findFirst()
                .orElseThrow()
                .getContent()).isEqualTo("主食可用玉米替换");

        CustomerMenuMealForm mergedBreakfast = form.getMeals().stream()
                .filter(item -> "breakfast".equals(item.getMealCode()))
                .findFirst()
                .orElseThrow();
        assertThat(mergedBreakfast.getMealTime()).isEqualTo("07:30");
        assertThat(mergedBreakfast.getItems()).allSatisfy(item -> assertThat(item.getAllowImage()).isTrue());
        assertThat(mergedBreakfast.getItems().stream()
                .filter(item -> "protein".equals(item.getItemCode()))
                .findFirst()
                .orElseThrow()
                .getItemValue()).isEqualTo("鸡蛋 2 个");

        assertThat(form.getMeals().stream()
                .filter(item -> "late_snack".equals(item.getMealCode()))
                .findFirst())
                .isPresent();
    }

    @Test
    void publishMenu_shouldUpdateStatusAndCreateShareRecord() {
        CustomerMenuForm form = new CustomerMenuForm();
        form.setCustomerId(2001L);
        form.setTemplateId(1001L);
        form.setMenuDate(LocalDate.of(2026, 3, 22));
        form.setTitle("发布测试餐单");
        form.setShowWeeklyTip(true);
        form.setShowSwapGuide(true);

        Long menuId = customerMenuService.saveMenu(form);

        customerMenuService.publishMenu(menuId);

        CustomerMenu saved = customerMenuService.getById(menuId);
        assertThat(saved.getStatus()).isEqualTo("PUBLISHED");

        List<MenuPublishRecord> records = menuPublishRecordMapper.selectList(null);
        assertThat(records)
                .filteredOn(item -> menuId.equals(item.getCustomerMenuId()))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getExportType()).isEqualTo("SHARE_LINK");
                    assertThat(record.getFilePath()).contains("/share/menu/");
                });

        CustomerMenuForm detail = customerMenuService.getFormById(menuId);
        assertThat(detail.getPublishCount()).isEqualTo(1);
        assertThat(detail.getLastPublishedAt()).isNotNull();
    }

    @Test
    void deleteById_shouldDeleteMenuAndSnapshots() {
        CustomerMenuForm form = new CustomerMenuForm();
        form.setCustomerId(2001L);
        form.setTemplateId(1001L);
        form.setMenuDate(LocalDate.of(2026, 3, 22));
        form.setTitle("删除测试餐单");

        CustomerMenuSectionForm section = new CustomerMenuSectionForm();
        section.setSectionType("WEEKLY_TIP");
        section.setTitle("每周提示");
        section.setContent("测试内容");
        section.setSortOrder(1);
        form.setSections(List.of(section));

        Long menuId = customerMenuService.saveMenu(form);
        customerMenuService.publishMenu(menuId);

        customerMenuService.deleteById(menuId);

        assertThat(customerMenuService.getById(menuId)).isNull();
        assertThat(customerMenuSectionContentMapper.selectList(null))
                .filteredOn(item -> menuId.equals(item.getCustomerMenuId()))
                .isEmpty();
        assertThat(customerMenuMealMapper.selectList(null))
                .filteredOn(item -> menuId.equals(item.getCustomerMenuId()))
                .isEmpty();
        assertThat(menuPublishRecordMapper.selectList(null))
                .filteredOn(item -> menuId.equals(item.getCustomerMenuId()))
                .isEmpty();
    }
}
