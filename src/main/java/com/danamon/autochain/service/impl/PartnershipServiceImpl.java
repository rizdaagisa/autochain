package com.danamon.autochain.service.impl;

import com.danamon.autochain.constant.ActorType;
import com.danamon.autochain.constant.PartnershipStatus;
import com.danamon.autochain.dto.company.CompanyResponse;
import com.danamon.autochain.dto.company.NewCompanyRequest;
import com.danamon.autochain.dto.partnership.NewPartnershipRequest;
import com.danamon.autochain.dto.partnership.PartnershipResponse;
import com.danamon.autochain.dto.partnership.SearchPartnershipRequest;
import com.danamon.autochain.entity.*;
import com.danamon.autochain.repository.PartnershipRepository;
import com.danamon.autochain.service.CompanyService;
import com.danamon.autochain.service.PartnershipService;
import com.danamon.autochain.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnershipServiceImpl implements PartnershipService {
    private final PartnershipRepository partnershipRepository;
    private final ValidationUtil validationUtil;
    private final CompanyService companyService;

    @Override
    public String rejectPartnership(String partnershipId) {
        Partnership partnership = findByIdOrThrowNotFound(partnershipId);

        partnershipRepository.delete(partnership);

        return "Rejected partnership";
    }

    @Override
    public PartnershipResponse acceptPartnership(String partnershipId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Credential userCredential = (Credential) authentication.getPrincipal();

        Partnership partnership = findByIdOrThrowNotFound(partnershipId);
        partnership.setPartnerStatus(PartnershipStatus.IN_PARTNER);
        partnership.setPartnerConfirmationDate(LocalDateTime.now());
        partnership.setConfirmedBy(userCredential);
        Partnership partnershipUpdated = partnershipRepository.saveAndFlush(partnership);

        return mapToResponse(partnershipUpdated);
    }

    private Partnership findByIdOrThrowNotFound(String id) {
        return partnershipRepository.findById(id).orElseThrow(() -> new RuntimeException("partnerships not found"));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public PartnershipResponse addPartnership(NewPartnershipRequest request) {
        try {
            validationUtil.validate(request);

            Company company = companyService.getById(request.getCompanyId());
            Company partner = companyService.getById(request.getPartnerId());

            String id = "CP-" + request.getCompanyId() + "-" +request.getPartnerId();
            // check by id partnership
            Optional<Partnership> partnershipByPartnershipNo = partnershipRepository.findPartnershipByPartnershipNo(id);
            if (partnershipByPartnershipNo.isPresent()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Partnership already exist");

            // check by reverse company and partner (indicator partner)
            Page<Partnership> allByPartnerId = partnershipRepository.findAllByCompanyId(request.getPartnerId(), null);
            Optional<Partnership> partnershipByPartner = allByPartnerId.stream().findFirst().filter(partnership -> partnership.getPartner().getCompany_id() == request.getCompanyId());
            if (partnershipByPartner.isPresent()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Partnership already exist");

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }

            Credential userCredential = (Credential) authentication.getPrincipal();
            System.out.println();
            Partnership partnership = Partnership.builder()
                    .partnershipNo(id)
                    .company(company)
                    .partner(partner)
                    .partnerStatus(userCredential.getActor() == ActorType.BACKOFFICE ? PartnershipStatus.IN_PARTNER : PartnershipStatus.PENDING)
                    .partnerRequestedDate(LocalDateTime.now())
                    .partnerConfirmationDate(null)
                    .requestedBy(userCredential)
                    .confirmedBy(null)
                    .build();

            Partnership partnershipSaved = partnershipRepository.saveAndFlush(partnership);

            return mapToResponse(partnershipSaved);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    @Transactional(readOnly = true)
    @Override
    public Page<PartnershipResponse> getAll(String id, SearchPartnershipRequest request) {
        Sort.Direction direction = Sort.Direction.fromString(request.getDirection());
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), direction, request.getSortBy());
        Page<Partnership> parterships = partnershipRepository.findAllByCompanyId(id, pageable);
        return parterships.map(this::mapToResponse);
    }

    private PartnershipResponse mapToResponse(Partnership partnership) {
        return PartnershipResponse.builder()
                .partnershipId(partnership.getPartnershipNo())
                .company(
                        CompanyResponse.builder()
                                .companyId(partnership.getCompany().getCompany_id())
                                .companyName(partnership.getCompany().getCompanyName())
                                .province(partnership.getCompany().getProvince())
                                .city(partnership.getCompany().getCity())
                                .address(partnership.getCompany().getAddress())
                                .phoneNumber(partnership.getCompany().getPhoneNumber())
                                .companyEmail(partnership.getCompany().getCompanyEmail())
                                .accountNumber(partnership.getCompany().getAccountNumber())
                                .financingLimit(partnership.getCompany().getFinancingLimit())
                                .reaminingLimit(partnership.getCompany().getRemainingLimit())
                                .username(partnership.getCompany().getUser().getCredential().getUsername())
                                .emailUser(partnership.getCompany().getUser().getCredential().getEmail())
                                .files(null)
                                .build()
                )
                .partner(
                        CompanyResponse.builder()
                                .companyId(partnership.getPartner().getCompany_id())
                                .companyName(partnership.getPartner().getCompanyName())
                                .province(partnership.getPartner().getProvince())
                                .city(partnership.getPartner().getCity())
                                .address(partnership.getPartner().getAddress())
                                .phoneNumber(partnership.getPartner().getPhoneNumber())
                                .companyEmail(partnership.getPartner().getCompanyEmail())
                                .accountNumber(partnership.getPartner().getAccountNumber())
                                .financingLimit(partnership.getPartner().getFinancingLimit())
                                .reaminingLimit(partnership.getPartner().getRemainingLimit())
                                .username(partnership.getPartner().getUser().getCredential().getUsername())
                                .emailUser(partnership.getPartner().getUser().getCredential().getEmail())
                                .files(null)
                                .build()
                )
                .partnerStatus(partnership.getPartnerStatus().toString())
                .partnerRequestedDate(partnership.getPartnerRequestedDate().toString())
                .partnerConfirmationDate(partnership.getPartnerConfirmationDate() != null ? partnership.getPartnerConfirmationDate().toString() : null)
                .requestedBy(partnership.getRequestedBy().getCredentialId())
                .confirmedBy(partnership.getConfirmedBy() != null ? partnership.getConfirmedBy().getCredentialId() : null)
                .build();
    }
}
