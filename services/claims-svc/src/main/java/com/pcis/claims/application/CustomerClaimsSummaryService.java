package com.pcis.claims.application;

import com.pcis.claims.domain.ClaimEntity;
import com.pcis.claims.domain.ClaimReserveEntity;
import com.pcis.claims.domain.repository.ClaimRepository;
import com.pcis.claims.domain.repository.ClaimReserveRepository;
import com.pcis.claims.dto.CustomerClaimsSummaryResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerClaimsSummaryService {

  private static final String OPEN_CLAIM_STATUS = "O";

  private final ClaimRepository claimRepository;
  private final ClaimReserveRepository claimReserveRepository;

  public CustomerClaimsSummaryService(
      ClaimRepository claimRepository, ClaimReserveRepository claimReserveRepository) {
    this.claimRepository = claimRepository;
    this.claimReserveRepository = claimReserveRepository;
  }

  @Transactional(readOnly = true)
  public CustomerClaimsSummaryResponse getSummary(Integer custId) {
    List<ClaimEntity> openClaims =
        claimRepository.findByCustId(custId).stream()
            .filter(claim -> OPEN_CLAIM_STATUS.equals(claim.getClaimStatus()))
            .toList();

    List<CustomerClaimsSummaryResponse.ClaimItem> items =
        openClaims.stream()
            .map(
                claim ->
                    new CustomerClaimsSummaryResponse.ClaimItem(
                        claim.getClaimNbr(),
                        claim.getClaimStatus(),
                        totalOpenReserve(claim.getClaimNbr())))
            .toList();

    return new CustomerClaimsSummaryResponse(items.size(), items);
  }

  private BigDecimal totalOpenReserve(String claimNbr) {
    return claimReserveRepository.findByClaimClaimNbr(claimNbr).stream()
        .filter(reserve -> "O".equals(reserve.getReserveStatus()))
        .map(ClaimReserveEntity::getApprovedAmt)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
