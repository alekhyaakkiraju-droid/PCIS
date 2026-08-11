      *================================================================*
      * PROGRAM:     PRM005B                                          *
      * MODULE:      PREMIUM                                          *
      * TYPE:        BATCH                                            *
      * DESCRIPTION: Premium Delinquency Batch                        *
      *----------------------------------------------------------------*
      * CALLS:       PRMCLC01                                         *
      *              AUDLOG01                                         *
      * TABLES:      BILLING_SCHEDULE_T                               *
      *              POLICY_T                                         *
      *              CUSTOMER_T                                       *
      * UI:                                                           *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PRM005B.
       AUTHOR. PCIS-DEVELOPMENT.
       DATE-WRITTEN. 2024-02-10.
      *----------------------------------------------------------------*
      * Premium Delinquency Batch program.                            *
      * Reads overdue billing schedules, updates delinquency status,  *
      * increments recalculated counter on status updates.            *
      * ADR-002: Prologue lists PRMCLC01 as a called program but the  *
      * procedure division contains no CALL 'PRMCLC01' statement.    *
      * This discrepancy was identified during architecture review.   *
      *----------------------------------------------------------------*
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ISERIES.
       OBJECT-COMPUTER. IBM-ISERIES.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
           EXEC SQL INCLUDE SQLCA END-EXEC.

      * Counters
       01  WS-COUNTERS.
           05  WS-CNT-READ            PIC S9(7)      COMP-3 VALUE 0.
           05  WS-CNT-UPDATED         PIC S9(7)      COMP-3 VALUE 0.
           05  WS-CNT-RECALCULATED    PIC S9(7)      COMP-3 VALUE 0.
           05  WS-CNT-ERRORS          PIC S9(7)      COMP-3 VALUE 0.

      * Run-log timing and counter host variables (WO-237)
       01  WS-START-TIMESTAMP         PIC X(26).
       01  WS-END-TIMESTAMP           PIC X(26).
       01  WS-RL-PGM-NAME             PIC X(10)      VALUE 'PRM005B'.
       01  WS-RL-RUN-DATE             PIC X(10).
       01  WS-RL-SELECTED             PIC S9(9)      COMP-3 VALUE 0.
       01  WS-RL-UPDATED              PIC S9(9)      COMP-3 VALUE 0.
       01  WS-RL-ERRORS               PIC S9(9)      COMP-3 VALUE 0.
       01  WS-RL-DELINQUENT           PIC S9(9)      COMP-3 VALUE 0.

      * Return code and status
       01  WS-RETURN-CODE             PIC S9(4)      COMP VALUE 0.
       01  WS-SQLCODE-SAVE            PIC S9(9)      COMP VALUE 0.
       01  WS-END-OF-CURSOR           PIC X          VALUE 'N'.

      * Delinquency threshold days
       01  WS-DELINQ-DAYS             PIC S9(4)      COMP VALUE 30.

      * Audit logging parameters
       01  WS-AUDIT-PARMS.
           05  WS-AUD-PROGRAM         PIC X(10)      VALUE 'PRM005B'.
           05  WS-AUD-ACTION          PIC X(10)      VALUE SPACES.
           05  WS-AUD-TABLE           PIC X(20)      VALUE SPACES.
           05  WS-AUD-KEY             PIC X(30)      VALUE SPACES.
           05  WS-AUD-USER            PIC X(10)      VALUE SPACES.
           05  WS-AUD-RESULT          PIC X(4)       VALUE SPACES.

      * Host variables for SQL
       01  WS-HOST-VARS.
           05  HV-SCHED-ID            PIC S9(9)      COMP.
           05  HV-POLICY-ID           PIC X(20).
           05  HV-CUSTOMER-ID         PIC X(10).
           05  HV-DUE-DATE            PIC X(10).
           05  HV-AMOUNT-DUE          PIC S9(9)V9(2) COMP-3.
           05  HV-OLD-STATUS          PIC X(10).
           05  HV-DAYS-OVERDUE        PIC S9(7)      COMP-3.
           05  HV-CUST-EMAIL          PIC X(80).

       PROCEDURE DIVISION.

      * Main control paragraph
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-OPEN-CURSOR
           PERFORM 3000-PROCESS-LOOP
               UNTIL WS-END-OF-CURSOR = 'Y'
           PERFORM 4000-FINALIZE
           STOP RUN.

      * Initialization
       1000-INITIALIZE.
           EXEC SQL
               VALUES (CURRENT TIMESTAMP)
               INTO :WS-START-TIMESTAMP
           END-EXEC
           MOVE 0  TO WS-CNT-READ
           MOVE 0  TO WS-CNT-UPDATED
           MOVE 0  TO WS-CNT-RECALCULATED
           MOVE 0  TO WS-CNT-ERRORS
           MOVE 'N' TO WS-END-OF-CURSOR
           MOVE 0  TO WS-RETURN-CODE
           MOVE 'INIT' TO WS-AUD-ACTION
           MOVE 'BILLING_SCHEDULE_T' TO WS-AUD-TABLE
           CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           .

      * Open cursor for overdue billing schedule records
       2000-OPEN-CURSOR.
           EXEC SQL
               DECLARE DLQ_CURSOR CURSOR FOR
               SELECT B.SCHED_ID,
                      B.POLICY_ID,
                      P.CUSTOMER_ID,
                      B.DUE_DATE,
                      B.AMOUNT_DUE,
                      B.STATUS,
                      DAYS(CURRENT_DATE) - DAYS(B.DUE_DATE)
               FROM   BILLING_SCHEDULE_T B
               JOIN   POLICY_T P
                   ON P.POLICY_ID = B.POLICY_ID
               WHERE  B.STATUS IN ('OPEN', 'PART')
               AND    B.DUE_DATE < CURRENT_DATE
               ORDER BY B.DUE_DATE
           END-EXEC
           EXEC SQL
               OPEN DLQ_CURSOR
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               DISPLAY 'PRM005B: ERROR OPENING CURSOR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
               MOVE 8 TO WS-RETURN-CODE
               MOVE 'Y' TO WS-END-OF-CURSOR
           END-IF
           .

      * Process loop
       3000-PROCESS-LOOP.
           PERFORM 3100-FETCH-RECORD
           IF WS-END-OF-CURSOR = 'N'
               PERFORM 3200-UPDATE-DELINQUENCY
               ADD 1 TO WS-CNT-READ
           END-IF
           .

      * Fetch next overdue record
       3100-FETCH-RECORD.
           EXEC SQL
               FETCH DLQ_CURSOR
               INTO  :HV-SCHED-ID,
                     :HV-POLICY-ID,
                     :HV-CUSTOMER-ID,
                     :HV-DUE-DATE,
                     :HV-AMOUNT-DUE,
                     :HV-OLD-STATUS,
                     :HV-DAYS-OVERDUE
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           EVALUATE TRUE
               WHEN SQLCODE = 0
                   CONTINUE
               WHEN SQLCODE = 100
                   MOVE 'Y' TO WS-END-OF-CURSOR
               WHEN OTHER
                   DISPLAY 'PRM005B: FETCH ERROR SQLCODE='
                           WS-SQLCODE-SAVE
                   ADD 1 TO WS-CNT-ERRORS
                   MOVE 'Y' TO WS-END-OF-CURSOR
           END-EVALUATE
           .

      * Update delinquency status based on days overdue.
      * Increments WS-CNT-RECALCULATED when status is changed.
       3200-UPDATE-DELINQUENCY.
           EVALUATE TRUE
               WHEN HV-DAYS-OVERDUE > 90
                   EXEC SQL
                       UPDATE BILLING_SCHEDULE_T
                       SET    STATUS = 'DLNQ3',
                              LAST_UPDATE_TS = CURRENT_TIMESTAMP
                       WHERE  SCHED_ID = :HV-SCHED-ID
                       AND    STATUS != 'DLNQ3'
                   END-EXEC
               WHEN HV-DAYS-OVERDUE > 60
                   EXEC SQL
                       UPDATE BILLING_SCHEDULE_T
                       SET    STATUS = 'DLNQ2',
                              LAST_UPDATE_TS = CURRENT_TIMESTAMP
                       WHERE  SCHED_ID = :HV-SCHED-ID
                       AND    STATUS != 'DLNQ2'
                   END-EXEC
               WHEN HV-DAYS-OVERDUE > WS-DELINQ-DAYS
                   EXEC SQL
                       UPDATE BILLING_SCHEDULE_T
                       SET    STATUS = 'DLNQ1',
                              LAST_UPDATE_TS = CURRENT_TIMESTAMP
                       WHERE  SCHED_ID = :HV-SCHED-ID
                       AND    STATUS != 'DLNQ1'
                   END-EXEC
               WHEN OTHER
                   CONTINUE
           END-EVALUATE
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE = 0
               ADD 1 TO WS-CNT-UPDATED
               ADD 1 TO WS-CNT-RECALCULATED
               MOVE 'UPDATE' TO WS-AUD-ACTION
               MOVE 'BILLING_SCHEDULE_T' TO WS-AUD-TABLE
               MOVE HV-POLICY-ID TO WS-AUD-KEY
               CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           END-IF
           IF SQLCODE < 0
               DISPLAY 'PRM005B: UPDATE ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

      * Finalize - close cursor, commit, display counts
       4000-FINALIZE.
           EXEC SQL
               CLOSE DLQ_CURSOR
           END-EXEC
           EXEC SQL
               COMMIT
           END-EXEC
           DISPLAY 'PRM005B PREMIUM DELINQUENCY COMPLETE'
           DISPLAY '  RECORDS READ    : ' WS-CNT-READ
           DISPLAY '  STATUSES UPDATED: ' WS-CNT-UPDATED
           DISPLAY '  RECALCULATED    : ' WS-CNT-RECALCULATED
           DISPLAY '  ERRORS          : ' WS-CNT-ERRORS
           MOVE 'FINALIZE' TO WS-AUD-ACTION
           MOVE 'BILLING_SCHEDULE_T' TO WS-AUD-TABLE
           CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           PERFORM 8000-WRITE-RUN-LOG
           IF WS-CNT-ERRORS > 0
               MOVE 8 TO WS-RETURN-CODE
           END-IF
           MOVE WS-RETURN-CODE TO RETURN-CODE
           .

      * Write batch run-log row with wall-clock timing (WO-237)
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
           MOVE WS-CNT-READ         TO WS-RL-SELECTED
           MOVE WS-CNT-UPDATED      TO WS-RL-UPDATED
           MOVE WS-CNT-ERRORS       TO WS-RL-ERRORS
           MOVE WS-CNT-RECALCULATED TO WS-RL-DELINQUENT
           EXEC SQL
               INSERT INTO RPT_RUN_LOG_T
                   (PGM_NAME,
                    RUN_DATE,
                    REC_SELECTED,
                    REC_UPDATED,
                    REC_ERRORS,
                    REC_DELINQUENT,
                    START_TIMESTAMP,
                    END_TIMESTAMP,
                    CRT_TIMESTAMP)
               VALUES
                   (:WS-RL-PGM-NAME,
                    :WS-RL-RUN-DATE,
                    :WS-RL-SELECTED,
                    :WS-RL-UPDATED,
                    :WS-RL-ERRORS,
                    :WS-RL-DELINQUENT,
                    :WS-START-TIMESTAMP,
                    :WS-END-TIMESTAMP,
                    CURRENT TIMESTAMP)
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               DISPLAY 'PRM005B: RUN LOG INSERT ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

       END PROGRAM PRM005B.
