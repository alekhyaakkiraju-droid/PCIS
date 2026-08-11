      *================================================================*
      * PROGRAM:     TSTPROG1                                          *
      * MODULE:      TEST                                              *
      * TYPE:        BATCH                                             *
      * DESCRIPTION: Test program with full prologue                   *
      *----------------------------------------------------------------*
      * CALLS:       AUDLOG01                                          *
      *              SECCHK01                                          *
      * TABLES:      CUSTOMER_T                                        *
      *              POLICY_T                                          *
      * UI:          CUSMNTD1                                          *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. TSTPROG1.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-AUDIT-PARMS.
           05  WS-AUD-ACTION   PIC X(3).
           05  WS-AUD-OBJECT   PIC X(30).
       PROCEDURE DIVISION.
       0000-MAIN.
           CALL 'AUDLOG01' USING WS-AUDIT-PARMS
           CALL 'SECCHK01' USING WS-AUDIT-PARMS
           COPY CUSTCOPY.
           STOP RUN.
