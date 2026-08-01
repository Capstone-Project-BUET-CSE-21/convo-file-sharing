package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.MetadataPatchDto;
import com.convo.file_sharing.dto.MetadataRequestDto;
import com.convo.file_sharing.dto.MetadataResponseDto;
import com.convo.file_sharing.entity.TransferMetadata;
import com.convo.file_sharing.repository.TransferMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TransferMetadataServiceTest {

    @Mock
    private TransferMetadataRepository repository;

    @InjectMocks
    private TransferMetadataService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreatePendingTransfer_FreshFile_SetsOriginSessionIdToCurrent() {
        String sessionId = "ABC-1234";
        MetadataRequestDto req = new MetadataRequestDto(
                sessionId, UUID.randomUUID(), "test.txt", 100L, "text/plain", null);

        when(repository.save(any(TransferMetadata.class))).thenAnswer(i -> i.getArgument(0));

        MetadataResponseDto res = service.createPendingTransfer(req);

        ArgumentCaptor<TransferMetadata> captor = ArgumentCaptor.forClass(TransferMetadata.class);
        verify(repository).save(captor.capture());
        TransferMetadata saved = captor.getValue();

        assertEquals(sessionId, saved.getOriginSessionId());
        assertNull(saved.getPreviousHash());
    }

    @Test
    void testCreatePendingTransfer_WithPreviousHash_InheritsOriginSessionId() {
        String newSessionId = "XYZ-5678";
        String originSessionId = "ABC-1234";
        String prevHash = "old_hash";

        MetadataRequestDto req = new MetadataRequestDto(
                newSessionId, UUID.randomUUID(), "test.txt", 100L, "text/plain", prevHash);

        TransferMetadata prevEntity = new TransferMetadata();
        prevEntity.setOriginSessionId(originSessionId);

        when(repository.findByFileHash(prevHash)).thenReturn(Optional.of(prevEntity));
        when(repository.save(any(TransferMetadata.class))).thenAnswer(i -> i.getArgument(0));

        service.createPendingTransfer(req);

        ArgumentCaptor<TransferMetadata> captor = ArgumentCaptor.forClass(TransferMetadata.class);
        verify(repository).save(captor.capture());
        TransferMetadata saved = captor.getValue();

        assertEquals(originSessionId, saved.getOriginSessionId());
        assertEquals(prevHash, saved.getPreviousHash());
    }

    @Test
    void testAttachHashAndSignature_LaunderingGap_ThrowsException() {
        UUID transferId = UUID.randomUUID();
        MetadataPatchDto patch = new MetadataPatchDto("hash123", "sig123", "content123");

        TransferMetadata pending = new TransferMetadata();
        pending.setTransferId(transferId);
        pending.setPreviousHash(null); // claims to be fresh

        when(repository.findById(transferId)).thenReturn(Optional.of(pending));
        // Simulate that the content is already known
        when(repository.findByContentHashOrderByTimestampAsc("content123"))
                .thenReturn(List.of(new TransferMetadata()));

        assertThrows(IllegalArgumentException.class, () -> {
            service.attachHashAndSignature(transferId, patch);
        });
    }

    @Test
    void testAttachHashAndSignature_Success() {
        UUID transferId = UUID.randomUUID();
        MetadataPatchDto patch = new MetadataPatchDto("hash123", "sig123", "content123");

        TransferMetadata pending = new TransferMetadata();
        pending.setTransferId(transferId);
        pending.setPreviousHash(null);

        when(repository.findById(transferId)).thenReturn(Optional.of(pending));
        when(repository.findByContentHashOrderByTimestampAsc("content123"))
                .thenReturn(List.of()); // Not found, so no laundering gap
        when(repository.save(any(TransferMetadata.class))).thenAnswer(i -> i.getArgument(0));

        MetadataResponseDto res = service.attachHashAndSignature(transferId, patch);
        
        ArgumentCaptor<TransferMetadata> captor = ArgumentCaptor.forClass(TransferMetadata.class);
        verify(repository).save(captor.capture());
        TransferMetadata saved = captor.getValue();

        assertEquals("hash123", saved.getFileHash());
        assertEquals("sig123", saved.getSignature());
        assertEquals("content123", saved.getContentHash());
    }
}