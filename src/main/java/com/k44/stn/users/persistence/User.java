package com.k44.stn.users.persistence;

import com.k44.stn.common.persistence.AuditedEntity;
import com.k44.stn.users.domain.AccountStatus;
import com.k44.stn.users.domain.SystemRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditedEntity {

    public void delete(){
        this.accountStatus = AccountStatus.DELETED;
    }

    public void block(){
        this.accountStatus = AccountStatus.BLOCKED;
    }

    public void changePasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }

    public void markDead(){
        this.isAlive = true;
    }

    public void markAlive(){
        this.isAlive = true;
    }

    public void changeAvatarIcon(String avatarIcon){
        this.avatarIcon = avatarIcon;
    }

    public void changeNickname(String nickname){
        this.nickname = nickname;
    }

    public User(String email, String nickname, String passwordHash) {
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.systemRole = SystemRole.USER;
        this.accountStatus = AccountStatus.ACTIVE;
        this.isAlive = true;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "nickname", nullable = false, length = 64)
    private String nickname;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false)
    private SystemRole systemRole = SystemRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "avatar_icon", length = 64)
    private String avatarIcon;

    @Column(name = "is_alive", nullable = false)
    private boolean isAlive = true;

}
