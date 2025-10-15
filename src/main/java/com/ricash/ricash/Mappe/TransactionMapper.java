package com.ricash.ricash.Mappe;

import com.ricash.ricash.dto.TransactionDTO;
import com.ricash.ricash.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    @Mapping(target = "expediteurNom", source = "expediteur.nom")
    @Mapping(target = "expediteurTelephone", source = "expediteur.telephone")
    @Mapping(target = "destinataireNom", source = "destinataire.nom")
    @Mapping(target = "destinataireTelephone", source = "destinataire.telephone")
    @Mapping(target = "agentNom", source = "agent.nom")
    @Mapping(target = "agentTelephone", source = "agent.telephone")
    @Mapping(target = "expediteurAgentNom", source = "expediteurAgent.nom")
    @Mapping(target = "expediteurAgentTelephone", source = "expediteurAgent.telephone")
    @Mapping(target = "destinataireAgentNom", source = "destinataireAgent.nom")
    @Mapping(target = "destinataireAgentTelephone", source = "destinataireAgent.telephone")
    @Mapping(target = "beneficiaireNom", source = "beneficiaire.nom")
    TransactionDTO toDTO(Transaction transaction);

    @Mapping(target = "expediteur", ignore = true)
    @Mapping(target = "destinataire", ignore = true)
    @Mapping(target = "agent", ignore = true)
    @Mapping(target = "expediteurAgent", ignore = true)
    @Mapping(target = "destinataireAgent", ignore = true)
    @Mapping(target = "beneficiaire", ignore = true)
    @Mapping(target = "parametreSysteme", ignore = true)
    Transaction toEntity(TransactionDTO transactionDTO);
}