package com.project.nagarSetu.service.user;

import com.project.nagarSetu.entity.User;
import com.project.nagarSetu.repository.UserRepository;
import com.project.nagarSetu.service.redis.RedisService;
import com.project.nagarSetu.util.dto.user.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;

    public UserResponseDto getUser(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        User user = userRepository.findByUserId(id);
        if (user == null || Boolean.FALSE.equals(user.getEnable())) {
            throw new UsernameNotFoundException(id.toString());
        }

        return UserResponseDto
                .builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .roles(user.getRoles())
                .enable(user.getEnable())
                .age(user.getAge())
                .gender(user.getGender())
                .location(user.getLocation())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
