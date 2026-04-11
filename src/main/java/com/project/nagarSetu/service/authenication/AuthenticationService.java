package com.project.nagarSetu.service.authenication;

import com.project.nagarSetu.entity.User;
import com.project.nagarSetu.error.exception.VerificationCodeExpiredException;
import com.project.nagarSetu.event.SendPinEvents;
import com.project.nagarSetu.repository.UserRepository;
import com.project.nagarSetu.service.redis.RedisService;
import com.project.nagarSetu.util.dto.authentication.LoginRequestDto;
import com.project.nagarSetu.util.dto.authentication.LoginResponseDto;
import com.project.nagarSetu.util.dto.authentication.RegisterResponse;
import com.project.nagarSetu.util.dto.authentication.RegistrationRequestDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.project.nagarSetu.repository.WorkerRepository workerRepository;
    private final com.project.nagarSetu.repository.SupervisiorRepository supervisiorRepository;

    private final String userName = "USER_";

    public void getOTP(String email) {
        String existingCode = redisService.get(userName + email, String.class);
        if (existingCode != null) {
            throw new RuntimeException("Please wait before requesting another OTP for this email.");
        }

        SecureRandom random = new SecureRandom();
        Integer code = 100_000 + random.nextInt(900_000);
        String codeString = String.valueOf(code);

        log.trace("Code is {} with mail {} ", codeString, email);
        redisService.set(userName + email, codeString, 600);
        eventPublisher.publishEvent(new SendPinEvents(email, codeString));
    }

    @Transactional
    public RegisterResponse registration(RegistrationRequestDto requestDto) {
        String code = redisService.get(userName + requestDto.getEmail(), String.class);
        if (code == null) {
            log.info("Code is expired");
            throw new VerificationCodeExpiredException(requestDto.getEmail());
        }

        if (!requestDto.getCode().equals(code)) {
            log.info("Wrong verification code provided for {}", requestDto.getEmail());
            throw new VerificationCodeExpiredException(requestDto.getEmail());
        }

        redisService.delete(userName + requestDto.getEmail());

        User user = User.builder()
                .fullName(requestDto.getFullName())
                .phoneNumber(requestDto.getPhoneNumber())
                .email(requestDto.getEmail())
                .passwordHash(passwordEncoder.encode(requestDto.getPassword()))
                .roles(requestDto.getRole())
                .gender(requestDto.getGender())
                .age(requestDto.getAge())
                .location(requestDto.getLocation())
                .enable(true)
                .createdAt(LocalDateTime.now())
                .enable(true)
                .build();

        userRepository.save(user);
        
        if (user.getRoles() == com.project.nagarSetu.util.enums.Roles.WORKER) {
            com.project.nagarSetu.entity.Worker worker = com.project.nagarSetu.entity.Worker.builder()
                    .id(user.getId())
                    .user(user)
                    .build();
            workerRepository.save(worker);
        } else if (user.getRoles() == com.project.nagarSetu.util.enums.Roles.SUPERVISOR) {
            com.project.nagarSetu.entity.Supervisior supervisior = com.project.nagarSetu.entity.Supervisior.builder()
                    .id(user.getId())
                    .user(user)
                    .build();
            supervisiorRepository.save(supervisior);
        }

        log.info("User is created with email : {}({})", requestDto.getEmail(), requestDto.getRole());

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail(), user.getRoles().toString());

        return RegisterResponse
                .builder()
                .id(user.getId())
                .token(token)
                .build();
    }

    public LoginResponseDto getLogin(LoginRequestDto dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));

        if (authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException(dto.getEmail());
        }
        UserDetail userDetails = (UserDetail) authentication.getPrincipal();
        User user = userDetails.getUser();

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail(), user.getRoles().toString());

        return LoginResponseDto
                .builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .token(token)
                .build();
    }

}
