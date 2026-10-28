package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class InteractionNoteTest {
    @Test
    public void constructor_unsupportedYears_rejectedBeforeTheyCanBePersisted() {
        for (LocalDate date : List.of(LocalDate.MIN, LocalDate.of(-1, 1, 1), LocalDate.of(0, 1, 1),
                LocalDate.of(10000, 1, 1), LocalDate.MAX)) {
            IllegalArgumentException legacyError = assertThrows(IllegalArgumentException.class, () ->
                    new InteractionNote(date, "Text"));
            assertEquals(InteractionNote.MESSAGE_INVALID_DATE, legacyError.getMessage());
            IllegalArgumentException typedError = assertThrows(IllegalArgumentException.class, () ->
                    new InteractionNote(date, "Text", InteractionType.CALL, InteractionOutcome.PENDING));
            assertEquals(InteractionNote.MESSAGE_INVALID_DATE, typedError.getMessage());
        }
        for (LocalDate date : List.of(LocalDate.of(1, 1, 1), LocalDate.of(9999, 12, 31))) {
            assertEquals(date, new InteractionNote(date, "Text").getDate());
        }
    }

    @Test
    public void equality_independentFieldsAndDuplicateKey_areDistinctContracts() {
        LocalDate date = LocalDate.of(2000, 1, 1);
        InteractionNote note = new InteractionNote(date, "Text", InteractionType.CALL, InteractionOutcome.PENDING);
        assertEquals(note, note);
        assertNotEquals(note, null);
        assertNotEquals(note, "Text");
        for (InteractionNote different : List.of(
                new InteractionNote(date.plusDays(1), "Text", InteractionType.CALL, InteractionOutcome.PENDING),
                new InteractionNote(date, "text", InteractionType.CALL, InteractionOutcome.PENDING),
                new InteractionNote(date, "Text", InteractionType.MEETING, InteractionOutcome.PENDING),
                new InteractionNote(date, "Text", InteractionType.CALL, InteractionOutcome.SUCCESSFUL))) {
            assertNotEquals(note, different);
            assertNotEquals(different, note);
        }
        InteractionNote differentMetadata = new InteractionNote(date, "Text", InteractionType.OTHER,
                InteractionOutcome.UNSUCCESSFUL);
        assertTrue(note.hasSameContent(differentMetadata));
        assertFalse(note.hasSameContent(new InteractionNote(date, "text")));
        assertFalse(note.hasSameContent(new InteractionNote(date.plusDays(1), "Text")));
    }

    @Test
    public void constructor_nullDateAndText_rejected() {
        assertThrows(NullPointerException.class, () -> new InteractionNote(null, "Text"));
        assertThrows(NullPointerException.class, () -> new InteractionNote(LocalDate.now(), null));
    }
    @Test
    public void metadata_defaultsEqualityDisplayAndNullValidation() {
        LocalDate date = LocalDate.of(2000, 1, 1);
        InteractionNote legacy = new InteractionNote(date, "Called");
        InteractionNote typed = new InteractionNote(date, "Called", InteractionType.CALL,
                InteractionOutcome.UNSUCCESSFUL);
        assertEquals(InteractionType.UNSPECIFIED, legacy.getType());
        assertEquals(InteractionOutcome.UNSPECIFIED, legacy.getOutcome());
        assertEquals("2000-01-01 [Type: CALL; Outcome: UNSUCCESSFUL]: Called", typed.toString());
        assertEquals("2000-01-01 [Type: UNSPECIFIED; Outcome: UNSPECIFIED]: Called", legacy.toString());
        assertNotEquals(legacy, typed);
        InteractionNote same = new InteractionNote(date, " Called ", InteractionType.CALL,
                InteractionOutcome.UNSUCCESSFUL);
        assertEquals(typed, same);
        assertEquals(typed.hashCode(), same.hashCode());
        assertThrows(NullPointerException.class, () -> new InteractionNote(date, "Text", null,
                InteractionOutcome.PENDING));
        assertThrows(NullPointerException.class, () -> new InteractionNote(date, "Text", InteractionType.CALL, null));
    }

    @Test
    public void metadataParsing_allValuesAndLocaleIndependentCase() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            for (InteractionType type : InteractionType.values()) {
                assertEquals(type, InteractionType.parse(" " + type.name().toLowerCase(Locale.ROOT) + " "));
            }
            for (InteractionOutcome outcome : InteractionOutcome.values()) {
                assertEquals(outcome, InteractionOutcome.parse(outcome.name().toLowerCase(Locale.ROOT)));
            }
            assertThrows(IllegalArgumentException.class, () -> InteractionType.parse(null));
            assertThrows(IllegalArgumentException.class, () -> InteractionOutcome.parse(null));
        } finally {
            Locale.setDefault(original);
        }
    }
    @Test
    public void parseDate_boundaryYears_accepted() {
        assertEquals(LocalDate.of(1, 1, 1), InteractionNote.parseDate("0001-01-01"));
        assertEquals(LocalDate.of(9999, 12, 31), InteractionNote.parseDate("9999-12-31"));
    }

    @Test
    public void parseDate_unsupportedYears_rejected() {
        for (String date : List.of("0000-01-01", "0000-02-29", "0000-12-31", "-0001-01-01",
                "10000-01-01", "+10000-01-01")) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                    InteractionNote.parseDate(date));
            assertEquals(InteractionNote.MESSAGE_INVALID_DATE, error.getMessage());
        }
    }

    @Test
    public void person_historySortedByDate_preservesTiesAndDoesNotModifyInput() {
        InteractionNote newer = new InteractionNote(LocalDate.of(2026, 10, 20), "Newer");
        InteractionNote older = new InteractionNote(LocalDate.of(2026, 10, 10), "Older");
        InteractionNote middle = new InteractionNote(LocalDate.of(2026, 10, 15), "Middle");
        InteractionNote sameDate = new InteractionNote(middle.getDate(), "Same date, added later");
        List<InteractionNote> source = new ArrayList<>(List.of(newer, middle, older, sameDate, middle));
        Person person = new Person(ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(), ALICE.getTags(),
                ALICE.getRelationship(), null, null, null, source);

        assertEquals(List.of(older, middle, sameDate, middle, newer), person.getInteractionNotes());
        assertEquals(List.of(newer, middle, older, sameDate, middle), source);
        source.clear();
        assertEquals(List.of(older, middle, sameDate, middle, newer), person.getInteractionNotes());
        assertThrows(UnsupportedOperationException.class, () -> person.getInteractionNotes().clear());
    }

    @Test
    public void parseDate_realDatesOnly() {
        assertEquals(LocalDate.of(2000, 2, 29), InteractionNote.parseDate("2000-02-29"));
        for (String invalid : List.of("2026-02-30", "2026-13-01", "2026-2-01", "06-10-2026", "")) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                    InteractionNote.parseDate(invalid));
            assertEquals(InteractionNote.MESSAGE_INVALID_DATE, error.getMessage());
        }
    }

    @Test
    public void text_realisticContentPreserved_blankRejected() {
        String text = "客户's plan — £100; https://example.com/a/b n/example d/2026-01-01";
        InteractionNote note = new InteractionNote(LocalDate.of(2000, 1, 1), "  " + text + "  ");
        assertEquals(text, note.getText());
        assertEquals(note, new InteractionNote(note.getDate(), text));
        assertNotEquals(note, new InteractionNote(note.getDate(), text.toUpperCase()));
        assertThrows(IllegalArgumentException.class, () -> new InteractionNote(note.getDate(), " \t "));
    }

    @Test
    public void person_historyIsImmutableAndIncludedInEquality() {
        InteractionNote note = new InteractionNote(LocalDate.of(2000, 1, 1), "Discussed retirement");
        List<InteractionNote> source = new ArrayList<>(List.of(note, note));
        Person person = new Person(ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(), ALICE.getTags(),
                ALICE.getRelationship(), null, null, null, source);
        source.clear();
        assertEquals(List.of(note, note), person.getInteractionNotes());
        assertThrows(UnsupportedOperationException.class, () -> person.getInteractionNotes().clear());
        assertEquals(person, new PersonBuilder(person).build());
        assertNotEquals(ALICE, person);
    }
}
