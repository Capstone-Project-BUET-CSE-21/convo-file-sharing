package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.DownloadRecordDto;
import com.convo.file_sharing.dto.DownloadRegistrationDto;
import com.convo.file_sharing.entity.FileDownload;
import com.convo.file_sharing.exception.ForbiddenException;
import com.convo.file_sharing.repository.FileDownloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Same Mockito/Lombok null-analysis noise as TransferMetadataServiceTest.
@SuppressWarnings("null")
class FileDownloadServiceTest {

    @Mock
    private FileDownloadRepository repository;

    @Mock
    private UserLookupClient userLookupClient;

    @InjectMocks
    private FileDownloadService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private static FileDownload download(UUID userId, String contentHash) {
        return FileDownload.builder()
                .id(UUID.randomUUID())
                .sessionId("S1")
                .userId(userId)
                .contentHash(contentHash)
                .downloadedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void recordDownload_ForSomeoneElse_ThrowsForbiddenAndSavesNothing() {
        DownloadRegistrationDto req = new DownloadRegistrationDto(UUID.randomUUID(), "content-abc");

        assertThrows(ForbiddenException.class, () -> service.recordDownload("S1", req, UUID.randomUUID()));
        verifyNoInteractions(repository, userLookupClient);
    }

    @Test
    void recordDownload_OwnDownload_SavesServerTimestampAndReturnsName() {
        UUID me = UUID.randomUUID();
        DownloadRegistrationDto req = new DownloadRegistrationDto(me, "content-abc");
        when(repository.save(any(FileDownload.class))).thenAnswer(i -> i.getArgument(0));
        when(userLookupClient.getDisplayNames(List.of(me))).thenReturn(Map.of(me, "Me"));

        DownloadRecordDto res = service.recordDownload("S1", req, me);

        ArgumentCaptor<FileDownload> captor = ArgumentCaptor.forClass(FileDownload.class);
        verify(repository).save(captor.capture());
        FileDownload saved = captor.getValue();
        assertNotNull(saved.getId());
        assertNotNull(saved.getDownloadedAt());
        assertEquals("S1", saved.getSessionId());

        assertEquals(me, res.userId());
        assertEquals("Me", res.userDisplayName());
        assertEquals("content-abc", res.contentHash());
    }

    @Test
    void listDownloads_ResolvesEachDistinctUserOnce() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        when(repository.findByContentHashOrderByDownloadedAtAsc("content-abc")).thenReturn(List.of(
                download(alice, "content-abc"),
                download(bob, "content-abc"),
                download(alice, "content-abc")));
        when(userLookupClient.getDisplayNames(List.of(alice, bob)))
                .thenReturn(Map.of(alice, "Alice", bob, "Bob"));

        List<DownloadRecordDto> rows = service.listDownloads("content-abc");

        assertEquals(List.of("Alice", "Bob", "Alice"), rows.stream().map(DownloadRecordDto::userDisplayName).toList());
        verify(userLookupClient).getDisplayNames(List.of(alice, bob));
    }

    @Test
    void listDownloads_UnresolvableUser_NameIsNullRowStillReturned() {
        UUID ghost = UUID.randomUUID();
        when(repository.findByContentHashOrderByDownloadedAtAsc("content-abc"))
                .thenReturn(List.of(download(ghost, "content-abc")));
        when(userLookupClient.getDisplayNames(List.of(ghost))).thenReturn(Map.of());

        List<DownloadRecordDto> rows = service.listDownloads("content-abc");

        assertEquals(1, rows.size());
        assertEquals(ghost, rows.get(0).userId());
        assertNull(rows.get(0).userDisplayName());
    }

    @Test
    void listDownloads_NoDownloads_SkipsLookupEntirely() {
        when(repository.findByContentHashOrderByDownloadedAtAsc("nothing")).thenReturn(List.of());

        assertEquals(List.of(), service.listDownloads("nothing"));
        verifyNoInteractions(userLookupClient);
    }
}
