      *================================================================*
      * PROGRAM:     CLM006B                                          *
      * MODULE:      CLAIMS                                           *
      * TYPE:        BATCH                                            *
      * DESCRIPTION: Claim Payment Batch Processing                   *
      *              Reads approved claims from CLAIM_T and issues    *
      *              payments for the full outstanding reserve amount. *
      *              GAP G-02: No SECCHK01 authority check performed. *
      *              GAP X-07: APPROVAL_T linkage not enforced here.  *
      *----------------------------------------------------------------*
      * CALLS:       AUDLOG01                                         *
      *              CLMVAL01                                         *
      * TABLES:      CLAIM_T                                          *
      *              CLAIM_PAYMENT_T                                  *
      *              CLAIM_RESERVE_T                                  *
      *              CLAIM_ADJUSTER_T                                 *
      * UI:          CLMPAYD1                                         *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CLM006B.
       AUTHOR. PCIS-DEVELOPMENT.
       DATE-WRITTEN. 2023-09-12.
      *
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ISERIES.
       OBJECT-COMPUTER. IBM-ISERIES.
      *
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
      *
       DATA DIVISION.
      *
       WORKING-STORAGE SECTION.
      *
       01  WS-PROGRAM-NAME         PIC X(8)  VALUE 'CLM006B '.
       01  WS-STATUS               PIC X(1)  VALUE ' '.
           88  WS-STATUS-OK                  VALUE ' '.
           88  WS-STATUS-AUDIT-FAIL          VALUE 'A'.
      *
       01  WS-COUNTERS.
           05  WS-CNT-READ         PIC 9(7)  VALUE ZEROS.
           05  WS-CNT-PAID         PIC 9(7)  VALUE ZEROS.
           05  WS-CNT-SKIPPED      PIC 9(7)  VALUE ZEROS.
           05  WS-CNT-ERRORS       PIC 9(7)  VALUE ZEROS.
      *
      * Run-log timing and counter host variables (WO-237)
       01  WS-START-TIMESTAMP      PIC X(26).
       01  WS-END-TIMESTAMP        PIC X(26).
       01  WS-RL-PGM-NAME          PIC X(10) VALUE 'CLM006B'.
       01  WS-RL-RUN-DATE          PIC X(10).
       01  WS-RL-SELECTED          PIC S9(9) COMP-3 VALUE 0.
       01  WS-RL-UPDATED           PIC S9(9) COMP-3 VALUE 0.
       01  WS-RL-ERRORS            PIC S9(9) COMP-3 VALUE 0.
      *
       01  WS-CLAIM-RECORD.
           05  HV-CLAIM-ID         PIC 9(10).
           05  HV-POLICY-ID        PIC 9(10).
           05  HV-CUSTOMER-ID      PIC 9(10).
           05  HV-CLAIM-STATUS     PIC X(10).
           05  HV-APPROVED-AMT     PIC S9(11)V99 COMP-3.
           05  HV-PAID-TO-DATE     PIC S9(11)V99 COMP-3.
           05  HV-RESERVE-AMT      PIC S9(11)V99 COMP-3.
           05  HV-ADJUSTER-ID      PIC 9(10).
           05  HV-LOSS-DATE        PIC X(10).
           05  HV-APPROVAL-DATE    PIC X(10).
      *
       01  WS-PAYMENT-RECORD.
           05  HV-PMT-CLAIM-ID     PIC 9(10).
           05  HV-PMT-AMOUNT       PIC S9(11)V99 COMP-3.
           05  HV-PMT-DATE         PIC X(10).
           05  HV-PMT-STATUS       PIC X(10).
           05  HV-PMT-ADJUSTER     PIC 9(10).
      *
       01  WS-OUTSTANDING-AMT      PIC S9(11)V99 COMP-3.
       01  WS-PAYMENT-DATE         PIC X(10).
      *
       01  WS-SQL-CODE             PIC S9(9) COMP.
       01  WS-SQLSTATE             PIC X(5).
      *
      * Audit log parameter block (batch shape: X(3)/X(30))
       01  WS-AUDIT-PARMS.
           05  WS-AUD-ACTION       PIC X(3).
           05  WS-AUD-OBJECT       PIC X(30).
      *
       01  WS-EOF-FLAG             PIC X(1)  VALUE 'N'.
           88  WS-EOF                        VALUE 'Y'.
      *
       01  WS-CHUNK-SIZE           PIC 9(5)  VALUE 00001.
       01  WS-CHUNK-COUNT          PIC 9(7)  VALUE ZEROS.
      *
      * Run-log fields
       01  WS-RUN-LOG.
           05  WS-RUN-DATE         PIC X(10).
           05  WS-RUN-TIME         PIC X(8).
           05  WS-RUN-USER         PIC X(10) VALUE 'BATCHUSR  '.
           05  WS-RUN-JOB          PIC X(10) VALUE 'CLM006B   '.
           05  WS-RUN-STATUS       PIC X(10) VALUE 'RUNNING   '.
      *
      * Payment window control
       01  WS-MAX-PAYMENT-AMT      PIC S9(11)V99 COMP-3
                                   VALUE 9999999.99.
       01  WS-MIN-PAYMENT-AMT      PIC S9(11)V99 COMP-3
                                   VALUE 0.01.
       01  WS-PAYMENT-VALID        PIC X(1)  VALUE 'Y'.
           88  WS-PAYMENT-IN-RANGE           VALUE 'Y'.
      *
      * Reserve update fields
       01  WS-NEW-RESERVE-AMT      PIC S9(11)V99 COMP-3.
       01  WS-RESERVE-THRESHOLD    PIC S9(11)V99 COMP-3
                                   VALUE 0.01.
      *
       PROCEDURE DIVISION.
      *
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS-CLAIMS
               UNTIL WS-EOF
           PERFORM 9000-FINALIZE
           STOP RUN.
      *
       1000-INITIALIZE.
           EXEC SQL
               VALUES (CURRENT TIMESTAMP)
               INTO :WS-START-TIMESTAMP
           END-EXEC
           MOVE 'N' TO WS-EOF-FLAG
           MOVE ZEROS TO WS-COUNTERS
           PERFORM 1100-OPEN-CURSOR
           .
      *
       1100-OPEN-CURSOR.
           EXEC SQL
               DECLARE CLAIM_CUR CURSOR FOR
               SELECT C.CLAIM_ID,
                      C.POLICY_ID,
                      C.CUSTOMER_ID,
                      C.CLAIM_STATUS,
                      C.APPROVED_AMOUNT,
                      C.PAID_TO_DATE,
                      C.RESERVE_AMOUNT,
                      C.ADJUSTER_ID,
                      CHAR(C.LOSS_DATE, ISO),
                      CHAR(C.APPROVAL_DATE, ISO)
               FROM   INSPRDDTA.CLAIM_T C
               WHERE  C.CLAIM_STATUS = 'APPROVED'
               AND    C.PAID_TO_DATE < C.APPROVED_AMOUNT
               ORDER  BY C.CLAIM_ID
               FOR UPDATE
           END-EXEC
           EXEC SQL OPEN CLAIM_CUR END-EXEC
           MOVE SQLCODE TO WS-SQL-CODE
           IF WS-SQL-CODE NOT EQUAL ZEROS
               DISPLAY 'CLM006B: OPEN CURSOR FAILED SQLCODE=' WS-SQL-CODE
               MOVE 'Y' TO WS-EOF-FLAG
           END-IF
           .
      *
       2000-PROCESS-CLAIMS.
           PERFORM 2100-FETCH-CLAIM
           IF NOT WS-EOF
               PERFORM 2200-VALIDATE-CLAIM
               IF WS-STATUS-OK
                   PERFORM 2300-CALCULATE-PAYMENT
                   PERFORM 2400-INSERT-PAYMENT
                   PERFORM 2500-UPDATE-CLAIM
                   PERFORM 2600-AUDIT-PAYMENT
               END-IF
           END-IF
           .
      *
       2100-FETCH-CLAIM.
           EXEC SQL
               FETCH CLAIM_CUR
               INTO  :HV-CLAIM-ID,
                     :HV-POLICY-ID,
                     :HV-CUSTOMER-ID,
                     :HV-CLAIM-STATUS,
                     :HV-APPROVED-AMT,
                     :HV-PAID-TO-DATE,
                     :HV-RESERVE-AMT,
                     :HV-ADJUSTER-ID,
                     :HV-LOSS-DATE,
                     :HV-APPROVAL-DATE
           END-EXEC
           MOVE SQLCODE TO WS-SQL-CODE
           IF WS-SQL-CODE EQUAL +100
               MOVE 'Y' TO WS-EOF-FLAG
           ELSE IF WS-SQL-CODE NOT EQUAL ZEROS
               DISPLAY 'CLM006B: FETCH ERROR SQLCODE=' WS-SQL-CODE
               ADD 1 TO WS-CNT-ERRORS
               MOVE 'Y' TO WS-EOF-FLAG
           ELSE
               ADD 1 TO WS-CNT-READ
           END-IF
           .
      *
       2200-VALIDATE-CLAIM.
           MOVE ' ' TO WS-STATUS
           CALL 'CLMVAL01' USING HV-CLAIM-ID WS-STATUS
           IF NOT WS-STATUS-OK
               ADD 1 TO WS-CNT-SKIPPED
           END-IF
           .
      *
       2300-CALCULATE-PAYMENT.
      * ADR-003: Batch path pays full outstanding amount for PARITY
           COMPUTE WS-OUTSTANDING-AMT =
               HV-APPROVED-AMT - HV-PAID-TO-DATE
           MOVE FUNCTION CURRENT-DATE(1:10) TO WS-PAYMENT-DATE
           .
      *
       2400-INSERT-PAYMENT.
           MOVE HV-CLAIM-ID     TO HV-PMT-CLAIM-ID
           MOVE WS-OUTSTANDING-AMT TO HV-PMT-AMOUNT
           MOVE WS-PAYMENT-DATE TO HV-PMT-DATE
           MOVE 'PROCESSED'     TO HV-PMT-STATUS
           MOVE HV-ADJUSTER-ID  TO HV-PMT-ADJUSTER
           EXEC SQL
               INSERT INTO INSPRDDTA.CLAIM_PAYMENT_T
               (CLAIM_ID, PAYMENT_AMOUNT, PAYMENT_DATE,
                PAYMENT_STATUS, ADJUSTER_ID)
               VALUES (:HV-PMT-CLAIM-ID, :HV-PMT-AMOUNT,
                       :HV-PMT-DATE, :HV-PMT-STATUS, :HV-PMT-ADJUSTER)
           END-EXEC
           MOVE SQLCODE TO WS-SQL-CODE
           IF WS-SQL-CODE NOT EQUAL ZEROS
               DISPLAY 'CLM006B: INSERT PAYMENT FAILED CLAIM='
                       HV-CLAIM-ID ' SQLCODE=' WS-SQL-CODE
               ADD 1 TO WS-CNT-ERRORS
               MOVE 'A' TO WS-STATUS
           ELSE
               ADD 1 TO WS-CNT-PAID
           END-IF
           .
      *
       2500-UPDATE-CLAIM.
           IF WS-STATUS-OK
               EXEC SQL
                   UPDATE INSPRDDTA.CLAIM_T
                   SET    PAID_TO_DATE = PAID_TO_DATE + :HV-PMT-AMOUNT,
                          CLAIM_STATUS = 'PAID',
                          LAST_UPDATED = CURRENT_TIMESTAMP
                   WHERE  CLAIM_ID = :HV-CLAIM-ID
               END-EXEC
               MOVE SQLCODE TO WS-SQL-CODE
               IF WS-SQL-CODE NOT EQUAL ZEROS
                   DISPLAY 'CLM006B: UPDATE CLAIM FAILED CLAIM='
                           HV-CLAIM-ID ' SQLCODE=' WS-SQL-CODE
                   ADD 1 TO WS-CNT-ERRORS
               END-IF
           END-IF
           .
      *
       2600-AUDIT-PAYMENT.
      * GAP G-05: Audit failure does not roll back — known defect
           MOVE 'PAY' TO WS-AUD-ACTION
           MOVE HV-CLAIM-ID TO WS-AUD-OBJECT(1:10)
           CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           .
      *
       9000-FINALIZE.
           EXEC SQL CLOSE CLAIM_CUR END-EXEC
           PERFORM 9100-LOG-RUN-SUMMARY
           DISPLAY 'CLM006B: COMPLETE'
           DISPLAY '  CLAIMS READ:    ' WS-CNT-READ
           DISPLAY '  CLAIMS PAID:    ' WS-CNT-PAID
           DISPLAY '  CLAIMS SKIPPED: ' WS-CNT-SKIPPED
           DISPLAY '  ERRORS:         ' WS-CNT-ERRORS
           PERFORM 8000-WRITE-RUN-LOG
           .
      *
       8000-WRITE-RUN-LOG.
           EXEC SQL
               VALUES (CURRENT TIMESTAMP)
               INTO :WS-END-TIMESTAMP
           END-EXEC
           EXEC SQL
               SELECT CHAR(CURRENT_DATE, ISO)
               INTO   :WS-RL-RUN-DATE
               FROM   SYSIBM.SYSDUMMY1
           END-EXEC
           MOVE WS-CNT-READ TO WS-RL-SELECTED
           MOVE WS-CNT-PAID TO WS-RL-UPDATED
           MOVE WS-CNT-ERRORS TO WS-RL-ERRORS
           EXEC SQL
               INSERT INTO RPT_RUN_LOG_T
                   (PGM_NAME,
                    RUN_DATE,
                    REC_SELECTED,
                    REC_UPDATED,
                    REC_ERRORS,
                    START_TIMESTAMP,
                    END_TIMESTAMP,
                    CRT_TIMESTAMP)
               VALUES
                   (:WS-RL-PGM-NAME,
                    :WS-RL-RUN-DATE,
                    :WS-RL-SELECTED,
                    :WS-RL-UPDATED,
                    :WS-RL-ERRORS,
                    :WS-START-TIMESTAMP,
                    :WS-END-TIMESTAMP,
                    CURRENT TIMESTAMP)
           END-EXEC
           MOVE SQLCODE TO WS-SQL-CODE
           IF WS-SQL-CODE NOT EQUAL ZEROS
               DISPLAY 'CLM006B: RUN LOG INSERT ERROR SQLCODE='
                       WS-SQL-CODE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .
      *
       9100-LOG-RUN-SUMMARY.
           MOVE FUNCTION CURRENT-DATE(1:10) TO WS-RUN-DATE
           MOVE FUNCTION CURRENT-DATE(12:8) TO WS-RUN-TIME
           IF WS-CNT-ERRORS GREATER THAN ZEROS
               MOVE 'COMPLETED ' TO WS-RUN-STATUS
           ELSE
               MOVE 'SUCCESS   ' TO WS-RUN-STATUS
           END-IF
           DISPLAY 'CLM006B: RUN STATUS=' WS-RUN-STATUS
           .
