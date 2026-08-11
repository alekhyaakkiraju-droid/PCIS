      *================================================================*
      * PROGRAM:     BIL003B                                          *
      * MODULE:      BILLING                                          *
      * TYPE:        BATCH                                            *
      * DESCRIPTION: Billing Installments Batch                       *
      *----------------------------------------------------------------*
      * CALLS:       AUDLOG01                                         *
      *              CUSVAL01                                         *
      * TABLES:      BILLING_PLAN_T                                   *
      *              BILLING_SCHEDULE_T                               *
      *              POLICY_T                                         *
      *              CUSTOMER_T                                       *
      * UI:                                                           *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. BIL003B.
       AUTHOR. PCIS-DEVELOPMENT.
       DATE-WRITTEN. 2024-02-15.
      *----------------------------------------------------------------*
      * Billing Installments Batch program.                           *
      * Reads BILLING_PLAN_T joined with BILLING_SCHEDULE_T, selects  *
      * installments due within the next billing cycle, validates     *
      * customer via CUSVAL01, generates installment bills, and logs  *
      * activity via AUDLOG01.                                        *
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
           05  WS-CNT-BILLED          PIC S9(7)      COMP-3 VALUE 0.
           05  WS-CNT-SKIPPED         PIC S9(7)      COMP-3 VALUE 0.
           05  WS-CNT-ERRORS          PIC S9(7)      COMP-3 VALUE 0.

      * Run-log timing and counter host variables (WO-237)
       01  WS-START-TIMESTAMP         PIC X(26).
       01  WS-END-TIMESTAMP           PIC X(26).
       01  WS-RL-PGM-NAME             PIC X(10)      VALUE 'BIL003B'.
       01  WS-RL-RUN-DATE             PIC X(10).
       01  WS-RL-SELECTED             PIC S9(9)      COMP-3 VALUE 0.
       01  WS-RL-UPDATED              PIC S9(9)      COMP-3 VALUE 0.
       01  WS-RL-ERRORS               PIC S9(9)      COMP-3 VALUE 0.

      * Return code and status
       01  WS-RETURN-CODE             PIC S9(4)      COMP VALUE 0.
       01  WS-SQLCODE-SAVE            PIC S9(9)      COMP VALUE 0.
       01  WS-END-OF-CURSOR           PIC X          VALUE 'N'.
       01  WS-SKIP-FLAG               PIC X          VALUE 'N'.

      * Billing cycle days lookahead
       01  WS-CYCLE-DAYS              PIC S9(4)      COMP VALUE 7.

      * Audit logging parameters
       01  WS-AUDIT-PARMS.
           05  WS-AUD-PROGRAM         PIC X(10)      VALUE 'BIL003B'.
           05  WS-AUD-ACTION          PIC X(10)      VALUE SPACES.
           05  WS-AUD-TABLE           PIC X(20)      VALUE SPACES.
           05  WS-AUD-KEY             PIC X(30)      VALUE SPACES.
           05  WS-AUD-USER            PIC X(10)      VALUE SPACES.
           05  WS-AUD-RESULT          PIC X(4)       VALUE SPACES.

      * Subprogram parameters
       01  WS-CUSVAL-PARMS.
           05  WS-CUSVAL-CUST-ID      PIC X(10).
           05  WS-CUSVAL-RESULT       PIC X(4).
           05  WS-CUSVAL-MSG          PIC X(78).

      * Host variables for SQL
       01  WS-HOST-VARS.
           05  HV-PLAN-ID             PIC S9(9)      COMP.
           05  HV-SCHED-ID            PIC S9(9)      COMP.
           05  HV-POLICY-ID           PIC X(20).
           05  HV-CUSTOMER-ID         PIC X(10).
           05  HV-INSTALLMENT-NBR     PIC S9(7)      COMP-3.
           05  HV-DUE-DATE            PIC X(10).
           05  HV-AMOUNT-DUE          PIC S9(9)V9(2) COMP-3.
           05  HV-PLAN-TYPE           PIC X(4).
           05  HV-CUST-NAME           PIC X(60).
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
           MOVE 0  TO WS-CNT-BILLED
           MOVE 0  TO WS-CNT-SKIPPED
           MOVE 0  TO WS-CNT-ERRORS
           MOVE 'N' TO WS-END-OF-CURSOR
           MOVE 0  TO WS-RETURN-CODE
           MOVE 'INIT' TO WS-AUD-ACTION
           MOVE 'BILLING_SCHEDULE_T' TO WS-AUD-TABLE
           CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           .

      * Open cursor joining BILLING_PLAN_T and BILLING_SCHEDULE_T
       2000-OPEN-CURSOR.
           EXEC SQL
               DECLARE BIL_CURSOR CURSOR FOR
               SELECT BP.PLAN_ID,
                      BS.SCHED_ID,
                      BS.POLICY_ID,
                      P.CUSTOMER_ID,
                      BS.INSTALLMENT_NBR,
                      BS.DUE_DATE,
                      BS.AMOUNT_DUE,
                      BP.PLAN_TYPE
               FROM   BILLING_PLAN_T  BP
               JOIN   BILLING_SCHEDULE_T BS
                   ON BS.POLICY_ID = BP.POLICY_ID
               JOIN   POLICY_T P
                   ON P.POLICY_ID = BS.POLICY_ID
               WHERE  BS.STATUS = 'OPEN'
               AND    BS.DUE_DATE <= (CURRENT_DATE + 7 DAYS)
               AND    BS.BILLED_FLAG = 'N'
               ORDER BY BS.DUE_DATE, BS.POLICY_ID
           END-EXEC
           EXEC SQL
               OPEN BIL_CURSOR
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               DISPLAY 'BIL003B: ERROR OPENING CURSOR SQLCODE='
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
               MOVE 'N' TO WS-SKIP-FLAG
               PERFORM 3200-VALIDATE-CUSTOMER
               IF WS-SKIP-FLAG = 'N'
                   PERFORM 3300-GET-CUSTOMER-CONTACT
                   PERFORM 3400-GENERATE-BILL
                   PERFORM 3500-MARK-BILLED
               ELSE
                   ADD 1 TO WS-CNT-SKIPPED
               END-IF
               ADD 1 TO WS-CNT-READ
           END-IF
           .

      * Fetch next installment record
       3100-FETCH-RECORD.
           EXEC SQL
               FETCH BIL_CURSOR
               INTO  :HV-PLAN-ID,
                     :HV-SCHED-ID,
                     :HV-POLICY-ID,
                     :HV-CUSTOMER-ID,
                     :HV-INSTALLMENT-NBR,
                     :HV-DUE-DATE,
                     :HV-AMOUNT-DUE,
                     :HV-PLAN-TYPE
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           EVALUATE TRUE
               WHEN SQLCODE = 0
                   CONTINUE
               WHEN SQLCODE = 100
                   MOVE 'Y' TO WS-END-OF-CURSOR
               WHEN OTHER
                   DISPLAY 'BIL003B: FETCH ERROR SQLCODE='
                           WS-SQLCODE-SAVE
                   ADD 1 TO WS-CNT-ERRORS
                   MOVE 'Y' TO WS-END-OF-CURSOR
           END-EVALUATE
           .

      * Call CUSVAL01 to validate customer is billable
       3200-VALIDATE-CUSTOMER.
           MOVE HV-CUSTOMER-ID TO WS-CUSVAL-CUST-ID
           CALL 'CUSVAL01' USING WS-CUSVAL-PARMS
           IF WS-CUSVAL-RESULT NOT = 'OK'
               MOVE 'Y' TO WS-SKIP-FLAG
               DISPLAY 'BIL003B: CUSTOMER VALIDATION FAILED FOR '
                       HV-CUSTOMER-ID
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

      * Retrieve customer contact information for bill delivery
       3300-GET-CUSTOMER-CONTACT.
           EXEC SQL
               SELECT CUST_NAME,
                      EMAIL_ADDRESS
               INTO   :HV-CUST-NAME,
                      :HV-CUST-EMAIL
               FROM   CUSTOMER_T
               WHERE  CUSTOMER_ID = :HV-CUSTOMER-ID
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               DISPLAY 'BIL003B: CUSTOMER CONTACT LOOKUP ERROR '
                       HV-CUSTOMER-ID
               MOVE 'Y' TO WS-SKIP-FLAG
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

      * Generate installment bill record
       3400-GENERATE-BILL.
           EXEC SQL
               INSERT INTO BILLING_NOTICE_T
                   (POLICY_ID,
                    SCHED_ID,
                    CUSTOMER_ID,
                    INSTALLMENT_NBR,
                    AMOUNT_DUE,
                    DUE_DATE,
                    NOTICE_DATE,
                    DELIVERY_EMAIL,
                    STATUS,
                    CREATE_TS)
               VALUES
                   (:HV-POLICY-ID,
                    :HV-SCHED-ID,
                    :HV-CUSTOMER-ID,
                    :HV-INSTALLMENT-NBR,
                    :HV-AMOUNT-DUE,
                    :HV-DUE-DATE,
                    CURRENT_DATE,
                    :HV-CUST-EMAIL,
                    'PEND',
                    CURRENT_TIMESTAMP)
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               DISPLAY 'BIL003B: BILL INSERT ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
               MOVE 'Y' TO WS-SKIP-FLAG
           END-IF
           .

      * Mark billing schedule as billed
       3500-MARK-BILLED.
           EXEC SQL
               UPDATE BILLING_SCHEDULE_T
               SET    BILLED_FLAG = 'Y',
                      BILLED_DATE = CURRENT_DATE,
                      LAST_UPDATE_TS = CURRENT_TIMESTAMP
               WHERE  SCHED_ID = :HV-SCHED-ID
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE = 0
               ADD 1 TO WS-CNT-BILLED
               MOVE 'BILL' TO WS-AUD-ACTION
               MOVE 'BILLING_SCHEDULE_T' TO WS-AUD-TABLE
               MOVE HV-POLICY-ID TO WS-AUD-KEY
               CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           ELSE
               DISPLAY 'BIL003B: MARK BILLED ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

      * Finalize - close cursor, commit, display counts
       4000-FINALIZE.
           EXEC SQL
               CLOSE BIL_CURSOR
           END-EXEC
           EXEC SQL
               COMMIT
           END-EXEC
           DISPLAY 'BIL003B BILLING INSTALLMENTS COMPLETE'
           DISPLAY '  INSTALLMENTS READ  : ' WS-CNT-READ
           DISPLAY '  BILLS GENERATED    : ' WS-CNT-BILLED
           DISPLAY '  SKIPPED            : ' WS-CNT-SKIPPED
           DISPLAY '  ERRORS             : ' WS-CNT-ERRORS
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
           MOVE WS-CNT-READ   TO WS-RL-SELECTED
           MOVE WS-CNT-BILLED TO WS-RL-UPDATED
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
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               DISPLAY 'BIL003B: RUN LOG INSERT ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

       END PROGRAM BIL003B.
