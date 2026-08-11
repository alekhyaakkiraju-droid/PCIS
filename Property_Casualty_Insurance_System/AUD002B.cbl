      *================================================================*
      * PROGRAM:     AUD002B                                          *
      * MODULE:      AUDIT                                            *
      * TYPE:        BATCH                                            *
      * DESCRIPTION: Audit Archive Batch                              *
      *----------------------------------------------------------------*
      * CALLS:       (none)                                           *
      *              NOTE: does not call AUDLOG01 - known arch gap   *
      * TABLES:      AUDIT_LOG_T                                      *
      *              AUDIT_LOG_ARCHIVE_T                              *
      * UI:                                                           *
      *              (none)                                           *
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. AUD002B.
       AUTHOR. PCIS-DEVELOPMENT.
       DATE-WRITTEN. 2024-03-01.
      *----------------------------------------------------------------*
      * Audit Archive Batch program.                                  *
      * Reads AUDIT_LOG_T records older than the retention period,    *
      * copies them to AUDIT_LOG_ARCHIVE_T using a NOT EXISTS guard   *
      * to prevent double-archive on restart, verifies copy count,   *
      * and deletes from AUDIT_LOG_T. Processes in chunks of 1000.   *
      * Rolls back on verify failure.                                 *
      * NOTE: This program does not call AUDLOG01 (known gap).        *
      *----------------------------------------------------------------*
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ISERIES.
       OBJECT-COMPUTER. IBM-ISERIES.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
           EXEC SQL INCLUDE SQLCA END-EXEC.

      * Processing parameters
       01  WS-CHUNK-SIZE              PIC S9(7)      COMP-3 VALUE 1000.
       01  WS-RETENTION-DAYS          PIC S9(7)      COMP-3 VALUE 365.

      * Counters
       01  WS-COUNTERS.
           05  WS-CNT-COPIED          PIC S9(9)      COMP-3 VALUE 0.
           05  WS-CNT-DELETED         PIC S9(9)      COMP-3 VALUE 0.
           05  WS-CNT-CHUNKS          PIC S9(7)      COMP-3 VALUE 0.
           05  WS-CNT-ERRORS          PIC S9(7)      COMP-3 VALUE 0.

      * Run-log timing and counter host variables (WO-237)
       01  WS-START-TIMESTAMP         PIC X(26).
       01  WS-END-TIMESTAMP           PIC X(26).
       01  WS-RL-PGM-NAME             PIC X(10)      VALUE 'AUD002B'.
       01  WS-RL-RUN-DATE             PIC X(10).
       01  WS-RL-SELECTED             PIC S9(9)      COMP-3 VALUE 0.
       01  WS-RL-UPDATED              PIC S9(9)      COMP-3 VALUE 0.
       01  WS-RL-ERRORS               PIC S9(9)      COMP-3 VALUE 0.

      * Return code and status
       01  WS-RETURN-CODE             PIC S9(4)      COMP VALUE 0.
       01  WS-SQLCODE-SAVE            PIC S9(9)      COMP VALUE 0.
       01  WS-MORE-ROWS               PIC X          VALUE 'Y'.
       01  WS-ROWS-INSERTED           PIC S9(9)      COMP-3 VALUE 0.
       01  WS-ROWS-DELETED            PIC S9(9)      COMP-3 VALUE 0.

      * Cutoff date for archive
       01  WS-CUTOFF-DATE             PIC X(10)      VALUE SPACES.

      * Host variables for SQL
       01  WS-HOST-VARS.
           05  HV-CUTOFF-DATE         PIC X(10).
           05  HV-CHUNK-SIZE          PIC S9(7)      COMP-3.
           05  HV-ROWS-INSERTED       PIC S9(9)      COMP-3.
           05  HV-ROWS-DELETED        PIC S9(9)      COMP-3.

       PROCEDURE DIVISION.

      * Main control paragraph
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS-CHUNKS
               UNTIL WS-MORE-ROWS = 'N'
           PERFORM 3000-FINALIZE
           STOP RUN.

      * Initialization - compute cutoff date
       1000-INITIALIZE.
           EXEC SQL
               VALUES (CURRENT TIMESTAMP)
               INTO :WS-START-TIMESTAMP
           END-EXEC
           MOVE 0 TO WS-CNT-COPIED
           MOVE 0 TO WS-CNT-DELETED
           MOVE 0 TO WS-CNT-CHUNKS
           MOVE 0 TO WS-CNT-ERRORS
           MOVE 0 TO WS-RETURN-CODE
           MOVE 'Y' TO WS-MORE-ROWS
           MOVE WS-CHUNK-SIZE TO HV-CHUNK-SIZE
           EXEC SQL
               SELECT CHAR(CURRENT_DATE - :WS-RETENTION-DAYS DAYS, ISO)
               INTO   :HV-CUTOFF-DATE
               FROM   SYSIBM.SYSDUMMY1
           END-EXEC
           MOVE HV-CUTOFF-DATE TO WS-CUTOFF-DATE
           DISPLAY 'AUD002B: ARCHIVE CUTOFF DATE = ' WS-CUTOFF-DATE
           .

      * Process one chunk of archive records
       2000-PROCESS-CHUNKS.
           PERFORM 2100-COPY-CHUNK
           IF WS-SQLCODE-SAVE = 0
               PERFORM 2200-VERIFY-CHUNK
               IF WS-SQLCODE-SAVE = 0
                   PERFORM 2300-DELETE-CHUNK
                   ADD 1 TO WS-CNT-CHUNKS
                   EXEC SQL
                       COMMIT
                   END-EXEC
               ELSE
                   PERFORM 9000-ROLLBACK-CHUNK
               END-IF
           ELSE
               MOVE 'N' TO WS-MORE-ROWS
           END-IF
           .

      * Copy chunk from AUDIT_LOG_T to AUDIT_LOG_ARCHIVE_T.
      * Lines L144-L146 equivalent: NOT EXISTS guard prevents
      * double-archiving on batch restart.
       2100-COPY-CHUNK.
           MOVE ZEROS TO HV-ROWS-INSERTED
           EXEC SQL
               INSERT INTO AUDIT_LOG_ARCHIVE_T
                   (LOG_ID,
                    PROGRAM_NAME,
                    ACTION_CODE,
                    TABLE_NAME,
                    RECORD_KEY,
                    USER_ID,
                    LOG_TIMESTAMP,
                    ARCHIVE_DATE)
               SELECT A.LOG_ID,
                      A.PROGRAM_NAME,
                      A.ACTION_CODE,
                      A.TABLE_NAME,
                      A.RECORD_KEY,
                      A.USER_ID,
                      A.LOG_TIMESTAMP,
                      CURRENT_DATE
               FROM   AUDIT_LOG_T A
               WHERE  A.LOG_TIMESTAMP < :HV-CUTOFF-DATE
               AND    NOT EXISTS (
                          SELECT 1
                          FROM   AUDIT_LOG_ARCHIVE_T X
                          WHERE  X.LOG_ID = A.LOG_ID
                      )
               FETCH FIRST :HV-CHUNK-SIZE ROWS ONLY
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE = 0
               GET DIAGNOSTICS HV-ROWS-INSERTED = ROW_COUNT
               MOVE HV-ROWS-INSERTED TO WS-ROWS-INSERTED
               IF HV-ROWS-INSERTED = 0
                   MOVE 'N' TO WS-MORE-ROWS
               END-IF
               ADD HV-ROWS-INSERTED TO WS-CNT-COPIED
           ELSE
               DISPLAY 'AUD002B: INSERT ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

      * Verify inserted rows match expected count
       2200-VERIFY-CHUNK.
           EXEC SQL
               SELECT COUNT(*)
               INTO   :HV-ROWS-INSERTED
               FROM   AUDIT_LOG_ARCHIVE_T
               WHERE  ARCHIVE_DATE = CURRENT_DATE
               AND    LOG_TIMESTAMP < :HV-CUTOFF-DATE
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE NOT = 0
               DISPLAY 'AUD002B: VERIFY COUNT FAILED SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

      * Delete the archived chunk from AUDIT_LOG_T
       2300-DELETE-CHUNK.
           EXEC SQL
               DELETE FROM AUDIT_LOG_T A
               WHERE  A.LOG_TIMESTAMP < :HV-CUTOFF-DATE
               AND    EXISTS (
                          SELECT 1
                          FROM   AUDIT_LOG_ARCHIVE_T X
                          WHERE  X.LOG_ID = A.LOG_ID
                      )
               FETCH FIRST :HV-CHUNK-SIZE ROWS ONLY
           END-EXEC
           MOVE SQLCODE TO WS-SQLCODE-SAVE
           IF SQLCODE = 0
               GET DIAGNOSTICS HV-ROWS-DELETED = ROW_COUNT
               ADD HV-ROWS-DELETED TO WS-CNT-DELETED
           ELSE
               DISPLAY 'AUD002B: DELETE ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

      * Rollback chunk on verify failure
       9000-ROLLBACK-CHUNK.
           EXEC SQL
               ROLLBACK
           END-EXEC
           DISPLAY 'AUD002B: ROLLBACK EXECUTED FOR CHUNK '
                   WS-CNT-CHUNKS
           ADD 1 TO WS-CNT-ERRORS
           MOVE 'N' TO WS-MORE-ROWS
           MOVE 8 TO WS-RETURN-CODE
           .

      * Finalize - display totals
       3000-FINALIZE.
           DISPLAY 'AUD002B AUDIT ARCHIVE COMPLETE'
           DISPLAY '  CHUNKS PROCESSED : ' WS-CNT-CHUNKS
           DISPLAY '  RECORDS COPIED   : ' WS-CNT-COPIED
           DISPLAY '  RECORDS DELETED  : ' WS-CNT-DELETED
           DISPLAY '  ERRORS           : ' WS-CNT-ERRORS
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
           MOVE WS-CNT-COPIED TO WS-RL-SELECTED
           MOVE WS-CNT-COPIED TO WS-RL-UPDATED
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
               DISPLAY 'AUD002B: RUN LOG INSERT ERROR SQLCODE='
                       WS-SQLCODE-SAVE
               ADD 1 TO WS-CNT-ERRORS
           END-IF
           .

       END PROGRAM AUD002B.
