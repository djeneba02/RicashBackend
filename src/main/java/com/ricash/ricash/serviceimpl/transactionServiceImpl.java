package com.ricash.ricash.serviceimpl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.ricash.ricash.config.FirebaseTokendFilter;
import com.ricash.ricash.model.*;
import com.ricash.ricash.model.Enum.statutTransaction;
import com.ricash.ricash.dto.TransferRequest;
import com.google.firebase.auth.FirebaseAuthException;
import com.ricash.ricash.repository.*;
import com.ricash.ricash.service.transactionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class transactionServiceImpl implements transactionService {

    private final transactionRepository transactionRepository;
    private final userRepository userRepository;
    private final agentRepository agentRepository;
    private final walletRepository walletRepository;
    private final FirebaseTokendFilter firebaseTokendFilter;
    private final parametreSysRepository parametreSystemeRepository;
    private final beneficiaireRepository beneficiaireRepository;
    private final RestTemplate restTemplate;

    @Value("${currencyfreaks.apikey}")
    private String apiKey;

    public transactionServiceImpl(transactionRepository transactionRepository,
                                  userRepository userRepository,
                                  agentRepository agentRepository,
                                  walletRepository walletRepository,
                                  FirebaseTokendFilter firebaseTokendFilter,
                                  parametreSysRepository parametreSystemeRepository,
                                  beneficiaireRepository beneficiaireRepository,
                                  RestTemplate restTemplate) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.walletRepository = walletRepository;
        this.firebaseTokendFilter = firebaseTokendFilter;
        this.parametreSystemeRepository = parametreSystemeRepository;
        this.beneficiaireRepository = beneficiaireRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Transaction initierTransfert(TransferRequest request, String token) {
        // Validation des paramètres avec vérification de l'utilisateur
        validerTransfert(request, token);

        // Récupération des paramètres système avec gestion d'erreur améliorée
        ParametreSysteme parametres = parametreSystemeRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException(
                        "Paramètres système non configurés. Veuillez contacter l'administrateur."
                ));

        // Obtenir le taux de change en temps réel
        double tauxChangeReel = obtenirTauxChangeReel(request.getDevise(), parametres);

        // Calcul des frais et montant total
        double frais = calculerFrais(request.getMontant(), parametres);
        double montantTotal = request.getMontant() + frais;

        // Création de la transaction
        Transaction transaction = new Transaction();
        transaction.setReference(genererReference());
        transaction.setCodeTransaction(genererCodeTransaction());
        transaction.setMontant(request.getMontant());
        transaction.setFrais(frais);
        transaction.setMontantTotal(montantTotal);
        transaction.setDevise(request.getDevise());
        transaction.setTauxChange(tauxChangeReel);
        transaction.setStatut(statutTransaction.INITIEE);
        transaction.setType(request.getType());
        transaction.setMethodePaiement(request.getMethodePaiement());
        transaction.setDateCreation(new Date());
        transaction.setParametreSysteme(parametres);

        // Détermination de l'expéditeur et du destinataire selon le type
        configurerPartiesTransaction(transaction, request, token);

        return transactionRepository.save(transaction);
    }

    private String genererCodeTransaction() {
        // Génère un code unique de 8 caractères alphanumériques
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return "TRX-" + code.toString();
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

    private double obtenirTauxChangeReel(String devise, ParametreSysteme parametres) {
        try {
            // Si la devise est XOF, utiliser l'API CurrencyFreaks
            if ("XOF".equalsIgnoreCase(devise)) {
                String url = String.format(
                        "https://api.currencyfreaks.com/v2.0/rates/latest?apikey=%s&symbols=XOF",
                        apiKey
                );

                CurrencyResponse response = restTemplate.getForObject(url, CurrencyResponse.class);
                if (response != null && response.getRates() != null && response.getRates().containsKey("XOF")) {
                    return Double.parseDouble(response.getRates().get("XOF"));
                }
            }

            // Fallback: utiliser le taux par défaut des paramètres
            return parametres.getTauxChange().doubleValue();

        } catch (Exception e) {
            System.err.println("Erreur API CurrencyFreaks, utilisation du taux par défaut: " + e.getMessage());
            return parametres.getTauxChange().doubleValue();
        }
    }

    // Classe interne pour la réponse de l'API
    private static class CurrencyResponse {
        private String date;
        private String base;
        private java.util.Map<String, String> rates;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getBase() { return base; }
        public void setBase(String base) { this.base = base; }
        public java.util.Map<String, String> getRates() { return rates; }
        public void setRates(java.util.Map<String, String> rates) { this.rates = rates; }
    }

    private FirebaseTokendFilter.UserDetailsWrapper getCurrentUserDetails(String token) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokendFilter.findUserInAllTables(uid);
            if (userDetails == null) {
                throw new RuntimeException("Utilisateur non trouvé");
            }

            return userDetails;

        } catch (Exception e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }

    private void validerTransfert(TransferRequest request, String token) {
        // Récupérer l'utilisateur connecté
        FirebaseTokendFilter.UserDetailsWrapper currentUser = getCurrentUserDetails(token);
        String userRole = currentUser.getRole();
        Long userId = currentUser.getId();

        // Validation du montant
        if (request.getMontant() <= 0) {
            throw new RuntimeException("Le montant doit être positif");
        }

        // Validation selon le type de transaction et vérification des permissions
        switch (request.getType()) {
            case CLIENT_TO_CLIENT:
                if (request.getExpediteurId() == null || request.getDestinataireId() == null) {
                    throw new RuntimeException("Expéditeur et destinataire requis");
                }
                if ("ROLE_CLIENT".equals(userRole) && !userId.equals(request.getExpediteurId())) {
                    throw new RuntimeException("Vous ne pouvez pas initier un transfert pour un autre client");
                }
                break;

            case CLIENT_TO_AGENT:
                if (request.getExpediteurId() == null || request.getAgentId() == null) {
                    throw new RuntimeException("Expéditeur et agent requis");
                }
                if ("ROLE_CLIENT".equals(userRole) && !userId.equals(request.getExpediteurId())) {
                    throw new RuntimeException("Vous ne pouvez pas initier un transfert pour un autre client");
                }
                break;

            case AGENT_TO_CLIENT:
                if (request.getAgentId() == null || request.getDestinataireId() == null) {
                    throw new RuntimeException("Agent et destinataire requis");
                }
                if ("ROLE_AGENT".equals(userRole) && !userId.equals(request.getAgentId())) {
                    throw new RuntimeException("Vous ne pouvez pas initier un transfert pour un autre agent");
                }
                break;

            case AGENT_TO_AGENT:
                if (request.getAgentExpediteurId() == null || request.getAgentDestinataireId() == null) {
                    throw new RuntimeException("Agents expéditeur et destinataire requis");
                }
                if ("ROLE_AGENT".equals(userRole) && !userId.equals(request.getAgentExpediteurId())) {
                    throw new RuntimeException("Vous ne pouvez pas initier un transfert pour un autre agent");
                }
                break;
        }
    }

    private void configurerPartiesTransaction(Transaction transaction, TransferRequest request, String token) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

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
                    Agent agentExpediteur = agentRepository.findById(request.getAgentExpediteurId())
                            .orElseThrow(() -> new RuntimeException("Agent expéditeur non trouvé"));
                    Agent agentDestinataire = agentRepository.findById(request.getAgentDestinataireId())
                            .orElseThrow(() -> new RuntimeException("Agent destinataire non trouvé"));
                    transaction.setExpediteurAgent(agentExpediteur);
                    transaction.setDestinataireAgent(agentDestinataire);
                    break;
            }

            if (request.getBeneficiaireId() != null) {
                Beneficiaire beneficiaire = beneficiaireRepository.findById(request.getBeneficiaireId())
                        .orElseThrow(() -> new RuntimeException("Bénéficiaire non trouvé"));
                transaction.setBeneficiaire(beneficiaire);
            }

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la configuration des parties: " + e.getMessage());
        }
    }

    private void debiterExpediteur(Transaction transaction) {
        switch (transaction.getType()) {
            case CLIENT_TO_CLIENT:
            case CLIENT_TO_AGENT:
                Wallet wallet = transaction.getExpediteur().getPortefeuille();
                if (wallet.getSolde().doubleValue() < transaction.getMontantTotal()) {
                    throw new RuntimeException("Solde client insuffisant");
                }
                wallet.setSolde(BigDecimal.valueOf(wallet.getSolde().doubleValue() - transaction.getMontantTotal()));
                walletRepository.save(wallet);
                break;

            case AGENT_TO_CLIENT:
                Agent agent = transaction.getAgent();
                if (agent == null) {
                    throw new RuntimeException("Agent expéditeur non trouvé dans la transaction");
                }
                if (agent.getSoldeCaisse() < transaction.getMontantTotal()) {
                    throw new RuntimeException("Solde caisse agent expéditeur insuffisant");
                }
                agent.setSoldeCaisse(agent.getSoldeCaisse() - transaction.getMontantTotal());
                agentRepository.save(agent);
                break;

            case AGENT_TO_AGENT:
                Agent agentExpediteur = transaction.getExpediteurAgent();
                if (agentExpediteur == null) {
                    throw new RuntimeException("Agent expéditeur non trouvé dans la transaction");
                }
                if (agentExpediteur.getSoldeCaisse() < transaction.getMontantTotal()) {
                    throw new RuntimeException("Solde caisse agent expéditeur insuffisant");
                }
                agentExpediteur.setSoldeCaisse(agentExpediteur.getSoldeCaisse() - transaction.getMontantTotal());
                agentRepository.save(agentExpediteur);
                break;
        }
    }

    private void crediterDestinataire(Transaction transaction) {
        switch (transaction.getType()) {
            case CLIENT_TO_CLIENT:
            case AGENT_TO_CLIENT:
                User destinataire = transaction.getDestinataire();
                if (destinataire == null) {
                    throw new RuntimeException("Destinataire non trouvé dans la transaction");
                }
                Wallet wallet = destinataire.getPortefeuille();
                wallet.setSolde(BigDecimal.valueOf(wallet.getSolde().doubleValue() + transaction.getMontant()));
                walletRepository.save(wallet);
                break;

            case CLIENT_TO_AGENT:
                Agent agentDestinataire = transaction.getAgent();
                if (agentDestinataire == null) {
                    throw new RuntimeException("Agent destinataire non trouvé dans la transaction");
                }
                agentDestinataire.setSoldeCaisse(agentDestinataire.getSoldeCaisse() + transaction.getMontant());
                agentRepository.save(agentDestinataire);
                break;

            case AGENT_TO_AGENT:
                Agent agentDestinataireAgent = transaction.getDestinataireAgent();
                if (agentDestinataireAgent == null) {
                    throw new RuntimeException("Agent destinataire non trouvé dans la transaction");
                }
                agentDestinataireAgent.setSoldeCaisse(agentDestinataireAgent.getSoldeCaisse() + transaction.getMontant());
                agentRepository.save(agentDestinataireAgent);
                break;
        }
    }

    private double calculerFrais(double montant, ParametreSysteme parametres) {
        return montant * parametres.getFraisTransfert().doubleValue() / 100;
    }

    private String genererReference() {
        return "TRX" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    @Override
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée"));
    }

    @Override
    public Transaction getTransactionByCode(String codeTransaction) {
        return transactionRepository.findByCodeTransaction(codeTransaction)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée avec le code: " + codeTransaction));
    }

    @Override
    public List<Transaction> getTransactionsByStatut(statutTransaction statut) {
        return transactionRepository.findByStatut(statut);
    }

    @Override
    public List<Transaction> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByExpediteurIdOrDestinataireId(userId, userId);
    }

    @Override
    public List<Transaction> getTransactionsByAgentId(Long agentId) {
        return transactionRepository.findByAgentIdOrExpediteurAgentIdOrDestinataireAgentId(agentId, agentId, agentId);
    }

    @Override
    public Transaction annulerTransaction(Long transactionId) {
        Transaction transaction = getTransactionById(transactionId);

        if (transaction.getStatut() != statutTransaction.INITIEE &&
                transaction.getStatut() != statutTransaction.EN_COURS) {
            throw new RuntimeException("Seules les transactions INITIEE ou EN_COURS peuvent être annulées");
        }

        transaction.setStatut(statutTransaction.ANNULEE);
        transaction.setDateCompletion(new Date());

        return transactionRepository.save(transaction);
    }

    @Override
    public Transaction rejeterTransaction(Long transactionId, String raison) {
        Transaction transaction = getTransactionById(transactionId);

        if (transaction.getStatut() != statutTransaction.INITIEE &&
                transaction.getStatut() != statutTransaction.EN_COURS) {
            throw new RuntimeException("Seules les transactions INITIEE ou EN_COURS peuvent être rejetées");
        }

        transaction.setStatut(statutTransaction.REJETEE);
        transaction.setRaisonRejet(raison);
        transaction.setDateCompletion(new Date());

        return transactionRepository.save(transaction);
    }
}