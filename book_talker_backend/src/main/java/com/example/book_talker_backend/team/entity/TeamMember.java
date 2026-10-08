package com.example.book_talker_backend.team.entity;

import com.example.book_talker_backend.user.entity.OAuth2UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "TeamMember",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_team_member_provider_id_team_id",
        columnNames = {"provider_id", "team_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@ToString 
public class TeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long teamMemberId;
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
    @ManyToOne
    @JoinColumn(name = "provider_id", referencedColumnName = "provider_id", nullable = false)
    private OAuth2UserEntity oAuth2User;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    MemberRoleEnum role;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    MemberStatusEnum status;
    String displayName;
    LocalDateTime joinedAt;

    public TeamMember(Team team, OAuth2UserEntity oAuth2User, MemberRoleEnum role, MemberStatusEnum status, String displayName) {
        this.team = team;
        this.oAuth2User = oAuth2User;
        this.role = role;
        this.status = status;
        this.displayName = displayName;
        this.joinedAt = LocalDateTime.now();
    }
}
