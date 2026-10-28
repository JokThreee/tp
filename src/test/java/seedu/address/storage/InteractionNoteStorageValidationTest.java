package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.ReadOnlyClientBook;
import seedu.address.model.person.InteractionNote;
import seedu.address.model.person.InteractionOutcome;
import seedu.address.model.person.InteractionType;

/** Validates nested note JSON through the same file-loading path used when reopening ClientBook. */
public class InteractionNoteStorageValidationTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void read_corruptedNote_rejectsFileWithoutRewritingItAndRetainsCause() throws Exception {
        List<String> invalidNotes = List.of("null", "{}", "{\"date\":null,\"text\":\"Text\"}",
                "{\"date\":\"2000-01-01\",\"text\":null}", "{\"date\":\"2000-01-01\",\"text\":\"  \"}",
                "{\"date\":\"0000-01-01\",\"text\":\"Text\"}",
                "{\"date\":\"2000-01-01\",\"text\":\"Text\",\"type\":\"PHONE\"}",
                "{\"date\":\"2000-01-01\",\"text\":\"Text\",\"outcome\":\"FAILED\"}");
        List<String> messages = List.of("Interaction note cannot be null.", InteractionNote.MESSAGE_BLANK,
                InteractionNote.MESSAGE_INVALID_DATE, InteractionNote.MESSAGE_BLANK, InteractionNote.MESSAGE_BLANK,
                InteractionNote.MESSAGE_INVALID_DATE, InteractionType.MESSAGE_CONSTRAINTS,
                InteractionOutcome.MESSAGE_CONSTRAINTS);
        Path path = temporaryFolder.resolve("invalid-notes.json");
        JsonClientBookStorage storage = new JsonClientBookStorage(path);
        for (int i = 0; i < invalidNotes.size(); i++) {
            String json = bookWithNotes("[" + invalidNotes.get(i) + "]");
            Files.writeString(path, json);
            DataLoadingException error = assertThrows(DataLoadingException.class, storage::readClientBook);
            IllegalValueException cause = assertInstanceOf(IllegalValueException.class, error.getCause());
            assertEquals(messages.get(i), cause.getMessage());
            assertEquals(json, Files.readString(path));
        }
    }

    @Test
    public void read_legacyNullAndMissingHistoriesAndUnspecifiedMetadata_roundTrip() throws Exception {
        Path path = temporaryFolder.resolve("legacy-notes.json");
        JsonClientBookStorage storage = new JsonClientBookStorage(path);
        for (String notes : List.of("null", "[]")) {
            Files.writeString(path, bookWithNotes(notes));
            assertEquals(List.of(), storage.readClientBook().orElseThrow().getPersonList().getFirst()
                    .getInteractionNotes());
        }
        Files.writeString(path, bookWithNotes("[]").replace(",\"interactionNotes\":[]", ""));
        assertEquals(List.of(), storage.readClientBook().orElseThrow().getPersonList().getFirst()
                .getInteractionNotes());
        Files.writeString(path, bookWithNotes("[{\"date\":\"2000-01-01\",\"text\":\"Text\","
                + "\"type\":\"unspecified\",\"outcome\":\"UNSPECIFIED\"}]"));
        ReadOnlyClientBook loaded = storage.readClientBook().orElseThrow();
        assertEquals(List.of(new InteractionNote(LocalDate.of(2000, 1, 1), "Text")),
                loaded.getPersonList().getFirst().getInteractionNotes());
        storage.saveClientBook(loaded);
        assertEquals(loaded, storage.readClientBook().orElseThrow());
    }

    @Test
    public void toModelType_boundaryModelDatesAndMetadata_roundTrip() throws Exception {
        for (LocalDate date : List.of(LocalDate.of(1, 1, 1), LocalDate.of(9999, 12, 31))) {
            InteractionNote note = new InteractionNote(date, "Boundary", InteractionType.CALL,
                    InteractionOutcome.PENDING);
            assertEquals(note, new JsonAdaptedInteractionNote(note).toModelType());
        }
    }

    private static String bookWithNotes(String notes) {
        return "{\"persons\":[{\"name\":\"Test Client\",\"phone\":\"81234567\",\"email\":\"test@example.com\","
                + "\"interactionNotes\":" + notes + "}]}";
    }
}
