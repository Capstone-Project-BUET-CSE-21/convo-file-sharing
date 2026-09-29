package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.ChainHistoryResponseDto;
import com.convo.file_sharing.dto.MetadataPatchDto;
import com.convo.file_sharing.dto.MetadataRequestDto;
import com.convo.file_sharing.dto.MetadataResponseDto;
import com.convo.file_sharing.dto.RecipientDto;
import com.convo.file_sharing.entity.TransferMetadata;
import com.convo.file_sharing.entity.TransferRecipient;
import com.convo.file_sharing.exception.ForbiddenException;
import com.convo.file_sharing.repository.TransferMetadataRepository;
import com.convo.file_sharing.repository.TransferRecipientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// Same reasoning as TransferMetadataService's own suppression: Mockito's
// any(X.class) matchers and Lombok-generated entity getters aren't
// annotated for nullability, so Eclipse's null analysis flags them
// throughout this file for no real reason.
@SuppressWarnings("null")
public class TransferMetadataServiceTest {

    @Mock
    private TransferMetadataRepository repository;

    @Mock
    private TransferRecipientRepository recipientRepository;

    @Mock
    private UserLookupClient userLookupClient;

    @InjectMocks
    private TransferMetadataService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(repository.save(any(TransferMetadata.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static MetadataRequestDto request(UUID sender, String session, UUID... recipients) {
        return new MetadataRequestDto(session, sender, "f.txt", 10L, "text/plain", "content-abc", List.of(recipients));
    }

    private TransferMetadata savedEntity() {
        ArgumentCaptor<TransferMetadata> captor = ArgumentCaptor.forClass(TransferMetadata.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    // ---- createPendingTransfer: who the parent is ---------------------------

    @Test
    void senderNeverReceivedThisContent_StartsNewRoot() {
        UUID charlie = UUID.randomUUID();
        when(recipientRepository.findSharesReceivedBy(charlie, "content-abc")).thenReturn(List.of());

        MetadataResponseDto res = service.createPendingTransfer(request(charlie, "S1", UUID.randomUUID()), charlie);

        TransferMetadata saved = savedEntity();
        assertNull(saved.getPreviousHash());
        assertEquals("S1", saved.getOriginSessionId());
        assertEquals("content-abc", saved.getContentHash());
        assertNull(saved.getFileHash(), "stays pending until the PATCH");
        assertNull(res.previousHash(), "the block the client signs carries the server's choice");
    }

    @Test
    void senderReceivedThisContent_ContinuesTheLatestShareThatNamedThem() {
        UUID alice = UUID.randomUUID();
        TransferMetadata received = new TransferMetadata();
        received.setFileHash("hash-of-charlie-to-alice");
        received.setOriginSessionId("S-origin");
        when(recipientRepository.findSharesReceivedBy(alice, "content-abc")).thenReturn(List.of(received));

        MetadataResponseDto res = service.createPendingTransfer(request(alice, "S2", UUID.randomUUID()), alice);

        TransferMetadata saved = savedEntity();
        assertEquals("hash-of-charlie-to-alice", saved.getPreviousHash());
        assertEquals("S-origin", saved.getOriginSessionId());
        assertEquals("hash-of-charlie-to-alice", res.previousHash());
    }

    @Test
    void senderIdNotAuthenticatedUser_ThrowsForbiddenAndSavesNothing() {
        assertThrows(ForbiddenException.class,
                () -> service.createPendingTransfer(request(UUID.randomUUID(), "S1", UUID.randomUUID()), UUID.randomUUID()));
        verify(repository, never()).save(any());
        verifyNoInteractions(recipientRepository);
    }

    @Test
    void recipientsAreRecordedAndEchoed() {
        UUID sender = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(recipientRepository.findSharesReceivedBy(any(), any())).thenReturn(List.of());

        MetadataResponseDto res = service.createPendingTransfer(request(sender, "S1", a, b), sender);

        assertEquals(List.of(a, b), res.recipients());
        verify(recipientRepository).saveAll(anyList());
    }

    // ---- attachHashAndSignature ---------------------------------------------

    private TransferMetadata pending(UUID sender) {
        TransferMetadata t = new TransferMetadata();
        t.setTransferId(UUID.randomUUID());
        t.setSenderId(sender);
        t.setContentHash("content-abc");
        return t;
    }

    @Test
    void attach_Success_SetsFileHashAndSignature() {
        UUID sender = UUID.randomUUID();
        TransferMetadata t = pending(sender);
        when(repository.findById(t.getTransferId())).thenReturn(Optional.of(t));

        service.attachHashAndSignature(t.getTransferId(), new MetadataPatchDto("h", "sig", "content-abc"), sender);

        TransferMetadata saved = savedEntity();
        assertEquals("h", saved.getFileHash());
        assertEquals("sig", saved.getSignature());
    }

    @Test
    void attach_NotOriginalSender_ThrowsForbidden() {
        TransferMetadata t = pending(UUID.randomUUID());
        when(repository.findById(t.getTransferId())).thenReturn(Optional.of(t));

        assertThrows(ForbiddenException.class, () -> service.attachHashAndSignature(
                t.getTransferId(), new MetadataPatchDto("h", "sig", "content-abc"), UUID.randomUUID()));
        verify(repository, never()).save(any());
    }

    @Test
    void attach_DifferentContentHashThanDeclared_Rejected() {
        UUID sender = UUID.randomUUID();
        TransferMetadata t = pending(sender);
        when(repository.findById(t.getTransferId())).thenReturn(Optional.of(t));

        assertThrows(IllegalArgumentException.class, () -> service.attachHashAndSignature(
                t.getTransferId(), new MetadataPatchDto("h", "sig", "some-other-content"), sender));
        verify(repository, never()).save(any());
    }

    // ---- getChainHistory ----------------------------------------------------

    private static TransferMetadata hop(UUID transferId, UUID senderId, String fileHash, String previousHash) {
        TransferMetadata t = new TransferMetadata();
        t.setTransferId(transferId);
        t.setSenderId(senderId);
        t.setSessionId("S1");
        t.setOriginSessionId("S1");
        t.setFileName("f.txt");
        t.setFileSize(10L);
        t.setMimeType("text/plain");
        t.setTimestamp(OffsetDateTime.now());
        t.setContentHash("content-abc");
        t.setFileHash(fileHash);
        t.setPreviousHash(previousHash);
        t.setSignature("sig");
        return t;
    }

    @Test
    void history_NamesSendersAndRecipients_OneLookupForEveryone() {
        UUID charlie = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        UUID dave = UUID.randomUUID();
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        TransferMetadata root = hop(t1, charlie, "h1", null);
        TransferMetadata forward = hop(t2, alice, "h2", "h1");

        when(repository.findByContentHashAndFileHashIsNotNullOrderByTimestampAsc("content-abc"))
                .thenReturn(List.of(root, forward));
        when(recipientRepository.findByTransfer_TransferIdIn(List.of(t1, t2))).thenReturn(List.of(
                TransferRecipient.builder().transfer(root).recipientId(alice).build(),
                TransferRecipient.builder().transfer(forward).recipientId(dave).build()));
        when(userLookupClient.getDisplayNames(anyList()))
                .thenReturn(Map.of(charlie, "Charlie", alice, "Alice", dave, "Dave"));

        List<ChainHistoryResponseDto> history = service.getChainHistory("content-abc");

        assertEquals("Charlie", history.get(0).senderDisplayName());
        assertEquals(List.of(new RecipientDto(alice, "Alice")), history.get(0).recipients());
        assertEquals("Alice", history.get(1).senderDisplayName());
        assertEquals("h1", history.get(1).previousHash());
        assertEquals(List.of(new RecipientDto(dave, "Dave")), history.get(1).recipients());
        // Alice is both a recipient and a sender — still looked up once.
        verify(userLookupClient).getDisplayNames(List.of(charlie, alice, dave));
    }

    @Test
    void history_UnresolvedPeople_NamesAreNullNotAnError() {
        UUID ghost = UUID.randomUUID();
        UUID t1 = UUID.randomUUID();
        TransferMetadata root = hop(t1, ghost, "h1", null);
        when(repository.findByContentHashAndFileHashIsNotNullOrderByTimestampAsc("content-abc")).thenReturn(List.of(root));
        when(recipientRepository.findByTransfer_TransferIdIn(List.of(t1))).thenReturn(List.of(
                TransferRecipient.builder().transfer(root).recipientId(ghost).build()));
        when(userLookupClient.getDisplayNames(anyList())).thenReturn(Map.of());

        ChainHistoryResponseDto only = service.getChainHistory("content-abc").get(0);

        assertNull(only.senderDisplayName());
        assertEquals(List.of(new RecipientDto(ghost, null)), only.recipients());
    }

    @Test
    void history_UnknownContent_ReturnsEmpty() {
        when(repository.findByContentHashAndFileHashIsNotNullOrderByTimestampAsc("nothing")).thenReturn(List.of());
        when(userLookupClient.getDisplayNames(anyList())).thenReturn(Map.of());

        assertEquals(List.of(), service.getChainHistory("nothing"));
        verifyNoInteractions(recipientRepository);
    }
}
