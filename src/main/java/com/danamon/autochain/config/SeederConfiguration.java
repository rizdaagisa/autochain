package com.danamon.autochain.config;

import com.danamon.autochain.constant.ActorType;
import com.danamon.autochain.constant.RoleType;
import com.danamon.autochain.entity.*;
import com.danamon.autochain.repository.*;
import com.danamon.autochain.security.BCryptUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SeederConfiguration implements CommandLineRunner {
    private final CredentialRepository credentialRepository;
    private final BackOfficeRepository backOfficeRepository;
    private final CompanyRepository companyRepository;
    private final RolesRepository rolesRepository;
    private final UserRepository userRepository;
    private final UserRolesRepository userRolesRepository;
    private final BCryptUtil bCryptUtil;

//   ============================== SUPER ADMIN ===================================
    private final String email = "rizdaagisa@gmail.com";
    private final String username = "rizda";
    private final String password = "string";

//    ============================ SUPER USER =====================================
    private final String superUserEmail = "oreofinalprojectdtt@gmail.com";
    private final String superUserUsername = "oreo";
    private final String superUserPassword = "test12345";

    @Override
    public void run(String... args) {
        Optional<Credential> byUsername = credentialRepository.findByEmail(email);
        if (byUsername.isEmpty()) {
            rolesSeeder();
            companySeeder();
            backofficeSeeder();
            userSeeder();
        }
    }

    public void backofficeSeeder() {
        Roles superadmin = rolesRepository.findByRoleName("SUPER_ADMIN").orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "roles not exist"));

        List<UserRole> role = new ArrayList<>();
        // Create and save seed data for Credential entity
        Credential adminCredential = new Credential();
        adminCredential.setEmail(email);
        adminCredential.setUsername(username);
        adminCredential.setPassword(bCryptUtil.hashPassword(password));
        adminCredential.setActor(ActorType.BACKOFFICE);
        adminCredential.setRoles(role);
        adminCredential.setModifiedDate(LocalDateTime.now());
        adminCredential.setCreatedDate(LocalDateTime.now());
        adminCredential.setCreatedBy(username);
        adminCredential.setModifiedBy(username);

        BackOffice backOffice = new BackOffice();
        backOffice.setCredential(adminCredential);

        role.add(
                UserRole.builder()
                        .role(superadmin)
                        .credential(adminCredential)
                        .build()
        );

        credentialRepository.saveAndFlush(adminCredential);
        backOfficeRepository.saveAndFlush(backOffice);

    }

    public void userSeeder(){
        Roles superUser = rolesRepository.findByRoleName("SUPER_USER").orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "roles not exist"));
        Company company = companyRepository.findBycompanyName("root").orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "company ID not found"));

        List<UserRole> roles = new ArrayList<>();

        Credential userCredential = Credential.builder()
                .email(superUserEmail)
                .username(superUserUsername)
                .password(bCryptUtil.hashPassword(superUserPassword))
                .actor(ActorType.BACKOFFICE)
                .modifiedDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now())
                .roles(roles)
                .createdBy("oreo")
                .modifiedBy("oreo")
                .build();


        User user = new User();
        user.setCompany(company);
        user.setCredential(userCredential);

        List<UserRole> roleUser = new ArrayList<>();
        roleUser.add(
                UserRole.builder()
                        .role(superUser)
                        .credential(userCredential)
                        .build()
        );

        userCredential.setRoles(roleUser);

        userRepository.saveAndFlush(user);
        credentialRepository.saveAndFlush(userCredential);
    }

    public void companySeeder() {
        Company company = new Company();
        company.setCompany_id("0");
        company.setCompanyName("root");
        company.setCompanyEmail("root");
        company.setCity("root");
        company.setAddress("root");
        company.setAccountNumber("root");
        company.setFinancingLimit(12313d);
        company.setRemainingLimit(123513d);
        company.setPhoneNumber("root");
        company.setProvince("root");

        companyRepository.saveAndFlush(company);
    }

    public void rolesSeeder() {
        List<Roles> roles = rolesRepository.findAll();

        if (roles.isEmpty()) {
            List<Roles> allRole = List.of(
//                ====================== BACK OFFICE ROLE ===============
                    Roles.builder().roleName(RoleType.SUPER_ADMIN.getName()).build(),
                    Roles.builder().roleName(RoleType.ADMIN.getName()).build(),
                    Roles.builder().roleName(RoleType.RELATIONSHIP_MANAGER.getName()).build(),
                    Roles.builder().roleName(RoleType.CREDIT_ANALYST.getName()).build(),
//                ========================= USER ROLE ====================
                    Roles.builder().roleName(RoleType.SUPER_USER.getName()).build(),
                    Roles.builder().roleName(RoleType.FINANCE_STAFF.getName()).build(),
                    Roles.builder().roleName(RoleType.INVOICE_STAFF.getName()).build(),
                    Roles.builder().roleName(RoleType.PAYMENT_STAFF.getName()).build()
            );
            rolesRepository.saveAllAndFlush(allRole);
        }
    }
}
