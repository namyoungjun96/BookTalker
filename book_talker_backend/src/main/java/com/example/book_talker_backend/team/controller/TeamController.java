package com.example.book_talker_backend.team.controller;

import com.example.book_talker_backend.team.entity.Team;
import com.example.book_talker_backend.team.entity.dto.CreateTeamRequest;
import com.example.book_talker_backend.team.entity.dto.JoinByInviteCodeRequest;
import com.example.book_talker_backend.team.entity.dto.UpdateTeamRequest;
import com.example.book_talker_backend.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


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

    @GetMapping
    public ResponseEntity<List<Team>> getMyTeams(@AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String providerId = (String) response.get("id");

        List<Team> teams = teamService.getMyTeams(providerId);

        return new ResponseEntity<>(teams, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateTeam(@PathVariable Long id,
        @RequestBody UpdateTeamRequest request,
        @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String providerId = (String) response.get("id");

        teamService.updateTeam(id, request, providerId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id,
        @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String providerId = (String) response.get("id");

        teamService.deleteTeam(id, providerId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
