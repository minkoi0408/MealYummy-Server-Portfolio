package mealyummy.mealservice.service.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.location.LocationClient;
import software.amazon.awssdk.services.location.model.Place;
import software.amazon.awssdk.services.location.model.SearchForTextResult;
import software.amazon.awssdk.services.location.model.SearchPlaceIndexForTextRequest;
import software.amazon.awssdk.services.location.model.SearchPlaceIndexForTextResponse;

import java.util.*;

/**
 * MapService — tìm chợ/siêu thị gần người dùng qua Amazon Location Service.
 */
@Service
public class MapService {

    private static final Logger log = LoggerFactory.getLogger(MapService.class);

    @Value("${AWS_ACCESS_KEY_ID:}")
    private String awsAccessKey;

    @Value("${AWS_SECRET_ACCESS_KEY:}")
    private String awsSecretKey;

    @Value("${AWS_REGION:ap-southeast-1}")
    private String awsRegion;

    @Value("${AWS_LOCATION_PLACE_INDEX:MealYummyPlaceIndex}")
    private String placeIndexName;

    // ─────────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Object getNearbyMarkets(double lat, double lng) {
        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        // Hàm helper để thêm vào list kết quả không bị trùng lặp
        java.util.function.Consumer<List<Map<String, Object>>> addResults = (list) -> {
            for (Map<String, Object> item : list) {
                String id = (String) item.get("place_id");
                if (!seenIds.contains(id)) {
                    seenIds.add(id);
                    results.add(item);
                }
            }
        };

        // 1️⃣ Tìm kiếm các danh mục chính trong bán kính 4km để không bị văng ra quá xa
        addResults.accept(awsSearch(lat, lng, "Supermarket", "supermarket", 4.0));
        addResults.accept(awsSearch(lat, lng, "Market", "market", 4.0));
        addResults.accept(awsSearch(lat, lng, "Convenience Store", "convenience", 4.0));

        // 2️⃣ Nếu ở các vùng ven mà không có kết quả trong 4km, nới lỏng ra 10km
        if (results.size() < 3) {
            addResults.accept(awsSearch(lat, lng, "Grocery", "supermarket", 10.0));
        }

        // 3️⃣ Sắp xếp kết quả theo khoảng cách tăng dần (gần nhất lên đầu)
        results.sort((a, b) -> {
            Map<String, Object> geomA = (Map<String, Object>) a.get("geometry");
            Map<String, Object> locA = (Map<String, Object>) geomA.get("location");
            double latA = (double) locA.get("lat");
            double lngA = (double) locA.get("lng");

            Map<String, Object> geomB = (Map<String, Object>) b.get("geometry");
            Map<String, Object> locB = (Map<String, Object>) geomB.get("location");
            double latB = (double) locB.get("lat");
            double lngB = (double) locB.get("lng");

            double distA = calculateDistance(lat, lng, latA, lngA);
            double distB = calculateDistance(lat, lng, latB, lngB);

            return Double.compare(distA, distB);
        });

        if (!results.isEmpty()) {
            log.info("[MapService] ✅ AWS Location → {} địa điểm", results.size());
            Map<String, Object> out = new HashMap<>();
            out.put("status", "OK");
            out.put("source", "aws_location");
            // Trả về tối đa 15 kết quả gần nhất cho Frontend nhẹ nhàng
            out.put("results", results.size() > 15 ? results.subList(0, 15) : results); 
            return out;
        }

        // 4️⃣ Mock fallback
        log.warn("[MapService] ⚠️ AWS Location thất bại hoặc không tìm thấy, trả về dữ liệu mẫu.");
        return buildMockResponse(lat, lng);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AWS Location Client Builder
    // ─────────────────────────────────────────────────────────────────────────

    private LocationClient getLocationClient() {
        if (awsAccessKey != null && !awsAccessKey.isEmpty() && awsSecretKey != null && !awsSecretKey.isEmpty()) {
            return LocationClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                ))
                .build();
        }
        return LocationClient.builder().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AWS Search Place Index
    // ─────────────────────────────────────────────────────────────────────────

    private List<Map<String, Object>> awsSearch(double lat, double lng, String query, String inferredType, double radiusKm) {
        try (LocationClient client = getLocationClient()) {
            SearchPlaceIndexForTextRequest req = SearchPlaceIndexForTextRequest.builder()
                .indexName(placeIndexName)
                .text(query)
                .filterBBox(getBoundingBox(lat, lng, radiusKm)) // Ép cứng giới hạn khu vực tìm kiếm
                .maxResults(15)
                .language("vi")
                .build();

            SearchPlaceIndexForTextResponse res = client.searchPlaceIndexForText(req);

            List<Map<String, Object>> out = new ArrayList<>();
            for (SearchForTextResult result : res.results()) {
                Map<String, Object> item = convertAwsPlace(result.placeId(), result.place(), inferredType);
                if (item != null) out.add(item);
            }
            return out;

        } catch (Exception e) {
            log.error("[MapService] AWS Search lỗi '{}': {}", query, e.getMessage());
            return new ArrayList<>();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers (BBox & Distance)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo Bounding Box xung quanh 1 toạ độ với bán kính cho trước (km).
     * Format: [minLng, minLat, maxLng, maxLat]
     */
    private List<Double> getBoundingBox(double lat, double lng, double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));
        return Arrays.asList(lng - lngDelta, lat - latDelta, lng + lngDelta, lat + latDelta);
    }

    /**
     * Tính khoảng cách đường chim bay (Haversine)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // Bán kính trái đất (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convert AWS Place response → our format
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> convertAwsPlace(String placeId, Place place, String inferredType) {
        if (place == null) return null;

        String fullLabel = place.label() != null ? place.label() : "Cửa hàng/Chợ";
        String[] parts = fullLabel.split(",");
        String name = parts[0].trim();

        Map<String, Object> item = new HashMap<>();
        item.put("place_id", placeId != null ? placeId : UUID.randomUUID().toString());
        item.put("name", name);
        item.put("vicinity", fullLabel);

        // Giả lập rating vì AWS chưa hỗ trợ field này trong Location
        double randomRating = 4.0 + (Math.random() * 0.9);
        item.put("rating", Math.round(randomRating * 10.0) / 10.0); 
        item.put("place_type", inferredType);

        if (place.geometry() != null && place.geometry().point() != null && place.geometry().point().size() >= 2) {
            Map<String, Object> geom = new HashMap<>();
            Map<String, Object> loc = new HashMap<>();
            loc.put("lng", place.geometry().point().get(0));
            loc.put("lat", place.geometry().point().get(1));
            geom.put("location", loc);
            item.put("geometry", geom);
        }

        Map<String, Object> oh = new HashMap<>();
        oh.put("open_now", true);
        item.put("opening_hours", oh);

        List<Map<String, Object>> mappedPhotos = new ArrayList<>();
        Map<String, Object> photo = new HashMap<>();
        photo.put("photo_reference", "aws_mock_photo");
        mappedPhotos.add(photo);
        item.put("photos", mappedPhotos);

        return item;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mock fallback 
    // ─────────────────────────────────────────────────────────────────────────

    private Object buildMockResponse(double lat, double lng) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(buildMock("Co.opmart (Mock)", "Siêu thị lân cận", 4.5, "supermarket", lat + 0.005, lng + 0.005));
        results.add(buildMock("WinMart+ (Mock)", "Cửa hàng tiện lợi", 4.2, "convenience", lat - 0.003, lng - 0.004));
        results.add(buildMock("Chợ Truyền Thống (Mock)", "Chợ", 4.8, "market", lat + 0.002, lng - 0.006));

        Map<String, Object> out = new HashMap<>();
        out.put("status", "OK");
        out.put("source", "mock");
        out.put("results", results);
        return out;
    }

    private Map<String, Object> buildMock(String name, String vicinity, double rating,
                                           String type, double mLat, double mLng) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("vicinity", vicinity);
        item.put("rating", rating);
        item.put("place_type", type);

        Map<String, Object> geom = new HashMap<>();
        Map<String, Object> loc  = new HashMap<>();
        loc.put("lat", mLat);
        loc.put("lng", mLng);
        geom.put("location", loc);
        item.put("geometry", geom);
        return item;
    }
}
