      *================================================================*
      * PROGRAM:     FIXAUDI                                          *
      * MODULE:      TEST                                             *
      * TYPE:        INTERACTIVE                                      *
      * DESCRIPTION: Fixture for interactive AUDLOG01 CALL shape      *
      *----------------------------------------------------------------*
      * CALLS:       AUDLOG01                                         *
      * TABLES:      CUSTOMER_T                                       *
      * UI:          CUSMNTD1                                         *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. FIXAUDI.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-AUDIT-PARMS.
           05  WS-AUD-ACTION       PIC X(1).
           05  WS-AUD-OLD-VALUE    PIC X(100).
           05  WS-AUD-NEW-VALUE    PIC X(100).
           05  WS-AUD-KEY          PIC X(40).
       01  WS-USER-ID              PIC X(10) VALUE 'TELLER01'.
       PROCEDURE DIVISION.
       1000-UPDATE-CUSTOMER.
           MOVE 'U' TO WS-AUD-ACTION
           MOVE 'OLD-NAME-VALUE' TO WS-AUD-OLD-VALUE
           MOVE 'NEW-NAME-VALUE' TO WS-AUD-NEW-VALUE
           MOVE 'CUST-0001' TO WS-AUD-KEY
           CALL 'AUDLOG01' USING WS-AUD-ACTION
                                 WS-AUD-OLD-VALUE
                                 WS-AUD-NEW-VALUE
                                 WS-AUD-KEY
           STOP RUN.
