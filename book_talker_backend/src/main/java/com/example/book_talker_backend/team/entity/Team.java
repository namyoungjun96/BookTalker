package com.example.book_talker_backend.team.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Team")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long teamId;
    String teamName;
    String teamDescription;
    @CreatedDate
    LocalDateTime createdAt;
    @LastModifiedDate 
    LocalDateTime updatedAt;
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    List<TeamMember> teamMembers = new ArrayList<>();
    @Column(length = 13, unique = true, nullable = false)
    String code;
}
