package com.oldagehome.portal.donor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public interface DonorService {

    /** Paginated list with optional keyword search. */
    Page<Donor> getDonors(String keyword, Pageable pageable);

    /** Flat list (for export). */
    List<Donor> getAllDonors();

    /** Fetch single donor by primary key. */
    Donor getDonorById(Long id);

    /** Create a new donor record. */
    Donor saveDonor(Donor donor);

    /** Update an existing donor record. */
    Donor updateDonor(Donor donor);

    /** Delete a donor by primary key. */
    void deleteDonor(Long id);

    /** Parse, validate, persist from Excel; returns row-level result DTOs. */
    List<com.oldagehome.portal.dto.DonorImportDTO> importFromExcel(MultipartFile file) throws Exception;

    /** Export all donor records to an Excel byte array. */
    byte[] exportToExcel() throws IOException;

    // --- Dashboard stats ---
    long countTotalDonors();
    long countTodayDonations();
    long countThisMonthDonations();
    BigDecimal sumTotalDonationAmount();
}
