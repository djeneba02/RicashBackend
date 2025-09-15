package com.ricash.ricash.model;

import com.ricash.ricash.model.Enum.typeDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentIdentite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private typeDocument type;

    private String numero;

    @Temporal(TemporalType.DATE)
    private Date dateExpiration;

    private String imageRectoUrl;

    private String imageVersoUrl;

    private LocalDateTime dateValidation;

    // Relation avec User
    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private User utilisateur;

}
