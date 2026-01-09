package com.mopl.domain.user.entity;

import com.mopl.domain.user.enums.Role;
import com.mopl.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name  = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = true, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Column (name = "locked", nullable = false)
    private Boolean locked;

    @Column(name = "profile_image_url", nullable = true)
    private String profileImageUrl;

    @Column(name = "provider", nullable = true)
    private String provider;

    @Column(name  = "provider_id", nullable = true)
    private String providerId;

    public User(String name,String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = Role.USER;
        this.locked = false;
    }
    public void updateRole(Role role){
        this.role = role;
    }
}
