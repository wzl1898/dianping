-- 高并发本地生活服务平台 DDL
-- 版本: v1.0
-- 注意: 请先创建数据库 dianping

CREATE DATABASE IF NOT EXISTS dianping DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dianping;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `phone`       VARCHAR(11)  NOT NULL COMMENT '手机号',
    `password`    VARCHAR(128)     NULL COMMENT '密码（预留）',
    `nick_name`   VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '昵称',
    `icon`        VARCHAR(500) NOT NULL DEFAULT '' COMMENT '头像URL',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 商户分类表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_shop_type` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`        VARCHAR(32)  NOT NULL COMMENT '分类名称',
    `icon`        VARCHAR(500) NOT NULL DEFAULT '' COMMENT '图标URL',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序权重（升序）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户分类表';

-- 初始化分类数据
INSERT INTO `tb_shop_type` (`name`, `icon`, `sort`) VALUES
('美食', '/images/type/food.png', 1),
('电影', '/images/type/movie.png', 2),
('酒店住宿', '/images/type/hotel.png', 3),
('休闲娱乐', '/images/type/fun.png', 4),
('运动健身', '/images/type/sport.png', 5),
('美妆美发', '/images/type/beauty.png', 6),
('购物逛街', '/images/type/shop.png', 7),
('生活服务', '/images/type/life.png', 8);

-- ============================================================
-- 3. 商户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_shop` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商户ID',
    `name`        VARCHAR(128)  NOT NULL COMMENT '商户名称',
    `type_id`     BIGINT        NOT NULL COMMENT '分类ID',
    `images`      VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '图片URL（多张逗号分隔）',
    `area`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '所在区域',
    `address`     VARCHAR(256)  NOT NULL DEFAULT '' COMMENT '详细地址',
    `x`           DOUBLE        NOT NULL DEFAULT 0 COMMENT '经度',
    `y`           DOUBLE        NOT NULL DEFAULT 0 COMMENT '纬度',
    `avg_price`   BIGINT        NOT NULL DEFAULT 0 COMMENT '均价（单位：分）',
    `sold`        INT           NOT NULL DEFAULT 0 COMMENT '销量',
    `comments`    INT           NOT NULL DEFAULT 0 COMMENT '评论数',
    `score`       INT           NOT NULL DEFAULT 0 COMMENT '评分（0-50，乘以10存储）',
    `open_hours`  VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '营业时间',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户表';

-- ============================================================
-- 4. 优惠券表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_voucher` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    `shop_id`      BIGINT       NOT NULL COMMENT '所属商户ID',
    `title`        VARCHAR(128) NOT NULL COMMENT '标题',
    `sub_title`    VARCHAR(256) NOT NULL DEFAULT '' COMMENT '副标题',
    `rules`        VARCHAR(512) NOT NULL DEFAULT '' COMMENT '使用规则',
    `pay_value`    BIGINT       NOT NULL COMMENT '支付金额（单位：分）',
    `actual_value` BIGINT       NOT NULL COMMENT '实际价值（单位：分）',
    `type`         TINYINT      NOT NULL DEFAULT 0 COMMENT '类型：0-普通券，1-秒杀券',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

-- ============================================================
-- 5. 秒杀券额外信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_seckill_voucher` (
    `voucher_id`  BIGINT   NOT NULL COMMENT '关联优惠券ID',
    `stock`       INT      NOT NULL COMMENT '库存',
    `begin_time`  DATETIME NOT NULL COMMENT '秒杀开始时间',
    `end_time`    DATETIME NOT NULL COMMENT '秒杀结束时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`voucher_id`),
    KEY `idx_begin_end` (`begin_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀券信息表';

-- ============================================================
-- 6. 订单表（分片表，按 user_id MOD 4 路由到 tb_voucher_order_0~3）
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_voucher_order_0` (
    `id`          BIGINT   NOT NULL COMMENT '订单ID（分布式ID生成）',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `voucher_id`  BIGINT   NOT NULL COMMENT '优惠券ID',
    `pay_type`    TINYINT  NOT NULL DEFAULT 1 COMMENT '支付类型：1-余额，2-微信（预留）',
    `status`      TINYINT  NOT NULL DEFAULT 1 COMMENT '订单状态：1-未支付，2-已支付（预留）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `pay_time`    DATETIME     NULL COMMENT '支付时间（预留）',
    `use_time`    DATETIME     NULL COMMENT '使用时间（预留）',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_voucher` (`user_id`, `voucher_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单分片表0';

CREATE TABLE IF NOT EXISTS `tb_voucher_order_1` LIKE `tb_voucher_order_0`;
CREATE TABLE IF NOT EXISTS `tb_voucher_order_2` LIKE `tb_voucher_order_0`;
CREATE TABLE IF NOT EXISTS `tb_voucher_order_3` LIKE `tb_voucher_order_0`;

-- ============================================================
-- 7. 探店笔记表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_blog` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
    `shop_id`     BIGINT        NOT NULL COMMENT '关联商户ID',
    `user_id`     BIGINT        NOT NULL COMMENT '作者ID',
    `title`       VARCHAR(32)   NOT NULL COMMENT '标题',
    `images`      VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '图片URL（多张逗号分隔）',
    `content`     VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '文字内容',
    `liked`       INT           NOT NULL DEFAULT 0 COMMENT '点赞数（冗余字段）',
    `comments`    INT           NOT NULL DEFAULT 0 COMMENT '评论数（预留）',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探店笔记表';

-- ============================================================
-- 8. 关注关系表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_follow` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关系ID',
    `user_id`        BIGINT   NOT NULL COMMENT '关注者ID',
    `follow_user_id` BIGINT   NOT NULL COMMENT '被关注用户ID',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`),
    KEY `idx_follow_user` (`follow_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注关系表';

-- ============================================================
-- 9. 管理员表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_admin` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username`    VARCHAR(32)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname`    VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '昵称',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 初始化管理员（密码: admin123）
INSERT INTO `tb_admin` (`username`, `password`, `nickname`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员');
