-- MySQL dump 10.13  Distrib 9.2.0, for macos15.2 (arm64)
--
-- Host: localhost    Database: campus_shop
-- ------------------------------------------------------
-- Server version	9.2.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ai_service_logs`
--

DROP TABLE IF EXISTS `ai_service_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_service_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `serviceType` varchar(50) NOT NULL COMMENT '服务类型: summary, guide, faq',
  `promptTokens` int DEFAULT NULL COMMENT '提示词token数',
  `completionTokens` int DEFAULT NULL COMMENT '生成内容token数',
  `processingTime` int DEFAULT NULL COMMENT '处理时间(ms)',
  `status` varchar(20) DEFAULT NULL COMMENT '状态: success, error',
  `errorMessage` text COMMENT '错误信息',
  `createdAt` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_service_type` (`serviceType`),
  KEY `idx_created_at` (`createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI服务调用日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_service_logs`
--

LOCK TABLES `ai_service_logs` WRITE;
/*!40000 ALTER TABLE `ai_service_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_service_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `aspect_summary`
--

DROP TABLE IF EXISTS `aspect_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aspect_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `restaurant_id` bigint NOT NULL COMMENT '餐厅ID',
  `aspect` varchar(50) NOT NULL COMMENT '方面名称(环境、服务、口味等)',
  `positive_count` int NOT NULL DEFAULT '0' COMMENT '好评数量',
  `negative_count` int NOT NULL DEFAULT '0' COMMENT '差评数量',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '总提及次数',
  `positive_percentage` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '好评百分比',
  `last_updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_restaurant_aspect` (`restaurant_id`,`aspect`),
  KEY `idx_restaurant_id` (`restaurant_id`),
  KEY `idx_last_updated` (`last_updated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='餐厅方面情感分析汇总表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `aspect_summary`
--

LOCK TABLES `aspect_summary` WRITE;
/*!40000 ALTER TABLE `aspect_summary` DISABLE KEYS */;
/*!40000 ALTER TABLE `aspect_summary` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `aspect_summary_evidence`
--

DROP TABLE IF EXISTS `aspect_summary_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aspect_summary_evidence` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `summary_id` bigint NOT NULL COMMENT '关联的aspect_summary表ID',
  `review_id` bigint DEFAULT NULL COMMENT '关联的评论ID',
  `aspect` varchar(50) NOT NULL COMMENT '方面名称',
  `sentiment` varchar(20) NOT NULL COMMENT '情感(好/差)',
  `evidence_text` varchar(255) NOT NULL COMMENT '证据文本',
  `confidence` decimal(5,4) NOT NULL DEFAULT '0.0000' COMMENT '置信度',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_summary_id` (`summary_id`),
  KEY `idx_review_id` (`review_id`),
  KEY `idx_aspect` (`aspect`),
  KEY `idx_confidence` (`confidence` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='方面情感分析证据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `aspect_summary_evidence`
--

LOCK TABLES `aspect_summary_evidence` WRITE;
/*!40000 ALTER TABLE `aspect_summary_evidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `aspect_summary_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_histories`
--

DROP TABLE IF EXISTS `chat_histories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_histories` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '聊天记录ID',
  `userId` bigint NOT NULL COMMENT '用户ID',
  `restaurantId` bigint NOT NULL COMMENT '商店ID',
  `sessionId` varchar(100) NOT NULL COMMENT '会话ID',
  `isUserMessage` tinyint(1) NOT NULL COMMENT '是否用户消息: 0-否(AI), 1-是',
  `content` text NOT NULL COMMENT '消息内容',
  `createdAt` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`sessionId`),
  KEY `idx_user_id` (`userId`),
  KEY `idx_restaurant_id` (`restaurantId`),
  KEY `idx_created_at` (`createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天历史记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_histories`
--

LOCK TABLES `chat_histories` WRITE;
/*!40000 ALTER TABLE `chat_histories` DISABLE KEYS */;
/*!40000 ALTER TABLE `chat_histories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dishes`
--

DROP TABLE IF EXISTS `dishes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dishes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
  `restaurantId` bigint NOT NULL COMMENT '商店ID',
  `name` varchar(100) NOT NULL COMMENT '菜品名称',
  `description` text COMMENT '菜品描述',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `isSpecial` tinyint DEFAULT '0' COMMENT '是否特色菜: 0-否, 1-是',
  `isAvailable` tinyint DEFAULT '1' COMMENT '是否可售: 0-否, 1-是',
  `createdAt` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedAt` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_restaurant_id` (`restaurantId`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dishes`
--

LOCK TABLES `dishes` WRITE;
/*!40000 ALTER TABLE `dishes` DISABLE KEYS */;
/*!40000 ALTER TABLE `dishes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `faqs`
--

DROP TABLE IF EXISTS `faqs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `faqs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'FAQ ID',
  `restaurantId` bigint NOT NULL COMMENT '商店ID',
  `question` varchar(500) NOT NULL COMMENT '问题',
  `answer` text NOT NULL COMMENT '回答',
  `keywords` varchar(500) DEFAULT NULL COMMENT '关键词，逗号分隔',
  `createdAt` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedAt` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_restaurant_id` (`restaurantId`),
  KEY `idx_keywords` (`keywords`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='常见问题解答表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `faqs`
--

LOCK TABLES `faqs` WRITE;
/*!40000 ALTER TABLE `faqs` DISABLE KEYS */;
/*!40000 ALTER TABLE `faqs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `favorites`
--

DROP TABLE IF EXISTS `favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_shop` (`user_id`,`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `favorites`
--

LOCK TABLES `favorites` WRITE;
/*!40000 ALTER TABLE `favorites` DISABLE KEYS */;
INSERT INTO `favorites` VALUES (1,9,10,'2025-04-21 21:58:27'),(2,18,11,'2025-04-24 10:41:15');
/*!40000 ALTER TABLE `favorites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `likes`
--

DROP TABLE IF EXISTS `likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `likes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `userId` bigint NOT NULL COMMENT '用户ID',
  `reviewId` bigint NOT NULL COMMENT '评论ID',
  `type` varchar(10) NOT NULL DEFAULT 'LIKE' COMMENT '类型(LIKE,DISLIKE)',
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_review` (`userId`,`reviewId`) COMMENT '确保一个用户只能点赞一次',
  KEY `idx_review_id` (`reviewId`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='点赞表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `likes`
--

LOCK TABLES `likes` WRITE;
/*!40000 ALTER TABLE `likes` DISABLE KEYS */;
INSERT INTO `likes` VALUES (2,9,3,'1','2025-04-23 00:28:26'),(3,9,2,'1','2025-04-23 00:28:32'),(4,9,1,'1','2025-04-23 00:28:33'),(5,18,3,'1','2025-04-24 12:33:12'),(6,18,2,'1','2025-04-24 12:33:15'),(7,18,1,'1','2025-04-24 12:33:16');
/*!40000 ALTER TABLE `likes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review_dish_mentions`
--

DROP TABLE IF EXISTS `review_dish_mentions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_dish_mentions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `reviewId` bigint NOT NULL COMMENT '评论ID',
  `dishId` bigint NOT NULL COMMENT '菜品ID',
  `sentiment` varchar(20) DEFAULT NULL COMMENT '情感倾向（positive,negative,neutral）',
  `createdAt` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_dish` (`reviewId`,`dishId`),
  KEY `dishId` (`dishId`),
  CONSTRAINT `review_dish_mentions_ibfk_1` FOREIGN KEY (`reviewId`) REFERENCES `reviews` (`id`) ON DELETE CASCADE,
  CONSTRAINT `review_dish_mentions_ibfk_2` FOREIGN KEY (`dishId`) REFERENCES `dishes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论菜品关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review_dish_mentions`
--

LOCK TABLES `review_dish_mentions` WRITE;
/*!40000 ALTER TABLE `review_dish_mentions` DISABLE KEYS */;
/*!40000 ALTER TABLE `review_dish_mentions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review_guides`
--

DROP TABLE IF EXISTS `review_guides`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_guides` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '引导ID',
  `dishName` varchar(100) DEFAULT NULL COMMENT '针对菜品的引导',
  `restaurantType` varchar(50) DEFAULT NULL COMMENT '针对商店类型的引导',
  `guideContent` text NOT NULL COMMENT '引导内容',
  `createdAt` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedAt` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_name` (`dishName`),
  UNIQUE KEY `uk_restaurant_type` (`restaurantType`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI评论引导模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review_guides`
--

LOCK TABLES `review_guides` WRITE;
/*!40000 ALTER TABLE `review_guides` DISABLE KEYS */;
INSERT INTO `review_guides` VALUES (4,'df',NULL,'<think>\n嗯，用户让我帮他们生成一个餐厅顾客评论引导提示。他们需要帮助撰写针对菜品的全面评价，所以我得先理解他们的需求是什么。首先，他们提到了具体的评价维度，包括口感、服务和环境，这说明他们希望评论不仅仅是简单的好评，而是有深度的反馈。\n\n接下来，我需要分析用户提供的通用维度：口感、服务和环境。这三个方面都是顾客关心的点，所以引导提示应该涵盖这些内容。每个维度下还有具体的问题提示，比如菜品的质地、味道，服务的效率和态度，环境的卫生和氛围。我要确保提示词能够覆盖这些细节。\n\n用户要求用友好且专业的语气书写，而且要简洁，不超过200字。这意味着我需要在用词上既要有温度，又不失专业性，同时保持信息的清晰和到位。不能太冗长，也不能遗漏重要点。\n\n菜品名称是“df”，看起来像是一个品牌或者特定餐厅的名字，所以提示词应该针对这个具体的菜品进行评价。不过，用户并没有提供更多关于这个菜品的信息，比如烹饪方式或特色成分，所以我需要保持引导足够开放，让顾客可以根据他们的实际体验来填充内容。\n\n另外，考虑到用户可能是餐厅老板或者管理人员，他们希望通过这些评论来改进服务和菜品，因此提示词需要具有建设性。比如，在口感方面，可以提到食材是否新鲜，味道是否平衡；在服务上，可以询问是否有细节处理，比如餐具是否清洁等。\n\n最后，我要确保每个维度下的问题提示都具体明确，让顾客能够轻松地按照提示撰写评论。同时，整个引导提示应该流畅自然，没有生硬的地方，这样才能让顾客感到被尊重和帮助，而不是被强迫。\n\n综上所述，我需要将每个维度的关键点用简洁、友好的语言表达出来，同时保持专业性，让顾客能够全面而有条理地撰写评论。\n</think>\n\n针对菜品 **df** 的评价可以从以下三个维度进行反馈：  \n1. **口感**：请描述菜品的质地和味道，是否鲜美、是否有层次感，以及食材是否新鲜。  \n2. **服务**：分享服务员的态度和效率，是否细致周到，如餐具清洁、菜单解读等方面的情况。  \n3. **环境**：评价餐厅的整体氛围和卫生状况，包括装修风格、是否安静，以及餐厅是否干净。  \n\n希望这些建议能帮助您全面表达对餐品的感受！','2025-04-26 12:19:22','2025-04-26 12:19:22'),(5,'红烧肉',NULL,'针对红烧肉的评价提示：  \n[口感] 肉质是否软糯？肥瘦比例是否适中？肉是否有嚼劲？  \n[味道] 咸甜度是否合适？菜品是否入味？是否有独特的调料风味？  \n[外观] 造型是否吸引人？摆盘是否美观？  \n[服务] 服务态度如何？是否及时更换肉类或替换菜品？','2025-04-26 23:22:11','2025-04-26 23:22:11'),(6,'水煮菜心',NULL,'**水煮菜心评价指南**\n\n您的水煮菜心品尝体验是很重要的!我们希望您能够在以下维度中提供详细而友好的反馈，让我们更好地为您提供最佳的服务。\n\n**口感：**\n* 菜品的质地和味道如何？是否符合您的期望？\n* 请描述一下您的口感体验，例如 whether 菜品太软、太硬、太甜或太酸等。','2025-04-27 09:56:06','2025-04-27 09:56:06');
/*!40000 ALTER TABLE `review_guides` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review_summaries`
--

DROP TABLE IF EXISTS `review_summaries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_summaries` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '摘要ID',
  `restaurantId` bigint NOT NULL COMMENT '商店ID',
  `summary` text NOT NULL COMMENT '摘要内容',
  `mentionedDishes` text COMMENT '提及的菜品，逗号分隔',
  `mainSentiment` varchar(20) DEFAULT NULL COMMENT '主要情感倾向',
  `reviewCount` int DEFAULT NULL COMMENT '分析的评论数量',
  `generatedAt` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `restaurantId` (`restaurantId`),
  KEY `idx_restaurant_id` (`restaurantId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI生成的评论摘要';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review_summaries`
--

LOCK TABLES `review_summaries` WRITE;
/*!40000 ALTER TABLE `review_summaries` DISABLE KEYS */;
/*!40000 ALTER TABLE `review_summaries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `content` varchar(1000) NOT NULL COMMENT '评论内容',
  `compositeScore` int NOT NULL COMMENT '评分(1-5)',
  `userId` bigint NOT NULL COMMENT '用户ID',
  `restaurantId` bigint NOT NULL COMMENT '商店ID',
  `likeCount` int DEFAULT '0' COMMENT '点赞数',
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE,HIDDEN,DELETED)',
  `environmentScore` decimal(10,0) DEFAULT NULL,
  `serviceScore` decimal(10,0) DEFAULT NULL,
  `tasteScore` decimal(10,0) DEFAULT NULL,
  `sentiment_analyzed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '情感分析状态(0-未分析,1-已分析)',
  `analyzed_at` datetime DEFAULT NULL COMMENT '情感分析时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`userId`),
  KEY `idx_restaurant_id` (`restaurantId`),
  KEY `idx_created_at` (`createdAt`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
INSERT INTO `reviews` VALUES (1,'f',3,18,10,2,'2025-04-21 16:11:44','2025-04-24 12:33:15','ACTIVE',2,4,4,0,NULL),(2,'12',4,18,10,2,'2025-04-21 17:37:03','2025-04-24 12:33:14','ACTIVE',4,4,4,0,NULL),(3,'123',4,18,10,2,'2025-04-21 18:24:03','2025-04-24 12:33:12','ACTIVE',3,5,5,0,NULL),(4,'d',4,9,10,0,'2025-04-22 10:14:27','2025-04-22 10:14:27','DELETED',4,4,4,0,NULL),(5,'j',4,18,10,0,'2025-04-24 13:28:07','2025-04-24 13:28:07','ACTIVE',3,4,4,0,NULL);
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shop`
--

DROP TABLE IF EXISTS `shop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shop` (
  `shopId` int NOT NULL AUTO_INCREMENT,
  `shopName` varchar(100) NOT NULL,
  `password` varchar(100) NOT NULL,
  `address` varchar(255) NOT NULL,
  `contactTel` varchar(20) NOT NULL,
  `businessHours` varchar(100) DEFAULT NULL COMMENT '营业时间',
  `category` varchar(50) DEFAULT NULL COMMENT '分类（如餐饮、零售）',
  `district` varchar(50) DEFAULT NULL COMMENT '所属区域',
  `description` text COMMENT '商铺描述',
  `compositeScore` decimal(3,2) DEFAULT '0.00' COMMENT '综合评分',
  `status` tinyint DEFAULT '1' COMMENT '状态（0-禁用 1-正常）',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `reviewCount` bigint DEFAULT '0',
  `tasteScore` decimal(10,0) DEFAULT NULL COMMENT '口味',
  `environmentScore` decimal(10,0) DEFAULT NULL COMMENT 'enviromentScore',
  `serviceScore` decimal(10,0) DEFAULT NULL COMMENT '服务评分',
  PRIMARY KEY (`shopId`),
  UNIQUE KEY `idx_shop_name` (`shopName`),
  UNIQUE KEY `idx_contact_tel` (`contactTel`),
  KEY `idx_category_district` (`category`,`district`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商铺信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shop`
--

LOCK TABLES `shop` WRITE;
/*!40000 ALTER TABLE `shop` DISABLE KEYS */;
INSERT INTO `shop` VALUES (10,'name','$2a$10$Tie4G8ro2BrANVKB0B05hex2JbrmUpTufbSWPctUxaYOBP1E82EKS','name','18923322233','09:00-21:00','中餐','白云区','',3.80,1,'2025-04-08 02:55:22','2025-04-25 10:13:42',4,4,3,4),(11,'1','$2a$10$FE3cxR0k1f6rzwq6e7zCKubdlcZdJsSRL1H6VnFzA.xEQ6Jdq83EO','somewhere','18927522222','09:00-21:00','中餐','天河区','没有',0.00,1,'2025-04-15 03:58:46','2025-04-15 04:23:04',0,NULL,NULL,NULL),(24,'123','$2a$10$iBge6J83Aaks4kUipQdR6O0UXFFm.5Fsx6y.B.tI2e9Cf0qZ1MqHa','1','18927577314','09:00-21:00','中餐','白云区','我',0.00,1,'2025-04-21 12:28:39','2025-04-21 12:28:39',0,NULL,NULL,NULL),(25,'123323','$2a$10$zqfOKkMxkb1IQuZYnTvjQu0eXm./M1fnSERLE0Ezh.TEZ6tqK4/Ze','1','1','09:00-21:00','西餐','越秀区','123',0.00,1,'2025-04-21 12:47:42','2025-04-22 16:40:25',0,NULL,NULL,NULL),(26,'1233','$2a$10$YndtAakmHPQNTrEWn3/Sf.tYzGDBgY.3x9zh0Pd0uUldhmbNWWr9.','1','dijef','09:00-21:00','a','s','a',0.00,1,'2025-04-21 12:49:10','2025-04-21 12:49:10',0,NULL,NULL,NULL),(28,'xie','$2a$10$kJmzymqlNOW9f4XsyQA76euarf4hVXrlh7sApZLWuy/9QRTJ7s1sG','xie','299293','09:00-21:00','中餐','白云区','无',0.00,1,'2025-04-21 12:54:39','2025-04-21 12:54:39',0,NULL,NULL,NULL),(29,'xiej','$2a$10$VqDSehlWo7HbZucWLlOimuiXM5wz1rq3X/3KiE6aNBM7Mlu3S9lfy','xiej','18927366643','09:00-21:00','中餐','白云区','无',0.00,1,'2025-04-21 12:56:28','2025-04-21 12:56:28',0,NULL,NULL,NULL);
/*!40000 ALTER TABLE `shop` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `userId` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `userName` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` int DEFAULT '1' COMMENT '账号状态: 0-禁用, 1-正常',
  PRIMARY KEY (`userId`),
  UNIQUE KEY `idx_username` (`userName`),
  UNIQUE KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (9,'123','$2a$10$krFtrS9dB4XBdz.QLiUjyuCUHzClO.v0n.DuvKG8KToGmkw6OnyLK','18927773222','2025-04-08 11:48:06','2025-04-08 11:48:06',1),(10,'1','$2a$10$ZL7dIKYW.ExT6D/lTay8YOj6xLx/Hmf6M7lsaC7aFO5JaWb1enDlW','1892323','2025-04-09 04:42:20','2025-04-16 01:46:51',1),(11,'tv1','$2a$10$dArclAfmA8T6jEOXiy7Osebh/NeuK4huJfbWvyjWvJYJOZE6P7WSK','dsfwe','2025-04-11 11:39:03','2025-04-11 11:39:03',1),(12,'tv','$2a$10$aGxJlckOY.chAO90xrQ.pOXZoUSvsTI7Bozyr.CYc2daZ7nnbIfuu','123','2025-04-11 11:39:17','2025-04-11 11:39:17',1),(13,'1234','$2a$10$MwPAsT6OvhAg4rFhkU2/1eAn7L7ot1s83c6AP.ou6erR0nfrJx8iS','1238483','2025-04-14 09:03:18','2025-04-14 09:03:18',1),(18,'12','$2a$10$Y0JAt05ubWZsWJn0Y5/IlOTlugp.Zb7js0Aip85xiWW7aGDUdUPfC','18937473748','2025-04-21 01:48:45','2025-04-21 01:48:45',1),(19,'12345','$2a$10$gkhXmwMoXPHMGE.axItzBeOYiLJa6Rqppy6HwkbW.8JTgsZQKpgw2','18927364536','2025-04-21 12:30:51','2025-04-21 12:30:51',1);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-05-06 16:44:57
