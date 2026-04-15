package com.project.nagarSetu.controller;

import com.project.nagarSetu.service.PerformanceService;
import com.project.nagarSetu.util.dto.WardPerformanceDto;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/api/performance")
public class PerformanceController {

    @Autowired
    private PerformanceService performanceService;

    @GetMapping("/top-wards")
    public ResponseEntity<List<WardPerformanceDto>> getTopPerformingWards() {
        List<WardPerformanceDto> topWards = performanceService.getTopPerformingWards();
        return ResponseEntity.ok(topWards);
    }
}
