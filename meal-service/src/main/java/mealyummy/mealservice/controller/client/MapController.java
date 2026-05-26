package mealyummy.mealservice.controller.client;

import mealyummy.mealservice.service.map.MapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/nearby-markets")
    public ResponseEntity<Object> getNearbyMarkets(
            @RequestParam double lat,
            @RequestParam double lng) {
        Object markets = mapService.getNearbyMarkets(lat, lng);
        return ResponseEntity.ok(markets);
    }
}
