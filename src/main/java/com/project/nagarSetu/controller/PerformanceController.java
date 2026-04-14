package com.project.nagarSetu.controller;

import com.project.nagarSetu.service.PerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    @Autowired
    private PerformanceService performanceService;

    @GetMapping("/wards/top")
    public ResponseEntity<?> getTopPerformingWards() {
        return ResponseEntity.ok(performanceService.getTopPerformingWards());
    }
}
