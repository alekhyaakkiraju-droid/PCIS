      *================================================================*
      * PROGRAM:     CUS001A                                          *
      * MODULE:      CUSTOMER                                         *
      * TYPE:        INTERACTIVE                                      *
      * DESCRIPTION: Customer Maintenance Interactive                  *
      *----------------------------------------------------------------*
      * CALLS:       AUDLOG01                                         *
      *              CUSVAL01                                         *
      *              SECCHK01                                         *
      * TABLES:      CUSTOMER_T                                       *
      *              ADDRESS_T                                        *
      *              CONTACT_T                                        *
      * UI:          CUSMNTD1                                         *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CUS001A.
       AUTHOR. PCIS-DEVELOPMENT.
       DATE-WRITTEN. 2024-03-10.
      *----------------------------------------------------------------*
      * Customer Maintenance Interactive program.                     *
      * Reads and writes customer records, validates customer data    *
      * via CUSVAL01, checks user authorization via SECCHK01, and     *
      * logs all changes via AUDLOG01.                                *
      *----------------------------------------------------------------*
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ISERIES.
       OBJECT-COMPUTER. IBM-ISERIES.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
           EXEC SQL INCLUDE SQLCA END-EXEC.

      * Screen action flags
       01  WS-SCREEN-ACTION           PIC X(4)       VALUE SPACES.
       01  WS-CUSTOMER-ID             PIC X(10)      VALUE SPACES.
       01  WS-USER-ID                 PIC X(10)      VALUE SPACES.

      * Return code and status
       01  WS-RETURN-CODE             PIC S9(4)      COMP VALUE 0.
       01  WS-SQLCODE-SAVE            PIC S9(9)      COMP VALUE 0.
       01  WS-VALID-FLAG              PIC X          VALUE 'Y'.
       01  WS-AUTH-FLAG               PIC X          VALUE 'N'.
       01  WS-ERROR-MSG               PIC X(78)      VALUE SPACES.

      * Audit logging parameters
       01  WS-AUDIT-PARMS.
           05  WS-AUD-PROGRAM         PIC X(10)      VALUE 'CUS001A'.
           05  WS-AUD-ACTION          PIC X(10)      VALUE SPACES.
           05  WS-AUD-TABLE           PIC X(20)      VALUE SPACES.
           05  WS-AUD-KEY             PIC X(30)      VALUE SPACES.
           05  WS-AUD-USER            PIC X(10)      VALUE SPACES.
           05  WS-AUD-RESULT          PIC X(4)       VALUE SPACES.

      * Subprogram parameter areas
       01  WS-CUSVAL-PARMS.
           05  WS-CUSVAL-CUST-ID      PIC X(10).
           05  WS-CUSVAL-RESULT       PIC X(4).
           05  WS-CUSVAL-MSG          PIC X(78).

       01  WS-SECCHK-PARMS.
           05  WS-SEC-USER-ID         PIC X(10).
           05  WS-SEC-PROGRAM         PIC X(10).
           05  WS-SEC-ACTION          PIC X(4).
           05  WS-SEC-RESULT          PIC X(4).

      * Host variables for SQL
       01  WS-HOST-VARS.
           05  HV-CUSTOMER-ID         PIC X(10).
           05  HV-CUST-NAME           PIC X(60).
           05  HV-CUST-STATUS         PIC X(4).
           05  HV-CUST-TYPE           PIC X(4).
           05  HV-TAX-ID              PIC X(15).
           05  HV-BIRTH-DATE          PIC X(10).
           05  HV-ADDR-ID             PIC S9(9)      COMP.
           05  HV-STREET-1            PIC X(50).
           05  HV-STREET-2            PIC X(50).
           05  HV-CITY                PIC X(30).
           05  HV-STATE               PIC X(2).
           05  HV-POSTAL-CODE         PIC X(10).
           05  HV-CONTACT-ID          PIC S9(9)      COMP.
           05  HV-PHONE               PIC X(20).
           05  HV-EMAIL               PIC X(80).

       PROCEDURE DIVISION.

      * Main control paragraph
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-CHECK-AUTHORIZATION
           IF WS-AUTH-FLAG = 'Y'
               EVALUATE WS-SCREEN-ACTION
                   WHEN 'INQR'
                       PERFORM 3000-INQUIRE-CUSTOMER
                   WHEN 'UPDT'
                       PERFORM 4000-UPDATE-CUSTOMER
                   WHEN 'ADD'
                       PERFORM 5000-ADD-CUSTOMER
                   WHEN OTHER
                       DISPLAY 'CUS001A: INVALID SCREEN ACTION '
                               WS-SCREEN-ACTION
                       MOVE 4 TO WS-RETURN-CODE
               END-EVALUATE
           ELSE
               MOVE 'CUS001A: AUTHORIZATION DENIED' TO WS-ERROR-MSG
               MOVE 8 TO WS-RETURN-CODE
           END-IF
           MOVE WS-RETURN-CODE TO RETURN-CODE
           STOP RUN.

      * Initialization
       1000-INITIALIZE.
           MOVE SPACES TO WS-ERROR-MSG
           MOVE 'Y' TO WS-VALID-FLAG
           MOVE 0 TO WS-RETURN-CODE
           MOVE WS-CUSTOMER-ID TO HV-CUSTOMER-ID
           MOVE WS-USER-ID TO WS-AUD-USER
           .

      * Call SECCHK01 to verify user authorization
       2000-CHECK-AUTHORIZATION.
           MOVE WS-USER-ID     TO WS-SEC-USER-ID
           MOVE 'CUS001A'      TO WS-SEC-PROGRAM
           MOVE WS-SCREEN-ACTION TO WS-SEC-ACTION
           CALL 'SECCHK01' USING WS-SECCHK-PARMS
           IF WS-SEC-RESULT = 'OK'
               MOVE 'Y' TO WS-AUTH-FLAG
           ELSE
               MOVE 'N' TO WS-AUTH-FLAG
               DISPLAY 'CUS001A: USER ' WS-USER-ID
                       ' NOT AUTHORIZED FOR ' WS-SCREEN-ACTION
           END-IF
           .

      * Inquire - read customer and related records
       3000-INQUIRE-CUSTOMER.
           EXEC SQL
               SELECT C.CUSTOMER_ID,
                      C.CUST_NAME,
                      C.STATUS,
                      C.CUST_TYPE,
                      C.TAX_ID,
                      C.BIRTH_DATE
               INTO   :HV-CUSTOMER-ID,
                      :HV-CUST-NAME,
                      :HV-CUST-STATUS,
                      :HV-CUST-TYPE,
                      :HV-TAX-ID,
                      :HV-BIRTH-DATE
               FROM   CUSTOMER_T C
               WHERE  C.CUSTOMER_ID = :HV-CUSTOMER-ID
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE = 100
               MOVE 'CUSTOMER NOT FOUND' TO WS-ERROR-MSG
               MOVE 4 TO WS-RETURN-CODE
           END-IF
           IF SQLCODE < 0
               DISPLAY 'CUS001A: CUSTOMER READ ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               MOVE 8 TO WS-RETURN-CODE
           END-IF
           .

      * Update customer record
       4000-UPDATE-CUSTOMER.
           MOVE HV-CUSTOMER-ID TO WS-CUSVAL-CUST-ID
           CALL 'CUSVAL01' USING WS-CUSVAL-PARMS
           IF WS-CUSVAL-RESULT NOT = 'OK'
               MOVE WS-CUSVAL-MSG TO WS-ERROR-MSG
               MOVE 'N' TO WS-VALID-FLAG
           END-IF
           IF WS-VALID-FLAG = 'Y'
               EXEC SQL
                   UPDATE CUSTOMER_T
                   SET    CUST_NAME     = :HV-CUST-NAME,
                          STATUS        = :HV-CUST-STATUS,
                          CUST_TYPE     = :HV-CUST-TYPE,
                          LAST_UPDATE_TS = CURRENT_TIMESTAMP
                   WHERE  CUSTOMER_ID = :HV-CUSTOMER-ID
               END-EXEC
               MOVE SQLCODE TO WS-SQLCODE-SAVE
               IF SQLCODE = 0
                   PERFORM 4100-UPDATE-ADDRESS
                   PERFORM 4200-UPDATE-CONTACT
                   EXEC SQL
                       COMMIT
                   END-EXEC
                   MOVE 'UPDATE' TO WS-AUD-ACTION
                   MOVE 'CUSTOMER_T' TO WS-AUD-TABLE
                   MOVE HV-CUSTOMER-ID TO WS-AUD-KEY
                   CALL 'AUDLOG01' USING WS-AUDIT-PARMS
               ELSE
                   DISPLAY 'CUS001A: CUSTOMER UPDATE ERROR SQLCODE='
                           WS-SQLCODE-SAVE
                   MOVE 8 TO WS-RETURN-CODE
               END-IF
           END-IF
           .

      * Update associated address record
       4100-UPDATE-ADDRESS.
           EXEC SQL
               UPDATE ADDRESS_T
               SET    STREET_LINE_1  = :HV-STREET-1,
                      STREET_LINE_2  = :HV-STREET-2,
                      CITY           = :HV-CITY,
                      STATE_CODE     = :HV-STATE,
                      POSTAL_CODE    = :HV-POSTAL-CODE,
                      LAST_UPDATE_TS = CURRENT_TIMESTAMP
               WHERE  CUSTOMER_ID = :HV-CUSTOMER-ID
               AND    ADDRESS_TYPE = 'MAIL'
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0 AND SQLCODE NOT = 100
               DISPLAY 'CUS001A: ADDRESS UPDATE ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-RETURN-CODE
           END-IF
           .

      * Update associated contact record
       4200-UPDATE-CONTACT.
           EXEC SQL
               UPDATE CONTACT_T
               SET    PHONE_NUMBER   = :HV-PHONE,
                      EMAIL_ADDRESS  = :HV-EMAIL,
                      LAST_UPDATE_TS = CURRENT_TIMESTAMP
               WHERE  CUSTOMER_ID = :HV-CUSTOMER-ID
               AND    CONTACT_TYPE = 'PRIM'
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0 AND SQLCODE NOT = 100
               DISPLAY 'CUS001A: CONTACT UPDATE ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-RETURN-CODE
           END-IF
           .

      * Add new customer record
       5000-ADD-CUSTOMER.
           MOVE HV-CUSTOMER-ID TO WS-CUSVAL-CUST-ID
           CALL 'CUSVAL01' USING WS-CUSVAL-PARMS
           IF WS-CUSVAL-RESULT NOT = 'OK'
               MOVE WS-CUSVAL-MSG TO WS-ERROR-MSG
               MOVE 'N' TO WS-VALID-FLAG
           END-IF
           IF WS-VALID-FLAG = 'Y'
               EXEC SQL
                   INSERT INTO CUSTOMER_T
                       (CUSTOMER_ID,
                        CUST_NAME,
                        STATUS,
                        CUST_TYPE,
                        TAX_ID,
                        BIRTH_DATE,
                        CREATE_TS)
                   VALUES
                       (:HV-CUSTOMER-ID,
                        :HV-CUST-NAME,
                        'ACTV',
                        :HV-CUST-TYPE,
                        :HV-TAX-ID,
                        :HV-BIRTH-DATE,
                        CURRENT_TIMESTAMP)
               END-EXEC
               MOVE SQLCODE TO WS-SQLCODE-SAVE
               IF SQLCODE = 0
                   EXEC SQL
                       COMMIT
                   END-EXEC
                   MOVE 'INSERT' TO WS-AUD-ACTION
                   MOVE 'CUSTOMER_T' TO WS-AUD-TABLE
                   MOVE HV-CUSTOMER-ID TO WS-AUD-KEY
                   CALL 'AUDLOG01' USING WS-AUDIT-PARMS
               ELSE
                   DISPLAY 'CUS001A: CUSTOMER INSERT ERROR SQLCODE='
                           WS-SQLCODE-SAVE
                   MOVE 8 TO WS-RETURN-CODE
               END-IF
           END-IF
           .

       END PROGRAM CUS001A.
