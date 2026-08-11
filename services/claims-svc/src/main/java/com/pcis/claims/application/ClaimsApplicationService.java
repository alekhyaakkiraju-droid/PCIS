package com.pcis.claims.application;

import com.pcis.error.ResourceNotFoundException;
import com.pcis.claims.domain.ApprovalEntity;
import com.pcis.claims.domain.ClaimEntity;
import com.pcis.claims.domain.ClaimReserveEntity;
import com.pcis.claims.domain.repository.ApprovalRepository;
import com.pcis.claims.domain.repository.ClaimRepository;
import com.pcis.claims.domain.repository.ClaimReserveRepository;
import com.pcis.claims.dto.CreateApprovalRequest;
import com.pcis.claims.dto.CreateClaimRequest;
import com.pcis.claims.dto.CreateReserveRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimsApplicationService {

  private final ClaimRepository claimRepository;
  private final ClaimReserveRepository claimReserveRepository;
  private final ApprovalRepository approvalRepository;

  public ClaimsApplicationService(
      ClaimRepository claimRepository,
      ClaimReserveRepository claimReserveRepository,
      ApprovalRepository approvalRepository) {
    this.claimRepository = claimRepository;
    this.claimReserveRepository = claimReserveRepository;
    this.approvalRepository = approvalRepository;
  }

  @Transactional(readOnly = true)
  public List<ClaimEntity> listClaims() {
    return claimRepository.findAll();
  }

  @Transactional(readOnly = true)
  public List<ClaimEntity> listClaimsByCustomer(Integer custId) {
    return claimRepository.findByCustId(custId);
  }

  @Transactional(readOnly = true)
  public ClaimEntity getClaim(String claimNbr) {
    return claimRepository
        .findById(claimNbr)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Claim not found: " + claimNbr, "system", "claim:" + claimNbr, "read"));
  }

  @Transactional
  public ClaimEntity createClaim(CreateClaimRequest request) {
    ClaimEntity claim = new ClaimEntity();
    claim.setClaimNbr(request.claimNbr());
    claim.setPolNbr(request.polNbr());
    claim.setCustId(request.custId());
    claim.setLossDate(request.lossDate());
    claim.setClaimType(request.claimType());
    claim.setClaimStatus("O");
    return claimRepository.save(claim);
  }

  @Transactional(readOnly = true)
  public List<ClaimReserveEntity> listReserves(String claimNbr) {
    requireClaim(claimNbr);
    return claimReserveRepository.findByClaimClaimNbr(claimNbr);
  }

  @Transactional
  public ClaimReserveEntity createReserve(String claimNbr, CreateReserveRequest request) {
    ClaimEntity claim = requireClaim(claimNbr);
    ClaimReserveEntity reserve = new ClaimReserveEntity();
    reserve.setClaim(claim);
    reserve.setReserveType(request.reserveType());
    reserve.setApprovedAmt(request.approvedAmt());
    reserve.setPaidToDate(BigDecimal.ZERO);
    reserve.setReserveStatus("O");
    return claimReserveRepository.save(reserve);
  }

  @Transactional(readOnly = true)
  public List<ApprovalEntity> listApprovals(String claimNbr) {
    requireClaim(claimNbr);
    return approvalRepository.findByClaimClaimNbr(claimNbr);
  }

  @Transactional
  public ApprovalEntity createApproval(String claimNbr, CreateApprovalRequest request) {
    ClaimEntity claim = requireClaim(claimNbr);
    ClaimReserveEntity reserve =
        claimReserveRepository
            .findById(request.reserveId())
            .filter(r -> r.getClaim().getClaimNbr().equals(claimNbr))
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Reserve not found for claim: " + claimNbr,
                        "system",
                        "claim:" + claimNbr + "/reserve:" + request.reserveId(),
                        "create-approval"));

    ApprovalEntity approval = new ApprovalEntity();
    approval.setClaim(claim);
    approval.setReserve(reserve);
    approval.setApproverId(request.approverId());
    approval.setApprovalStatus("P");
    return approvalRepository.save(approval);
  }

  private ClaimEntity requireClaim(String claimNbr) {
    return claimRepository
        .findById(claimNbr)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Claim not found: " + claimNbr, "system", "claim:" + claimNbr, "read"));
  }
}
