package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalClientBook;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.ClientBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyClientBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.InteractionNote;
import seedu.address.model.person.InteractionOutcome;
import seedu.address.model.person.InteractionType;
import seedu.address.model.person.Person;
import seedu.address.model.person.Relationship;
import seedu.address.storage.JsonClientBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

public class NoteCommandTest {
    @TempDir
    public Path temporaryFolder;

    private final Model model = new ModelManager(getTypicalClientBook(), new UserPrefs());
    private final ClientBookParser parser = new ClientBookParser();

    @Test
    public void equals_comparesContactAndCompleteNoteIncludingMetadata() {
        LocalDate date = LocalDate.of(2000, 1, 1);
        InteractionNote note = new InteractionNote(date, "Called", InteractionType.CALL, InteractionOutcome.PENDING);
        NoteCommand command = new NoteCommand(INDEX_FIRST_PERSON, note);
        assertEquals(command, command);
        assertEquals(command, new NoteCommand(INDEX_FIRST_PERSON,
                new InteractionNote(date, "Called", InteractionType.CALL, InteractionOutcome.PENDING)));
        assertNotEquals(command, null);
        assertNotEquals(command, "note");
        assertNotEquals(command, new NoteCommand(INDEX_SECOND_PERSON, note));
        assertNotEquals(command, new NoteCommand(INDEX_FIRST_PERSON,
                new InteractionNote(date, "Called", InteractionType.MEETING, InteractionOutcome.PENDING)));
        assertNotEquals(command, new NoteCommand(INDEX_FIRST_PERSON,
                new InteractionNote(date, "Called", InteractionType.CALL, InteractionOutcome.SUCCESSFUL)));
    }

    @Test
    public void constructorAndExecute_nullArguments_rejected() {
        InteractionNote note = new InteractionNote(LocalDate.of(2000, 1, 1), "Called");
        assertThrows(NullPointerException.class, () -> new NoteCommand(null, note));
        assertThrows(NullPointerException.class, () -> new NoteCommand(INDEX_FIRST_PERSON, null));
        assertThrows(NullPointerException.class, () -> new NoteCommand(INDEX_FIRST_PERSON, note).execute(null, null));
    }

    @Test
    public void execute_throughLogic_validDatesPersistAndInvalidDatesPreserveRecords() throws Exception {
        JsonClientBookStorage storage = new JsonClientBookStorage(temporaryFolder.resolve("clientbook.json"));
        LogicManager logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))));
        InteractionNote earliest = new InteractionNote(LocalDate.of(1, 1, 1), "Earliest date");
        InteractionNote past = new InteractionNote(LocalDate.of(2000, 2, 29), "Past date");
        logic.execute("note 1 d/0001-01-01 n/Earliest date");
        logic.execute("note 1 d/2000-02-29 n/Past date");
        assertEquals(List.of(earliest, past), model.getFilteredPersonList().get(0).getInteractionNotes());
        ReadOnlyClientBook saved = storage.readClientBook().orElseThrow();
        assertEquals(model.getClientBook(), saved);
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        List<Person> displayed = List.copyOf(model.getFilteredPersonList());
        String savedJson = Files.readString(storage.getClientBookFilePath());

        for (String date : List.of("0000-01-01", "0000-02-29", "10000-01-01", "2026-02-30")) {
            ParseException error = assertThrows(ParseException.class, () ->
                    logic.execute("note 1 d/" + date + " n/Test invalid date"));
            assertEquals(InteractionNote.MESSAGE_INVALID_DATE, error.getMessage());
            assertEquals(saved, model.getClientBook());
            assertEquals(displayed, model.getFilteredPersonList());
            assertEquals(savedJson, Files.readString(storage.getClientBookFilePath()));
            assertEquals(saved, storage.readClientBook().orElseThrow());
        }
    }

    @Test
    public void execute_todayAndPastDates_appendNotes() throws Exception {
        LocalDate today = LocalDate.now();
        Person original = model.getFilteredPersonList().get(0);
        InteractionNote past = new InteractionNote(today.minusDays(1), "Yesterday's meeting");
        InteractionNote current = new InteractionNote(today, "Today's meeting");

        parser.parseCommand("note 1 d/" + past.getDate() + " n/" + past.getText()).execute(model, null);
        parser.parseCommand("note 1 d/" + current.getDate() + " n/" + current.getText()).execute(model, null);

        assertEquals(new PersonBuilder(original).withInteractionNotes(past, current).build(),
                model.getFilteredPersonList().get(0));
    }

    @Test
    public void execute_futureDate_rejectsWithoutChangingRecordsOrFilter() throws Exception {
        InteractionNote existing = new InteractionNote(LocalDate.now().minusDays(1), "Initial meeting");
        Person original = model.getFilteredPersonList().get(1);
        model.setPerson(original, new PersonBuilder(original).withInteractionNotes(existing).build());
        showPersonAtIndex(model, INDEX_SECOND_PERSON);

        Command command = parser.parseCommand("note 2 d/" + LocalDate.now().plusDays(1) + " n/met");

        assertCommandFailure(command, model, NoteCommand.MESSAGE_FUTURE_DATE);
    }

    @Test
    public void execute_newNote_succeedsAndRepeatedNoteIsRejected() throws Exception {
        Person original = new PersonBuilder(model.getFilteredPersonList().get(0))
                .withRelationship(Relationship.CLIENT).withFinancialNeed("Retirement")
                .withPriority("High").withAreaOfInterest("Insurance")
                .withPreferredContactChannel("Phone").withPreferredContactTiming("Evenings").build();
        model.setPerson(model.getFilteredPersonList().get(0), original);
        InteractionNote note = new InteractionNote(LocalDate.of(2000, 1, 1), "Discussed retirement");
        Command command = parser.parseCommand("note 1 d/2000-01-01 n/Discussed retirement");
        assertEquals("Added interaction note for " + original.getName().fullName + ".",
                command.execute(model, null).getFeedbackToUser());
        assertEquals(new PersonBuilder(original).withInteractionNotes(note).build(),
                model.getFilteredPersonList().get(0));
        assertCommandFailure(command, model, NoteCommand.MESSAGE_DUPLICATE_NOTE);
        assertEquals(List.of(note), model.getFilteredPersonList().get(0).getInteractionNotes());
    }

    @Test
    public void execute_sameDateOrSameTextOnly_allowsDistinctNotes() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 20);
        List<InteractionNote> notes = List.of(new InteractionNote(date, "Met client"),
                new InteractionNote(date, "Reviewed policy"), new InteractionNote(date.minusDays(1), "Met client"),
                new InteractionNote(date, "met client"), new InteractionNote(date, "Met  client"));
        for (InteractionNote note : notes) {
            new NoteCommand(INDEX_FIRST_PERSON, note).execute(model, null);
        }
        assertEquals(List.of(notes.get(2), notes.get(0), notes.get(1), notes.get(3), notes.get(4)),
                model.getFilteredPersonList().get(0).getInteractionNotes());
    }

    @Test
    public void execute_identicalNotesForDifferentContacts_succeeds() throws Exception {
        InteractionNote note = new InteractionNote(LocalDate.of(2026, 10, 20), "Met client");
        new NoteCommand(INDEX_FIRST_PERSON, note).execute(model, null);
        new NoteCommand(INDEX_SECOND_PERSON, note).execute(model, null);

        assertEquals(List.of(note), model.getFilteredPersonList().get(0).getInteractionNotes());
        assertEquals(List.of(note), model.getFilteredPersonList().get(1).getInteractionNotes());
    }

    @Test
    public void execute_afterReload_duplicatePreservesRecordsFilterAndSavedFile() throws Exception {
        Path dataFile = temporaryFolder.resolve("clientbook.json");
        JsonClientBookStorage storage = new JsonClientBookStorage(dataFile);
        StorageManager storageManager = new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));
        LogicManager logic = new LogicManager(model, storageManager);
        String command = "note 1 d/2026-10-20 n/Met client to review policy";
        logic.execute(command);
        ReadOnlyClientBook saved = storage.readClientBook().orElseThrow();
        Model reopened = new ModelManager(saved, new UserPrefs());
        showPersonAtIndex(reopened, INDEX_FIRST_PERSON);
        List<Person> displayed = List.copyOf(reopened.getFilteredPersonList());
        String savedJson = Files.readString(dataFile);
        LogicManager reopenedLogic = new LogicManager(reopened, storageManager);

        CommandException error = assertThrows(CommandException.class, () -> reopenedLogic.execute(command));

        assertEquals(NoteCommand.MESSAGE_DUPLICATE_NOTE, error.getMessage());
        assertEquals(saved, reopened.getClientBook());
        assertEquals(displayed, reopened.getFilteredPersonList());
        assertEquals(savedJson, Files.readString(dataFile));
        assertEquals(saved, storage.readClientBook().orElseThrow());
        assertCommandFailure(parser.parseCommand(command + "   "), reopened, NoteCommand.MESSAGE_DUPLICATE_NOTE);
    }

    @Test
    public void execute_existingHistory_sortsBackdatedNoteAndPreservesOtherFields() throws Exception {
        InteractionNote existing = new InteractionNote(LocalDate.of(2026, 10, 6), "Initial meeting");
        InteractionNote backdated = new InteractionNote(LocalDate.of(2000, 1, 1), "Earlier interaction");
        Person original = new PersonBuilder(model.getFilteredPersonList().get(0))
                .withRelationship(Relationship.CLIENT).withFinancialNeed("Retirement")
                .withPriority("High").withAreaOfInterest("Insurance").withInteractionNotes(existing).build();
        model.setPerson(model.getFilteredPersonList().get(0), original);

        new NoteCommand(INDEX_FIRST_PERSON, backdated).execute(model, null);

        assertEquals(List.of(backdated, existing), model.getFilteredPersonList().get(0).getInteractionNotes());
        assertEquals(new PersonBuilder(original).withInteractionNotes(backdated, existing).build(),
                model.getFilteredPersonList().get(0));
    }

    @Test
    public void execute_filteredIndex_updatesDisplayedPersonOnly() throws Exception {
        Person first = model.getFilteredPersonList().get(0);
        Person target = model.getFilteredPersonList().get(1);
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        parser.parseCommand("note 2 d/2000-01-01 n/Called").execute(model, null);
        assertEquals(first, model.getClientBook().getPersonList().get(0));
        assertEquals(target.getName(), model.getFilteredPersonList().get(0).getName());
        assertEquals(1, model.getFilteredPersonList().size());
        assertEquals(1, model.getFilteredPersonList().get(0).getInteractionNotes().size());
        assertCommandFailure(parser.parseCommand("note 2 d/2000-01-01 n/Called"), model,
                NoteCommand.MESSAGE_DUPLICATE_NOTE);
        assertCommandFailure(new NoteCommand(INDEX_FIRST_PERSON,
                new InteractionNote(LocalDate.of(2000, 1, 1), "Called")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_emptyList_invalidIndex() {
        assertCommandFailure(new NoteCommand(INDEX_FIRST_PERSON,
                new InteractionNote(LocalDate.of(2000, 1, 1), "Called")), new ModelManager(),
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_editAndInfo_preserveNotes() throws Exception {
        parser.parseCommand("note 1 d/2000-01-01 n/Called").execute(model, null);
        List<InteractionNote> notes = model.getFilteredPersonList().get(0).getInteractionNotes();
        parser.parseCommand("edit 1 p/81234567").execute(model, null);
        assertEquals(notes, model.getFilteredPersonList().get(0).getInteractionNotes());
        assertEquals("81234567", model.getFilteredPersonList().get(0).getPhone().value);
        parser.parseCommand("info 1 /fn Retirement").execute(model, null);
        assertEquals(notes, model.getFilteredPersonList().get(0).getInteractionNotes());
        assertEquals("Retirement", model.getFilteredPersonList().get(0).getFinancialNeed().orElseThrow().value);
    }
}
