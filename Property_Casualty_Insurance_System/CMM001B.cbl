      *================================================================*
      * PROGRAM:     CMM001B                                          *
      * MODULE:      COMMISSION                                       *
      * TYPE:        BATCH                                            *
      * DESCRIPTION: Commission Calculation Batch                     *
      *----------------------------------------------------------------*
      * CALLS:       AUDLOG01                                         *
      * TABLES:      COMMISSION_T                                     *
      *              POLICY_T                                         *
      *              AGENT_T                                          *
      *              COMMISSION_RATE_T                                *
      * UI:                                                           *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CMM001B.
       AUTHOR. PCIS-DEVELOPMENT.
       DATE-WRITTEN. 2024-01-15.
      *----------------------------------------------------------------*
      * Calculate commissions for active policies. Reads POLICY_T,   *
      * looks up rates in COMMISSION_RATE_T, inserts into COMMISSION_T*
      *----------------------------------------------------------------*
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ISERIES.
       OBJECT-COMPUTER. IBM-ISERIES.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * SQL Communication Area
           EXEC SQL INCLUDE SQLCA END-EXEC.

      * Commission calculation work fields
       01  WS-COMMISSION-RATE         PIC S9(5)V9(4) COMP-3.
       01  WS-PREMIUM-AMT             PIC S9(9)V9(2) COMP-3.
       01  WS-COMMISSION-AMT          PIC S9(9)V9(2) COMP-3.
       01  WS-WORK-FIELD              PIC S9(9)V9(4) COMP-3.

      * Counters
       01  WS-COUNTERS.
           05  WS-CNT-READ            PIC S9(7)      COMP-3 VALUE 0.
           05  WS-CNT-WRITTEN         PIC S9(7)      COMP-3 VALUE 0.
           05  WS-CNT-SKIPPED         PIC S9(7)      COMP-3 VALUE 0.
           05  WS-CNT-ERRORS          PIC S9(7)      COMP-3 VALUE 0.

      * Return code and status fields
       01  WS-RETURN-CODE             PIC S9(4)      COMP VALUE 0.
       01  WS-SQLCODE-SAVE            PIC S9(9)      COMP VALUE 0.
       01  WS-END-OF-CURSOR           PIC X          VALUE 'N'.

      * Audit logging parameters
       01  WS-AUDIT-PARMS.
           05  WS-AUD-PROGRAM         PIC X(10)      VALUE 'CMM001B'.
           05  WS-AUD-ACTION          PIC X(10)      VALUE SPACES.
           05  WS-AUD-TABLE           PIC X(20)      VALUE SPACES.
           05  WS-AUD-KEY             PIC X(30)      VALUE SPACES.
           05  WS-AUD-USER            PIC X(10)      VALUE SPACES.
           05  WS-AUD-RESULT          PIC X(4)       VALUE SPACES.

      * Host variables for SQL
       01  WS-HOST-VARS.
           05  HV-POLICY-ID           PIC X(20).
           05  HV-AGENT-ID            PIC X(10).
           05  HV-POLICY-TYPE         PIC X(4).
           05  HV-PREMIUM-AMT         PIC S9(9)V9(2) COMP-3.
           05  HV-EFF-DATE            PIC X(10).
           05  HV-EXP-DATE            PIC X(10).
           05  HV-RATE-PCT            PIC S9(5)V9(4) COMP-3.
           05  HV-COMMISSION-AMT      PIC S9(9)V9(2) COMP-3.
           05  HV-CALC-DATE           PIC X(10).

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
           MOVE 0 TO WS-RETURN-CODE
           MOVE 0 TO WS-CNT-READ
           MOVE 0 TO WS-CNT-WRITTEN
           MOVE 0 TO WS-CNT-SKIPPED
           MOVE 0 TO WS-CNT-ERRORS
           MOVE 'N' TO WS-END-OF-CURSOR
           MOVE 'INIT' TO WS-AUD-ACTION
           MOVE 'COMMISSION_T' TO WS-AUD-TABLE
           CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           .

      * Open SQL cursor for active policies
       2000-OPEN-CURSOR.
           EXEC SQL
               DECLARE POL_CURSOR CURSOR FOR
               SELECT P.POLICY_ID,
                      P.AGENT_ID,
                      P.POLICY_TYPE,
                      P.ANNUAL_PREMIUM,
                      P.EFFECTIVE_DATE,
                      P.EXPIRATION_DATE
               FROM   POLICY_T P
               WHERE  P.STATUS = 'ACTV'
               AND    P.COMMISSION_CALC_DATE IS NULL
               ORDER BY P.POLICY_ID
           END-EXEC
           EXEC SQL
               OPEN POL_CURSOR
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               DISPLAY 'CMM001B: ERROR OPENING CURSOR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
               MOVE 8 TO WS-RETURN-CODE
               MOVE 'Y' TO WS-END-OF-CURSOR
           END-IF
           .

      * Process loop - fetch and process each policy row
       3000-PROCESS-LOOP.
           PERFORM 3100-FETCH-POLICY
           IF WS-END-OF-CURSOR = 'N'
               PERFORM 3200-GET-COMMISSION-RATE
               PERFORM 3300-CALCULATE-COMMISSION
               PERFORM 3400-INSERT-COMMISSION
               PERFORM 3500-UPDATE-POLICY-CALC-DATE
               ADD 1 TO WS-CNT-READ
           END-IF
           .

      * Fetch next policy from cursor
       3100-FETCH-POLICY.
           EXEC SQL
               FETCH POL_CURSOR
               INTO  :HV-POLICY-ID,
                     :HV-AGENT-ID,
                     :HV-POLICY-TYPE,
                     :HV-PREMIUM-AMT,
                     :HV-EFF-DATE,
                     :HV-EXP-DATE
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           EVALUATE TRUE
               WHEN SQLCODE = 0
                   CONTINUE
               WHEN SQLCODE = 100
                   MOVE 'Y' TO WS-END-OF-CURSOR
               WHEN OTHER
                   DISPLAY 'CMM001B: FETCH ERROR SQLCODE='
                           WS-SQLCODE-SAVE
                   ADD 1 TO WS-CNT-ERRORS
                   MOVE 'Y' TO WS-END-OF-CURSOR
           END-EVALUATE
           .

      * Look up commission rate from COMMISSION_RATE_T
       3200-GET-COMMISSION-RATE.
           MOVE ZEROS TO HV-RATE-PCT
           EXEC SQL
               SELECT RATE_PERCENT
               INTO   :HV-RATE-PCT
               FROM   COMMISSION_RATE_T
               WHERE  POLICY_TYPE = :HV-POLICY-TYPE
               AND    EFFECTIVE_DATE <= CURRENT_DATE
               AND    (END_DATE IS NULL
                       OR END_DATE >= CURRENT_DATE)
               FETCH FIRST 1 ROW ONLY
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE = 100
               DISPLAY 'CMM001B: NO RATE FOUND FOR TYPE '
                       HV-POLICY-TYPE
               ADD 1 TO WS-CNT-SKIPPED
               MOVE ZEROS TO HV-RATE-PCT
           END-IF
           IF SQLCODE < 0
               DISPLAY 'CMM001B: RATE LOOKUP ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

      * Calculate commission amount
       3300-CALCULATE-COMMISSION.
           MOVE HV-PREMIUM-AMT TO WS-PREMIUM-AMT
           MOVE HV-RATE-PCT    TO WS-COMMISSION-RATE
           COMPUTE WS-COMMISSION-AMT ROUNDED =
               WS-PREMIUM-AMT * WS-COMMISSION-RATE / 100
           MOVE WS-COMMISSION-AMT TO HV-COMMISSION-AMT
           .

      * Insert commission record into COMMISSION_T
       3400-INSERT-COMMISSION.
           EXEC SQL
               SELECT CHAR(CURRENT_DATE, ISO)
               INTO   :HV-CALC-DATE
               FROM   SYSIBM.SYSDUMMY1
           END-EXEC
           EXEC SQL
               INSERT INTO COMMISSION_T
                   (COMMISSION_ID,
                    POLICY_ID,
                    AGENT_ID,
                    COMMISSION_AMT,
                    CALC_DATE,
                    STATUS,
                    CREATE_TS)
               VALUES
                   (NEXT VALUE FOR COMMISSION_SEQ,
                    :HV-POLICY-ID,
                    :HV-AGENT-ID,
                    :HV-COMMISSION-AMT,
                    :HV-CALC-DATE,
                    'PEND',
                    CURRENT_TIMESTAMP)
           END-EXEC

           MOVE SQLCODE TO WS-SQLCODE-SAVE

           IF SQLCODE = 0
               ADD 1 TO WS-CNT-WRITTEN
               MOVE 'INSERT' TO WS-AUD-ACTION
               MOVE 'COMMISSION_T' TO WS-AUD-TABLE
               MOVE HV-POLICY-ID TO WS-AUD-KEY
               CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           ELSE
               DISPLAY 'CMM001B: INSERT ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

      * Update policy with commission calculation date
       3500-UPDATE-POLICY-CALC-DATE.
           EXEC SQL
               UPDATE POLICY_T
               SET    COMMISSION_CALC_DATE = CURRENT_DATE,
                      LAST_UPDATE_TS       = CURRENT_TIMESTAMP
               WHERE  POLICY_ID = :HV-POLICY-ID
           END-EXEC

           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               DISPLAY 'CMM001B: UPDATE POLICY ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

      * Finalize - close cursor, commit, display counts
       4000-FINALIZE.
           EXEC SQL
               CLOSE POL_CURSOR
           END-EXEC
           EXEC SQL
               COMMIT
           END-EXEC

           DISPLAY 'CMM001B COMMISSION CALCULATION COMPLETE'
           DISPLAY '  POLICIES READ   : ' WS-CNT-READ
           DISPLAY '  COMMISSIONS WRT : ' WS-CNT-WRITTEN
           DISPLAY '  SKIPPED         : ' WS-CNT-SKIPPED
           DISPLAY '  ERRORS          : ' WS-CNT-ERRORS
           MOVE 'FINALIZE' TO WS-AUD-ACTION
           MOVE 'COMMISSION_T' TO WS-AUD-TABLE
           CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           IF WS-CNT-ERRORS > 0
               MOVE 8 TO WS-RETURN-CODE
           END-IF
           MOVE WS-RETURN-CODE TO RETURN-CODE
           .

       END PROGRAM CMM001B.
