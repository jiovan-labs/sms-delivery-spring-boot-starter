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

package com.jiovan.spring.starter.smsdelivery.composer.impl;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import com.jiovan.plexus.common.toolkit.constant.CharVariantConst;
import com.jiovan.plexus.common.toolkit.constant.StringVariantConst;
import com.jiovan.plexus.common.toolkit.util.ArrayUtilsWraps;
import com.jiovan.plexus.common.toolkit.util.CollectionPlainWraps;
import com.jiovan.plexus.common.toolkit.util.LocalDateWraps;
import com.jiovan.plexus.common.toolkit.util.MapPlainWraps;
import com.jiovan.plexus.common.toolkit.util.NumberUtilsWraps;
import com.jiovan.plexus.common.toolkit.util.RegexUtilsWraps;
import com.jiovan.plexus.common.toolkit.util.StringUtilsWraps;
import com.jiovan.plexus.spring.toolkit.util.ObjectUtilsWraps;
import com.jiovan.spring.starter.smsdelivery.composer.SmsDeliverySender;
import com.jiovan.spring.starter.smsdelivery.enumeration.SmsDeliveryType;
import com.jiovan.spring.starter.smsdelivery.enumeration.SmsSendStatus;
import com.jiovan.spring.starter.smsdelivery.event.SmsSentEvent;
import com.jiovan.spring.starter.smsdelivery.exception.SmsQueryException;
import com.jiovan.spring.starter.smsdelivery.exception.SmsSendException;
import com.jiovan.spring.starter.smsdelivery.property.TencentSmsDeliveryProperties;
import com.jiovan.spring.starter.smsdelivery.structure.QuerySmsStatusParam;
import com.jiovan.spring.starter.smsdelivery.structure.QuerySmsStatusResult;
import com.jiovan.spring.starter.smsdelivery.structure.SendPlainSmsParam;
import com.jiovan.spring.starter.smsdelivery.structure.TencentQuerySmsStatusParam;
import com.jiovan.spring.starter.smsdelivery.structure.TencentQuerySmsStatusResult;
import com.jiovan.spring.starter.smsdelivery.structure.TencentSendPlainSmsParam;
import com.jiovan.spring.starter.smsdelivery.util.TencentSmsConfigUtils;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.PullSmsSendStatusByPhoneNumberRequest;
import com.tencentcloudapi.sms.v20210111.models.PullSmsSendStatusByPhoneNumberResponse;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


/**
 * Sms sender composer for tencent-sms
 *
 * @author David Hsing
 */
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class TencentSmsDeliverySender implements SmsDeliverySender, ApplicationEventPublisherAware, InitializingBean {
    private final TencentSmsDeliveryProperties properties;
    private final boolean publishEvent;
    private SmsClient smsClient;

    @Setter
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void afterPropertiesSet() throws Exception {
        smsClient = TencentSmsConfigUtils.smsClient(properties);
    }

    @Override
    public <T extends SendPlainSmsParam> String sendPlainSms(@Nonnull T param) throws SmsSendException {
        if (CollectionPlainWraps.isEmpty(param.getMobilePhones())) {
            throw new SmsSendException("Mobile phones can not be empty");
        }
        Set<String> mobilePhones = param.getMobilePhones().stream().map(item -> RegexUtilsWraps.removeAll(item, CharVariantConst.HYPHEN)).collect(Collectors.toSet());
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumberSet(mobilePhones.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        request.setSignName(StringUtils.defaultIfBlank(param.getSignName(), properties.getDefaultSignName()));
        request.setTemplateId(param.getTemplateCode());
        if (MapPlainWraps.isNotEmpty(param.getTemplateParams())) {
            List<String> templateParams = param.getTemplateParams().values().stream().map(item -> ObjectUtilsWraps.getDisplayString(item, StringUtils.EMPTY)).toList();
            request.setTemplateParamSet(templateParams.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        }
        if (param instanceof TencentSendPlainSmsParam alias) {
            request.setSmsSdkAppId(alias.getSmsSdkAppId());
            request.setExtendCode(alias.getExtendCode());
            request.setSessionContext(alias.getSessionContext());
            request.setSenderId(alias.getSenderId());
        }
        StringUtilsWraps.ifBlank(request.getSmsSdkAppId(), () -> request.setSmsSdkAppId(properties.getDefaultAppId()));
        try {
            SendSmsResponse response = smsClient.SendSms(request);
            SendStatus sendStatus = ArrayUtilsWraps.getFirst(response.getSendStatusSet());
            if (sendStatus == null) {
                throw new SmsSendException("Sms send request done, but none SendStatus respond");    // $NON-NLS-1$
            }
            if (StringUtilsWraps.equalsIgnoreCase(sendStatus.getCode(), StringVariantConst.OK)) {
                if (publishEvent) {
                    applicationEventPublisher.publishEvent(new SmsSentEvent(param, sendStatus.getSerialNo(), SmsDeliveryType.TENCENT));
                }
                return sendStatus.getSerialNo();
            }
            throw new SmsSendException(sendStatus.getMessage());
        } catch (Exception ex) {
            throw (ex instanceof SmsSendException alias) ? alias : new SmsSendException(ex);
        }
    }

    @Override
    public <T extends QuerySmsStatusParam> List<QuerySmsStatusResult> querySmsStatus(@Nonnull T param) throws SmsQueryException {
        PullSmsSendStatusByPhoneNumberRequest request = new PullSmsSendStatusByPhoneNumberRequest();
        request.setPhoneNumber(RegexUtilsWraps.removeAll(param.getMobilePhone(), CharVariantConst.HYPHEN));
        request.setLimit(param.getPageLimit());
        if (param.getSentDate() != null) {
            LocalDateTime startTime = LocalDateWraps.getDayStartDateTime(param.getSentDate());
            LocalDateTime endTime = LocalDateWraps.getDayEndDateTime(param.getSentDate());
            request.setBeginTime(LocalDateWraps.toEpochSecond(startTime));
            request.setEndTime(LocalDateWraps.toEpochSecond(endTime));
        }
        if (param instanceof TencentQuerySmsStatusParam alias) {
            request.setSmsSdkAppId(alias.getSmsSdkAppId());
            request.setOffset(alias.getOffset());
        }
        StringUtilsWraps.ifBlank(request.getSmsSdkAppId(), () -> request.setSmsSdkAppId(properties.getDefaultAppId()));
        ObjectUtilsWraps.ifNull(request.getOffset(), () -> request.setOffset(0L));
        try {
            PullSmsSendStatusByPhoneNumberResponse response = smsClient.PullSmsSendStatusByPhoneNumber(request);
            if (response == null || ArrayUtils.isEmpty(response.getPullSmsSendStatusSet())) {
                throw new SmsQueryException("Sms query request done, but none SendStatus respond");    // $NON-NLS-1$
            }
            return Arrays.stream(response.getPullSmsSendStatusSet()).map(item -> {
                TencentQuerySmsStatusResult result = new TencentQuerySmsStatusResult();
                result.setMobilePhone(item.getPhoneNumber());
                result.setSubscriberNumber(item.getSubscriberNumber());
                result.setCountryCode(item.getCountryCode());
                result.setDescription(item.getDescription());
                result.setSessionContext(item.getSessionContext());
                NumberUtilsWraps.ifPositive(item.getUserReceiveTime(), alias -> result.setReceiveDate(LocalDateWraps.ofEpochSecond(alias)));
                if (StringUtilsWraps.equalsIgnoreCase(item.getReportStatus(), "SUCCESS")) {    // $NON-NLS-1$
                    result.setSendStatus(SmsSendStatus.SEND_SUCCESS);
                } else if (StringUtilsWraps.equalsIgnoreCase(item.getReportStatus(), "FAIL")) {    // $NON-NLS-1$
                    result.setSendStatus(SmsSendStatus.SEND_FAILURE);
                } else {
                    result.setSendStatus(SmsSendStatus.UNKNOWN);
                }
                return result;
            }).collect(Collectors.toList());
        } catch (Exception ex) {
            throw (ex instanceof SmsQueryException alias) ? alias : new SmsQueryException(ex);
        }
    }

    @Override
    public Object getRawClient() {
        return smsClient;
    }

    @Nonnull
    @Override
    public SmsDeliveryType getDeliveryType() {
        return SmsDeliveryType.TENCENT;
    }
}
