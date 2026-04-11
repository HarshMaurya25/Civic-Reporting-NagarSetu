package com.project.nagarSetu.controller;

import com.project.nagarSetu.error.exception.AccessDeniedUserException;
import com.project.nagarSetu.service.authenication.UserDetail;
import com.project.nagarSetu.service.issue.IssueService;
import com.project.nagarSetu.service.user.UserService;
import com.project.nagarSetu.util.dto.user.UserLeaderboardDto;
import com.project.nagarSetu.util.dto.user.UserMatrixDto;
import com.project.nagarSetu.util.dto.user.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.AccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.security.sasl.AuthenticationException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final IssueService issueService;

    @GetMapping("/get")
    public ResponseEntity<UserResponseDto> getProfile(@RequestParam UUID id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail userDetail = (UserDetail) authentication.getPrincipal();
        if(!userDetail.getUser().getId().equals(id)){
            throw new AccessDeniedUserException(id.toString());
        }
        return ResponseEntity.ok().body(userService.getUser(id));
    }

    @GetMapping("/getMatrix")
    public ResponseEntity<UserMatrixDto> getMatrix(@RequestParam UUID id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail userDetail = (UserDetail) authentication.getPrincipal();
        if(!userDetail.getUser().getId().equals(id)){
            throw new AccessDeniedUserException(id.toString());
        }
        return ResponseEntity.ok().body(issueService.getMatrix(id));
    }

    @GetMapping("/getLeaderboard")
    public ResponseEntity<List<UserLeaderboardDto>> getLeaderBoard(){
        List<UserLeaderboardDto> response = issueService.getLeaderBoard();
        return new ResponseEntity<>(response , HttpStatus.OK);
    }

}
