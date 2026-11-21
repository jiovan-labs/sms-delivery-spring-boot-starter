[English](README.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md)

# 短信发送 Spring Boot Starter

让 Spring Boot 应用快速集成短信发送，支持多种短信引擎。

## 快速开始

- 导入依赖

```xml
    <dependency>
        <groupId>com.jiovan.spring.starter</groupId>
        <artifactId>sms-delivery-spring-boot-starter</artifactId>
        <version>LATEST</version>
    </dependency>
```

> 默认情况下，此 starter 会自动生效，你可以通过 `jiovan.sms-delivery.enabled = false` 关闭它。

- 使用前缀 `jiovan.sms-delivery` 配置 Spring Boot 的 `application.yml`

```yml
jiovan:
    sms-delivery:
        sender-type: 'tencent'
        aliyun:
            access-key-id: 'your-access-key'
            access-key-secret: 'your-secret-key'
            region: 'cn-beijing'
        tencent:
            secret-id: 'your-secret-id'
            secret-key: 'your-secret-key'
            region: 'ap-beijing'
            default-app-id: 'your-default-app-id'
            default-sign-name: 'your-default-sign-name'
```

- 随后在代码中即可使用 `SmsDeliverySender` 发送与查询短信

```java
@Service
public class DemoService {
    @Autowired
    private SmsDeliverySender smsDeliverySender;

    public void demoMethod() {
        // Sends a plain sms
        SendPlainSmsParam sendParam = new SendPlainSmsParam();
        sendParam.setMobilePhones(Collections.singleton("+86-13800138000"));
        sendParam.setTemplateCode("1234567890");
        sendParam.setTemplateParams(Maps.of("verifyCode", "123456"));
        String receiptId = smsDeliverySender.sendPlainSms(sendParam);

        // Queries the status of a sms
        QuerySmsStatusParam queryParam = new QuerySmsStatusParam();
        sendParam.setMobilePhone("+86-13800138000");
        sendParam.setReceiptId(receiptId);
        sendParam.setSentDate(LocalDate.now());
        List<QuerySmsStatusResult> queryResults = smsDeliverySender.querySmsStatus(queryParam);

        // Gets the raw client for advanced usage
        smsDeliverySender.getRawClient();
        smsDeliverySender.getRawClientAs(expectType);
    }
}
```

## 文档

- GitHub：https://github.com/jiovan-labs/sms-delivery-spring-boot-starter

## 环境要求

- jdk 17+

## 许可证

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 许可证。

请参阅 `NOTICE.txt` 文件了解所需的声明与归属信息。

## 捐赠

喜欢这个工具包吗？欢迎[向我们捐赠](https://jiovan.com/donation)以支持开发。

## 版权

北京玖漫科技有限公司

## 网站

- 玖漫：https://jiovan.com
