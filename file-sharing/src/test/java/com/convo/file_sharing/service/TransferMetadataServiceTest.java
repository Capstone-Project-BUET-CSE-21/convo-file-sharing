package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.MetadataPatchDto;
import com.convo.file_sharing.dto.MetadataRequestDto;
import com.convo.file_sharing.dto.MetadataResponseDto;
import com.convo.file_sharing.entity.ChainRoot;
import com.convo.file_sharing.entity.TransferMetadata;
import com.convo.file_sharing.exception.ForbiddenException;
import com.convo.file_sharing.repository.ChainRootRepository;
import com.convo.file_sharing.repository.TransferMetadataRepository;
import com.convo.file_sharing.repository.TransferRecipientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Same reasoning as TransferMetadataService's own suppression: Mockito's
// any(X.class) matchers and Lombok-generated entity getters aren't
// annotated for nullability, so Eclipse's null analysis flags them
// throughout this file for no real reason — any(X.class) returning null
// statically is a known, harmless Mockito quirk (it's intercepted before
// reaching real code), not an actual null risk here.
@SuppressWarnings("null")
public class TransferMetadataServiceTest {

    @Mock
    private TransferMetadataRepository repository;

    @Mock
    private ChainRootRepository chainRootRepository;

    @Mock
    private TransferRecipientRepository recipientRepository;

    @InjectMocks
    private TransferMetadataService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreatePendingTransfer_FreshFile_SetsOriginSessionIdToCurrent() {
        String sessionId = "ABC-1234";
        UUID senderId = UUID.randomUUID();
        MetadataRequestDto req = new MetadataRequestDto(
                sessionId, senderId, "test.txt", 100L, "text/plain", null, List.of(UUID.randomUUID()));

        when(repository.save(any(TransferMetadata.class))).thenAnswer(i -> i.getArgument(0));

        MetadataResponseDto res = service.createPendingTransfer(req, senderId);

        ArgumentCaptor<TransferMetadata> captor = ArgumentCaptor.forClass(TransferMetadata.class);
        verify(repository).save(captor.capture());
        TransferMetadata saved = captor.getValue();

        assertEquals(sessionId, saved.getOriginSessionId());
        assertNull(saved.getPreviousHash());

        // The response DTO is a separate mapping (toResponse) from what gets
        // persisted — assert on it too, not just the captured entity, so a
        // broken toResponse() wiring would actually fail this test.
        assertEquals(saved.getTransferId(), res.transferId());
        assertEquals(sessionId, res.sessionId());
        assertNull(res.previousHash());
        assertEquals(req.recipients(), res.recipients());
    }

    @Test
    void testCreatePendingTransfer_SenderIdNotAuthenticatedUser_ThrowsForbidden() {
        MetadataRequestDto req = new MetadataRequestDto(
                "ABC-1234", UUID.randomUUID(), "test.txt", 100L, "text/plain", null, List.of(UUID.randomUUID()));

        assertThrows(ForbiddenException.class, () -> service.createPendingTransfer(req, UUID.randomUUID()));
        verifyNoInteractions(repository);
    }

    @Test
    void testCreatePendingTransfer_WithPreviousHash_InheritsOriginSessionId() {
        String newSessionId = "XYZ-5678";
        String originSessionId = "ABC-1234";
        String prevHash = "old_hash";
        UUID senderId = UUID.randomUUID();

        MetadataRequestDto req = new MetadataRequestDto(
                newSessionId, senderId, "test.txt", 100L, "text/plain", prevHash, List.of(UUID.randomUUID()));

        TransferMetadata prevEntity = new TransferMetadata();
        prevEntity.setOriginSessionId(originSessionId);

        when(repository.findByFileHash(prevHash)).thenReturn(Optional.of(prevEntity));
        when(repository.save(any(TransferMetadata.class))).thenAnswer(i -> i.getArgument(0));

        service.createPendingTransfer(req, senderId);

        ArgumentCaptor<TransferMetadata> captor = ArgumentCaptor.forClass(TransferMetadata.class);
        verify(repository).save(captor.capture());
        TransferMetadata saved = captor.getValue();

        assertEquals(originSessionId, saved.getOriginSessionId());
        assertEquals(prevHash, saved.getPreviousHash());
    }

    @Test
    void testAttachHashAndSignature_RootAlreadyClaimed_ThrowsException() {
        UUID transferId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        MetadataPatchDto patch = new MetadataPatchDto("hash123", "sig123", "content123");

        TransferMetadata pending = new TransferMetadata();
        pending.setTransferId(transferId);
        pending.setSenderId(senderId);
        pending.setPreviousHash(null); // claims to be fresh

        when(repository.findById(transferId)).thenReturn(Optional.of(pending));
        // Another transfer already claimed this content hash's root — the
        // unique-key insert loses the race and the DB rejects it.
        when(chainRootRepository.saveAndFlush(any(ChainRoot.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(IllegalArgumentException.class, () -> {
            service.attachHashAndSignature(transferId, patch, senderId);
        });
    }

    @Test
    void testAttachHashAndSignature_NotOriginalSender_ThrowsForbidden() {
        UUID transferId = UUID.randomUUID();
        MetadataPatchDto patch = new MetadataPatchDto("hash123", "sig123", "content123");

        TransferMetadata pending = new TransferMetadata();
        pending.setTransferId(transferId);
        pending.setSenderId(UUID.randomUUID());
        pending.setPreviousHash(null);

        when(repository.findById(transferId)).thenReturn(Optional.of(pending));

        assertThrows(ForbiddenException.class, () -> {
            service.attachHashAndSignature(transferId, patch, UUID.randomUUID());
        });
        verifyNoInteractions(chainRootRepository);
    }

    @Test
    void testAttachHashAndSignature_Success() {
        UUID transferId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        MetadataPatchDto patch = new MetadataPatchDto("hash123", "sig123", "content123");

        TransferMetadata pending = new TransferMetadata();
        pending.setTransferId(transferId);
        pending.setSenderId(senderId);
        pending.setPreviousHash(null);

        when(repository.findById(transferId)).thenReturn(Optional.of(pending));
        when(repository.save(any(TransferMetadata.class))).thenAnswer(i -> i.getArgument(0));

        MetadataResponseDto res = service.attachHashAndSignature(transferId, patch, senderId);

        verify(chainRootRepository).saveAndFlush(any(ChainRoot.class));
        ArgumentCaptor<TransferMetadata> captor = ArgumentCaptor.forClass(TransferMetadata.class);
        verify(repository).save(captor.capture());
        TransferMetadata saved = captor.getValue();

        assertEquals("hash123", saved.getFileHash());
        assertEquals("sig123", saved.getSignature());
        assertEquals("content123", saved.getContentHash());

        // fileHash/signature/contentHash aren't part of the response DTO's
        // own fields, but its identity should still trace back to the same
        // entity that got saved — same reasoning as the other test above.
        assertEquals(saved.getTransferId(), res.transferId());
        assertEquals(senderId, res.senderId());
    }
}