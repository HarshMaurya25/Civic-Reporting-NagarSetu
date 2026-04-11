package com.project.nagarSetu.service.authenication;

import com.project.nagarSetu.entity.User;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
public class UserDetail implements UserDetails {

    private User user;

    public UserDetail(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> simpleGrantedAuthorities = new HashSet<>();
        simpleGrantedAuthorities.add(new SimpleGrantedAuthority("ROLE_"+user.getRoles().toString()));
        return simpleGrantedAuthorities;
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.getEnable();
    }
}
