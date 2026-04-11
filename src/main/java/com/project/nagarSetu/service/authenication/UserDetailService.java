package com.project.nagarSetu.service.authenication;

import com.project.nagarSetu.entity.User;
import com.project.nagarSetu.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class UserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            User user = userRepository.findByEmail(username);
            if(user == null){
                throw new UsernameNotFoundException(username);
            }
            return new UserDetail(user);
        } catch (Exception e) {
            log.error("User Detail Service : {} " ,e.getMessage());
            throw new UsernameNotFoundException(username);
        }
    }

    public UserDetails loadUserById(UUID userId) {
        try {
            User userProfile = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException(userId.toString()));

            return new UserDetail(userProfile);

        } catch (Exception e) {
            log.error("User Detail Service : {} " ,e.getMessage());
            throw new UsernameNotFoundException(userId.toString());
        }
    }
}
