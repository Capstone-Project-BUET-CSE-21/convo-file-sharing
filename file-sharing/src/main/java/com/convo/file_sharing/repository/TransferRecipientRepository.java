package com.convo.file_sharing.repository;

import com.convo.file_sharing.entity.TransferMetadata;
import com.convo.file_sharing.entity.TransferRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TransferRecipientRepository extends JpaRepository<TransferRecipient, UUID> {

    List<TransferRecipient> findByTransfer_TransferId(UUID transferId);

    // Batch fetch for a whole chain history response, so getChainHistory
    // doesn't run one recipients query per hop.
    List<TransferRecipient> findByTransfer_TransferIdIn(Collection<UUID> transferIds);

    // Completed shares of this content that named recipientId as a
    // recipient, newest first — the first element is the share a new send
    // by recipientId continues.
    @Query("""
            select r.transfer from TransferRecipient r
            where r.recipientId = :recipientId
              and r.transfer.contentHash = :contentHash
              and r.transfer.fileHash is not null
            order by r.transfer.timestamp desc
            """)
    List<TransferMetadata> findSharesReceivedBy(@Param("recipientId") UUID recipientId,
                                                @Param("contentHash") String contentHash);
}
