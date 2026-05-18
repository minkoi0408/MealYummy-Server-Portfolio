package mealyummy.mealservice.service.payment;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.config.MomoConfig;
import mealyummy.mealservice.core.utils.PaymentUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MomoService {

    private final MomoConfig momoConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public String createPaymentUrl(String orderId, double amount) {
        String partnerCode = momoConfig.getPartnerCode();
        String accessKey = momoConfig.getAccessKey();
        String secretKey = momoConfig.getSecretKey();
        String returnUrl = momoConfig.getReturnUrl();
        String notifyUrl = momoConfig.getNotifyUrl();
        String orderInfo = "Thanh toán gói dịch vụ: " + orderId;
        String requestId = String.valueOf(System.currentTimeMillis());
        String extraData = "";
        String amountStr = String.valueOf((long) amount);
        
        String requestType = "captureWallet";

        String rawSignature = "accessKey=" + accessKey + "&amount=" + amountStr + "&extraData=" + extraData
                + "&ipnUrl=" + notifyUrl + "&orderId=" + orderId + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode + "&redirectUrl=" + returnUrl
                + "&requestId=" + requestId + "&requestType=" + requestType;

        String signature = PaymentUtil.hmacSHA256(rawSignature, secretKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("partnerName", "Meal Yummy");
        requestBody.put("storeId", "MealYummyStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", (long) amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", returnUrl);
        requestBody.put("ipnUrl", notifyUrl);
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", requestType);
        requestBody.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map response = restTemplate.postForObject(momoConfig.getEndpoint(), entity, Map.class);
            if (response != null && response.containsKey("payUrl")) {
                return (String) response.get("payUrl");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Handle gracefully in real life
    }

    public boolean verifyIpn(String orderId, String amount, String resultCode, String message, String responseTime, String extraData, String signature) {
        String rawSignature = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + amount + "&extraData=" + extraData
                + "&message=" + message + "&orderId=" + orderId
                + "&partnerCode=" + momoConfig.getPartnerCode()
                + "&resultCode=" + resultCode + "&responseTime=" + responseTime;

        String calculatedSignature = PaymentUtil.hmacSHA256(rawSignature, momoConfig.getSecretKey());
        return calculatedSignature.equals(signature);
    }
}
