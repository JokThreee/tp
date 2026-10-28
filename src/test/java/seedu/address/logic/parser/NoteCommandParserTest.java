package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.NoteCommand;
import seedu.address.model.person.InteractionNote;
import seedu.address.model.person.InteractionOutcome;
import seedu.address.model.person.InteractionType;

public class NoteCommandParserTest {
    private final NoteCommandParser parser = new NoteCommandParser();

    @Test
    public void parse_null_rejected() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void parse_optionalMetadata_acceptsEitherOrderAndCaseWithoutParsingBodyPrefixes() {
        LocalDate date = LocalDate.of(2000, 1, 1);
        assertParseSuccess(parser, "1 d/2000-01-01 t/cAlL o/unSUCCESSFUL n/No answer t/OTHER o/PENDING",
                new NoteCommand(INDEX_FIRST_PERSON, new InteractionNote(date, "No answer t/OTHER o/PENDING",
                        InteractionType.CALL, InteractionOutcome.UNSUCCESSFUL)));
        assertParseSuccess(parser, "1 d/2000-01-01 o/pending t/meeting n/Text",
                new NoteCommand(INDEX_FIRST_PERSON, new InteractionNote(date, "Text",
                        InteractionType.MEETING, InteractionOutcome.PENDING)));
        assertParseSuccess(parser, "1 d/2000-01-01\tt/message n/Text",
                new NoteCommand(INDEX_FIRST_PERSON, new InteractionNote(date, "Text",
                        InteractionType.MESSAGE, InteractionOutcome.UNSPECIFIED)));
        assertParseSuccess(parser, "1 d/2000-01-01 o/successful n/Text",
                new NoteCommand(INDEX_FIRST_PERSON, new InteractionNote(date, "Text",
                        InteractionType.UNSPECIFIED, InteractionOutcome.SUCCESSFUL)));
    }

    @Test
    public void parse_invalidMetadataAndMalformedPrefixes_rejected() {
        for (String type : List.of("", "PHONE", "CALL MEETING", "CALL o=PENDING")) {
            assertParseFailure(parser, "1 d/2000-01-01 t/" + type + " n/Text",
                    InteractionType.MESSAGE_CONSTRAINTS);
        }
        for (String outcome : List.of("", "FAILED", "SUCCESSFUL PENDING")) {
            assertParseFailure(parser, "1 d/2000-01-01 o/" + outcome + " n/Text",
                    InteractionOutcome.MESSAGE_CONSTRAINTS);
        }
        assertParseFailure(parser, "1 d/2000-01-01 t=CALL n/Text", InteractionNote.MESSAGE_INVALID_DATE);
        assertParseFailure(parser, "1 d/2000-01-01 t/CALL o/PENDING", NoteCommandParser.MESSAGE_NOTE_REQUIRED);
        assertParseFailure(parser, "1 t/CALL n/Text", NoteCommandParser.MESSAGE_DATE_REQUIRED);
        assertParseFailure(parser, "t/CALL n/Text", NoteCommandParser.MESSAGE_INDEX_REQUIRED);
        assertParseFailure(parser, "o/PENDING n/Text", NoteCommandParser.MESSAGE_INDEX_REQUIRED);
        assertParseFailure(parser, "1 d/2000-01-01 t/CALL n/ ", InteractionNote.MESSAGE_BLANK);
    }

    @Test
    public void parse_duplicateHeaderPrefixes_rejected() {
        for (Prefix prefix : List.of(CliSyntax.PREFIX_DUE_DATE, CliSyntax.PREFIX_INTERACTION_TYPE,
                CliSyntax.PREFIX_INTERACTION_OUTCOME)) {
            assertParseFailure(parser, "1 d/2000-01-01 t/CALL o/PENDING " + prefix.getPrefix() + "CALL n/Text",
                    seedu.address.logic.Messages.getErrorMessageForDuplicatePrefixes(prefix));
        }
    }

    @Test
    public void parse_boundaryYears_success() {
        for (String date : List.of("0001-01-01", "9999-12-31")) {
            assertParseSuccess(parser, "1 d/" + date + " n/Text", new NoteCommand(INDEX_FIRST_PERSON,
                    new InteractionNote(LocalDate.parse(date), "Text")));
        }
    }

    @Test
    public void parse_pastDateAndRealisticText_success() {
        String text = "客户's plan — https://example.com/a/b; n/text d/2000-01-01 " + "A".repeat(1000);
        assertParseSuccess(parser, " 1 d/2000-02-29 n/" + text + "  ",
                new NoteCommand(INDEX_FIRST_PERSON, new InteractionNote(LocalDate.of(2000, 2, 29), text)));
    }

    @Test
    public void parse_missingFields_clearErrors() {
        for (String args : List.of("", "d/2026-10-06 n/Text", "n/Text")) {
            assertParseFailure(parser, args, NoteCommandParser.MESSAGE_INDEX_REQUIRED);
        }
        assertParseFailure(parser, "1 n/Text", NoteCommandParser.MESSAGE_DATE_REQUIRED);
        assertParseFailure(parser, "1 d/ n/Text", NoteCommandParser.MESSAGE_DATE_REQUIRED);
        assertParseFailure(parser, "1 d/2026-10-06", NoteCommandParser.MESSAGE_NOTE_REQUIRED);
        assertParseFailure(parser, "1 d/2026-10-06 n/   ", InteractionNote.MESSAGE_BLANK);
        assertParseFailure(parser, "0 d/2026-10-06 n/Text", ParserUtil.MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_invalidDates_clearError() {
        for (String date : List.of("0000-01-01", "0000-02-29", "10000-01-01", "2026-02-30",
                "2025-02-29", "2026-00-01", "2026-2-01", "tomorrow")) {
            assertParseFailure(parser, "1 d/" + date + " n/Text", InteractionNote.MESSAGE_INVALID_DATE);
        }
    }
}
