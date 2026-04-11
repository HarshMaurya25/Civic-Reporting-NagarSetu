package com.project.nagarSetu.controller;

import com.project.nagarSetu.service.authenication.AuthenticationService;
import com.project.nagarSetu.util.dto.authentication.LoginRequestDto;
import com.project.nagarSetu.util.dto.authentication.LoginResponseDto;
import com.project.nagarSetu.util.dto.authentication.RegisterResponse;
import com.project.nagarSetu.util.dto.authentication.RegistrationRequestDto;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/authenication")
public class AuthenticationController {

    private final AuthenticationService authService;

    @PostMapping("/registration")
    public ResponseEntity<RegisterResponse> registration(
           @Validated @RequestBody RegistrationRequestDto requestDto
    ){
        RegisterResponse response = authService.registration(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
        @Validated @RequestBody LoginRequestDto requestDto
    ){
        return new ResponseEntity<>(authService.getLogin(requestDto) , HttpStatus.OK);
    }

    @GetMapping("/getCode")
    public ResponseEntity<Void> getCode(@RequestParam String email){
        authService.getOTP(email);
        return ResponseEntity.ok().build();
    }

}
