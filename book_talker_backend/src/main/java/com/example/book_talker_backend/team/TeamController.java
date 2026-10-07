package com.example.book_talker_backend.team;

import com.example.book_talker_backend.team.entity.dto.*;
import com.example.book_talker_backend.team.service.TeamService;
import jakarta.validation.Valid;
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
@RequestMapping("/teams")
@Slf4j
public class TeamController {
    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<CreateTeamResponse> createTeam(@RequestBody @Valid CreateTeamRequest request,
                                                         @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String providerId = (String) response.get("id");

        return new ResponseEntity<>(teamService.createTeam(request, providerId), HttpStatus.CREATED);
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
    public ResponseEntity<List<TeamListResponse>> getMyTeams(@AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String providerId = (String) response.get("id");

        List<TeamListResponse> teams = teamService.getMyTeams(providerId);

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
