package com.pcis.claims.application;

import com.pcis.claims.domain.ApprovalEntity;
import com.pcis.claims.domain.ClaimAdjusterEntity;
import com.pcis.claims.domain.ClaimEntity;
import com.pcis.claims.domain.ClaimNoteEntity;
import com.pcis.claims.domain.ClaimPaymentEntity;
import com.pcis.claims.domain.ClaimReserveEntity;
import com.pcis.claims.domain.ClaimReserveLedgerEntity;
import com.pcis.claims.domain.repository.ApprovalRepository;
import com.pcis.claims.domain.repository.ClaimAdjusterRepository;
import com.pcis.claims.domain.repository.ClaimNoteRepository;
import com.pcis.claims.domain.repository.ClaimPaymentRepository;
import com.pcis.claims.domain.repository.ClaimRepository;
import com.pcis.claims.domain.repository.ClaimReserveLedgerRepository;
import com.pcis.claims.domain.repository.ClaimReserveRepository;
import com.pcis.claims.dto.ClaimDetailResponse;
import com.pcis.claims.dto.ClaimListItemResponse;
import com.pcis.claims.dto.ClaimResponseMapper;
import com.pcis.claims.dto.CreateApprovalRequest;
import com.pcis.claims.dto.CreateClaimRequest;
import com.pcis.claims.dto.CreateNoteRequest;
import com.pcis.claims.dto.CreatePaymentRequest;
import com.pcis.claims.dto.CreateReserveRequest;
import com.pcis.claims.dto.UpdateClaimRequest;
import com.pcis.claims.exception.DuplicateApprovalException;
import com.pcis.claims.integration.PolicyInForceValidator;
import com.pcis.claims.outbox.ClaimsOutboxWriter;
import com.pcis.claims.security.SecurityPrincipalAccessor;
import com.pcis.error.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

  private static final String DEFAULT_ADJUSTER_ID = "ADJ90001";
  private static final AtomicLong CLAIM_SEQUENCE = new AtomicLong(System.nanoTime() % 1_000_000_000L);

  private final ClaimRepository claimRepository;
  private final ClaimReserveRepository claimReserveRepository;
  private final ClaimReserveLedgerRepository claimReserveLedgerRepository;
  private final ApprovalRepository approvalRepository;
  private final ClaimPaymentRepository claimPaymentRepository;
  private final ClaimNoteRepository claimNoteRepository;
  private final ClaimAdjusterRepository claimAdjusterRepository;
  private final PaymentAuthorityService paymentAuthorityService;
  private final ClaimsOutboxWriter claimsOutboxWriter;
  private final SecurityPrincipalAccessor securityPrincipalAccessor;
  private final ClaimResponseMapper claimResponseMapper;
  private final PolicyInForceValidator policyInForceValidator;

  public ClaimsApplicationService(
      ClaimRepository claimRepository,
      ClaimReserveRepository claimReserveRepository,
      ClaimReserveLedgerRepository claimReserveLedgerRepository,
      ApprovalRepository approvalRepository,
      ClaimPaymentRepository claimPaymentRepository,
      ClaimNoteRepository claimNoteRepository,
      ClaimAdjusterRepository claimAdjusterRepository,
      PaymentAuthorityService paymentAuthorityService,
      ClaimsOutboxWriter claimsOutboxWriter,
      SecurityPrincipalAccessor securityPrincipalAccessor,
      ClaimResponseMapper claimResponseMapper,
      PolicyInForceValidator policyInForceValidator) {
    this.claimRepository = claimRepository;
    this.claimReserveRepository = claimReserveRepository;
    this.claimReserveLedgerRepository = claimReserveLedgerRepository;
    this.approvalRepository = approvalRepository;
    this.claimPaymentRepository = claimPaymentRepository;
    this.claimNoteRepository = claimNoteRepository;
    this.claimAdjusterRepository = claimAdjusterRepository;
    this.paymentAuthorityService = paymentAuthorityService;
    this.claimsOutboxWriter = claimsOutboxWriter;
    this.securityPrincipalAccessor = securityPrincipalAccessor;
    this.claimResponseMapper = claimResponseMapper;
    this.policyInForceValidator = policyInForceValidator;
  }

  @Transactional(readOnly = true)
  public List<ClaimEntity> listClaims(String status) {
    if (status == null || status.isBlank()) {
      return claimRepository.findAll();
    }
    return claimRepository.findByClaimStatus(status.trim());
  }

  @Transactional(readOnly = true)
  public List<ClaimListItemResponse> listClaimSummaries(String status, String view) {
    if (view == null || view.isBlank()) {
      return listClaims(status).stream().map(this::toListItem).toList();
    }
    String normalizedView = view.trim().toLowerCase();
    String statusFilter =
        switch (normalizedView) {
          case "closed" -> "C";
          case "open", "pending", "escalated" -> "O";
          default -> status;
        };
    List<ClaimEntity> claims = listClaims(statusFilter != null ? statusFilter : status);
    return claims.stream()
        .map(this::toListItem)
        .filter(item -> matchesView(item, normalizedView))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ClaimEntity> listClaimsByCustomer(Integer custId) {
    return claimRepository.findByCustId(custId);
  }

  @Transactional(readOnly = true)
  public ClaimDetailResponse getClaimDetail(String claimNbr) {
    ClaimEntity claim = requireClaim(claimNbr);
    List<ClaimReserveEntity> reserves = claimReserveRepository.findByClaimClaimNbr(claimNbr);
    BigDecimal reserveRemaining = computeReserveRemaining(reserves);
    AdjusterInfo adjuster = resolveAdjusterInfo(claim);
    return claimResponseMapper.toClaimDetailResponse(
        claim,
        resolveAuthorityLimit(),
        adjuster.id(),
        adjuster.name(),
        reserveRemaining,
        reserves,
        claimPaymentRepository.findByClaimClaimNbrOrderByPaymentIdAsc(claimNbr),
        claimNoteRepository.findByClaimClaimNbrOrderByNoteIdAsc(claimNbr),
        claimReserveLedgerRepository.findByClaimClaimNbrOrderByLedgerIdAsc(claimNbr));
  }

  @Transactional(readOnly = true)
  public ClaimEntity getClaim(String claimNbr) {
    return requireClaim(claimNbr);
  }

  @Transactional
  public ClaimEntity createClaim(CreateClaimRequest request) {
    policyInForceValidator.validate(request.polNbr(), request.custId(), request.lossDate());

    String claimNbr =
        request.claimNbr() == null || request.claimNbr().isBlank()
            ? generateClaimNbr()
            : request.claimNbr();

    String adjusterId = securityPrincipalAccessor.currentSubject();
    ensureAdjuster(adjusterId, new BigDecimal("25000.00"));

    ClaimEntity claim = new ClaimEntity();
    claim.setClaimNbr(claimNbr);
    claim.setPolNbr(request.polNbr());
    claim.setCustId(request.custId());
    claim.setLossDate(request.lossDate());
    claim.setClaimType(request.claimType());
    claim.setClaimStatus("O");
    claim.setAssignedAdjusterId(adjusterId);
    ClaimEntity saved = claimRepository.save(claim);

    if (request.description() != null && !request.description().isBlank()) {
      ClaimNoteEntity note = new ClaimNoteEntity();
      note.setClaim(saved);
      note.setNoteText(request.description());
      claimNoteRepository.save(note);
    }

    if (request.initialReserveAmt() != null
        && request.initialReserveAmt().compareTo(BigDecimal.ZERO) > 0) {
      String reserveType =
          request.initialReserveType() == null || request.initialReserveType().isBlank()
              ? "PRO"
              : request.initialReserveType();
      ClaimReserveEntity reserve = new ClaimReserveEntity();
      reserve.setClaim(saved);
      reserve.setReserveType(reserveType);
      reserve.setApprovedAmt(request.initialReserveAmt());
      reserve.setPaidToDate(BigDecimal.ZERO);
      reserve.setReserveStatus("O");
      ClaimReserveEntity savedReserve = claimReserveRepository.save(reserve);
      writeLedgerEntry(
          saved,
          savedReserve,
          saved.getLossDate(),
          "Initial FNOL reserve",
          request.initialReserveAmt(),
          request.initialReserveAmt(),
          adjusterId,
          "SET");
      writeOutbox(
          claimNbr,
          "ReserveCreated",
          Map.of(
              "claimNbr", claimNbr,
              "reserveId", savedReserve.getReserveId(),
              "approvedAmt", savedReserve.getApprovedAmt(),
              "reason", "Initial FNOL reserve"));
    }

    writeOutbox(
        claimNbr,
        "ClaimCreated",
        Map.of(
            "claimNbr", claimNbr,
            "polNbr", request.polNbr(),
            "claimType", request.claimType(),
            "claimStatus", "O",
            "adjusterId", adjusterId));
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
    String actor = securityPrincipalAccessor.currentSubject();
    String reason =
        request.reason() == null || request.reason().isBlank()
            ? "Reserve update"
            : request.reason();

    ClaimReserveEntity existingIncreaseTarget =
        claimReserveRepository.findByClaimClaimNbr(claimNbr).stream()
            .filter(
                reserve ->
                    "O".equals(reserve.getReserveStatus())
                        && reserve.getReserveType().equals(request.reserveType())
                        && request.approvedAmt().compareTo(reserve.getApprovedAmt()) > 0)
            .findFirst()
            .orElse(null);

    if (existingIncreaseTarget != null) {
      BigDecimal delta = request.approvedAmt().subtract(existingIncreaseTarget.getApprovedAmt());
      existingIncreaseTarget.setApprovedAmt(request.approvedAmt());
      ClaimReserveEntity saved = claimReserveRepository.save(existingIncreaseTarget);
      BigDecimal balanceAfter = saved.getApprovedAmt().subtract(saved.getPaidToDate());
      writeLedgerEntry(claim, saved, LocalDate.now(), reason, delta, balanceAfter, actor, "INCR");
      writeOutbox(
          claimNbr,
          "ReserveIncreased",
          Map.of(
              "claimNbr", claimNbr,
              "reserveId", saved.getReserveId(),
              "approvedAmt", saved.getApprovedAmt(),
              "reason", reason));
      return saved;
    }

    ClaimReserveEntity reserve = new ClaimReserveEntity();
    reserve.setClaim(claim);
    reserve.setReserveType(request.reserveType());
    reserve.setApprovedAmt(request.approvedAmt());
    reserve.setPaidToDate(BigDecimal.ZERO);
    reserve.setReserveStatus("O");
    ClaimReserveEntity saved = claimReserveRepository.save(reserve);
    writeLedgerEntry(
        claim, saved, LocalDate.now(), reason, request.approvedAmt(), request.approvedAmt(), actor, "SET");
    writeOutbox(
        claimNbr,
        "ReserveCreated",
        Map.of(
            "claimNbr", claimNbr,
            "reserveId", saved.getReserveId(),
            "approvedAmt", saved.getApprovedAmt(),
            "reason", reason));
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
    ClaimEntity claim = requireClaim(claimNbr);
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
    BigDecimal balanceAfter = reserve.getApprovedAmt().subtract(reserve.getPaidToDate());
    if (balanceAfter.compareTo(BigDecimal.ZERO) <= 0) {
      reserve.setReserveStatus("C");
    }
    claimReserveRepository.save(reserve);

    writeLedgerEntry(
        claim,
        reserve,
        LocalDate.now(),
        "Drawdown on payment CLM-PMT-" + String.format("%04d", saved.getPaymentId()),
        request.amount().negate(),
        balanceAfter.max(BigDecimal.ZERO),
        disburser,
        "DRAW");

    maybeCloseClaim(claim, claimNbr);

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

  private void maybeCloseClaim(ClaimEntity claim, String claimNbr) {
    List<ClaimReserveEntity> reserves = claimReserveRepository.findByClaimClaimNbr(claimNbr);
    boolean allClosed =
        !reserves.isEmpty()
            && reserves.stream()
                .allMatch(
                    reserve ->
                        "C".equals(reserve.getReserveStatus())
                            || reserve
                                    .getApprovedAmt()
                                    .subtract(reserve.getPaidToDate())
                                    .compareTo(BigDecimal.ZERO)
                                <= 0);
    if (allClosed && "O".equals(claim.getClaimStatus())) {
      claim.setClaimStatus("C");
      claimRepository.save(claim);
      writeOutbox(claimNbr, "ClaimClosed", Map.of("claimNbr", claimNbr, "claimStatus", "C"));
    }
  }

  private ClaimListItemResponse toListItem(ClaimEntity claim) {
    List<ClaimReserveEntity> reserves = claimReserveRepository.findByClaimClaimNbr(claim.getClaimNbr());
    BigDecimal totalApproved =
        reserves.stream()
            .map(ClaimReserveEntity::getApprovedAmt)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalPaid =
        reserves.stream().map(ClaimReserveEntity::getPaidToDate).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal reserveRemaining = computeReserveRemaining(reserves);
    AdjusterInfo adjuster = resolveAdjusterInfo(claim);
    return claimResponseMapper.toClaimListItemResponse(
        claim,
        reserveRemaining,
        totalApproved,
        totalPaid,
        adjuster.id(),
        adjuster.name(),
        computePendingApproval(reserves));
  }

  private boolean matchesView(ClaimListItemResponse item, String view) {
    return switch (view) {
      case "closed" -> "C".equals(item.claimStatus());
      case "pending" -> "O".equals(item.claimStatus()) && item.pendingApproval();
      case "escalated" -> false;
      default -> "O".equals(item.claimStatus()) && !item.pendingApproval();
    };
  }

  private boolean computePendingApproval(List<ClaimReserveEntity> reserves) {
    return reserves.stream()
        .anyMatch(
            reserve -> {
              if (!"O".equals(reserve.getReserveStatus())) {
                return false;
              }
              BigDecimal outstanding = reserve.getApprovedAmt().subtract(reserve.getPaidToDate());
              return reserve.getPaidToDate().compareTo(BigDecimal.ZERO) > 0
                  && outstanding.compareTo(BigDecimal.ZERO) > 0;
            });
  }

  private BigDecimal computeReserveRemaining(List<ClaimReserveEntity> reserves) {
    return reserves.stream()
        .filter(reserve -> "O".equals(reserve.getReserveStatus()))
        .map(reserve -> reserve.getApprovedAmt().subtract(reserve.getPaidToDate()))
        .filter(balance -> balance.compareTo(BigDecimal.ZERO) > 0)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private AdjusterInfo resolveAdjusterInfo(ClaimEntity claim) {
    String adjusterId =
        claim.getAssignedAdjusterId() != null ? claim.getAssignedAdjusterId() : DEFAULT_ADJUSTER_ID;
    String adjusterName =
        claimAdjusterRepository
            .findById(adjusterId)
            .map(ClaimAdjusterEntity::getAdjusterName)
            .orElse("K. Alvarez");
    return new AdjusterInfo(adjusterId, adjusterName);
  }

  private void writeLedgerEntry(
      ClaimEntity claim,
      ClaimReserveEntity reserve,
      LocalDate eventDate,
      String reason,
      BigDecimal amount,
      BigDecimal balanceAfter,
      String actorId,
      String eventType) {
    ClaimReserveLedgerEntity ledger = new ClaimReserveLedgerEntity();
    ledger.setClaim(claim);
    ledger.setReserve(reserve);
    ledger.setEventDate(eventDate);
    ledger.setReason(reason);
    ledger.setAmount(amount);
    ledger.setBalanceAfter(balanceAfter);
    ledger.setActorId(actorId);
    ledger.setEventType(eventType);
    claimReserveLedgerRepository.save(ledger);
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

  private record AdjusterInfo(String id, String name) {}
}
