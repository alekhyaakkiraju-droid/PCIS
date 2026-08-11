package com.pcis.claims.domain.repository;

import com.pcis.claims.domain.ClaimNoteEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimNoteRepository extends JpaRepository<ClaimNoteEntity, Long> {
  List<ClaimNoteEntity> findByClaimClaimNbrOrderByNoteIdAsc(String claimNbr);
}
