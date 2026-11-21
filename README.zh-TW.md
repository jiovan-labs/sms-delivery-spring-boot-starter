[English](README.md) | [簡體中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md)

# 簡訊傳送 Spring Boot Starter

讓 Spring Boot 應用快速整合簡訊傳送，支援多種簡訊引擎。

## 快速開始

- 匯入相依性

```xml
    <dependency>
        <groupId>com.jiovan.spring.starter</groupId>
        <artifactId>sms-delivery-spring-boot-starter</artifactId>
        <version>LATEST</version>
    </dependency>
```

> 預設情況下，此 starter 會自動生效，你可以透過 `jiovan.sms-delivery.enabled = false` 關閉它。

- 使用字首 `jiovan.sms-delivery` 設定 Spring Boot 的 `application.yml`

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

- 隨後在程式碼中即可使用 `SmsDeliverySender` 傳送與查詢簡訊

```java
@Service
public class DemoService {
    @Autowired
    private SmsSenderComposer senderComposer;

    public void demoMethod() {
        // Sends a plain sms
        SendPlainSmsParam sendParam = new SendPlainSmsParam();
        sendParam.setMobilePhones(Collections.singleton("+86-13800138000"));
        sendParam.setTemplateCode("1234567890");
        sendParam.setTemplateParams(Maps.of("verifyCode", "123456"));
        String receiptId = senderComposer.sendPlainSms(sendParam);

        // Queries the status of a sms
        QuerySmsStatusParam queryParam = new QuerySmsStatusParam();
        sendParam.setMobilePhone("+86-13800138000");
        sendParam.setReceiptId(receiptId);
        sendParam.setSentDate(LocalDate.now());
        List<QuerySmsStatusResult> queryResults = senderComposer.querySmsStatus(queryParam);

        // Gets the raw client for advanced usage
        senderComposer.getRawClient();
        senderComposer.getRawClientAs(expectType);
    }
}
```

## 文件

- GitHub：https://github.com/jiovan-labs/sms-delivery-spring-boot-starter

## 環境要求

- jdk 17+

## 許可證

本專案採用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 許可證。

請參閱 `NOTICE.txt` 檔案瞭解所需的宣告與歸屬資訊。

## 捐贈

喜歡這個工具包嗎？歡迎[向我們捐贈](https://jiovan.com/donation)以支援開發。

## 版權

北京玖漫科技有限公司

## 網站

- 玖漫：https://jiovan.com
