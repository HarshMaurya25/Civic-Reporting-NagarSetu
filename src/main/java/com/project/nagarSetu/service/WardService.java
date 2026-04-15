package com.project.nagarSetu.service;

import com.project.nagarSetu.entity.Ward;
import com.project.nagarSetu.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WardService {

    private final WardRepository wardRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Transactional
    public void uploadWardGeoJson(Map<String, Object> geoJson) {
        if (!"FeatureCollection".equals(geoJson.get("type"))) {
            throw new IllegalArgumentException("Invalid GeoJSON: Must be a FeatureCollection");
        }

        List<Map<String, Object>> features = (List<Map<String, Object>>) geoJson.get("features");
        if (features == null || features.isEmpty()) {
            throw new IllegalArgumentException("Invalid GeoJSON: No features found");
        }

        List<Ward> wardsToSave = new ArrayList<>();

        for (Map<String, Object> feature : features) {
            Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
            if (properties == null)
                continue;

            String wardName = null;
            if (properties.containsKey("Ward")) {
                wardName = String.valueOf(properties.get("Ward"));
            } else if (properties.containsKey("ward")) {
                wardName = String.valueOf(properties.get("ward"));
            }

            if (wardName == null || wardName.trim().isEmpty()) {
                continue; // Missing name, skip
            }

            wardName = wardName.trim().toUpperCase();

            // Extract region
            String region = null;
            if (properties.containsKey("Region")) {
                region = String.valueOf(properties.get("Region"));
            } else if (properties.containsKey("region")) {
                region = String.valueOf(properties.get("region"));
            }
            if (region != null && region.trim().isEmpty()) {
                region = null;
            }

            // Check duplicate
            if (wardRepository.findByNameIgnoreCase(wardName).isPresent()) {
                throw new IllegalArgumentException("Duplicate ward name found: " + wardName);
            }

            Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
            if (geometry == null || !"Polygon".equals(geometry.get("type"))) {
                throw new IllegalArgumentException("Invalid GeoJSON: Geometry must be a Polygon for ward " + wardName);
            }

            List<List<List<Number>>> coordinates = (List<List<List<Number>>>) geometry.get("coordinates");
            if (coordinates == null || coordinates.isEmpty()) {
                throw new IllegalArgumentException("Invalid GeoJSON: Empty coordinates for ward " + wardName);
            }

            List<List<Number>> ringCoords = coordinates.get(0);
            Coordinate[] coords = new Coordinate[ringCoords.size()];
            for (int i = 0; i < ringCoords.size(); i++) {
                List<Number> point = ringCoords.get(i);
                coords[i] = new Coordinate(point.get(0).doubleValue(), point.get(1).doubleValue());
            }

            LinearRing shell = geometryFactory.createLinearRing(coords);
            Polygon polygon = geometryFactory.createPolygon(shell, null);
            polygon.setSRID(4326);

            Ward ward = Ward.builder()
                    .name(wardName)
                    .boundary(polygon)
                    .region(region)
                    .build();

            wardsToSave.add(ward);
        }

        wardRepository.saveAll(wardsToSave);
    }

    public List<Ward> getAllWards() {
        return wardRepository.findAll();
    }

    public List<Ward> getWardsInArea(List<List<Double>> boxCoords) {
        if (boxCoords == null || boxCoords.size() < 4) {
            throw new IllegalArgumentException("Bounding box must have at least 4 coordinates (closed ring).");
        }

        Coordinate[] coords = new Coordinate[boxCoords.size()];
        for (int i = 0; i < boxCoords.size(); i++) {
            List<Double> point = boxCoords.get(i);
            coords[i] = new Coordinate(point.get(0), point.get(1));
        }

        LinearRing shell = geometryFactory.createLinearRing(coords);
        Polygon polygon = geometryFactory.createPolygon(shell, null);
        polygon.setSRID(4326);

        return wardRepository.findWardsWithinBoundingBox(polygon);
    }

    public Optional<Ward> getWardByLocation(double lat, double lon) {
        Coordinate coord = new Coordinate(lon, lat); // x = lon, y = lat
        org.locationtech.jts.geom.Point point = geometryFactory.createPoint(coord);
        point.setSRID(4326);
        return wardRepository.findWardByLocation(point);
    }

    public Map<String, Object> exportGeoJson() {
        List<Ward> wards = wardRepository.findAll();
        List<Map<String, Object>> features = new ArrayList<>();

        for (Ward ward : wards) {
            Map<String, Object> feature = new HashMap<>();
            feature.put("type", "Feature");

            Map<String, Object> properties = new HashMap<>();
            properties.put("name", ward.getName());
            if (ward.getRegion() != null) {
                properties.put("region", ward.getRegion());
            }
            feature.put("properties", properties);

            // Jackon JTS datatype deals with parsing geometry if correctly configured.
            // But we will manually structure it for guaranteed flutter-compatible
            // exactness.
            Map<String, Object> geometry = new HashMap<>();
            geometry.put("type", "Polygon");

            Coordinate[] coords = ward.getBoundary().getCoordinates();
            List<List<Double>> ring = new ArrayList<>();
            for (Coordinate c : coords) {
                ring.add(Arrays.asList(c.x, c.y));
            }
            List<List<List<Double>>> polyCoords = new ArrayList<>();
            polyCoords.add(ring);

            geometry.put("coordinates", polyCoords);
            feature.put("geometry", geometry);

            features.add(feature);
        }

        Map<String, Object> featureCollection = new HashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);

        return featureCollection;
    }

    @Transactional
    public boolean deleteWardByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ward name is required");
        }

        long deletedCount = wardRepository.deleteByNameIgnoreCase(name.trim());
        return deletedCount > 0;
    }
}
