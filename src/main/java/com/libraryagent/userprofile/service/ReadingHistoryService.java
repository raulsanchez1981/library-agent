package com.libraryagent.userprofile.service;

import com.libraryagent.userprofile.dto.AddReadingHistoryRequest;
import com.libraryagent.userprofile.dto.ReadingHistoryDto;
import com.libraryagent.userprofile.dto.UpdateReadingHistoryRequest;

import java.util.List;
import java.util.UUID;

public interface ReadingHistoryService {

    List<ReadingHistoryDto> findAllByProfileId(UUID profileId);

    ReadingHistoryDto add(UUID profileId, AddReadingHistoryRequest request);

    ReadingHistoryDto update(UUID profileId, UUID historyId, UpdateReadingHistoryRequest request);

    void delete(UUID profileId, UUID historyId);
}
