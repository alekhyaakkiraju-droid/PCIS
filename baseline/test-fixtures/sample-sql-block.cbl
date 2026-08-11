      *================================================================*
      * PROGRAM:     FIXSQL01                                         *
      * MODULE:      TEST                                             *
      * TYPE:        BATCH                                            *
      * DESCRIPTION: Fixture for SQL cursor + INSERT extraction       *
      *----------------------------------------------------------------*
      * CALLS:       (none)                                           *
      * TABLES:      CLAIM_T                                          *
      *              PAYMENT_T                                        *
      * UI:          (none)                                           *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. FIXSQL01.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
           EXEC SQL INCLUDE SQLCA END-EXEC.
       01  WS-CHUNK-SIZE           PIC 9(5) VALUE 00050.
       01  HV-CLAIM-ID             PIC 9(10).
       01  HV-AMOUNT               PIC S9(9)V99 COMP-3.
       PROCEDURE DIVISION.
       0000-MAIN.
           EXEC SQL
               DECLARE C1 CURSOR FOR
               SELECT CLAIM_ID, APPROVED_AMT
               FROM   CLAIM_T
               WHERE  CLAIM_STATUS = 'APPROVED'
               FETCH FIRST :WS-CHUNK-SIZE ROWS ONLY
           END-EXEC
           EXEC SQL OPEN C1 END-EXEC
           EXEC SQL
               FETCH C1 INTO :HV-CLAIM-ID, :HV-AMOUNT
           END-EXEC
           EXEC SQL
               INSERT INTO PAYMENT_T (CLAIM_ID, AMOUNT)
               VALUES (:HV-CLAIM-ID, :HV-AMOUNT)
           END-EXEC
           EXEC SQL COMMIT END-EXEC
           STOP RUN.
