package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.ChainHistoryResponseDto;
import com.convo.file_sharing.dto.MetadataPatchDto;
import com.convo.file_sharing.dto.MetadataRequestDto;
import com.convo.file_sharing.dto.MetadataResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

// The two provenance scenarios the design is specified by, run against the
// real repositories and the H2 test database — so the parent-selection
// query itself is exercised, not a mock of it. Each test uses its own
// content hash, so tests never see each other's rows.
@SpringBootTest
class ProvenanceForestIntegrationTest {

    @Autowired
    private TransferMetadataService service;

    @MockBean
    private UserLookupClient userLookupClient;

    private final UUID charlie = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID dave = UUID.randomUUID();

    @BeforeEach
    void names() {
        when(userLookupClient.getDisplayNames(anyList())).thenReturn(Map.of(
                charlie, "Charlie", alice, "Alice", bob, "Bob", dave, "Dave"));
    }

    /** One full share (POST + PATCH), as the client does it. Returns the new hop's fileHash. */
    private String share(String content, UUID sender, UUID... recipients) {
        MetadataResponseDto pending = service.createPendingTransfer(new MetadataRequestDto(
                "S-" + sender, sender, "f.txt", 10L, "text/plain", content, List.of(recipients)), sender);
        String fileHash = "fh-" + UUID.randomUUID();
        service.attachHashAndSignature(pending.transferId(), new MetadataPatchDto(fileHash, "sig", content), sender);
        return fileHash;
    }

    private static ChainHistoryResponseDto byFileHash(List<ChainHistoryResponseDto> history, String fileHash) {
        return history.stream().filter(h -> h.fileHash().equals(fileHash)).findFirst().orElseThrow();
    }

    @Test
    void case1_OriginalSenderSharesTwice_ThenAliceForwards() {
        // Charlie -> Alice, later Charlie -> Bob, then Alice -> Dave.
        // Expected: Charlie -> Alice -> Dave, and Charlie -> Bob.
        String content = "content-" + UUID.randomUUID();
        String charlieToAlice = share(content, charlie, alice);
        String charlieToBob = share(content, charlie, bob);
        String aliceToDave = share(content, alice, dave);

        List<ChainHistoryResponseDto> history = service.getChainHistory(content);

        assertEquals(3, history.size());
        assertNull(byFileHash(history, charlieToAlice).previousHash(), "Charlie introduced it: a root");
        assertNull(byFileHash(history, charlieToBob).previousHash(),
                "Charlie never received it, so his second share is its own root — not hung under Charlie->Alice");
        assertEquals(charlieToAlice, byFileHash(history, aliceToDave).previousHash(),
                "Alice got it from Charlie->Alice, so that's what her share continues — not the later Charlie->Bob");
    }

    @Test
    void case2_BobGotItOutsideConvo_SharesBackToAliceAndToDave() {
        // Charlie -> Alice through Convo; Charlie gives Bob a copy elsewhere;
        // Bob -> Alice, Bob -> Dave. Expected: Charlie -> Alice, Bob -> Alice, Bob -> Dave.
        String content = "content-" + UUID.randomUUID();
        String charlieToAlice = share(content, charlie, alice);
        String bobToAlice = share(content, bob, alice);
        String bobToDave = share(content, bob, dave);

        List<ChainHistoryResponseDto> history = service.getChainHistory(content);

        assertEquals(3, history.size());
        assertNull(byFileHash(history, charlieToAlice).previousHash());
        assertNull(byFileHash(history, bobToAlice).previousHash(), "Bob has no Convo record of receiving it: a root");
        assertNull(byFileHash(history, bobToDave).previousHash(), "same for his second share");
        assertEquals("Bob", byFileHash(history, bobToDave).senderDisplayName());
        assertEquals("Dave", byFileHash(history, bobToDave).recipients().get(0).displayName());
    }

    @Test
    void receivedTheSameContentTwice_ForwardContinuesTheMostRecentReceipt() {
        String content = "content-" + UUID.randomUUID();
        share(content, charlie, alice);
        String bobToAlice = share(content, bob, alice);
        String aliceToDave = share(content, alice, dave);

        assertEquals(bobToAlice, byFileHash(service.getChainHistory(content), aliceToDave).previousHash());
    }

    @Test
    void unfinishedShare_IsNeitherHistoryNorAParent() {
        String content = "content-" + UUID.randomUUID();
        // Charlie starts sharing to Alice but never completes the PATCH.
        service.createPendingTransfer(new MetadataRequestDto(
                "S1", charlie, "f.txt", 10L, "text/plain", content, List.of(alice)), charlie);
        String aliceToDave = share(content, alice, dave);

        List<ChainHistoryResponseDto> history = service.getChainHistory(content);

        assertEquals(1, history.size(), "the pending row isn't history");
        assertNull(byFileHash(history, aliceToDave).previousHash(), "and Alice can't continue a share that never completed");
    }
}
