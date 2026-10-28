package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.testutil.TypicalPersons.getTypicalClientBook;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ClientBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyClientBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.InteractionNote;
import seedu.address.model.person.InteractionOutcome;
import seedu.address.model.person.InteractionType;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonClientBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Checks the transaction boundary for note changes, using only temporary data files. */
public class InteractionNoteFailureIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_saveFailure_preservesNotesFileAndFilterAndAllowsRetry() throws Exception {
        InteractionNote call = new InteractionNote(LocalDate.of(2000, 1, 1), "Called", InteractionType.CALL,
                InteractionOutcome.UNSUCCESSFUL);
        InteractionNote meeting = new InteractionNote(LocalDate.of(2000, 1, 2), "Met", InteractionType.MEETING,
                InteractionOutcome.SUCCESSFUL);
        List<String> commands = List.of("note 3 d/2000-01-03 t/MESSAGE o/PENDING n/New note",
                "noteedit 3 1 d/1999-12-31 n/Revised", "notedelete 3 2");
        List<List<InteractionNote>> expected = List.of(
                List.of(call, meeting, new InteractionNote(LocalDate.of(2000, 1, 3), "New note",
                        InteractionType.MESSAGE, InteractionOutcome.PENDING)),
                List.of(new InteractionNote(LocalDate.of(1999, 12, 31), "Revised", InteractionType.CALL,
                        InteractionOutcome.UNSUCCESSFUL), meeting), List.of(call));
        for (IOException failure : List.of(new IOException("Simulated write failure"),
                new AccessDeniedException("Simulated denied write"))) {
            for (int i = 0; i < commands.size(); i++) {
                Model model = new ModelManager(getTypicalClientBook(), new UserPrefs());
                Person original = model.getClientBook().getPersonList().get(2);
                model.setPerson(original, new PersonBuilder(original).withInteractionNotes(call, meeting).build());
                Path dataPath = Files.createTempDirectory(temporaryFolder, "notes-").resolve("clientbook.json");
                JsonClientBookStorage originalStorage = new JsonClientBookStorage(dataPath);
                originalStorage.saveClientBook(model.getClientBook());
                AtomicBoolean failWrites = new AtomicBoolean(true);
                JsonClientBookStorage failingStorage = new JsonClientBookStorage(dataPath) {
                    @Override
                    public void saveClientBook(ReadOnlyClientBook clientBook) throws IOException {
                        if (failWrites.get()) {
                            throw failure;
                        }
                        super.saveClientBook(clientBook);
                    }
                };
                LogicManager logic = new LogicManager(model, new StorageManager(failingStorage,
                        new JsonUserPrefsStorage(dataPath.resolveSibling("preferences.json"))));
                logic.execute("find Alice Carl");
                ReadOnlyClientBook before = new ClientBook(model.getClientBook());
                List<Person> displayedBefore = List.copyOf(model.getFilteredPersonList());
                String savedBefore = Files.readString(dataPath);
                String command = commands.get(i);

                CommandException error = assertThrows(CommandException.class, () -> logic.execute(command));

                assertEquals(LogicManager.FILE_OPS_ERROR_FORMAT, error.getMessage());
                assertSame(failure, error.getCause());
                assertEquals(before, model.getClientBook());
                assertEquals(displayedBefore, model.getFilteredPersonList());
                assertEquals(savedBefore, Files.readString(dataPath));
                assertEquals(before, originalStorage.readClientBook().orElseThrow());
                failWrites.set(false);
                logic.execute(command);
                assertEquals(expected.get(i), model.getClientBook().getPersonList().get(2).getInteractionNotes());
                assertEquals(2, model.getFilteredPersonList().size());
                assertEquals(model.getClientBook(), originalStorage.readClientBook().orElseThrow());
            }
        }
    }
}
