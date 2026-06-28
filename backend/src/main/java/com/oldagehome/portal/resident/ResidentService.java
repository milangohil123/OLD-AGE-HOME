package com.oldagehome.portal.resident;

import com.oldagehome.portal.dto.ResidentImportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResidentService {
    Page<Resident> getResidents(String keyword, Pageable pageable);
    List<Resident> getAllResidents();
    Resident getResidentById(Long id);
    Resident saveResident(Resident resident);
    Resident updateResident(Resident resident);
    void deleteResident(Long id);
    boolean existsByResidentId(String residentId);
    
    /**
     * Bulk imports resident entries from an uploaded Excel file.
     * Reports row-level errors and successes.
     */
    List<ResidentImportDTO> importFromExcel(MultipartFile file) throws Exception;
}
