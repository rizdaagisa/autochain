package com.danamon.autochain.entity;

import com.danamon.autochain.constant.PartnershipStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "t_partnership")
public class Partnership {
    @Id
//    @GenericGenerator(name = "uuid", strategy = "uuid")
//    @GeneratedValue(generator = "uuid")
    @Column(name = "partnership_no")
    private String partnershipNo;

    @ManyToOne
    @JoinColumn(name = "company_id")
    @JsonBackReference
    private Company company;

    @ManyToOne
    @JoinColumn(name = "partner_id")
    @JsonBackReference
    private Company partner;

    @Column(name = "partner_status", length = 128, nullable = false)
    @Enumerated(EnumType.STRING)
    private PartnershipStatus partnerStatus;

    @Column(name = "partner_requested_date", length = 128, nullable = false)
    private LocalDateTime partnerRequestedDate;

    @Column(name = "partner_confirmation_date", length = 128)
    private LocalDateTime partnerConfirmationDate;

//    @OneToOne
//    @JoinColumn(name= "requested_by")
    @ManyToOne
    @JoinColumn(name = "requested_by")
    @JsonBackReference
    private Credential requestedBy;

//    @OneToOne
//    @JoinColumn(name= "confirmed_by")
    @ManyToOne
    @JoinColumn(name = "confirmed_by")
    @JsonBackReference
    private Credential confirmedBy;
}
