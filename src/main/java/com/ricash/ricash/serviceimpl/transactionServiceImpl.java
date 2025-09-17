package com.ricash.ricash.serviceimpl;

import com.ricash.ricash.model.*;
import com.ricash.ricash.model.Enum.statutTransaction;
import com.ricash.ricash.dto.TransferRequest;
import com.ricash.ricash.model.Enum.typeTransaction;
import com.ricash.ricash.repository.*;
import com.ricash.ricash.service.transactionService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
@Transactional

public class transactionServiceImpl implements transactionService {

    private final transactionRepository transactionRepository;
    private final userRepository userRepository;
    private final agentRepository agentRepository;
    private final walletRepository walletRepository;
    private final parametreSysRepository parametreSystemeRepository;
    private final beneficiaireRepository beneficiaireRepository;

    public transactionServiceImpl(transactionRepository transactionRepository, userRepository userRepository, agentRepository agentRepository, walletRepository walletRepository, parametreSysRepository parametreSystemeRepository, beneficiaireRepository beneficiaireRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.walletRepository = walletRepository;
        this.parametreSystemeRepository = parametreSystemeRepository;
        this.beneficiaireRepository = beneficiaireRepository;
    }

    @Transactional
    public Transaction initierTransfert(TransferRequest request) {
        // Validation des paramètres
        validerTransfert(request);

        // Calcul des frais et montant total
        ParametreSysteme parametres = parametreSystemeRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("Paramètres système non configurés"));

        double frais = calculerFrais(request.getMontant(), parametres);
        double montantTotal = request.getMontant() + frais;

        // Création de la transaction
        Transaction transaction = new Transaction();
        transaction.setReference(genererReference());
        transaction.setMontant(request.getMontant());
        transaction.setFrais(frais);
        transaction.setMontantTotal(montantTotal);
        transaction.setDevise(request.getDevise());
        transaction.setTauxChange(parametres.getTauxChange().doubleValue());
        transaction.setStatut(statutTransaction.INITIEE);
        transaction.setType(request.getType());
        transaction.setMethodePaiement(request.getMethodePaiement());
        transaction.setDateCreation(new Date());

        // Détermination de l'expéditeur et du destinataire selon le type
        configurerPartiesTransaction(transaction, request);

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction executerTransfert(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée"));

        if (transaction.getStatut() != statutTransaction.INITIEE) {
            throw new RuntimeException("Transaction déjà traitée");
        }

        try {
            transaction.setStatut(statutTransaction.EN_COURS);
            transactionRepository.save(transaction);

            // Débiter l'expéditeur
            debiterExpediteur(transaction);

            // Créditer le destinataire
            crediterDestinataire(transaction);

            transaction.setStatut(statutTransaction.COMPLETEE);
            transaction.setDateCompletion(new Date());

        } catch (Exception e) {
            transaction.setStatut(statutTransaction.ECHOUEE);
            throw new RuntimeException("Échec du transfert: " + e.getMessage());
        }

        return transactionRepository.save(transaction);
    }

    private void validerTransfert(TransferRequest request) {
        // Validation du montant
        if (request.getMontant() <= 0) {
            throw new RuntimeException("Le montant doit être positif");
        }

        // Validation selon le type de transaction
        switch (request.getType()) {
            case CLIENT_TO_CLIENT:
                if (request.getExpediteurId() == null || request.getDestinataireId() == null) {
                    throw new RuntimeException("Expéditeur et destinataire requis");
                }
                break;
            case CLIENT_TO_AGENT:
            case AGENT_TO_CLIENT:
                if (request.getExpediteurId() == null || request.getAgentId() == null) {
                    throw new RuntimeException("Expéditeur et agent requis");
                }
                break;
            case AGENT_TO_AGENT:
                if (request.getAgentExpediteurId() == null || request.getAgentDestinataireId() == null) {
                    throw new RuntimeException("Agents expéditeur et destinataire requis");
                }
                break;
        }
    }

    private void configurerPartiesTransaction(Transaction transaction, TransferRequest request) {
        switch (request.getType()) {
            case CLIENT_TO_CLIENT:
                User expediteur = userRepository.findById(request.getExpediteurId())
                        .orElseThrow(() -> new RuntimeException("Expéditeur non trouvé"));
                User destinataire = userRepository.findById(request.getDestinataireId())
                        .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));
                transaction.setExpediteur(expediteur);
                transaction.setDestinataire(destinataire);
                break;

            case CLIENT_TO_AGENT:
                User client = userRepository.findById(request.getExpediteurId())
                        .orElseThrow(() -> new RuntimeException("Client non trouvé"));
                Agent agent = agentRepository.findById(request.getAgentId())
                        .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
                transaction.setExpediteur(client);
                transaction.setAgent(agent);
                break;

            case AGENT_TO_CLIENT:
                Agent agentExp = agentRepository.findById(request.getAgentId())
                        .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
                User clientDest = userRepository.findById(request.getDestinataireId())
                        .orElseThrow(() -> new RuntimeException("Client non trouvé"));
                transaction.setAgent(agentExp);
                transaction.setDestinataire(clientDest);
                break;

            case AGENT_TO_AGENT:
                Agent agent1 = agentRepository.findById(request.getAgentExpediteurId())
                        .orElseThrow(() -> new RuntimeException("Agent expéditeur non trouvé"));
                Agent agent2 = agentRepository.findById(request.getAgentDestinataireId())
                        .orElseThrow(() -> new RuntimeException("Agent destinataire non trouvé"));
                transaction.setAgent(agent1);
                // Pour les transferts agent-agent, on peut stocker le destinataire dans un champ supplémentaire
                break;
        }

        if (request.getBeneficiaireId() != null) {
            Beneficiaire beneficiaire = beneficiaireRepository.findById(request.getBeneficiaireId())
                    .orElseThrow(() -> new RuntimeException("Bénéficiaire non trouvé"));
            transaction.setBeneficiaire(beneficiaire);
        }
    }

    private void debiterExpediteur(Transaction transaction) {
        if (transaction.getExpediteur() != null) {
            // Débiter un client
            Wallet wallet = transaction.getExpediteur().getPortefeuille();
            if (wallet.getSolde().doubleValue() < transaction.getMontantTotal()) {
                throw new RuntimeException("Solde insuffisant");
            }
            wallet.setSolde(BigDecimal.valueOf(wallet.getSolde().doubleValue() - transaction.getMontantTotal()));
            walletRepository.save(wallet);
        } else if (transaction.getAgent() != null && transaction.getType() == typeTransaction.AGENT_TO_CLIENT) {
            // Débiter un agent
            Agent agent = transaction.getAgent();
            if (agent.getSoldeCaisse() < transaction.getMontantTotal()) {
                throw new RuntimeException("Solde caisse insuffisant");
            }
            agent.setSoldeCaisse(agent.getSoldeCaisse() - transaction.getMontantTotal());
            agentRepository.save(agent);
        }
    }

    private void crediterDestinataire(Transaction transaction) {
        if (transaction.getDestinataire() != null) {
            // Créditer un client
            Wallet wallet = transaction.getDestinataire().getPortefeuille();
            wallet.setSolde(BigDecimal.valueOf(wallet.getSolde().doubleValue() + transaction.getMontant()));
            walletRepository.save(wallet);
        } else if (transaction.getAgent() != null && transaction.getType() == typeTransaction.CLIENT_TO_AGENT) {
            // Créditer un agent
            Agent agent = transaction.getAgent();
            agent.setSoldeCaisse(agent.getSoldeCaisse() + transaction.getMontant());
            agentRepository.save(agent);
        }
    }

    private double calculerFrais(double montant, ParametreSysteme parametres) {
        // Logique de calcul des frais (exemple: pourcentage + fixe)
        return montant * parametres.getFraisTransfert().doubleValue() / 100;
    }

    private String genererReference() {
        return "TRX" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }


}