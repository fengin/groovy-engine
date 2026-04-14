-- Groovy 业务脚本配置表
-- 支持 MySQL 和 H2 (MODE=MySQL)
CREATE TABLE IF NOT EXISTS `sys_groovy_script` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_code` varchar(128) NOT NULL COMMENT '业务编码（路由标识）',
  `name` varchar(256) DEFAULT NULL COMMENT '接口名称',
  `script_content` mediumtext NOT NULL COMMENT 'Groovy 脚本源码',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  `project_code` varchar(64) DEFAULT NULL COMMENT '项目编码（用于按项目导入/导出）',
  `category` varchar(64) DEFAULT NULL COMMENT '分类标签',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_code` (`biz_code`)
);
