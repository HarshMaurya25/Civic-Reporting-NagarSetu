package com.project.nagarSetu.controller;

import com.project.nagarSetu.entity.Ward;
import com.project.nagarSetu.service.WardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wards")
@RequiredArgsConstructor
public class WardController {

    private final WardService wardService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadWardGeoJson(@RequestBody Map<String, Object> geoJson) {
        try {
            wardService.uploadWardGeoJson(geoJson);
            return ResponseEntity.ok("Wards uploaded successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to upload wards: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Ward>> getAllWards() {
        return ResponseEntity.ok(wardService.getAllWards());
    }

    @PostMapping("/within")
    public ResponseEntity<List<Ward>> getWardsInArea(@RequestBody Map<String, List<List<Double>>> request) {
        List<List<Double>> coordinates = request.get("coordinates");
        try {
            return ResponseEntity.ok(wardService.getWardsInArea(coordinates));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/geojson")
    public ResponseEntity<Map<String, Object>> exportGeoJson() {
        return ResponseEntity.ok(wardService.exportGeoJson());
    }

    @DeleteMapping("/name/{name}")
    public ResponseEntity<String> deleteWardByName(@PathVariable String name) {
        try {
            boolean deleted = wardService.deleteWardByName(name);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok("Ward deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to delete ward: " + e.getMessage());
        }
    }
}
