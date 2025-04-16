package data.service;

import data.dto.ApprovalLogDto;
import data.dto.ApprovalsDto;
import data.dto.DraftTemplatesDto;
import data.dto.DraftsDto;
import data.mapper.DraftMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DraftService {
    @Autowired
    DraftMapper draftMapper;
    @Autowired
    ApprovalsService approvalsService;

    public void createDraftTemplate(DraftTemplatesDto draftTemplatesDto) {
        draftMapper.createDraftTemplate(draftTemplatesDto);
    };

    public DraftTemplatesDto readDraftTemplate(int id) {
        return draftMapper.readDraftTemplate(id);
    };

    public List<DraftTemplatesDto> readAllDraftTemplate() {
        return draftMapper.readAllDraftTemplate();
    }

    public void updateDraftTemplate(DraftTemplatesDto draftTemplatesDto) {
        draftMapper.updateDraftTemplate(draftTemplatesDto);
    };

    public void deleteDraftTemplate(int id) {
        draftMapper.deleteDraftTemplate(id);
    };

    public void createDraft(DraftsDto draftsDto) {
        draftMapper.createDraft(draftsDto);
    };

    public DraftsDto readDraft(int id) {
        return draftMapper.readDraft(id);
    };

    public List<DraftsDto> readAllDrafts() {
        return draftMapper.readAllDrafts();
    };

    public void updateDraftStatus(int id, String status) {
        draftMapper.updateDraftStatus(id, status);
    };

    public List<DraftsDto> getPendingDraftsForUser(int userId, int size, int offset) {
        return draftMapper.readPendingDraftsForUser(userId, size, offset);
    }

    public Integer readCountDraftsForActions(int userId) {
        return draftMapper.readCountDraftsForActions(userId);
    }

    public Integer readCheckIsOrder(int userId, int draftId) {
        return draftMapper.readCheckIsOrder(userId, draftId);
    }

    public List<DraftsDto> readPendingDraftsById(int userId, int size, int offset) {
        return draftMapper.readPendingDraftsById(userId, size, offset);
    }

    public Integer readCountPendingdraftsById(int userId) {
        return draftMapper.readCountPendingdraftsById(userId);
    }

    public List<DraftsDto> readSentDoneById(int userId, int size, int offset) {
        return draftMapper.readSentDoneById(userId, size, offset);
    }

    public Integer readCountSentDoneById(int userId) {
        return draftMapper.readCountSentDoneById(userId);
    }

    public List<DraftsDto> readReceivedDoneById(int userId, int size, int offset) {
        return draftMapper.readReceivedDoneById(userId, size, offset);
    }

    public Integer readCountReceivedDoneById(int userId) {
        return draftMapper.readCountReceivedDoneById(userId);
    }

    public void stringToEnumAndUpdate(int id, String status) {
        ApprovalsDto.ApprovalStatus approvalStatus = ApprovalsDto.ApprovalStatus.valueOf(status);
    }

    public ApprovalLogDto.ApprovalAction stringToApprovalLogEnum(String status) {
        return ApprovalLogDto.ApprovalAction.valueOf(status.toUpperCase());
    }

    public void stringToApprovalLogEnumAndCreateLog(int draftId, int userId, String action, String reason) {
        ApprovalLogDto logDto = new ApprovalLogDto();
        logDto.setAction(stringToApprovalLogEnum(action));
        logDto.setDraftId(draftId);
        logDto.setApprovalId(userId);
        logDto.setReason(reason);
        approvalsService.createApprovalLog(logDto);
    }
    
    //기안자의 userId 조회
    public int readDrafterIdByDraftId(int id)
    {
    	return draftMapper.readDrafterIdByDraftId(id);
    }
}
