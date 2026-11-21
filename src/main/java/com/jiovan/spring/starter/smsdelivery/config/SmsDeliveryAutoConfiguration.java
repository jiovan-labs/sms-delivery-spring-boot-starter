/*
 * Copyright (c) 2025 Jiovan Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jiovan.spring.starter.smsdelivery.config;


import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import com.jiovan.plexus.spring.condition.annotation.ConditionalOnAllProperties;
import com.jiovan.spring.starter.smsdelivery.composer.SmsDeliverySender;
import com.jiovan.spring.starter.smsdelivery.composer.impl.AliyunSmsDeliverySender;
import com.jiovan.spring.starter.smsdelivery.composer.impl.TencentSmsDeliverySender;
import com.jiovan.spring.starter.smsdelivery.property.SmsDeliveryProperties;


/**
 * Configuration for file storage
 *
 * @author David Hsing
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(prefix = SmsDeliveryAutoConfiguration.PROPERTIES_PREFIX, name = "enabled", matchIfMissing = true)
@Import(value = {SmsDeliveryAutoConfiguration.Entry.class, SmsDeliveryAutoConfiguration.Stage.class})
public class SmsDeliveryAutoConfiguration {
    public static final String PROPERTIES_PREFIX = "jiovan.sms-delivery";    // $NON-NLS-1$


    @Order(value = 0)
    @Role(value = BeanDefinition.ROLE_INFRASTRUCTURE)
    static class Entry {
        /**
         * Returns a new {@link com.jiovan.spring.starter.smsdelivery.property.SmsDeliveryProperties} holding the SMS sender settings
         *
         * @return a new {@link com.jiovan.spring.starter.smsdelivery.property.SmsDeliveryProperties} bean registered when none is present
         */
        @Bean
        @ConditionalOnMissingBean
        public SmsDeliveryProperties fileStorageProperties() {
            return new SmsDeliveryProperties();
        }
    }


    @Order(value = 1)
    @Role(value = BeanDefinition.ROLE_INFRASTRUCTURE)
    static class Stage {
        /**
         * Returns a {@link com.jiovan.spring.starter.smsdelivery.composer.SmsDeliverySender} that sends SMS through Aliyun
         *
         * @param properties the SMS sender properties providing the Aliyun access key and event publishing settings
         *
         * @return an {@link com.jiovan.spring.starter.smsdelivery.composer.impl.AliyunSmsDeliverySender} configured from the given properties
         */
        @Bean
        @ConditionalOnAllProperties(value = {
            @ConditionalOnProperty(prefix = SmsDeliveryAutoConfiguration.PROPERTIES_PREFIX, name = "sender-type", havingValue = "aliyun"),
            @ConditionalOnProperty(prefix = SmsDeliveryAutoConfiguration.PROPERTIES_PREFIX + ".aliyun", name = "access-key-id")
        })
        @ConditionalOnClass(value = com.aliyun.dysmsapi20170525.Client.class)
        @ConditionalOnMissingBean
        public SmsDeliverySender aliyunSmsSenderComposer(@Nonnull SmsDeliveryProperties properties) {
            return new AliyunSmsDeliverySender(properties.getAliyun(), BooleanUtils.isTrue(properties.getPublishEvent()));
        }

        /**
         * Returns a {@link com.jiovan.spring.starter.smsdelivery.composer.SmsDeliverySender} that sends SMS through Tencent Cloud
         *
         * @param properties the SMS sender properties providing the Tencent secret credentials and event publishing settings
         *
         * @return a {@link com.jiovan.spring.starter.smsdelivery.composer.impl.TencentSmsDeliverySender} configured from the given properties
         */
        @Bean
        @ConditionalOnAllProperties(value = {
            @ConditionalOnProperty(prefix = SmsDeliveryAutoConfiguration.PROPERTIES_PREFIX, name = "sender-type", havingValue = "tencent"),
            @ConditionalOnProperty(prefix = SmsDeliveryAutoConfiguration.PROPERTIES_PREFIX + ".tencent", name = "secret-id")
        })
        @ConditionalOnClass(value = com.tencentcloudapi.sms.v20210111.SmsClient.class)
        @ConditionalOnMissingBean
        public SmsDeliverySender tencentSmsSenderComposer(@Nonnull SmsDeliveryProperties properties) {
            return new TencentSmsDeliverySender(properties.getTencent(), BooleanUtils.isTrue(properties.getPublishEvent()));
        }
    }
}
