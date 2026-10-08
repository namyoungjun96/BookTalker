package com.example.book_talker_backend.team.service;

import com.example.book_talker_backend.team.dao.TeamMemberRepository;
import com.example.book_talker_backend.team.dao.TeamRepository;
import com.example.book_talker_backend.team.exception.InviteCodeGenerationFailedException;
import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamServiceUnitTest {
    @Mock TeamRepository teamRepository;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock OAuth2UserRepository oAuth2UserRepository;

    @Test
    void 초대코드_3회_모두_충돌하면_500() {
        TeamService service = spy(new TeamService(teamRepository, teamMemberRepository, oAuth2UserRepository));
        doReturn("DUPLICATECODE").when(service).generateRandomCode();
        given(teamRepository.existsByCode("DUPLICATECODE")).willReturn(true);

        assertThatThrownBy(service::issueUniqueCode)
                .isInstanceOf(InviteCodeGenerationFailedException.class);
        verify(teamRepository, times(3)).existsByCode("DUPLICATECODE");
    }
}
