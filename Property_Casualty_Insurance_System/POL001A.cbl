      *================================================================*
      * PROGRAM:     POL001A                                          *
      * MODULE:      POLICY                                           *
      * TYPE:        INTERACTIVE                                      *
      * DESCRIPTION: Policy Issuance Interactive                      *
      *----------------------------------------------------------------*
      * CALLS:       AUDLOG01                                         *
      *              POLVAL01                                         *
      *              PRMCLC01                                         *
      * TABLES:      POLICY_T                                         *
      *              CUSTOMER_T                                       *
      *              COVERAGE_T                                       *
      *              BILLING_SCHEDULE_T                               *
      * UI:          POLMNTD1                                         *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. POL001A.
       AUTHOR. PCIS-DEVELOPMENT.
       DATE-WRITTEN. 2024-01-20.
      *----------------------------------------------------------------*
      * Policy Issuance Interactive program.                          *
      * Reads customer data, validates policy (POLVAL01), calculates  *
      * premium (PRMCLC01), creates policy record, creates first      *
      * billing schedule entry. NOTE GAP I-02: does not insert a      *
      * BILLING_PLAN_T row for the new policy.                        *
      *----------------------------------------------------------------*
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ISERIES.
       OBJECT-COMPUTER. IBM-ISERIES.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
           EXEC SQL INCLUDE SQLCA END-EXEC.

      * Screen I/O fields
       01  WS-SCREEN-ACTION           PIC X(4)       VALUE SPACES.
       01  WS-POLICY-ID               PIC X(20)      VALUE SPACES.
       01  WS-CUSTOMER-ID             PIC X(10)      VALUE SPACES.
       01  WS-POLICY-TYPE             PIC X(4)       VALUE SPACES.
       01  WS-PREMIUM-AMT             PIC S9(9)V9(2) COMP-3.
       01  WS-EFF-DATE                PIC X(10)      VALUE SPACES.
       01  WS-EXP-DATE                PIC X(10)      VALUE SPACES.

      * Validation and result fields
       01  WS-RETURN-CODE             PIC S9(4)      COMP VALUE 0.
       01  WS-SQLCODE-SAVE            PIC S9(9)      COMP VALUE 0.
       01  WS-VALID-FLAG              PIC X          VALUE 'Y'.
       01  WS-ERROR-MSG               PIC X(78)      VALUE SPACES.

      * Audit logging parameters
       01  WS-AUDIT-PARMS.
           05  WS-AUD-PROGRAM         PIC X(10)      VALUE 'POL001A'.
           05  WS-AUD-ACTION          PIC X(10)      VALUE SPACES.
           05  WS-AUD-TABLE           PIC X(20)      VALUE SPACES.
           05  WS-AUD-KEY             PIC X(30)      VALUE SPACES.
           05  WS-AUD-USER            PIC X(10)      VALUE SPACES.
           05  WS-AUD-RESULT          PIC X(4)       VALUE SPACES.

      * Subprogram parameter areas
       01  WS-POLVAL-PARMS.
           05  WS-VAL-POLICY-ID       PIC X(20).
           05  WS-VAL-RESULT          PIC X(4).
           05  WS-VAL-MSG             PIC X(78).

       01  WS-PRMCLC-PARMS.
           05  WS-PRM-POLICY-ID       PIC X(20).
           05  WS-PRM-POLICY-TYPE     PIC X(4).
           05  WS-PRM-PREMIUM-AMT     PIC S9(9)V9(2) COMP-3.
           05  WS-PRM-RESULT          PIC X(4).

      * Host variables for SQL
       01  WS-HOST-VARS.
           05  HV-POLICY-ID           PIC X(20).
           05  HV-CUSTOMER-ID         PIC X(10).
           05  HV-POLICY-TYPE         PIC X(4).
           05  HV-PREMIUM-AMT         PIC S9(9)V9(2) COMP-3.
           05  HV-EFF-DATE            PIC X(10).
           05  HV-EXP-DATE            PIC X(10).
           05  HV-CUST-NAME           PIC X(60).
           05  HV-CUST-STATUS         PIC X(4).
           05  HV-SCHED-AMT           PIC S9(9)V9(2) COMP-3.
           05  HV-DUE-DATE            PIC X(10).

       PROCEDURE DIVISION.

      * Main control
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-GET-SCREEN-INPUT
           PERFORM 3000-VALIDATE-CUSTOMER
           IF WS-VALID-FLAG = 'Y'
               PERFORM 4000-VALIDATE-POLICY
           END-IF
           IF WS-VALID-FLAG = 'Y'
               PERFORM 5000-CALCULATE-PREMIUM
           END-IF
           IF WS-VALID-FLAG = 'Y'
               PERFORM 6000-INSERT-POLICY
               PERFORM 7000-INSERT-BILLING-SCHED
               PERFORM 8000-COMMIT-AND-LOG
           ELSE
               PERFORM 9000-DISPLAY-ERROR
           END-IF
           STOP RUN.

      * Initialization
       1000-INITIALIZE.
           MOVE SPACES TO WS-SCREEN-ACTION
           MOVE SPACES TO WS-POLICY-ID
           MOVE SPACES TO WS-CUSTOMER-ID
           MOVE 'Y' TO WS-VALID-FLAG
           MOVE SPACES TO WS-ERROR-MSG
           MOVE 0 TO WS-RETURN-CODE
           .

      * Read screen input from POLMNTD1 display file
       2000-GET-SCREEN-INPUT.
           EXEC SQL
               SELECT CHAR(CURRENT_DATE, ISO)
               INTO   :HV-EFF-DATE
               FROM   SYSIBM.SYSDUMMY1
           END-EXEC
           MOVE WS-POLICY-ID   TO HV-POLICY-ID
           MOVE WS-CUSTOMER-ID TO HV-CUSTOMER-ID
           MOVE WS-POLICY-TYPE TO HV-POLICY-TYPE
           MOVE WS-EFF-DATE    TO HV-EFF-DATE
           .

      * Validate customer record exists and is active
       3000-VALIDATE-CUSTOMER.
           EXEC SQL
               SELECT CUST_NAME,
                      STATUS
               INTO   :HV-CUST-NAME,
                      :HV-CUST-STATUS
               FROM   CUSTOMER_T
               WHERE  CUSTOMER_ID = :HV-CUSTOMER-ID
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE = 100
               MOVE 'N' TO WS-VALID-FLAG
               MOVE 'CUSTOMER NOT FOUND' TO WS-ERROR-MSG
           END-IF
           IF SQLCODE < 0
               MOVE 'N' TO WS-VALID-FLAG
               MOVE 'DATABASE ERROR ON CUSTOMER LOOKUP' TO WS-ERROR-MSG
           END-IF
           IF HV-CUST-STATUS NOT = 'ACTV'
               MOVE 'N' TO WS-VALID-FLAG
               MOVE 'CUSTOMER IS NOT ACTIVE' TO WS-ERROR-MSG
           END-IF
           .

      * Call POLVAL01 to validate policy business rules
       4000-VALIDATE-POLICY.
           MOVE HV-POLICY-ID   TO WS-VAL-POLICY-ID
           CALL 'POLVAL01' USING WS-POLVAL-PARMS
           IF WS-VAL-RESULT NOT = 'OK'
               MOVE 'N' TO WS-VALID-FLAG
               MOVE WS-VAL-MSG TO WS-ERROR-MSG
           END-IF
           .

      * Call PRMCLC01 to calculate premium
       5000-CALCULATE-PREMIUM.
           MOVE HV-POLICY-ID   TO WS-PRM-POLICY-ID
           MOVE HV-POLICY-TYPE TO WS-PRM-POLICY-TYPE
           CALL 'PRMCLC01' USING WS-PRMCLC-PARMS
           IF WS-PRM-RESULT NOT = 'OK'
               MOVE 'N' TO WS-VALID-FLAG
               MOVE 'PREMIUM CALCULATION FAILED' TO WS-ERROR-MSG
           ELSE
               MOVE WS-PRM-PREMIUM-AMT TO HV-PREMIUM-AMT
               MOVE WS-PRM-PREMIUM-AMT TO WS-PREMIUM-AMT
           END-IF
           .

      * Insert new policy record into POLICY_T
       6000-INSERT-POLICY.
           EXEC SQL
               INSERT INTO POLICY_T
                   (POLICY_ID,
                    CUSTOMER_ID,
                    POLICY_TYPE,
                    ANNUAL_PREMIUM,
                    EFFECTIVE_DATE,
                    EXPIRATION_DATE,
                    STATUS,
                    CREATE_TS)
               VALUES
                   (:HV-POLICY-ID,
                    :HV-CUSTOMER-ID,
                    :HV-POLICY-TYPE,
                    :HV-PREMIUM-AMT,
                    :HV-EFF-DATE,
                    :HV-EXP-DATE,
                    'ACTV',
                    CURRENT_TIMESTAMP)
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               MOVE 'N' TO WS-VALID-FLAG
               MOVE 'POLICY INSERT FAILED' TO WS-ERROR-MSG
           END-IF
           .

      * Insert first billing schedule entry. GAP I-02: No BILLING_PLAN_T
      * row is inserted here. A BILLING_PLAN_T record must be created
      * separately before downstream billing processes will work.
       7000-INSERT-BILLING-SCHED.
           MOVE HV-PREMIUM-AMT TO HV-SCHED-AMT
           MOVE HV-EFF-DATE    TO HV-DUE-DATE
           EXEC SQL
               INSERT INTO BILLING_SCHEDULE_T
                   (POLICY_ID,
                    INSTALLMENT_NBR,
                    DUE_DATE,
                    AMOUNT_DUE,
                    STATUS,
                    CREATE_TS)
               VALUES
                   (:HV-POLICY-ID,
                    1,
                    :HV-DUE-DATE,
                    :HV-SCHED-AMT,
                    'OPEN',
                    CURRENT_TIMESTAMP)
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               MOVE 'N' TO WS-VALID-FLAG
               MOVE 'BILLING SCHEDULE INSERT FAILED' TO WS-ERROR-MSG
           END-IF
           .

      * Commit changes and log audit record
       8000-COMMIT-AND-LOG.
           EXEC SQL
               COMMIT
           END-EXEC
           MOVE 'INSERT' TO WS-AUD-ACTION
           MOVE 'POLICY_T' TO WS-AUD-TABLE
           MOVE HV-POLICY-ID TO WS-AUD-KEY
           CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           .

      * Display error to user
       9000-DISPLAY-ERROR.
           DISPLAY 'POL001A: POLICY ISSUANCE FAILED'
           DISPLAY '  REASON: ' WS-ERROR-MSG
           MOVE 8 TO WS-RETURN-CODE
           MOVE WS-RETURN-CODE TO RETURN-CODE
           .

       END PROGRAM POL001A.
