package com.danamon.autochain.service.impl;

import com.danamon.autochain.constant.ActorType;
import com.danamon.autochain.constant.RoleType;
import com.danamon.autochain.dto.FileResponse;
import com.danamon.autochain.dto.company.*;
import com.danamon.autochain.entity.*;
import com.danamon.autochain.repository.*;
import com.danamon.autochain.security.BCryptUtil;
import com.danamon.autochain.service.CompanyFileService;
import com.danamon.autochain.service.CompanyService;
import com.danamon.autochain.util.MailSender;
import com.danamon.autochain.util.RandomPasswordUtil;
import com.danamon.autochain.util.ValidationUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;
    private final ValidationUtil validationUtil;
    private final CompanyFileService companyFileService;
    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;
    private final RandomPasswordUtil randomPasswordUtil;
    private final CredentialRepository credentialRepository;
    private final UserRolesRepository userRolesRepository;
    private final BCryptUtil bCryptUtil;

    @Override
    public List<CompanyResponse> getNonPartnership(String companyId) {
        Company company = findByIdOrThrowNotFound(companyId);
        List<Company> companies = companyRepository.findAll();

        List<String> existingPartnershipCompanies = company.getPartnerships().stream()
                .map(partnership -> partnership.getPartner().getCompany_id())
                .collect(Collectors.toList());

        List<Company> filteredCompanies = companies.stream()
                .filter(c -> !c.getCompany_id().equals(company.getCompany_id()))
                .filter(c -> !existingPartnershipCompanies.contains(c.getCompany_id()))
                .collect(Collectors.toList());

        return filteredCompanies.stream().map(c -> mapToResponse(c)).collect(Collectors.toList());
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public CompanyResponse create(NewCompanyRequest request) {
        try {
            validationUtil.validate(request);
            List<CompanyFile> companyFiles = request.getMultipartFiles().stream().map(multipartFile ->
                    companyFileService.createFile(multipartFile)
            ).collect(Collectors.toList());

            String id = "";
            try {
                id = generateCompanyId(request);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company name format invalid");
            }

            Company company = Company.builder()
                    .company_id(id)
                    .companyName(request.getCompanyName())
                    .province(request.getProvince())
                    .city(request.getCity())
                    .address(request.getAddress())
                    .phoneNumber(request.getPhoneNumber())
                    .companyEmail(request.getCompanyEmail())
                    .accountNumber(request.getAccountNumber())
                    .financingLimit(request.getFinancingLimit())
                    .remainingLimit(request.getRemainingLimit())
                    .companyFiles(companyFiles)
                    .build();

                    Company companySaved = companyRepository.saveAndFlush(company);

            String password = randomPasswordUtil.generateRandomPassword(12);

            try {
                Credential credential = Credential.builder()
                        .email(request.getEmailUser())
                        .username(request.getUsername())
                        .password(bCryptUtil.hashPassword(password))
                        .actor(ActorType.USER)
                        .build();

                credentialRepository.saveAndFlush(credential);

                User user = User.builder()
                        .company(companySaved)
                        .credential(credential)
                        .build();

                userRepository.saveAndFlush(user);
                companySaved.setUser(user);

                Roles role = rolesRepository.findByRoleName(RoleType.SUPER_USER.toString()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ROLE not found"));
                UserRole userRole = UserRole.builder()
                        .role(role)
                        .credential(credential)
                        .build();

                credential.setRoles(List.of(userRole));

                userRolesRepository.saveAndFlush(userRole);
            }  catch (DataIntegrityViolationException e){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Email must be unique");
            }

            HashMap<String, String> info = new HashMap<>();

            try {
                String accountEmail = "<html style='width: 100%;'>" +
                        "<body style='width: 100%'>" +
                        "<div style='width: 100%;'>" +
                        "<header style='color:white; width: 100%; background: #F6833C; padding: 12px 10px; top:0;'>" +
                        "<span><h2 style='text-align: center;'>D-Auto Chain</h2></span>" +
                        "</header>" +
                        "<div style='margin: auto;'>" +
                        "<div><h5><center>Your Account</u></center></h5></div><br>" +
                        "<div><h4><center>Email: "+request.getEmailUser()+"</center></h4></div><br>" +
                        "</div>" +
                        "<div><h4><center>Password: "+password+"</center></h4></div><br>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>";

                info.put("emailBody", accountEmail);

                MailSender.mailer("Here Your Company Super Account", info, request.getEmailUser());
            }  catch (Exception e){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return mapToResponse(companySaved);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private String generateCompanyId(NewCompanyRequest request) {
        String kode = "";
        String companyCode = "";
        while (true) {
            String[] words = request.getCompanyName().split(" ");
            companyCode = words[1].substring(0, 3).toUpperCase();
            int min = 1;
            int max = 999;

            int randomNumber = (int) (Math.random() * (max - min + 1)) + min;

            kode = String.format("%03d", randomNumber);

            Optional<Company> availableCompany = companyRepository.findById(companyCode + kode);
            if(availableCompany.isEmpty()) {
                break;
            }
        }

        return companyCode + kode;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CompanyResponse update(UpdateCompanyRequest request) {
        try {
            validationUtil.validate(request);

            Company companyOld = findByIdOrThrowNotFound(request.getId());
            companyOld.setCompanyName(request.getCompanyName());
            companyOld.setProvince(request.getProvince());
            companyOld.setCity(request.getCity());
            companyOld.setAddress(request.getAddress());
            companyOld.setPhoneNumber(request.getPhoneNumber());
            companyOld.setCompanyEmail(request.getCompanyEmail());
            companyOld.setAccountNumber(request.getAccountNumber());
            Company company = companyRepository.saveAndFlush(companyOld);

            if (request.getIsGeneratePassword()) {
                Credential credential = credentialRepository.findByEmail(companyOld.getUser().getCredential().getEmail()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

                String newPassword = randomPasswordUtil.generateRandomPassword(12);

                credential.setPassword(bCryptUtil.hashPassword(newPassword));
                credential.setModifiedDate(LocalDateTime.now());
                credentialRepository.saveAndFlush(credential);

                HashMap<String, String> info = new HashMap<>();

                try {
                    String accountEmail = "<html style='width: 100%;'>" +
                            "<body style='width: 100%'>" +
                            "<div style='width: 100%;'>" +
                            "<header style='color:white; width: 100%; background: #F6833C; padding: 12px 10px; top:0;'>" +
                            "<span><h2 style='text-align: center;'>D-Auto Chain</h2></span>" +
                            "</header>" +
                            "<div style='margin: auto;'>" +
                            "<div><h5><center>Your Account</u></center></h5></div><br>" +
                            "<div><h4><center>Email: "+companyOld.getUser().getCredential().getEmail()+"</center></h4></div><br>" +
                            "</div>" +
                            "<div><h4><center>Password: "+newPassword+"</center></h4></div><br>" +
                            "</div>" +
                            "</div>" +
                            "</body>" +
                            "</html>";

                    info.put("emailBody", accountEmail);

                    MailSender.mailer("New Password for Company Account", info, companyOld.getUser().getCredential().getEmail());
                }  catch (Exception e){
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            if (request.getMultipartFiles() != null) {
                List<CompanyFile> companyFiles = request.getMultipartFiles().stream().map(multipartFile ->
                        companyFileService.createFile(multipartFile)
                ).collect(Collectors.toList());

                if(companyOld.getCompanyFiles().size() != 0) {
                    List<CompanyFile> oldFiles = new ArrayList<>(companyOld.getCompanyFiles());
                    companyFiles.addAll(oldFiles);
                }

                company.setCompanyFiles(companyFiles);
            }

            return mapToResponse(company);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Page<CompanyResponse> getAll(SearchCompanyRequest request) {
        Specification<Company> specification = getCompanySpecification(request);
        Sort.Direction direction = Sort.Direction.fromString(request.getDirection());
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), direction, request.getSortBy());
        Page<Company> companies = companyRepository.findAll(specification, pageable);
        return companies.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Company getById(String id) {
        return findByIdOrThrowNotFound(id);
    }

    @Transactional(readOnly = true)
    @Override
    public CompanyResponse findById(String id) {
        Company company = findByIdOrThrowNotFound(id);
        return mapToResponse(company);
    }

    private Company findByIdOrThrowNotFound(String id) {
        return companyRepository.findById(id).orElseThrow(() -> new RuntimeException("company not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Resource getCompanyFilesByIdFile(String idFile) {
        CompanyFile companyFile = companyFileService.findById(idFile);
        return companyFileService.findByPath(companyFile.getPath());
    }

    private CompanyResponse mapToResponse(Company company) {
        List<FileResponse> fileResponses = company.getCompanyFiles().stream().map(
                companyFiles -> FileResponse.builder()
                        .filename(companyFiles.getName())
                        .url("/api/companies/" + companyFiles.getId() + "/file")
                        .build()
        ).collect(Collectors.toList());

        return CompanyResponse.builder()
                .companyId(company.getCompany_id())
                .companyName(company.getCompanyName())
                .province(company.getProvince())
                .city(company.getCity())
                .address(company.getAddress())
                .phoneNumber(company.getPhoneNumber())
                .companyEmail(company.getCompanyEmail())
                .accountNumber(company.getAccountNumber())
                .financingLimit(company.getFinancingLimit())
                .reaminingLimit(company.getRemainingLimit())
                .username(company.getUser().getCredential().getUsername())
                .emailUser(company.getUser().getCredential().getEmail())
                .files(fileResponses)
                .build();
    }

    private Specification<Company> getCompanySpecification(SearchCompanyRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getName() != null) {
                Predicate name = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("companyName")),
                        "%" + request.getName().toLowerCase() + "%"
                );
                predicates.add(name);
            }

            return query
                    .where(predicates.toArray(new Predicate[]{}))
                    .getRestriction();
        };
    }
}
