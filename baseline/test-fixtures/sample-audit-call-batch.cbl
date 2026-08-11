      *================================================================*
      * PROGRAM:     FIXAUDB                                          *
      * MODULE:      TEST                                             *
      * TYPE:        BATCH                                            *
      * DESCRIPTION: Fixture for batch-style AUDLOG01 CALL            *
      *----------------------------------------------------------------*
      * CALLS:       AUDLOG01                                         *
      *              SECCHK01                                         *
      * TABLES:      CLAIM_T                                          *
      * UI:          (none)                                           *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. FIXAUDB.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-AUDIT-PARMS.
           05  WS-AUD-ACTION       PIC X(3).
           05  WS-AUD-OBJECT       PIC X(30).
       01  WS-AUD-RETURN           PIC S9(4) COMP VALUE 0.
       PROCEDURE DIVISION.
       0000-MAIN.
           MOVE 'PAY' TO WS-AUD-ACTION
           MOVE 'CLAIM_T' TO WS-AUD-OBJECT
           CALL 'AUDLOG01' USING WS-AUD-ACTION
                                 WS-AUD-OBJECT
           IF WS-AUD-RETURN NOT = 0
               DISPLAY 'AUDIT FAILED - CONTINUING'
           END-IF
           STOP RUN.
