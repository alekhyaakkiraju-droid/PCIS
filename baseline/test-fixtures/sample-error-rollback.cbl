      *================================================================*
      * PROGRAM:     FIXERR01                                         *
      * MODULE:      TEST                                             *
      * TYPE:        BATCH                                            *
      * DESCRIPTION: Fixture for SQLCODE error path with ROLLBACK     *
      *----------------------------------------------------------------*
      * CALLS:       (none)                                           *
      * TABLES:      AUDIT_LOG_T                                      *
      * UI:          (none)                                           *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. FIXERR01.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
           EXEC SQL INCLUDE SQLCA END-EXEC.
       01  WS-SQLCODE-SAVE            PIC S9(9) COMP VALUE 0.
       PROCEDURE DIVISION.
       0000-MAIN.
           EXEC SQL
               DELETE FROM AUDIT_LOG_T WHERE LOG_ID = 1
           END-EXEC
           IF SQLCODE NOT = 0
               MOVE SQLCODE TO WS-SQLCODE-SAVE
               EXEC SQL ROLLBACK END-EXEC
               DISPLAY 'ROLLBACK ISSUED'
           END-IF
           STOP RUN.
