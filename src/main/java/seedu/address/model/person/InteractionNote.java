package seedu.address.model.person;

import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/** An immutable dated interaction note. Dates need not be recent or unique. */
public class InteractionNote {
    public static final String MESSAGE_INVALID_DATE = "Interaction date must be a valid date in yyyy-MM-dd format.";
    public static final String MESSAGE_BLANK = "Interaction note cannot be empty.";

    private final LocalDate date;
    private final String text;
    private final InteractionType type;
    private final InteractionOutcome outcome;

    /** Creates a note with nonblank text, preserving its content and case. */
    public InteractionNote(LocalDate date, String text) {
        this(date, text, InteractionType.UNSPECIFIED, InteractionOutcome.UNSPECIFIED);
    }

    /** Creates a note with explicit type and outcome; unspecified values represent absent metadata. */
    public InteractionNote(LocalDate date, String text, InteractionType type, InteractionOutcome outcome) {
        requireAllNonNull(date, text, type, outcome);
        checkArgument(date.getYear() >= 1 && date.getYear() <= 9999, MESSAGE_INVALID_DATE);
        checkArgument(!text.isBlank(), MESSAGE_BLANK);
        this.date = date;
        this.text = text.strip();
        this.type = type;
        this.outcome = outcome;
    }

    /** Parses a real calendar date in years 0001 to 9999 in the documented four-digit-year format. */
    public static LocalDate parseDate(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new IllegalArgumentException(MESSAGE_INVALID_DATE);
        }
        try {
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            checkArgument(date.getYear() >= 1, MESSAGE_INVALID_DATE);
            return date;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(MESSAGE_INVALID_DATE, e);
        }
    }

    public LocalDate getDate() {
        return date;
    }

    public String getText() {
        return text;
    }

    public InteractionType getType() {
        return type;
    }

    public InteractionOutcome getOutcome() {
        return outcome;
    }

    /** Keeps the existing date-and-text duplicate rule independent of interaction metadata. */
    public boolean hasSameContent(InteractionNote other) {
        return date.equals(other.date) && text.equals(other.text);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof InteractionNote note
                && hasSameContent(note) && type == note.type && outcome == note.outcome;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, text, type, outcome);
    }

    @Override
    public String toString() {
        return date + " [Type: " + type + "; Outcome: " + outcome + "]: " + text;
    }
}
