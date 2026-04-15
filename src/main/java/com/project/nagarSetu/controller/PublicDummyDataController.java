package com.project.nagarSetu.controller;

import com.project.nagarSetu.service.PublicDummyDataService;
import com.project.nagarSetu.util.dto.publicdata.IssueDatesDto;
import com.project.nagarSetu.util.dto.publicdata.NameDateDto;
import com.project.nagarSetu.util.dto.publicdata.UpdateDateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/dummy")
@RequiredArgsConstructor
public class PublicDummyDataController {
    private final PublicDummyDataService dummyDataService;

    @GetMapping("/workers")
    public ResponseEntity<List<NameDateDto>> workers() {
        return ResponseEntity.ok(dummyDataService.listWorkers());
    }

    @GetMapping("/supervisors")
    public ResponseEntity<List<NameDateDto>> supervisors() {
        return ResponseEntity.ok(dummyDataService.listSupervisors());
    }

    @GetMapping("/wards")
    public ResponseEntity<List<NameDateDto>> wards() {
        return ResponseEntity.ok(dummyDataService.listWards());
    }

    @GetMapping("/issues")
    public ResponseEntity<List<IssueDatesDto>> issues() {
        return ResponseEntity.ok(dummyDataService.listIssues());
    }

    @PutMapping("/dates")
    public ResponseEntity<Void> updateDates(@RequestBody UpdateDateRequest req) {
        dummyDataService.updateDate(req);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/dates/batch")
    public ResponseEntity<Void> updateDatesBatch(@RequestBody List<UpdateDateRequest> requests) {
        dummyDataService.updateDates(requests);
        return ResponseEntity.ok().build();
    }
}

