package com.danamon.autochain.entity;

import com.danamon.autochain.constant.UserRoleType;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Table;
import jakarta.persistence.*;
import java.util.Collection;
import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Data
@Table(name = "m_user")
public class User {
    @Id
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @GeneratedValue(generator = "uuid")
    @Column(name = "user_id", length = 128, nullable = false)
    private String user_id;

    @OneToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company_id;

    @Column(name = "username", nullable = false, length =128, unique = true)
    private String username;

    @Column(name = "email",nullable = false, length =128, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length =128)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length =128)
    private UserRoleType user_type;

}
