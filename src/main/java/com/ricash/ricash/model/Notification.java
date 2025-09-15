package com.ricash.ricash.model;

import com.ricash.ricash.model.Enum.typeNotification;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private typeNotification type;

    private String contenu;

    private String destinataire;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateEnvoi;

    private boolean estLue;

    // Relation avec User
    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private User utilisateur;
}
