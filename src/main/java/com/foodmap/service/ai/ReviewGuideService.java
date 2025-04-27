package com.foodmap.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodmap.entity.pojo.Dish;
import com.foodmap.entity.pojo.ReviewGuide;
import com.foodmap.mapper.DishMapper;
import com.foodmap.mapper.ReviewGuideMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewGuideService {

    private final OllamaAiClient ollamaAiClient;
    private final ReviewGuideMapper reviewGuideMapper;
    private final DishMapper dishMapper;

    /**
     * 根据菜品名称生成评价引导
     */
    @Cacheable(value = "reviewGuides", key = "#dishName")
    public String generateGuide(String dishName) {
        if (!StringUtils.hasText(dishName)) {
            throw new IllegalArgumentException("菜品名称不能为空");
        }

        // 1. 尝试从数据库查找现有引导
        ReviewGuide existingGuide = reviewGuideMapper.findByDishName(dishName);
        if (existingGuide != null) {
            return existingGuide.getGuideContent();
        }

        // 2. 获取菜品信息
        String dishDescription = getDishDescriptionByName(dishName);

        // 3. 构建提示词
        String prompt = buildGuidePrompt(dishName, dishDescription);

        // 4. 调用AI生成引导
        String generatedText = ollamaAiClient.generateText(prompt);

        // 5. 移除 <think> 标签内容
        String guideContent = filterGeneratedContent(generatedText);

        // 6. 保存到数据库
        saveGuide(dishName, guideContent);

        return guideContent;
    }

    /**
     * 根据菜品名称获取描述
     */
    private String getDishDescriptionByName(String dishName) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getName, dishName).last("LIMIT 1");
        List<Dish> dishes = dishMapper.selectList(wrapper);
        return dishes.isEmpty() ? "" : dishes.get(0).getDescription();
    }

    /**
     * 构建引导生成提示词
     */
    private String buildGuidePrompt(String dishName, String dishDescription) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请为餐厅顾客生成一个评论引导提示，帮助他们撰写针对菜品的全面评价。\n");
        prompt.append("提示词应包括以下内容：\n");
        prompt.append("1. 针对菜品的具体评价维度（如口感、味道、服务等）。\n");
        prompt.append("2. 每个维度下的详细问题提示。\n");
        prompt.append("3. 必须以友好和专业的语气书写。\n");
        prompt.append("4. 提示词应简洁，不超过200字。\n\n");

        // 添加菜品信息
        prompt.append("菜品名称: ").append(dishName).append("\n");
        if (StringUtils.hasText(dishDescription)) {
            prompt.append("菜品描述: ").append(dishDescription).append("\n");
        }

        // 添加具体菜品类型的示例
        if (dishName.contains("粉")) {
            prompt.append("\n示例 - 针对米粉的评价提示：\n");
            prompt.append("[口感] 粉条的软硬度是否合适？是否有筋道感？\n");
            prompt.append("[味道] 汤底是否浓郁？调味是否均衡？\n");
            prompt.append("[配料] 配菜是否新鲜？种类是否丰富？\n");
        } else if (dishName.contains("肉")) {
            prompt.append("\n示例 - 针对红烧肉的评价提示：\n");
            prompt.append("[口感] 肉质是否软糯？肥瘦比例是否适中？\n");
            prompt.append("[味道] 咸甜度是否适中？是否入味？\n");
            prompt.append("[火候] 烹饪火候是否掌握得当？\n");
        } else {
            prompt.append("\n请根据以下通用维度进行评价：\n");
            prompt.append("[口感] 菜品的质地和味道如何？\n");
            prompt.append("[服务] 服务的效率和态度如何？\n");
            prompt.append("[环境] 餐厅的卫生和氛围如何？\n");
        }

        return prompt.toString();
    }

    /**
     * 过滤生成内容，移除 <think> 标签及其内容
     */
    private String filterGeneratedContent(String generatedContent) {
        if (!StringUtils.hasText(generatedContent)) {
            return generatedContent;
        }
        // 使用正则表达式移除 <think> 标签及其内容
        return generatedContent.replaceAll("<think>[\\s\\S]*?</think>", "").trim();
    }

    /**
     * 保存评论引导到数据库
     */
    private void saveGuide(String dishName, String guideContent) {
        ReviewGuide guide = new ReviewGuide();
        guide.setDishName(dishName);
        guide.setGuideContent(guideContent);
        guide.setCreatedAt(LocalDateTime.now());
        guide.setUpdatedAt(LocalDateTime.now());
        reviewGuideMapper.insert(guide);
    }
}