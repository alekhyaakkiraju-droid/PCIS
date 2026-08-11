      *================================================================*
      * PROGRAM:     FIXWS001                                         *
      * MODULE:      TEST                                             *
      * TYPE:        BATCH                                            *
      * DESCRIPTION: Fixture for WORKING-STORAGE VALUE literals       *
      *----------------------------------------------------------------*
      * CALLS:       (none)                                           *
      * TABLES:      (none)                                           *
      * UI:          (none)                                           *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. FIXWS001.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-RETENTION-DAYS          PIC S9(7)      COMP-3 VALUE 365.
       01  WS-CHUNK-SIZE              PIC S9(7)      COMP-3 VALUE 5000.
       01  WS-LEAD-DAYS               PIC S9(4)      COMP VALUE 15.
       01  WS-GRACE-DAYS              PIC S9(4)      COMP VALUE 10.
       01  WS-RENEWAL-WINDOW-DAYS     PIC S9(4)      COMP VALUE 60.
       01  WS-REI-CESSION-THRESHOLD   PIC S9(9)V99   COMP-3
                                      VALUE 100000.00.
       01  WS-RUN-USER                PIC X(10) VALUE 'BATCHAUD  '.
       PROCEDURE DIVISION.
       0000-MAIN.
           STOP RUN.
