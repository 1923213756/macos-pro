package com.foodmap.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.foodmap.entity.pojo.Dish;
import com.foodmap.entity.pojo.ReviewGuide;
import com.foodmap.mapper.DishMapper;
import com.foodmap.mapper.ReviewGuideMapper;
import com.foodmap.service.ai.OllamaAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
     * 根据菜品名称或餐厅类型生成评价引导
     */
    @Cacheable(value = "reviewGuides", key = "#dishName + '-' + #restaurantType")
    public String generateGuide(String dishName, String restaurantType) {
        // 1. 尝试从数据库查找现有引导
        ReviewGuide existingGuide = null;

        if (StringUtils.hasText(dishName)) {
            existingGuide = reviewGuideMapper.findByDishName(dishName);
        } else if (StringUtils.hasText(restaurantType)) {
            existingGuide = reviewGuideMapper.findByRestaurantType(restaurantType);
        }

        if (existingGuide != null) {
            return existingGuide.getGuideContent();
        }

        // 2. 获取菜品信息（如果有）
        String dishDescription = "";
        if (StringUtils.hasText(dishName)) {
            LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Dish::getName, dishName).last("LIMIT 1");
            List<Dish> dishes = dishMapper.selectList(wrapper);
            if (!dishes.isEmpty()) {
                dishDescription = dishes.get(0).getDescription();
            }
        }

        // 3. 构建提示词
        String prompt = buildGuidePrompt(dishName, restaurantType, dishDescription);

        // 4. 调用AI生成引导
        String guideContent = ollamaAiClient.generateText(prompt);

        // 5. 保存到数据库
        saveGuide(dishName, restaurantType, guideContent);

        return guideContent;
    }

    /**
     * 构建引导生成提示词
     */
    private String buildGuidePrompt(String dishName, String restaurantType, String dishDescription) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请为餐厅用户创建一个评论引导模板，帮助他们写出全面的评价。\n\n");

        // 添加具体信息
        if (StringUtils.hasText(dishName)) {
            prompt.append("菜品名称: ").append(dishName).append("\n");

            if (StringUtils.hasText(dishDescription)) {
                prompt.append("菜品描述: ").append(dishDescription).append("\n");
            }

            // 添加一些常见菜品的示例
            if (dishName.contains("粉")) {
                prompt.append("\n参考示例 - 米粉评价引导:\n");
                prompt.append("尊敬的顾客您好，感谢您品尝我们的米粉！请从以下方面给予评价：\n");
                prompt.append("[口感] 粉条软硬度/筋道感：\n");
                prompt.append("[味道] 汤底味道/调味：\n");
                prompt.append("[配料] 肉类/蔬菜搭配：\n");
                prompt.append("[服务] 上菜速度/服务态度：\n");
                prompt.append("[环境] 就餐环境/卫生情况：\n");
            }
            else if (dishName.contains("肉")) {
                prompt.append("\n参考示例 - 红烧肉评价引导:\n");
                prompt.append("尊敬的顾客，感谢您选择我们的红烧肉！请从以下方面给予评价：\n");
                prompt.append("[口感] 肉质软糯度/肥瘦比例：\n");
                prompt.append("[味道] 咸甜度/入味程度：\n");
                prompt.append("[火候] 烹饪火候掌握：\n");
                prompt.append("[服务] 上菜速度/温度：\n");
                prompt.append("[环境] 就餐环境：\n");
            }
        }
        else if (StringUtils.hasText(restaurantType)) {
            prompt.append("餐厅类型: ").append(restaurantType).append("\n");

            // 添加餐厅类型示例
            if (restaurantType.contains("火锅")) {
                prompt.append("\n参考示例 - 火锅店评价引导:\n");
                prompt.append("尊敬的顾客您好，感谢您光临本火锅店！请从以下维度给出宝贵意见：\n");
                prompt.append("[锅底] 口味/辣度/浓郁度：\n");
                prompt.append("[食材] 新鲜度/种类丰富度：\n");
                prompt.append("[蘸料] 调配/味道：\n");
                prompt.append("[服务] 上菜速度/服务态度：\n");
                prompt.append("[环境] 就餐环境/通风情况：\n");
            }
            else if (restaurantType.contains("西餐")) {
                prompt.append("\n参考示例 - 西餐厅评价引导:\n");
                prompt.append("亲爱的顾客，感谢您光临本西餐厅！请从以下角度给予评价：\n");
                prompt.append("[主菜] 肉质/熟度/口感：\n");
                prompt.append("[配菜] 搭配/新鲜度：\n");
                prompt.append("[酱料] 风味/搭配度：\n");
                prompt.append("[服务] 礼仪/专业性/速度：\n");
                prompt.append("[环境] 氛围/舒适度：\n");
            }
        }

        // 通用要求
        prompt.append("\n请创建一个友好、专业的评论引导模板，包含以下内容：\n");
        prompt.append("1. 开头问候语\n");
        prompt.append("2. 根据菜品/餐厅类型的特点，设计3-5个评价维度\n");
        prompt.append("3. 每个维度下设计1-2个具体的评价点\n");
        prompt.append("4. 整体格式清晰，方便用户填写\n");
        prompt.append("5. 字数控制在200字以内\n\n");
        prompt.append("生成的引导模板应直接以「尊敬的顾客您好」开头，不要有多余的说明。");

        return prompt.toString();
    }

    /**
     * 保存评论引导到数据库
     */
    private void saveGuide(String dishName, String restaurantType, String guideContent) {
        ReviewGuide guide = new ReviewGuide();
        guide.setDishName(dishName);
        guide.setRestaurantType(restaurantType);
        guide.setGuideContent(guideContent);
        guide.setCreatedAt(LocalDateTime.now());
        guide.setUpdatedAt(LocalDateTime.now());
        reviewGuideMapper.insert(guide);
    }
}