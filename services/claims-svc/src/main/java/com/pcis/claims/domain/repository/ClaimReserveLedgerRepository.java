package com.pcis.claims.domain.repository;

import com.pcis.claims.domain.ClaimReserveLedgerEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimReserveLedgerRepository
    extends JpaRepository<ClaimReserveLedgerEntity, Long> {
  List<ClaimReserveLedgerEntity> findByClaimClaimNbrOrderByLedgerIdAsc(String claimNbr);
}
