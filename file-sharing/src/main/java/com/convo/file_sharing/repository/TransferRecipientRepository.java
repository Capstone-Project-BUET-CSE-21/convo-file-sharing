package com.convo.file_sharing.repository;

import com.convo.file_sharing.entity.TransferRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TransferRecipientRepository extends JpaRepository<TransferRecipient, UUID> {

    List<TransferRecipient> findByTransferId(UUID transferId);

    // Batch fetch for a whole chain history response, so getChainHistory
    // doesn't run one recipients query per hop.
    List<TransferRecipient> findByTransferIdIn(Collection<UUID> transferIds);
}
