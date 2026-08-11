package com.pcis.claims.application;

import com.pcis.claims.domain.ApprovalEntity;
import com.pcis.claims.domain.ClaimAdjusterEntity;
import com.pcis.claims.domain.ClaimEntity;
import com.pcis.claims.domain.ClaimNoteEntity;
import com.pcis.claims.domain.ClaimPaymentEntity;
import com.pcis.claims.domain.ClaimReserveEntity;
import com.pcis.claims.domain.repository.ApprovalRepository;
import com.pcis.claims.domain.repository.ClaimAdjusterRepository;
import com.pcis.claims.domain.repository.ClaimNoteRepository;
import com.pcis.claims.domain.repository.ClaimPaymentRepository;
import com.pcis.claims.domain.repository.ClaimRepository;
import com.pcis.claims.domain.repository.ClaimReserveRepository;
import com.pcis.claims.dto.ClaimDetailResponse;
import com.pcis.claims.dto.ClaimResponseMapper;
import com.pcis.claims.dto.CreateApprovalRequest;
import com.pcis.claims.dto.CreateClaimRequest;
import com.pcis.claims.dto.CreateNoteRequest;
import com.pcis.claims.dto.CreatePaymentRequest;
import com.pcis.claims.dto.CreateReserveRequest;
import com.pcis.claims.dto.UpdateClaimRequest;
import com.pcis.claims.exception.DuplicateApprovalException;
import com.pcis.claims.outbox.ClaimsOutboxWriter;
import com.pcis.claims.security.SecurityPrincipalAccessor;
import com.pcis.error.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimsApplicationService {

  private static final AtomicLong CLAIM_SEQUENCE = new AtomicLong(System.nanoTime() % 1_000_000_000L);

  private final ClaimRepository claimRepository;
  private final ClaimReserveRepository claimReserveRepository;
  private final ApprovalRepository approvalRepository;
  private final ClaimPaymentRepository claimPaymentRepository;
  private final ClaimNoteRepository claimNoteRepository;
  private final ClaimAdjusterRepository claimAdjusterRepository;
  private final PaymentAuthorityService paymentAuthorityService;
  private final ClaimsOutboxWriter claimsOutboxWriter;
  private final SecurityPrincipalAccessor securityPrincipalAccessor;
  private final ClaimResponseMapper claimResponseMapper;

  public ClaimsApplicationService(
      ClaimRepository claimRepository,
      ClaimReserveRepository claimReserveRepository,
      ApprovalRepository approvalRepository,
      ClaimPaymentRepository claimPaymentRepository,
      ClaimNoteRepository claimNoteRepository,
      ClaimAdjusterRepository claimAdjusterRepository,
      PaymentAuthorityService paymentAuthorityService,
      ClaimsOutboxWriter claimsOutboxWriter,
      SecurityPrincipalAccessor securityPrincipalAccessor,
      ClaimResponseMapper claimResponseMapper) {
    this.claimRepository = claimRepository;
    this.claimReserveRepository = claimReserveRepository;
    this.approvalRepository = approvalRepository;
    this.claimPaymentRepository = claimPaymentRepository;
    this.claimNoteRepository = claimNoteRepository;
    this.claimAdjusterRepository = claimAdjusterRepository;
    this.paymentAuthorityService = paymentAuthorityService;
    this.claimsOutboxWriter = claimsOutboxWriter;
    this.securityPrincipalAccessor = securityPrincipalAccessor;
    this.claimResponseMapper = claimResponseMapper;
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
  public ClaimDetailResponse getClaimDetail(String claimNbr) {
    ClaimEntity claim = requireClaim(claimNbr);
    BigDecimal authorityLimit = resolveAuthorityLimit();
    return claimResponseMapper.toClaimDetailResponse(
        claim,
        authorityLimit,
        claimReserveRepository.findByClaimClaimNbr(claimNbr),
        claimPaymentRepository.findByClaimClaimNbrOrderByPaymentIdAsc(claimNbr),
        claimNoteRepository.findByClaimClaimNbrOrderByNoteIdAsc(claimNbr));
  }

  @Transactional(readOnly = true)
  public ClaimEntity getClaim(String claimNbr) {
    return requireClaim(claimNbr);
  }

  @Transactional
  public ClaimEntity createClaim(CreateClaimRequest request) {
    String claimNbr =
        request.claimNbr() == null || request.claimNbr().isBlank()
            ? generateClaimNbr()
            : request.claimNbr();

    ClaimEntity claim = new ClaimEntity();
    claim.setClaimNbr(claimNbr);
    claim.setPolNbr(request.polNbr());
    claim.setCustId(request.custId());
    claim.setLossDate(request.lossDate());
    claim.setClaimType(request.claimType());
    claim.setClaimStatus("O");
    ClaimEntity saved = claimRepository.save(claim);

    if (request.description() != null && !request.description().isBlank()) {
      ClaimNoteEntity note = new ClaimNoteEntity();
      note.setClaim(saved);
      note.setNoteText(request.description());
      claimNoteRepository.save(note);
    }

    writeOutbox(
        claimNbr,
        "ClaimCreated",
        Map.of(
            "claimNbr", claimNbr,
            "polNbr", request.polNbr(),
            "claimType", request.claimType(),
            "claimStatus", "O"));
    return saved;
  }

  @Transactional
  public ClaimEntity updateClaim(String claimNbr, Long expectedVersion, UpdateClaimRequest request) {
    ClaimEntity claim = requireClaim(claimNbr);
    if (expectedVersion != null && !expectedVersion.equals(claim.getVersion())) {
      throw new OptimisticLockingFailureException("Version mismatch for claim " + claimNbr);
    }
    claim.setClaimStatus(request.claimStatus());
    if (request.lossDate() != null) {
      claim.setLossDate(request.lossDate());
    }
    if (request.claimType() != null && !request.claimType().isBlank()) {
      claim.setClaimType(request.claimType());
    }
    ClaimEntity saved = claimRepository.save(claim);
    writeOutbox(
        claimNbr,
        "ClaimUpdated",
        Map.of(
            "claimNbr", claimNbr,
            "claimStatus", saved.getClaimStatus(),
            "version", saved.getVersion()));
    return saved;
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
    ClaimReserveEntity saved = claimReserveRepository.save(reserve);
    writeOutbox(
        claimNbr,
        "ReserveCreated",
        Map.of(
            "claimNbr", claimNbr,
            "reserveId", saved.getReserveId(),
            "approvedAmt", saved.getApprovedAmt()));
    return saved;
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

    if (approvalRepository.existsByReserveReserveIdAndApprovalStatus(request.reserveId(), "A")) {
      throw new DuplicateApprovalException(request.reserveId());
    }

    String approverId = securityPrincipalAccessor.currentSubject();
    ApprovalEntity approval = new ApprovalEntity();
    approval.setClaim(claim);
    approval.setReserve(reserve);
    approval.setApproverId(approverId);
    approval.setApprovalStatus("A");
    approval.setApprovalDate(Instant.now());
    ApprovalEntity saved = approvalRepository.save(approval);
    writeOutbox(
        claimNbr,
        "ApprovalCreated",
        Map.of(
            "claimNbr", claimNbr,
            "approvalId", saved.getApprovalId(),
            "reserveId", request.reserveId(),
            "approverId", approverId));
    return saved;
  }

  @Transactional
  public ClaimPaymentEntity createPayment(String claimNbr, CreatePaymentRequest request) {
    requireClaim(claimNbr);
    String disburser = securityPrincipalAccessor.currentSubject();
    PaymentAuthorityService.PaymentAuthorizationResult auth =
        paymentAuthorityService.validatePayment(
            claimNbr, request.reserveId(), request.amount(), disburser);

    ClaimPaymentEntity payment = new ClaimPaymentEntity();
    payment.setClaim(auth.reserve().getClaim());
    payment.setPaymentAmt(request.amount());
    payment.setPaymentStatus("P");
    payment.setPayeeId(request.payeeId());
    payment.setApproval(auth.approval());
    payment.setAdjuster(auth.adjuster());
    ClaimPaymentEntity saved = claimPaymentRepository.save(payment);

    ClaimReserveEntity reserve = auth.reserve();
    reserve.setPaidToDate(reserve.getPaidToDate().add(request.amount()));
    claimReserveRepository.save(reserve);

    writeOutbox(
        claimNbr,
        "ClaimPaymentInitiated",
        Map.of(
            "claimNbr", claimNbr,
            "paymentId", saved.getPaymentId(),
            "paymentAmt", request.amount(),
            "approvalId", auth.approval().getApprovalId()));
    return saved;
  }

  @Transactional(readOnly = true)
  public List<ClaimPaymentEntity> listPayments(String claimNbr) {
    requireClaim(claimNbr);
    return claimPaymentRepository.findByClaimClaimNbrOrderByPaymentIdAsc(claimNbr);
  }

  @Transactional
  public ClaimNoteEntity createNote(String claimNbr, CreateNoteRequest request) {
    ClaimEntity claim = requireClaim(claimNbr);
    ClaimNoteEntity note = new ClaimNoteEntity();
    note.setClaim(claim);
    note.setNoteText(request.noteText());
    ClaimNoteEntity saved = claimNoteRepository.save(note);
    writeOutbox(
        claimNbr,
        "ClaimNoteCreated",
        Map.of("claimNbr", claimNbr, "noteId", saved.getNoteId()));
    return saved;
  }

  @Transactional
  public ClaimAdjusterEntity ensureAdjuster(String adjusterId, BigDecimal authorityLimit) {
    return claimAdjusterRepository
        .findById(adjusterId)
        .orElseGet(
            () -> {
              ClaimAdjusterEntity adjuster = new ClaimAdjusterEntity();
              adjuster.setAdjusterId(adjusterId);
              adjuster.setAdjusterName(adjusterId);
              adjuster.setAuthorityLimit(authorityLimit);
              return claimAdjusterRepository.save(adjuster);
            });
  }

  private BigDecimal resolveAuthorityLimit() {
    if (!securityPrincipalAccessor.hasAuthority("CLAIMS_ADJUSTER")
        && !securityPrincipalAccessor.hasAuthority("claims:write")) {
      return null;
    }
    return claimAdjusterRepository
        .findById(securityPrincipalAccessor.currentSubject())
        .map(ClaimAdjusterEntity::getAuthorityLimit)
        .orElse(null);
  }

  private ClaimEntity requireClaim(String claimNbr) {
    return claimRepository
        .findById(claimNbr)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Claim not found: " + claimNbr, "system", "claim:" + claimNbr, "read"));
  }

  private void writeOutbox(String claimNbr, String eventType, Map<String, Object> payload) {
    Map<String, Object> enriched = new HashMap<>(payload);
    claimsOutboxWriter.writeDomainEvent(claimNbr, eventType, enriched, UUID.randomUUID());
  }

  static String generateClaimNbr() {
    long seq = CLAIM_SEQUENCE.incrementAndGet() % 1_000_000_000L;
    return "CLM" + String.format("%09d", seq);
  }
}
