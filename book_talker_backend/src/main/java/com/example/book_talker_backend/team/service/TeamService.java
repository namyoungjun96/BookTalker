package com.example.book_talker_backend.team.service;

public class TeamService {
    public void createTeam(String teamName, String teamDescription, String providerId) {}
    public void getMyTeams(String providerId) {}
    public void updateTeam(Long teamId, String teamName, String teamDescription, String providerId) {}
    public void deleteTeam(Long teamId, String providerId) {} 
    public void addMember(Long teamId, String actorProviderId, String requestProviderId) {}
    public void removeMember(Long teamId, Long teamMemberId, String providerId) {}
    private void leaveTeam(Long teamId, Long teamMemberId) {}
    private void kickMember(Long teamId, Long teamMemberId, String requestProviderId) {}
}
