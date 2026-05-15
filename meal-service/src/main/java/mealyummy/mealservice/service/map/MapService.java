package mealyummy.mealservice.service.map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MapService {

    @Value("${GOOGLE_MAPS_API_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public MapService() {
        this.restTemplate = new RestTemplate();
    }

    public Object getNearbyMarkets(double lat, double lng) {
        try {
            String keyword = java.net.URLEncoder.encode("chợ|siêu thị|mart", "UTF-8");
            String url = String.format(
                java.util.Locale.US,
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=3000&keyword=%s&key=%s",
                lat, lng, keyword, apiKey
            );

            // Call Google Places API
            java.util.Map<String, Object> response = restTemplate.getForObject(url, java.util.Map.class);
            
            // Check if Google returned an error (like Billing Not Enabled)
            if (response != null && "REQUEST_DENIED".equals(response.get("status"))) {
                return getMockMarkets(lat, lng);
            }
            
            return response;
        } catch (Exception e) {
            return getMockMarkets(lat, lng);
        }
    }

    private Object getMockMarkets(double lat, double lng) {
        // Fallback data when Google API fails
        java.util.Map<String, Object> mockResponse = new java.util.HashMap<>();
        mockResponse.put("status", "OK");
        mockResponse.put("mock_data", true); // to let frontend know it's mock
        
        java.util.List<java.util.Map<String, Object>> results = new java.util.ArrayList<>();
        
        // Mock Market 1
        java.util.Map<String, Object> m1 = new java.util.HashMap<>();
        m1.put("name", "Co.opmart (Dữ liệu mẫu)");
        m1.put("vicinity", "Siêu thị lân cận");
        m1.put("rating", 4.5);
        java.util.Map<String, Object> geom1 = new java.util.HashMap<>();
        java.util.Map<String, Object> loc1 = new java.util.HashMap<>();
        loc1.put("lat", lat + 0.005);
        loc1.put("lng", lng + 0.005);
        geom1.put("location", loc1);
        m1.put("geometry", geom1);
        results.add(m1);
        
        // Mock Market 2
        java.util.Map<String, Object> m2 = new java.util.HashMap<>();
        m2.put("name", "WinMart+ (Dữ liệu mẫu)");
        m2.put("vicinity", "Cửa hàng tiện lợi gần bạn");
        m2.put("rating", 4.2);
        java.util.Map<String, Object> geom2 = new java.util.HashMap<>();
        java.util.Map<String, Object> loc2 = new java.util.HashMap<>();
        loc2.put("lat", lat - 0.003);
        loc2.put("lng", lng - 0.004);
        geom2.put("location", loc2);
        m2.put("geometry", geom2);
        results.add(m2);

        // Mock Market 3
        java.util.Map<String, Object> m3 = new java.util.HashMap<>();
        m3.put("name", "Chợ Truyền Thống (Dữ liệu mẫu)");
        m3.put("vicinity", "Khu chợ địa phương");
        m3.put("rating", 4.8);
        java.util.Map<String, Object> geom3 = new java.util.HashMap<>();
        java.util.Map<String, Object> loc3 = new java.util.HashMap<>();
        loc3.put("lat", lat + 0.002);
        loc3.put("lng", lng - 0.006);
        geom3.put("location", loc3);
        m3.put("geometry", geom3);
        results.add(m3);

        mockResponse.put("results", results);
        return mockResponse;
    }
}
