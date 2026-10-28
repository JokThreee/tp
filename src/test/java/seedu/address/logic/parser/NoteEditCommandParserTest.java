package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.NoteEditCommand;
import seedu.address.model.person.InteractionNote;

public class NoteEditCommandParserTest {
    private final NoteEditCommandParser parser = new NoteEditCommandParser();

    @Test
    public void parse_null_rejected() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void parse_metadataPrefixesInsideBody_remainLiteralText() {
        String text = "t/CALL o/SUCCESSFUL; n/another prefix d/2000-01-01";
        assertParseSuccess(parser, "1 2 n/" + text,
                new NoteEditCommand(INDEX_FIRST_PERSON, INDEX_SECOND_PERSON, text));
        assertParseSuccess(parser, "1 2 d/2000-01-01 n/" + text,
                new NoteEditCommand(INDEX_FIRST_PERSON, INDEX_SECOND_PERSON, LocalDate.of(2000, 1, 1), text));
    }

    @Test
    public void parse_validText_preservesContentAndTrimsEdges() {
        String text = "客户's plan; https://example.com/a/b n/text d/2000-01-01 " + "A".repeat(1000);
        assertParseSuccess(parser, " 1 \t 2 n/  " + text + "  ",
                new NoteEditCommand(INDEX_FIRST_PERSON, INDEX_SECOND_PERSON, text));
    }

    @Test
    public void parse_missingOrExtraFields_rejected() {
        for (String args : List.of("", "1", "1 2", "1 n/Text", "1 2 3 n/Text", "1 2 x/Text")) {
            assertParseFailure(parser, args,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, NoteEditCommand.MESSAGE_USAGE));
        }
    }

    @Test
    public void parse_dateOnlyAndDateWithText_success() {
        for (LocalDate date : List.of(LocalDate.of(2026, 10, 19), LocalDate.now(), LocalDate.now().plusDays(1))) {
            assertParseSuccess(parser, "1 2 d/ " + date + "  ",
                    new NoteEditCommand(INDEX_FIRST_PERSON, INDEX_SECOND_PERSON, date, null));
            String text = "Corrected n/literal d/2099-01-01 https://example.com";
            assertParseSuccess(parser, "1 2 d/" + date + " n/ " + text + "  ",
                    new NoteEditCommand(INDEX_FIRST_PERSON, INDEX_SECOND_PERSON, date, text));
        }
    }

    @Test
    public void parse_invalidOrDuplicateDates_rejected() {
        for (String date : List.of("", "2026-02-30", "2025-02-29", "2026-2-01", "tomorrow",
                "2000-01-01 d/2000-01-02")) {
            assertParseFailure(parser, "1 2 d/" + date, InteractionNote.MESSAGE_INVALID_DATE);
            assertParseFailure(parser, "1 2 d/" + date + " n/Text", InteractionNote.MESSAGE_INVALID_DATE);
        }
        assertParseFailure(parser, "1 2 d/2000-01-01 n/  ", InteractionNote.MESSAGE_BLANK);
    }

    @Test
    public void parse_invalidIndices_rejected() {
        for (String index : List.of("0", "-1", "1.5", "abc", "2147483648")) {
            assertParseFailure(parser, index + " 1 n/Text", ParserUtil.MESSAGE_INVALID_INDEX);
            assertParseFailure(parser, "1 " + index + " n/Text", ParserUtil.MESSAGE_INVALID_INDEX);
        }
    }

    @Test
    public void parse_blankContent_rejected() {
        for (String args : List.of("1 2 n/", "1 2 n/  \t ", "1 2 n/\u2003")) {
            assertParseFailure(parser, args, InteractionNote.MESSAGE_BLANK);
        }
    }

    @Test
    public void parseCommand_routesToNoteEditParser() throws Exception {
        assertEquals(new NoteEditCommand(INDEX_FIRST_PERSON, INDEX_SECOND_PERSON, "Updated"),
                new ClientBookParser().parseCommand("noteedit 1 2 n/Updated"));
    }
}
