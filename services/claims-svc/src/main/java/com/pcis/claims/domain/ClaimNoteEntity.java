package com.pcis.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "claim_note")
public class ClaimNoteEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "note_id", nullable = false)
  private Long noteId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "claim_nbr", nullable = false)
  private ClaimEntity claim;

  @Column(name = "note_text", nullable = false, columnDefinition = "TEXT")
  private String noteText;

  public Long getNoteId() { return noteId; }

  public ClaimEntity getClaim() { return claim; }
  public void setClaim(ClaimEntity claim) { this.claim = claim; }

  public String getNoteText() { return noteText; }
  public void setNoteText(String noteText) { this.noteText = noteText; }
}
