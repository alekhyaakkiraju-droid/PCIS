      *================================================================*
      * PROGRAM:     POL006B                                          *
      * MODULE:      POLICY                                           *
      * TYPE:        BATCH                                            *
      * DESCRIPTION: Policy Renewal Batch                             *
      *----------------------------------------------------------------*
      * CALLS:       AUDLOG01                                         *
      *              PRMCLC01                                         *
      *              POLVAL01                                         *
      * TABLES:      POLICY_T                                         *
      *              COVERAGE_T                                       *
      *              BILLING_SCHEDULE_T                               *
      *              BILLING_PLAN_T                                   *
      * UI:                                                           *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. POL006B.
       AUTHOR. PCIS-DEVELOPMENT.
       DATE-WRITTEN. 2024-02-01.
      *----------------------------------------------------------------*
      * Policy Renewal Batch program.                                 *
      * Reads policies expiring within 30 days, calculates renewal    *
      * premium via PRMCLC01, validates via POLVAL01, creates new     *
      * policy term, updates billing plan, and logs via AUDLOG01.     *
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
           05  WS-CNT-RENEWED         PIC S9(7)      COMP-3 VALUE 0.
           05  WS-CNT-SKIPPED         PIC S9(7)      COMP-3 VALUE 0.
           05  WS-CNT-ERRORS          PIC S9(7)      COMP-3 VALUE 0.

      * Return code and status
       01  WS-RETURN-CODE             PIC S9(4)      COMP VALUE 0.
       01  WS-SQLCODE-SAVE            PIC S9(9)      COMP VALUE 0.
       01  WS-END-OF-CURSOR           PIC X          VALUE 'N'.
       01  WS-SKIP-FLAG               PIC X          VALUE 'N'.

      * Date arithmetic work fields
       01  WS-DATE-WORK               PIC 9(8).
       01  WS-NEW-EFF-DATE            PIC X(10).
       01  WS-NEW-EXP-DATE            PIC X(10).

      * Audit logging parameters
       01  WS-AUDIT-PARMS.
           05  WS-AUD-PROGRAM         PIC X(10)      VALUE 'POL006B'.
           05  WS-AUD-ACTION          PIC X(10)      VALUE SPACES.
           05  WS-AUD-TABLE           PIC X(20)      VALUE SPACES.
           05  WS-AUD-KEY             PIC X(30)      VALUE SPACES.
           05  WS-AUD-USER            PIC X(10)      VALUE SPACES.
           05  WS-AUD-RESULT          PIC X(4)       VALUE SPACES.

      * Subprogram parameter areas
       01  WS-PRMCLC-PARMS.
           05  WS-PRM-POLICY-ID       PIC X(20).
           05  WS-PRM-POLICY-TYPE     PIC X(4).
           05  WS-PRM-PREMIUM-AMT     PIC S9(9)V9(2) COMP-3.
           05  WS-PRM-RESULT          PIC X(4).

       01  WS-POLVAL-PARMS.
           05  WS-VAL-POLICY-ID       PIC X(20).
           05  WS-VAL-RESULT          PIC X(4).
           05  WS-VAL-MSG             PIC X(78).

      * Host variables for SQL
       01  WS-HOST-VARS.
           05  HV-POLICY-ID           PIC X(20).
           05  HV-NEW-POLICY-ID       PIC X(20).
           05  HV-CUSTOMER-ID         PIC X(10).
           05  HV-POLICY-TYPE         PIC X(4).
           05  HV-OLD-EXP-DATE        PIC X(10).
           05  HV-PREMIUM-AMT         PIC S9(9)V9(2) COMP-3.
           05  HV-NEW-PREMIUM-AMT     PIC S9(9)V9(2) COMP-3.
           05  HV-NEW-EFF-DATE        PIC X(10).
           05  HV-NEW-EXP-DATE        PIC X(10).

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
           MOVE 0  TO WS-CNT-READ
           MOVE 0  TO WS-CNT-RENEWED
           MOVE 0  TO WS-CNT-SKIPPED
           MOVE 0  TO WS-CNT-ERRORS
           MOVE 'N' TO WS-END-OF-CURSOR
           MOVE 'INIT' TO WS-AUD-ACTION
           MOVE 'POLICY_T' TO WS-AUD-TABLE
           CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           .

      * Open cursor for policies expiring within 30 days
       2000-OPEN-CURSOR.
           EXEC SQL
               DECLARE RNW_CURSOR CURSOR FOR
               SELECT P.POLICY_ID,
                      P.CUSTOMER_ID,
                      P.POLICY_TYPE,
                      P.ANNUAL_PREMIUM,
                      P.EXPIRATION_DATE
               FROM   POLICY_T P
               WHERE  P.STATUS = 'ACTV'
               AND    P.EXPIRATION_DATE BETWEEN CURRENT_DATE
                          AND (CURRENT_DATE + 30 DAYS)
               AND    P.RENEWAL_STATUS IS NULL
               ORDER BY P.EXPIRATION_DATE
           END-EXEC
           EXEC SQL
               OPEN RNW_CURSOR
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               DISPLAY 'POL006B: ERROR OPENING CURSOR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
               MOVE 8 TO WS-RETURN-CODE
               MOVE 'Y' TO WS-END-OF-CURSOR
           END-IF
           .

      * Process loop
       3000-PROCESS-LOOP.
           PERFORM 3100-FETCH-POLICY
           IF WS-END-OF-CURSOR = 'N'
               MOVE 'N' TO WS-SKIP-FLAG
               PERFORM 3200-CALCULATE-RENEWAL-DATES
               PERFORM 3300-CALC-RENEWAL-PREMIUM
               IF WS-SKIP-FLAG = 'N'
                   PERFORM 3400-VALIDATE-RENEWAL
               END-IF
               IF WS-SKIP-FLAG = 'N'
                   PERFORM 3500-INSERT-RENEWAL-POLICY
                   PERFORM 3600-UPDATE-BILLING-PLAN
                   ADD 1 TO WS-CNT-RENEWED
               ELSE
                   ADD 1 TO WS-CNT-SKIPPED
               END-IF
               ADD 1 TO WS-CNT-READ
           END-IF
           .

      * Fetch next expiring policy
       3100-FETCH-POLICY.
           EXEC SQL
               FETCH RNW_CURSOR
               INTO  :HV-POLICY-ID,
                     :HV-CUSTOMER-ID,
                     :HV-POLICY-TYPE,
                     :HV-PREMIUM-AMT,
                     :HV-OLD-EXP-DATE
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           EVALUATE TRUE
               WHEN SQLCODE = 0
                   CONTINUE
               WHEN SQLCODE = 100
                   MOVE 'Y' TO WS-END-OF-CURSOR
               WHEN OTHER
                   DISPLAY 'POL006B: FETCH ERROR SQLCODE='
                           WS-SQLCODE-SAVE
                   ADD 1 TO WS-CNT-ERRORS
                   MOVE 'Y' TO WS-END-OF-CURSOR
           END-EVALUATE
           .

      * Calculate new effective and expiration dates.
      * P4-1: Adding 1 year to expiration date via integer arithmetic
      * does not account for leap year edge cases (e.g. Feb 29 renewal).
       3200-CALCULATE-RENEWAL-DATES.
           MOVE HV-OLD-EXP-DATE TO WS-NEW-EFF-DATE
           MOVE HV-OLD-EXP-DATE TO HV-NEW-EFF-DATE
      *    NOTE P4-1: ADD 1 TO year component - leap year not handled
           ADD 1 TO HV-OLD-EXP-DATE
           MOVE HV-OLD-EXP-DATE TO HV-NEW-EXP-DATE
           MOVE HV-NEW-EXP-DATE TO WS-NEW-EXP-DATE
           .

      * Call PRMCLC01 for renewal premium
       3300-CALC-RENEWAL-PREMIUM.
           MOVE HV-POLICY-ID   TO WS-PRM-POLICY-ID
           MOVE HV-POLICY-TYPE TO WS-PRM-POLICY-TYPE
           CALL 'PRMCLC01' USING WS-PRMCLC-PARMS
           IF WS-PRM-RESULT NOT = 'OK'
               MOVE 'Y' TO WS-SKIP-FLAG
               ADD 1 TO WS-CNT-ERRORS
               DISPLAY 'POL006B: PREMIUM CALC FAILED FOR '
                       HV-POLICY-ID
           ELSE
               MOVE WS-PRM-PREMIUM-AMT TO HV-NEW-PREMIUM-AMT
           END-IF
           .

      * Call POLVAL01 to validate renewal
       3400-VALIDATE-RENEWAL.
           MOVE HV-POLICY-ID TO WS-VAL-POLICY-ID
           CALL 'POLVAL01' USING WS-POLVAL-PARMS
           IF WS-VAL-RESULT NOT = 'OK'
               MOVE 'Y' TO WS-SKIP-FLAG
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

      * Insert the renewal policy record
       3500-INSERT-RENEWAL-POLICY.
           EXEC SQL
               INSERT INTO POLICY_T
                   (POLICY_ID,
                    CUSTOMER_ID,
                    POLICY_TYPE,
                    ANNUAL_PREMIUM,
                    EFFECTIVE_DATE,
                    EXPIRATION_DATE,
                    STATUS,
                    RENEWAL_OF_POLICY_ID,
                    CREATE_TS)
               VALUES
                   (NEXT VALUE FOR POLICY_SEQ,
                    :HV-CUSTOMER-ID,
                    :HV-POLICY-TYPE,
                    :HV-NEW-PREMIUM-AMT,
                    :HV-NEW-EFF-DATE,
                    :HV-NEW-EXP-DATE,
                    'ACTV',
                    :HV-POLICY-ID,
                    CURRENT_TIMESTAMP)
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               DISPLAY 'POL006B: RENEWAL INSERT ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
               MOVE 'Y' TO WS-SKIP-FLAG
           END-IF
           .

      * Update billing plan for the renewed policy
       3600-UPDATE-BILLING-PLAN.
           EXEC SQL
               UPDATE BILLING_PLAN_T
               SET    RENEWAL_FLAG = 'Y',
                      LAST_UPDATE_TS = CURRENT_TIMESTAMP
               WHERE  POLICY_ID = :HV-POLICY-ID
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0 AND SQLCODE NOT = 100
               DISPLAY 'POL006B: BILLING PLAN UPDATE ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           MOVE 'RENEW' TO WS-AUD-ACTION
           MOVE 'POLICY_T' TO WS-AUD-TABLE
           MOVE HV-POLICY-ID TO WS-AUD-KEY
           CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           .

      * Finalize - close cursor, commit, print counts
       4000-FINALIZE.
           EXEC SQL
               CLOSE RNW_CURSOR
           END-EXEC
           EXEC SQL
               COMMIT
           END-EXEC
           DISPLAY 'POL006B POLICY RENEWAL COMPLETE'
           DISPLAY '  POLICIES READ   : ' WS-CNT-READ
           DISPLAY '  RENEWED         : ' WS-CNT-RENEWED
           DISPLAY '  SKIPPED         : ' WS-CNT-SKIPPED
           DISPLAY '  ERRORS          : ' WS-CNT-ERRORS
           MOVE 'FINALIZE' TO WS-AUD-ACTION
           CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           IF WS-CNT-ERRORS > 0
               MOVE 8 TO WS-RETURN-CODE
           END-IF
           MOVE WS-RETURN-CODE TO RETURN-CODE
           .

       END PROGRAM POL006B.
