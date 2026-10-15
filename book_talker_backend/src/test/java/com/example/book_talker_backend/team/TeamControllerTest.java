package com.example.book_talker_backend.team;

import com.example.book_talker_backend.config.SecurityConfig;
import com.example.book_talker_backend.exception.GlobalExceptionHandler;
import com.example.book_talker_backend.team.entity.dto.TeamPreviewResponse;
import com.example.book_talker_backend.team.exception.NotFoundInviteCodeException;
import com.example.book_talker_backend.team.service.TeamService;
import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import com.example.book_talker_backend.user.dao.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 보안 설정(SecurityConfig)까지 포함해 비로그인 요청을 검증한다. 인증 사용자(@WithMockUser 등) 없이 요청한다.
@WebMvcTest(TeamController.class)
class TeamControllerTest {

    // 메인 클래스(BookTalkerBackendApplication) 대신 쓰는 테스트 전용 설정.
    // 메인 클래스의 @EnableJpaRepositories가 슬라이스 테스트에서도 레포지토리를 등록해 entityManagerFactory를 요구하므로,
    // 필요한 빈만 직접 등록한다. 컴포넌트 스캔을 하지 않아 WebConfig 등 무관한 빈도 올라오지 않는다.
    @SpringBootConfiguration
    @Import({TeamController.class, SecurityConfig.class, GlobalExceptionHandler.class})
    static class TestConfig {
    }

    @Autowired MockMvc mockMvc;

    @MockitoBean TeamService teamService;

    // SecurityConfig 의존성
    @MockitoBean OAuth2UserRepository oAuth2UserRepository;
    @MockitoBean UserRepository userRepository;

    @Test
    void 모임_미리보기_비로그인_사용자도_조회할_수_있다() throws Exception {
        given(teamService.getTeamPreview("aB3dE5fG7hJ9k"))
                .willReturn(new TeamPreviewResponse("말랑콩떡", "말랑콩떡 독서 동호회"));

        mockMvc.perform(get("/teams/join/{code}", "aB3dE5fG7hJ9k"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamName").value("말랑콩떡"))
                .andExpect(jsonPath("$.teamDesc").value("말랑콩떡 독서 동호회"));
    }

    @Test
    void 모임_미리보기_없는_코드면_404() throws Exception {
        given(teamService.getTeamPreview(anyString()))
                .willThrow(new NotFoundInviteCodeException("유효한 코드가 아닙니다"));

        mockMvc.perform(get("/teams/join/{code}", "notexist"))
                .andExpect(status().isNotFound());
    }

    // 미리보기는 GET만 열었으므로 같은 경로의 가입은 로그인이 필요해야 한다
    @Test
    void 초대코드_가입_비로그인이면_로그인으로_리다이렉트() throws Exception {
        mockMvc.perform(post("/teams/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "aB3dE5fG7hJ9k", "displayName": "북토커"}
                                """))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/naver"));

        verifyNoInteractions(teamService);
    }
}
