package com.kfd.healthmenu.service.impl;

import com.kfd.healthmenu.dto.template.TemplateDesignSaveRequest;
import com.kfd.healthmenu.dto.template.TemplateMealItemSaveRequest;
import com.kfd.healthmenu.dto.template.TemplateMealSaveRequest;
import com.kfd.healthmenu.dto.template.TemplateSectionSaveRequest;
import com.kfd.healthmenu.dto.template.MenuTemplateDesignDto;
import com.kfd.healthmenu.entity.MenuTemplate;
import com.kfd.healthmenu.entity.MenuTemplateMeal;
import com.kfd.healthmenu.entity.MenuTemplateMealItem;
import com.kfd.healthmenu.mapper.MenuTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TemplateServiceImplTest {

    @Autowired
    private TemplateServiceImpl templateService;

    @Autowired
    private MenuTemplateMapper menuTemplateMapper;

    @BeforeEach
    void setUp() {
        if (menuTemplateMapper.selectById(1001L) == null) {
            MenuTemplate template = new MenuTemplate();
            template.setId(1001L);
            template.setName("标准模板");
            template.setDescription("默认模板");
            template.setThemeCode("standard");
            template.setStatus(1);
            template.setIsDefault(1);
            menuTemplateMapper.insert(template);
        }
    }

    @Test
    void saveDesign_shouldSyncSectionsMealsAndItems() {
        TemplateDesignSaveRequest request = new TemplateDesignSaveRequest();
        request.setId(1001L);
        request.setName("标准模板-回归测试");
        request.setDescription("updated");
        request.setThemeCode("warm");

        TemplateSectionSaveRequest section1 = new TemplateSectionSaveRequest();
        section1.setSectionType("WEEKLY_TIP");
        section1.setTitle("每周重点");
        section1.setEnabled(true);
        section1.setAllowImage(false);
        section1.setStyleConfigJson("{\"layout\":\"tip\"}");

        TemplateSectionSaveRequest section2 = new TemplateSectionSaveRequest();
        section2.setSectionType("REMARK");
        section2.setTitle("额外备注");
        section2.setEnabled(true);
        section2.setAllowImage(true);
        section2.setStyleConfigJson("{\"layout\":\"remark\"}");
        request.setSections(List.of(section1, section2));

        TemplateMealItemSaveRequest breakfastItem1 = new TemplateMealItemSaveRequest();
        breakfastItem1.setItemCode("protein");
        breakfastItem1.setItemName("优质蛋白");
        breakfastItem1.setContentFormat("PLAIN_TEXT");
        breakfastItem1.setEnabled(true);
        breakfastItem1.setAllowImage(false);

        TemplateMealItemSaveRequest breakfastItem2 = new TemplateMealItemSaveRequest();
        breakfastItem2.setItemCode("drink");
        breakfastItem2.setItemName("饮品");
        breakfastItem2.setContentFormat("RICH_TEXT");
        breakfastItem2.setEnabled(true);
        breakfastItem2.setAllowImage(true);

        TemplateMealSaveRequest breakfast = new TemplateMealSaveRequest();
        breakfast.setMealCode("breakfast");
        breakfast.setMealName("活力早餐");
        breakfast.setTimeLabel("07:30");
        breakfast.setEnabled(true);
        breakfast.setItems(List.of(breakfastItem1, breakfastItem2));

        TemplateMealItemSaveRequest supperItem = new TemplateMealItemSaveRequest();
        supperItem.setItemCode("soup");
        supperItem.setItemName("汤品");
        supperItem.setContentFormat("PLAIN_TEXT");
        supperItem.setEnabled(true);
        supperItem.setAllowImage(false);

        TemplateMealSaveRequest supper = new TemplateMealSaveRequest();
        supper.setMealCode("supper");
        supper.setMealName("夜宵");
        supper.setTimeLabel("21:00");
        supper.setEnabled(true);
        supper.setItems(List.of(supperItem));
        request.setMeals(List.of(breakfast, supper));

        MenuTemplateDesignDto result = templateService.saveDesign(request);

        assertThat(result.getName()).isEqualTo("标准模板-回归测试");
        assertThat(result.getSections())
                .extracting(item -> item.getTitle() + ":" + item.getSortOrder())
                .containsExactly("每周重点:1", "额外备注:2");
        assertThat(result.getMeals())
                .extracting(item -> item.getMealName() + ":" + item.getSortOrder())
                .containsExactly("活力早餐:1", "夜宵:2");
        assertThat(result.getMeals().get(0).getItems())
                .extracting(item -> item.getItemName() + ":" + item.getSortOrder())
                .containsExactly("优质蛋白:1", "饮品:2");

        List<MenuTemplateMeal> meals = templateService.listMeals(1001L);
        assertThat(meals).hasSize(2);
        assertThat(meals)
                .extracting(MenuTemplateMeal::getMealCode)
                .containsExactly("breakfast", "supper");

        Long breakfastMealId = meals.stream()
                .filter(item -> "breakfast".equals(item.getMealCode()))
                .findFirst()
                .orElseThrow()
                .getId();
        List<MenuTemplateMealItem> breakfastItems = templateService.listMealItems(breakfastMealId);
        assertThat(breakfastItems)
                .extracting(MenuTemplateMealItem::getItemCode)
                .containsExactly("protein", "drink");
        assertThat(breakfastItems)
                .extracting(MenuTemplateMealItem::getContentFormat)
                .containsExactly("PLAIN_TEXT", "RICH_TEXT");

        Long supperMealId = meals.stream()
                .filter(item -> "supper".equals(item.getMealCode()))
                .findFirst()
                .orElseThrow()
                .getId();
        assertThat(templateService.listMealItems(supperMealId))
                .extracting(MenuTemplateMealItem::getItemName)
                .containsExactly("汤品");
    }

    @Test
    void saveDesign_shouldDeleteMealCascadeItems() {
        TemplateDesignSaveRequest initial = new TemplateDesignSaveRequest();
        initial.setId(1001L);
        initial.setName("标准模板");
        initial.setThemeCode("standard");

        TemplateSectionSaveRequest section = new TemplateSectionSaveRequest();
        section.setSectionType("EXCLUSIVE_TITLE");
        section.setTitle("专属标题");
        section.setEnabled(true);
        section.setAllowImage(true);
        initial.setSections(List.of(section));

        TemplateMealItemSaveRequest breakfastItem = new TemplateMealItemSaveRequest();
        breakfastItem.setItemCode("staple");
        breakfastItem.setItemName("主食");
        breakfastItem.setContentFormat("RICH_TEXT");
        breakfastItem.setEnabled(true);
        breakfastItem.setAllowImage(true);

        TemplateMealSaveRequest breakfast = new TemplateMealSaveRequest();
        breakfast.setMealCode("breakfast");
        breakfast.setMealName("早餐");
        breakfast.setTimeLabel("早餐时间");
        breakfast.setEnabled(true);
        breakfast.setItems(List.of(breakfastItem));

        TemplateMealItemSaveRequest lunchItem = new TemplateMealItemSaveRequest();
        lunchItem.setItemCode("protein");
        lunchItem.setItemName("蛋白");
        lunchItem.setContentFormat("RICH_TEXT");
        lunchItem.setEnabled(true);
        lunchItem.setAllowImage(false);

        TemplateMealSaveRequest lunch = new TemplateMealSaveRequest();
        lunch.setMealCode("lunch");
        lunch.setMealName("午餐");
        lunch.setTimeLabel("午餐时间");
        lunch.setEnabled(true);
        lunch.setItems(List.of(lunchItem));
        initial.setMeals(List.of(breakfast, lunch));

        templateService.saveDesign(initial);

        Long lunchMealId = templateService.listMeals(1001L).stream()
                .filter(item -> "lunch".equals(item.getMealCode()))
                .findFirst()
                .orElseThrow()
                .getId();
        assertThat(templateService.listMealItems(lunchMealId)).hasSize(1);

        TemplateDesignSaveRequest update = new TemplateDesignSaveRequest();
        update.setId(1001L);
        update.setName("标准模板");
        update.setThemeCode("standard");
        update.setSections(List.of(section));
        update.setMeals(List.of(breakfast));

        templateService.saveDesign(update);

        assertThat(templateService.listMeals(1001L))
                .extracting(MenuTemplateMeal::getMealCode)
                .containsExactly("breakfast");
        assertThat(templateService.listMealItems(lunchMealId)).isEmpty();
    }
}
