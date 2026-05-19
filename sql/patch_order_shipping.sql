-- 订单收货/支付信息（已有库执行一次）
USE bookshop_admin;

ALTER TABLE shop_order ADD COLUMN receiver_name VARCHAR(64) NULL COMMENT '收货人' AFTER status;
ALTER TABLE shop_order ADD COLUMN receiver_phone VARCHAR(32) NULL COMMENT '收货电话' AFTER receiver_name;
ALTER TABLE shop_order ADD COLUMN shipping_address VARCHAR(512) NULL COMMENT '收货地址' AFTER receiver_phone;
ALTER TABLE shop_order ADD COLUMN payment_method VARCHAR(32) NULL COMMENT '支付方式 WECHAT/ALIPAY/CARD' AFTER shipping_address;
