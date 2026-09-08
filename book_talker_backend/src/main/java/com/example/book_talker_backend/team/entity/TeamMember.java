package com.example.book_talker_backend.team.entity;

import java.time.LocalDateTime;

import com.example.book_talker_backend.user.entity.OAuth2UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class TeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long teamMemberId;
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
    @ManyToOne
    @JoinColumn(name = "provider_id", referencedColumnName = "provider_id")
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
