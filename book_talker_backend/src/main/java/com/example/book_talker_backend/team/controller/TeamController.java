package com.example.book_talker_backend.team.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.book_talker_backend.team.entity.dto.CreateTeamRequest;
import com.example.book_talker_backend.team.entity.dto.JoinByInviteCodeRequest;
import com.example.book_talker_backend.team.service.TeamService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController 
@RequiredArgsConstructor 
@RequestMapping("/team")
@Slf4j 
public class TeamController {
    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<Void> createTeam(@RequestBody CreateTeamRequest request,
        @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String providerId = (String) response.get("id");
        
        teamService.createTeam(request, providerId);
        
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{teamId}/code")
    public ResponseEntity<String> createInviteCode(@PathVariable Long teamId,
        @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String providerId = (String) response.get("id");

        String code = teamService.createInviteCode(teamId, providerId);
        
        return new ResponseEntity<>(code, HttpStatus.OK);
    }

    @PostMapping("/code")
    public ResponseEntity<Void> joinTeamByInviteCode(@RequestBody JoinByInviteCodeRequest request,
        @AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String providerId = (String) response.get("id");

        teamService.joinTeamByInviteCode(request, providerId);
        
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
}
