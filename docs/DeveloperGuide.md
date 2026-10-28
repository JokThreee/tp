---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# ClientBook Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* _{List the sources of reused or adapted ideas, code, documentation, and third-party libraries here, with links to the originals.}_

* Junhan Liu (`ZachL1113`) used OpenAI ChatGPT/Codex to draft the v1.3 delete integration tests, additional parser
  boundary tests, and the accompanying delete documentation. This assistance covers `DeleteIntegrationTest`,
  the new cases in `DeleteCommandParserTest`, the UG delete section additions, and the DG deletion test instructions.
  Codex also helped correct the UG product/download references and synchronise UG/DG find instructions with the
  combined name-and-relationship search already merged in PR #67.

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.md).

--------------------------------------------------------------------------------------------------------------------

## **Design**

### Architecture

<puml src="diagrams/ArchitectureDiagram.puml" width="280" />

The ***Architecture Diagram*** given above explains the high-level design of the App.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/AY2627S1-CS2103T-F14a-4/tp/tree/master/src/main/java/seedu/address/Main.java) and [`MainApp`](https://github.com/AY2627S1-CS2103T-F14a-4/tp/tree/master/src/main/java/seedu/address/MainApp.java)) is in charge of the app launch and shut down.
* At app launch, it initializes the other components in the correct sequence, and connects them up with each other.
* At shut down, it shuts down the other components and invokes cleanup methods where necessary.

The bulk of the app's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of the App.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of the App in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

The *Sequence Diagram* below shows how the components interact with each other for the scenario where the user issues the command `delete 1`.

<puml src="diagrams/ArchitectureSequenceDiagram.puml" width="574" />

Each of the four main components (also shown in the diagram above),

* defines its *API* in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<puml src="diagrams/ComponentManagers.puml" width="300" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/AY2627S1-CS2103T-F14a-4/tp/tree/master/src/main/java/seedu/address/ui/Ui.java)

<puml src="diagrams/UiClassDiagram.puml" alt="Structure of the UI Component"/>

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `PersonListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-F14a-4/tp/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-F14a-4/tp/tree/master/src/main/java/seedu/address/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* displays the exact command submitted by the user above its success or error feedback in the `ResultDisplay`.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Person` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-F14a-4/tp/tree/master/src/main/java/seedu/address/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking an `add` command as an example.

<puml src="diagrams/AddSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the add command" />

<box type="info" seamless>

**Note:** The lifeline for `AddCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, the lifeline continues till the end of diagram.
</box>


How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to a `ClientBookParser` object, which in turn creates a parser that matches the command (e.g., `AddCommandParser`) and uses it to parse the command.
2. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `AddCommand`) which is executed by the `LogicManager`.
3. The command can communicate with the `Model` when it is executed (e.g. to add a person).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
4. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

`LogicManager` saves the client book only when a command changes its records. Read-only commands, such as `list`,
`find`, and `due`, update the displayed view from memory without writing to storage. Before saving a changed client
book, `LogicManager` keeps a copy of the previous records and filtered view. If saving fails, it restores both so the
application does not display a change that will be lost after restart.

The sequence diagram below shows the corresponding flow for an `edit` command.

<puml src="diagrams/EditSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the edit command" />

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<puml src="diagrams/ParserClasses.puml" width="600"/>

How the parsing works:
* When called upon to parse a user command, the `ClientBookParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddCommand`). The `ClientBookParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

### Model component
**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-F14a-4/tp/tree/master/src/main/java/seedu/address/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="450" />


The `Model` component,

* stores the client book data i.e., all `Person` objects (which are contained in a `UniquePersonList` object).
* stores the `Person` objects selected by the current filter, such as search results, in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Person>` that the UI can observe and bind to, so the UI updates when the list changes.
* retains the complete list as the source of displayed indexes. Filtering and editing do not change a person's index;
  deleting a person removes it from the source list and renumbers the persons after it.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)


<box type="info" seamless>

**Note:** The alternative, arguably more object-oriented, design below keeps a unique list of tags in `ClientBook`, and each `Person` references tags from that list. This lets `ClientBook` maintain one `Tag` object per unique tag instead of each `Person` holding its own `Tag` objects.<br>

<puml src="diagrams/BetterModelClassDiagram.puml" width="450" />
</box>


### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-F14a-4/tp/tree/master/src/main/java/seedu/address/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" width="550" />

The `Storage` component,
* can save both client book data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonClientBookStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

### Common classes

Classes used by multiple components are in the `seedu.address.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Find by name, relationship and tags

The `find` command supports three independently optional filters: name keywords, one relationship, and tag keywords.
`FindCommandParser` tokenizes the `r/` and `t/` prefixes, rejects duplicate filter prefixes, and delegates prefixed
value validation to `ParserUtil`. At least one filter must be present. Name keywords precede the prefixed filters;
multiple tag keywords follow one `t/` prefix and are separated by whitespace. The parser splits the name preamble
into independent keywords and validates each one against the search-keyword grammar. Ordinary keywords contain
letters with optional internal hyphens or apostrophes, while `s/o`, `d/o`, and `w/o` are also valid standalone
keywords. Digits, other symbols, and arbitrary slash forms are rejected. This differs from full-name validation,
which requires relationship markers to appear between name components.

The parser creates a `PersonMatchesFiltersPredicate`. Name keywords use the existing full-word, case-insensitive
matching and are joined with `OR`. Tag keywords use exact, case-insensitive tag matching and are also joined with
`OR`. The predicate joins the name, relationship and tag filter results with `AND`. For example,
`find Alex David r/client t/priority referral` returns clients whose name contains `Alex` or `David` and whose tags
contain `priority` or `referral`.

`FindCommand` passes the predicate to `Model#updateFilteredPersonList(Predicate<Person>)`, which updates the
`FilteredList<Person>` observed by the UI. Filtering does not reorder the source list, so matching persons retain the
same order and indexes as in the `list` command. `PersonListPanel` looks up each filtered person in the complete list
when rendering its index. Index-based commands use `Command#getVisiblePersonAtIndex` to resolve that complete-list
index and reject it if the corresponding person is not in the current filter.

The following sequence shows a combined search. The `FilteredList` applies the predicate to each source person;
omitted filter types evaluate as matched, while the supplied filter types must all match.

<puml src="diagrams/FindSequenceDiagram.puml" alt="Find by name, relationship and tags sequence" />

### Tagging a person

`TagCommandParser` reads the first whitespace-separated argument as the displayed-list index and validates each
remaining argument with `ParserUtil#parseTags(Collection<String>)`. Tag names use the existing alphanumeric-only
`Tag` validation. `TagCommand` resolves the visible person against its index in the complete list using
`Command#getVisiblePersonAtIndex`. It then intersects the supplied and existing tag sets. Tag equality is
case-insensitive, so a non-empty intersection raises a `CommandException` listing the duplicate tags and leaves the
contact unchanged. Otherwise, the command unions the supplied and existing tags and replaces the immutable `Person`
through `Model#setPerson`, preserving every non-tag field and the active list filter. The existing JSON adapter
persists a successfully updated set.

`AddCommandParser` uses the same validation and accepts zero or more `t/TAG` fields. Unlike the required name,
relationship, email and phone prefixes, `t/` is deliberately repeatable so a new person can be created with several
tags. The parser collects those values into a set before constructing the `Person`.

`PersonCard` renders the complete tag set in its existing `FlowPane`, sorting tags by name for deterministic display.
The list view observes the model, so tags supplied by `add` or appended by `tag` appear with the person's contact and
relationship details without a manual refresh. The `list` command restores all persons while retaining those tags.

The following sequence shows tags being appended to a person in the currently displayed list. Parsing and index
validation complete before the immutable `Person` is replaced and the updated client book is saved.

<puml src="diagrams/TagSequenceDiagram.puml" alt="Tagging a person sequence" />

### Deleting tags from a person

`TagDeleteCommandParser` reads the displayed-list index and an optional set of tag names. An empty set means all
tags; otherwise, `TagDeleteCommand` requires every supplied tag to exist on the person. Matching uses the same
case-insensitive `Tag` equality as tag addition. Missing tags, a person with no tags, or an invalid displayed index
raises a `CommandException` before confirmation or mutation.

Tag deletion uses a two-phase Logic flow. The initial `Logic#execute(String)` call parses and validates the command,
stores the pending `TagDeleteCommand`, and returns a `CommandResult` containing `Are you sure you want to delete
this?`, the contact name, the exact stored tags that will be removed, and the accepted responses. `MainWindow`
displays the result through the existing result box below the command box.

While a deletion is pending, `LogicManager` interprets the next command-box input as a confirmation response instead
of parsing it as a new command. `y` and `yes` execute the stored command; `n` and `no` discard it. Matching is
case-insensitive and ignores surrounding whitespace. Any other response raises a `CommandException` and leaves the
command pending. A confirmed command copies the person's tag set, clears it or removes the selected tags, replaces
the immutable `Person` through `Model#setPerson`, and saves the updated client book.

The following sequence shows the confirmation gate before selected tags are deleted.

<puml src="diagrams/TagDeleteSequenceDiagram.puml" alt="Deleting tags with confirmation sequence" />

### Contact preferences (US12)

`ContactCommandParser` accepts `contact INDEX [ch/CHANNEL|-] [tm/TIMING|-]` in either prefix order. It reuses
duplicate-prefix checks and `ParserUtil.parseIndex`, but retains raw preference values using the same ordinary-space
prefix boundaries as `ArgumentTokenizer`. `ClientBookParser` passes the original contact arguments so its initial
trimming cannot change marker recognition. The shared tokenizer is unchanged.

Only an exact `-` surrounded by ordinary spaces clears a preference. Non-marker values retain the existing trimming
and validation through immutable `PreferredContactChannel` and `PreferredContactTiming`. Thus a tab-padded hyphen is
normal text, not a clear operation. Free text, Unicode, punctuation and the 1-200-character limit remain supported.

`ContactDescriptor` holds a specified flag and nullable value for each preference: omitted means preserve,
specified non-null means replace, and specified null means clear. `ContactCommand` defensively copies the descriptor
and rejects an empty descriptor. It calls `Command#getVisiblePersonAtIndex` to resolve the original full-list index
and verify that the person is visible in the current filtered results. Search results retain their original indices,
matching the numbers shown by the UI. It then constructs a replacement `Person` with the resolved preferences and
calls `Model#setPerson`. `Person#withContactPreferences` retains its original null-means-keep
contract for other callers. All unrelated fields and the active filter are preserved. `edit`, `info`, `note`, `withNextAction` and
`PersonBuilder` also copy both preferences. `PersonCard` hides absent values and wraps recorded values.
`JsonAdaptedPerson` persists optional `preferredContactChannel` and `preferredContactTiming` strings; missing or
null properties mean absence, while invalid non-null values produce `IllegalValueException`.
Clearing uses this existing absence representation without changing the JSON format. Clearing absent fields and
repeating clears are safe. Tests cover set/clear combinations, raw markers, invalid-input atomicity, filtered indices,
unrelated-field preservation, descriptor equality/copying, JSON save/reload after clearing, and live card label hiding.

The following sequence shows a successful update; parser validation and index checks precede model mutation.

<puml src="diagrams/ContactSequenceDiagram.puml" alt="Contact preference update sequence" />

### Financial information (US13)

`FinancialNeed`, `Priority` and `AreaOfInterest` are immutable value objects, independently optional on `Person`.
Normal values contain 1-200 characters after trimming ordinary spaces; internal spaces are preserved. Validation
accepts Unicode letters and decimal digits, spaces and the punctuation `. , ' ( ) & -`; blanks, tabs, line breaks
and other characters are rejected. `Priority` stores validated text rather than a fixed enumeration.

`ClientBookParser` routes `info INDEX [/fn VALUE|-] [/pr VALUE|-] [/ai VALUE|-]` to `InfoCommandParser`.
The parser requires a positive index and at least one financial prefix, accepts any prefix order, and rejects
duplicate or unknown parameters. It recognizes an exact `-` surrounded only by ordinary spaces before calling
`ParserUtil` to validate normal values. Bare prefixes remain invalid. `InfoDescriptor` stores a specified flag
and nullable value per field: omitted means unchanged, specified non-null means replace, and specified null
means explicitly clear. `InfoCommand` defensively copies this descriptor, including its flags.

`InfoCommand#execute` resolves the visible person against its index in the complete list before mutation.
It constructs a replacement `Person` using the resolved financial fields and calls `Model#setPerson`.
Name, phone, email, relationship, tags, interaction notes, next action, preferred contact channel and preferred
contact timing are copied unchanged; the active filter is preserved. Clearing an absent field is idempotent.

`LogicManager` saves through the existing storage pipeline after execution. `JsonAdaptedPerson` persists optional
`financialNeed`, `priority` and `areaOfInterest` strings; missing or null properties load as absent, supporting
older files. Non-null values are validated on loading. `PersonCard` shows present values in wrapped labels prefixed
with `Financial need:`, `Priority:` and `Area of interest:`; absent labels are invisible and unmanaged.
Automated tests cover parser validation and boundaries, set/clear combinations, descriptor copying, filtered indices,
unrelated-field preservation, JSON compatibility, save/reload after clearing, and UI hiding/wrapping.

The following sequence shows `info 1 /pr High /ai -`; validation and index resolution precede model mutation.

<puml src="diagrams/InfoSequenceDiagram.puml" alt="Financial information update sequence" />

### Dated interaction notes (US14)

`note INDEX d/yyyy-MM-dd [t/TYPE] [o/OUTCOME] n/NOTE` appends an interaction note to the person at the displayed
client index. It works for clients and prospects, preserves the current filter, and does not replace earlier entries.

#### Model and command flow

1. `ClientBookParser` routes the command to `NoteCommandParser`. The parser reads the index, date and optional
   interaction metadata before the first `n/`, then treats the entire remaining body as text. URLs, slashes and further prefix-like strings remain note content.
2. `InteractionNote` stores an immutable `LocalDate`, nonblank text, `InteractionType` and `InteractionOutcome`. Date parsing checks the `yyyy-MM-dd` shape
   and uses strict ISO calendar validation. Past dates are allowed. Surrounding text whitespace is stripped;
   internal content and case are preserved without a character whitelist or maximum length.
3. `NoteCommand` checks the displayed index and rejects an exact duplicate in that person's history using
   `InteractionNote.hasSameContent()` (date and stripped text) before making changes. Otherwise, it copies the history, appends the new entry and calls
   `Model#setPerson` with a replacement `Person`. Other fields remain unchanged. The same date with different text,
   or the same text with different dates, is allowed. `Person` stably sorts its defensive copy by date, oldest first,
   preserving order for equal dates.
4. The observable list updates the corresponding `PersonListViewCell`. `LogicManager` saves the client book through
   the existing storage pipeline and returns `Added interaction note for NAME.` on success.

`Person#getInteractionNotes()` exposes an immutable defensive copy. History participates in `Person` equality and
hashing, but not duplicate identity detection. Both `EditCommand` and `InfoCommand` carry the existing history into
their replacement persons. `PersonBuilder` also preserves it when copying a person.

#### Persistence and display

`JsonAdaptedPerson` stores an `interactionNotes` array of `JsonAdaptedInteractionNote` objects, each containing
`date`, `text`, `type` and `outcome`. Missing or null arrays in older JSON files become empty histories.
Missing or null metadata fields default independently to `UNSPECIFIED`; unknown or blank stored enum values
raise `IllegalValueException`. Loading validates dates and note text; serialization preserves entry order and duplicates.

`PersonCard` displays dated, wrapped labels in the existing card's interaction-note container. Empty histories are
hidden and unmanaged. Labels have a zero minimum/preferred width and an unbounded maximum width so the parent
provides their available width during measurement. Their preferred width must not be bound to the card's actual
width: a newly replaced card has not yet been laid out, and that binding causes an excessive first wrapped-height
measurement. No fixed card height or forced list refresh is needed.

#### Validation and scope

The parser distinguishes missing index, date and note prefix; malformed calendar dates and blank text have specific
errors. Execution rejects indices outside the currently displayed list before changing the model.

This feature provides adding, displaying, editing and deleting dated notes, with chronological sorting and
structured type/outcome metadata. Searching interaction notes (US16) remains outside this feature.

#### Interaction type and outcome (US15)

`InteractionType` contains `MEETING`, `CALL`, `MESSAGE`, `OTHER`, and `UNSPECIFIED`.
`InteractionOutcome` contains `SUCCESSFUL`, `UNSUCCESSFUL`, `PENDING`, and `UNSPECIFIED`.
The explicit unspecified states represent absent information without inventing historical interaction details.
Both the legacy two-argument `InteractionNote` constructor and old JSON use these defaults.
The four-argument constructor rejects null metadata. Full value equality and hashing include both fields,
so changes to metadata participate in person equality and persistence; duplicate-note detection deliberately
continues to compare only date and text, preserving the existing rule regardless of metadata.

Both note constructors enforce the same supported year range (`0001`–`9999`) as `parseDate`, including when
called directly rather than through a command parser. This keeps model-created notes representable by the
JSON loader; future dates within that range remain valid model values for legacy data, with command execution
separately enforcing the existing no-future-date policy.

`NoteCommandParser` retains the date-first syntax and splits at the first whitespace-delimited `n/`.
It tokenizes only the header with `PREFIX_DUE_DATE`, `PREFIX_INTERACTION_TYPE` (`t/`) and
`PREFIX_INTERACTION_OUTCOME` (`o/`), using the existing `ArgumentTokenizer` and duplicate-prefix validation.
Header whitespace is normalized for tokenization; the body is preserved independently.
The optional metadata fields can appear in either order after the date. Enum parsers strip surrounding
whitespace and use `Locale.ROOT` for case-insensitive input, returning field-specific validation errors.
The `t/` spelling is shared with tags but interpreted only by each command's own parser; `find` and tag commands
retain their existing meaning. Prefix-like strings after `n/` remain content, keeping URLs and old note bodies compatible.

`NoteCommand` still appends through immutable person replacement and the existing `LogicManager` transaction.
`JsonAdaptedInteractionNote` writes uppercase enum names, accepts case-insensitive stored values, and preserves
its two-argument constructor for existing callers. No file migration or dependency is required. Missing/null
fields are backward compatible; invalid supplied values are rejected rather than silently discarded.
`PersonCard` already renders each note through `InteractionNote.toString()`, which now produces
`DATE [Type: TYPE; Outcome: OUTCOME]: TEXT`; wrapping, numbering and chronological ordering are unchanged.
`NoteEditCommand` copies the selected note's metadata when replacing date or text. Deletion removes only the
selected note; `edit`, `info`, contact preferences, tags and follow-up mutations carry the immutable history through.
`noteedit` does not expose metadata editing; its existing date/text syntax remains unchanged.

Enums were chosen over free text to enforce a small, consistent vocabulary. `UNSPECIFIED` was chosen over
`OTHER`/`PENDING` defaults because those values assert details the user may never have recorded. Full value equality
is separate from the date/text duplicate rule to avoid both missed persistence changes and duplicate-rule regressions.
Keeping metadata before `n/`, rather than scanning the entire command, protects existing free-form note content.

Tests cover all enum values, mixed-case parsing under a Turkish default locale, independent optional fields,
malformed/missing/duplicate prefixes, literal prefixes in the body, explicit defaults, invalid/null values,
JSON round-trips and legacy fields. Logic/storage integration tests exercise original filtered indexes,
chronological ties, unchanged duplicate rejection, note editing/deletion, unrelated contact mutations,
backup/restore and reopening saved records. JavaFX tests assert the exact canonical and unspecified display text.

#### Editing note date and text

`noteedit CONTACT_INDEX NOTE_INDEX [d/DATE] [n/NEW_NOTE_CONTENT]` is routed to `NoteEditCommandParser`.
The parser requires two positive indices and at least one editable field. When supplied, the date comes before
`n/` and is calendar-validated; explicitly supplied future dates are rejected during execution. As with `note`,
everything after `n/` is treated as note content, including further prefix-like strings.

`NoteEditCommand` resolves the contact against the filtered list and the note against its chronologically ordered
history, using one-based indices. It rejects out-of-range indices (including empty histories) before mutation.
It copies the history and replaces just the selected entry with an `InteractionNote` containing the supplied
date/text (preserving omitted values), original type and original outcome. Date changes trigger chronological reordering.
It then calls `Model#setPerson` with a replacement person preserving all other fields, including contact preferences
and the next action. The current filter remains active. `LogicManager` persists the change through the existing
JSON pipeline; no additional schema changes or note IDs are needed for editing.

`NoteEditCommandTest` and `NoteEditCommandParserTest` cover validation, filtered lists, duplicate notes,
date and contact-field preservation, command routing, and saving/reloading an edit through `LogicManager`.

#### Deleting a note

The following sequence diagram shows the successful execution of `notedelete 1 2`, which deletes the
second interaction note from the first contact in the currently filtered list.

<puml src="diagrams/NoteDeleteSequenceDiagram.puml" alt="Interaction note deletion sequence" />

`notedelete CONTACT_INDEX NOTE_INDEX` is routed to `NoteDeleteCommandParser`, which requires exactly two
positive integer indices using `ParserUtil.parseIndex`.

`NoteDeleteCommand` resolves the contact against the currently filtered list and the note against that
contact's chronologically ordered history. Both indices are one-based. Out-of-range indices, including an empty
history, are rejected before changing the model. The command copies the history and removes the selected
entry by position, preserving duplicates and the order of remaining entries. Subsequent note positions shift
down by one. It calls `Model#setPerson` with a replacement person preserving all other fields, including
contact preferences and the next action, and retains the current filter.

`PersonCard` prefixes each note label with its one-based chronological position, matching command indices.
Replacing the person rebuilds the card through the existing observable list, so remaining notes are renumbered
and an empty history is hidden without forcing a refresh or changing stored notes.

Deletion feedback uses the `InteractionNote` returned by positional removal, including its actual date and text.
Only the feedback excerpt has whitespace collapsed. It is limited to 80 Unicode code points using
`offsetByCodePoints`, followed by `…` when truncated, without splitting surrogate pairs or changing stored content.
Command and UI tests cover excerpt boundaries, supplementary Unicode characters, duplicate notes, chronological
order, automatic renumbering and hiding the last deleted note's history.

`LogicManager` saves the replacement through the existing JSON pipeline. No storage schema or model API
changes are required. `NoteDeleteCommandTest` and `NoteDeleteCommandParserTest` cover validation, filtered
lists, duplicate notes, contact-field preservation, command routing, and saving/reloading deletions,
including an empty history.

Automated and manual regression checks are described in the [Testing guide](Testing.md#us14-interaction-note-regression-checks).

### Follow-ups (US18, US19 and US20)

Each `Person` has zero or one immutable `NextAction`: an action description, a `LocalDate` due date,
and a boolean completion status. Absence is exposed through `Person#getNextAction()` as an empty `Optional`.
The value participates in full person equality and hashing, but does not affect duplicate identity detection.
`Person#withNextAction` creates a replacement person while preserving all unrelated information.
`EditCommand`, `InfoCommand` and `NoteCommand` must carry this field into their replacement persons.

#### Recording or replacing an action (US18)

`ClientBookParser` routes `next` to `NextCommandParser`, which uses `ArgumentTokenizer` for `a/` and `d/`.
The parser validates the index through `ParserUtil#parseIndex`, rejects duplicate prefixes, and distinguishes
missing action/date fields. `NextAction` strips surrounding description whitespace and checks a length of
1–200 Java string characters. Its date parser enforces the four-digit-year/two-digit-month/two-digit-day shape
and strict calendar validation. Both parsing and model construction restrict the year to 1–9999;
year zero is rejected, including when loading JSON. Backdated follow-ups within this range are valid.

`NextCommand` resolves the visible person against its index in the complete list, rejects invalid indices,
then passes the replacement person to `Model#setPerson`. Both new and replacement actions have
`completed = false`. The observable list updates the card. `LogicManager` then saves through the existing
storage pipeline; the command itself does not write files.

<puml src="diagrams/NextSequenceDiagram.puml" alt="Next command sequence diagram" />

Diagram source: [NextSequenceDiagram.puml](diagrams/NextSequenceDiagram.puml).
The diagram shows the successful path. Parser failures and invalid displayed indices stop before mutation;
save failures follow the existing `LogicManager` error handling.

#### Completing an action (US20)

`ClientBookParser` routes `done INDEX` to `DoneCommandParser`. Empty arguments report
`Person index is required.`; otherwise `ParserUtil#parseIndex` validates the entire argument string,
rejecting zero, negative, non-integer and multiple indices.

`DoneCommand` first resolves the visible person against its index in the complete list, then checks that the selected
person has a next action. These checks occur before any model mutation and report, respectively,
`Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX` and `NAME has no next action to complete.`.
If the action is already completed, the command returns a normal `CommandResult` with
`Next action for NAME is already completed.` without calling `Model#setPerson` or changing the filter.

`NextAction#markCompleted()` returns a new object with the original description and date and
`completed = true`. `DoneCommand` passes it to `Person#withNextAction` and calls `Model#setPerson`,
preserving every unrelated person field and the current filter. The card displays `Completed` through the
existing observable-list update. `LogicManager` saves through the existing JSON storage pipeline and returns
`Next action for NAME has been marked as completed.`.

Tests cover the immutable completed copy, preserved description/date and person fields, filtered display
indices, out-of-range indices, absent actions, repeated completion, unchanged data on errors, command equality,
string representation, parser validation and routing through `ClientBookParser`.

#### Rescheduling existing follow-ups (US21)

`ClientBookParser` routes `reschedule INDEX d/yyyy-MM-dd [a/ACTION]` to
`RescheduleCommandParser`. It reuses the existing `a/` and `d/` prefixes, index parsing,
`NextAction.parseDate` and description validation. The parser validates calendar dates and format;
`RescheduleCommand` resolves the client with `Command.getVisiblePersonAtIndex`, using its original
full-list index and rejecting clients hidden by the current filter, as `next` and `done` do.
It additionally rejects dates before today at execution with
`Follow-up date cannot be before today.`. Its default clock uses the system's local zone, with an
injectable `Clock` for deterministic tests. Duplicate prefixes, missing dates, invalid inputs and
past replacement dates fail before model mutation. `NextAction.parseDate` remains a calendar
validator, so existing stored overdue follow-ups remain loadable.

A separate minimal command keeps `next` creation/replacement semantics unchanged while permitting
an omitted description and requiring an existing follow-up. `RescheduleCommand` resolves the displayed
index, rejects a person without a follow-up, then calls `NextAction.reschedule` and
`Person.withNextAction` before `Model#setPerson`. The immutable copy always resets `completed`
to `false`, so rescheduling completed work reopens it. A follow-up rescheduled for today is included
when `due` is run again; a future follow-up remains pending and is excluded until its due date.
All other person fields and the active filter are preserved. A due action moved into the future
is therefore automatically removed from that filtered view.

`LogicManager` persists the updated person through the existing `JsonAdaptedNextAction`; no JSON
schema change or migration is required. Parser/command tests cover invalid inputs, displayed indices,
missing follow-ups, date-only and description changes, reopening completed actions, rejection of
past dates without mutation, local-zone date boundaries and due-filter updates. Model and JSON
round-trip tests verify immutable rescheduling, pending status and preservation of optional fields.

#### Viewing due actions (US19)

`ClientBookParser` routes argument-free `due` to `DueCommand`; any arguments are rejected with
`The due command takes no arguments.`. On each execution, `DueCommand` obtains today's date using
`LocalDate.now(clock)` and calls `Model#updateFilteredPersonList` with a new `NextActionDuePredicate`.
The predicate includes only people with an incomplete next action whose date is today or earlier.
This replaces any previous search filter without changing stored people. Completing an action through
`done` replaces the person, so the observable filtered list automatically excludes that person.

The default constructor uses `Clock.systemDefaultZone()`. Tests inject a `Clock.fixed` with an explicit
zone to make the date boundary deterministic. The predicate captures the execution date; rerunning `due`
refreshes it after midnight. Empty results report `No follow-ups are due.`; otherwise the command reports
the number of matching follow-ups.

The due view is sorted by due date ascending, then name ignoring case, with exact spelling as a final
tie-breaker (#138). `ModelManager` exposes a `SortedList` over its existing `FilteredList`, so sorting
does not mutate or persist the backing contact order. The model stores both its current predicate and
comparator. `LogicManager` carries both across its temporary-model execution and successful commit;
indexed commands continue to use the displayed client indices from the complete client book,
independent of sorted row positions. Replacing a person
updates the active order automatically. The original single-argument filter update clears sorting,
so `list` and `find` retain their original contact-order behaviour. Tests cover date/name ties, stored
order, live updates, and indexed commands through the complete logic/storage path.

#### Persistence and card display

`JsonAdaptedPerson` stores an optional `nextAction` object through `JsonAdaptedNextAction`:

```json
"nextAction": {
  "action": "Send retirement proposal",
  "dueDate": "2026-10-20",
  "completed": false
}
```

Missing or null `nextAction` fields load as absence, preserving compatibility with old files. An object
with invalid or missing action/date/completion fields is rejected through `IllegalValueException`.
Both pending and completed states survive a JSON round trip.

`PersonCard` hides and unmanages the follow-up label when absent. Otherwise it displays text and color:
completed takes precedence; incomplete dates earlier than today are overdue; remaining actions are pending.
The label wraps long descriptions using the available card width. Status is evaluated when a card is constructed;
there is no background midnight timer. Restarting the app refreshes all card labels after a date change.

#### Validation and regression tests

US18 tests cover missing/duplicate prefixes, zero/negative/non-integer/overflow indices, displayed-list bounds,
empty and whitespace-only actions, 200/201-character descriptions, trimming, invalid dates (`2026-02-30`),
leap days, past dates, replacement reset, filtered indices, preservation of unrelated fields, old JSON,
invalid stored values, completion round trips and card display/wrapping.

### \[Proposed\] Undo/redo feature

#### Proposed Implementation

The proposed undo/redo mechanism is facilitated by `VersionedClientBook`. It extends `ClientBook` with an undo/redo history, stored internally as an `clientBookStateList` and `currentStatePointer`. Additionally, it implements the following operations:

* `VersionedClientBook#commit()` -- Saves the current client book state in its history.
* `VersionedClientBook#undo()` -- Restores the previous client book state from its history.
* `VersionedClientBook#redo()` -- Restores a previously undone client book state from its history.

These operations are exposed in the `Model` interface as `Model#commitClientBook()`, `Model#undoClientBook()` and `Model#redoClientBook()` respectively.

Given below is an example usage scenario and how the undo/redo mechanism behaves at each step.

Step 1. The user launches the application for the first time. The `VersionedClientBook` will be initialized with the initial client book state, and the `currentStatePointer` pointing to that single client book state.

<puml src="diagrams/UndoRedoState0.puml" alt="UndoRedoState0" />

Step 2. The user executes `delete 5` command to delete the 5th person in the client book. The `delete` command calls `Model#commitClientBook()`, causing the modified state of the client book after the `delete 5` command executes to be saved in the `clientBookStateList`, and the `currentStatePointer` is shifted to the newly inserted client book state.

<puml src="diagrams/UndoRedoState1.puml" alt="UndoRedoState1" />

Step 3. The user executes `add n/David …​` to add a new person. The `add` command also calls `Model#commitClientBook()`, causing another modified client book state to be saved into the `clientBookStateList`.

<puml src="diagrams/UndoRedoState2.puml" alt="UndoRedoState2" />

<box type="info" seamless>

**Note:** If a command fails its execution, it will not call `Model#commitClientBook()`, so the client book state will not be saved into the `clientBookStateList`.
</box>

Step 4. The user now decides that adding the person was a mistake, and decides to undo that action by executing the `undo` command. The `undo` command will call `Model#undoClientBook()`, which will shift the `currentStatePointer` once to the left, pointing it to the previous client book state, and restores the client book to that state.

<puml src="diagrams/UndoRedoState3.puml" alt="UndoRedoState3" />


<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index 0, pointing to the initial ClientBook state, then there are no previous ClientBook states to restore. The `undo` command uses `Model#canUndoClientBook()` to check if this is the case. If so, it will return an error to the user rather
than attempting to perform the undo.
</box>

The following sequence diagram shows how an undo operation goes through the `Logic` component:

<puml src="diagrams/UndoSequenceDiagram-Logic.puml" alt="UndoSequenceDiagram-Logic" />

<box type="info" seamless>

**Note:** The lifeline for `UndoCommand` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</box>

Similarly, how an undo operation goes through the `Model` component is shown below:

<puml src="diagrams/UndoSequenceDiagram-Model.puml" alt="UndoSequenceDiagram-Model" />

The `redo` command does the opposite — it calls `Model#redoClientBook()`, which shifts the `currentStatePointer` once to the right, pointing to the previously undone state, and restores the client book to that state.

<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index `clientBookStateList.size() - 1`, pointing to the latest client book state, then there are no undone ClientBook states to restore. The `redo` command uses `Model#canRedoClientBook()` to check if this is the case. If so, it will return an error to the user rather than attempting to perform the redo.
</box>

Step 5. The user then decides to execute the command `list`. Commands that do not modify the client book, such as `list`, will usually not call `Model#commitClientBook()`, `Model#undoClientBook()` or `Model#redoClientBook()`. Thus, the `clientBookStateList` remains unchanged.

<puml src="diagrams/UndoRedoState4.puml" alt="UndoRedoState4" />

Step 6. The user executes `clear`, which calls `Model#commitClientBook()`. Since the `currentStatePointer` is not pointing at the end of the `clientBookStateList`, all client book states after the `currentStatePointer` will be purged. Reason: It no longer makes sense to redo the `add n/David …` command. This is the behavior that most modern desktop applications follow.

<puml src="diagrams/UndoRedoState5.puml" alt="UndoRedoState5" />

The following activity diagram summarizes what happens when a user executes a new command:

<puml src="diagrams/CommitActivityDiagram.puml" width="250" />

#### Design considerations:

**Aspect: How undo & redo execute:**

* **Alternative 1 (current choice):** Saves the entire client book.
  * Pros: Easy to implement.
  * Cons: May have performance issues in terms of memory usage.

* **Alternative 2:** Individual command knows how to undo/redo by
  itself.
  * Pros: Will use less memory (e.g. for `delete`, just save the person being deleted).
  * Cons: We must ensure that the implementation of each individual command is correct.

_{more aspects and alternatives to be added}_

### \[Proposed\] Data archiving

_{Explain here how the data archiving feature will be implemented}_


--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.md)
* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [DevOps guide](DevOps.md)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target user profile**:

* maintains one user's local records of clients and prospects
* stores contact details, relationship status, tags, preferences, interests, important dates, and other concise relationship context
* records high-level business needs such as financial goals, priorities, budget ranges, and areas of interest
* records dated interactions, outcomes, next actions, and reminders
* finds, filters, sorts, reviews, archives, and backs up relationship records
* supports frequent users who prefer fast typed input while using the interface for clear feedback

**Value proposition**: Keeps track of their clients' details, allowing them to quickly recall important personal context about their clients, so that they can maintain more personalised and informed client relationships.



### User stories

Priorities: High (must have) - `* * *`, Medium (nice to have) - `* *`, Low (unlikely to have) - `*`

#### First Use and Core Contact Records

| Priority | As a … | I want to … | So that I can … |
| --- | --- | --- | --- |
| * * * | As a financial consultant | add a client or prospect with their basic contact details | keep a record of every professional relationship I manage. |
| * * * | As a financial consultant | view all active clients and prospects | see the relationships currently under my care. |
| * * | As a financial consultant preparing to contact someone | view one person's complete profile | recall the relevant relationship context before the conversation. |
| * * | As a financial consultant | update a person's contact details | keep the record accurate when their information changes. |
| * * * | As a financial consultant | delete a record that I created by mistake | prevent incorrect entries from cluttering my records. |

#### Relationship Context

| Priority | As a … | I want to … | So that I can … |
| --- | --- | --- | --- |
| * * | As a financial consultant handling different kinds of relationships | assign one or more meaningful tags to a person | organise contacts according to how I work. |
| * * * | As a financial consultant managing a relationship pipeline | record whether a person is a prospect, active client, or inactive client | tell what kind of attention each relationship needs. |
| * * | As a financial consultant who relies on personal rapport | record a person's preferences and interests | make future conversations more relevant and personal. |
| * * | As a financial consultant | record important dates shared by a client | remember occasions that matter to the relationship. |
| * * | As a financial consultant | record a person's preferred contact channel and timing | approach them in a way that is convenient for them. |
| * * * | As a financial consultant | record a client's high-level financial needs, priorities, and areas of interest | continue the business discussion from the correct context. |

#### Interaction History

| Priority | As a … | I want to … | So that I can … |
| --- | --- | --- | --- |
| * | As a financial consultant after speaking with a client | add a dated interaction note | preserve important details while they are still fresh. |
| * | As a financial consultant | record the type and outcome of an interaction | distinguish a meeting, call, message, or unsuccessful contact attempt. |
| * | As a financial consultant reviewing a relationship | view interaction notes in chronological order | understand how the relationship has developed over time. |
| * | As a financial consultant who entered an inaccurate interaction note | correct or remove that note | keep the relationship history trustworthy. |

#### Follow Ups and Reminders

| Priority | As a … | I want to … | So that I can … |
| --- | --- | --- | --- |
| * * | As a financial consultant after an interaction | record the next action and its due date | turn the conversation into a concrete follow-up. |
| * * | As a financial consultant starting the workday | view follow-ups that are due or overdue | act on urgent relationships before they are forgotten. |
| * * | As a financial consultant who has completed a follow-up | mark the action as completed | distinguish finished work from work that still needs attention. |
| * * | As a financial consultant whose plan has changed | reschedule a follow-up | keep my next-action list realistic without losing the task. |
| * * | As a financial consultant managing recurring relationship events | set reminders for dates such as birthdays or renewal periods | maintain relationships and prepare for time-sensitive conversations. |

#### Retrieval and Prioritisation

| Priority | As a … | I want to … | So that I can … |
| --- | --- | --- | --- |
| * * * | As a financial consultant looking for a specific person | find clients or prospects using one or more name keywords | quickly locate the relevant client record. |
| * * | As a financial consultant who remembers context rather than a name | search across preferences and notes using keywords | find the relevant relationship from the detail I remember. |
| * * | As a financial consultant focusing on a segment of contacts | filter people by client or prospect relationship status | work with only the relevant group. |
| * * | As a financial consultant planning the order of work | sort contacts by next follow-up or last interaction date | prioritise relationships using timely information. |
| * | As a financial consultant maintaining long-term relationships | identify people I have not contacted recently | notice relationships that may otherwise be neglected. |
| * * | As a busy financial consultant about to meet or call someone | view a concise summary of their preferences, recent interactions, and next action | refresh my memory without reading the entire record. |

#### Long Term Use and Data Continuity

| Priority | As a … | I want to … | So that I can … |
| --- | --- | --- | --- |
| * | As a long-time user | move inactive records out of my active view and restore them later | reduce clutter without permanently losing relationship history. |
| * * | As a financial consultant moving or safeguarding my local data | create a backup copy of my records | protect the relationship information I have accumulated. |
| * * | As a financial consultant recovering from data loss or changing computers | restore my records from a backup | resume work without rebuilding my relationship history. |
| * * * | As a long-term user | have my data saved between sessions | continue managing my information without having to re-enter it each time. |


### Use cases

(For all use cases below, the **System** is `ClientBook` and the **Actor** is the `user`, unless specified otherwise)

**Use case: Delete a person**

**MSS**

1.  User requests to list persons
2.  ClientBook shows a list of persons
3.  User requests to delete a specific person in the list
4.  ClientBook deletes the person

    Use case ends.

**Extensions**

* 2a. The list is empty.

  Use case ends.

* 3a. The given index is invalid.

    * 3a1. ClientBook shows an error message.

      Use case resumes at step 2.

**Use case: Update a client relationship**

**MSS**

1.  User requests to find a client or prospect by name.
2.  ClientBook displays matching records.
3.  User updates the person’s relationship stage, preferences, or contact details.
4.  ClientBook saves the updated relationship context and displays a confirmation message.

    Use case ends.

**Extensions**

* 2a. No matching record is found.

    * 2a1. ClientBook shows a message that no matching records exist.

      Use case ends.

* 3a. The updated details are invalid.

    * 3a1. ClientBook shows an error message.

      Use case ends.

**Use case: Record a follow-up after a client interaction**

**MSS**

1.  User requests to find a client or prospect.
2.  ClientBook displays the matching record.
3.  User records the outcome of the interaction.
4.  User records the next action and its due date.
5.  ClientBook saves the interaction details and follow-up.

    Use case ends.

**Extensions**

* 2a. No matching client or prospect is found.

    * 2a1. ClientBook shows a message that no matching record exists.

      Use case ends.

* 4a. The supplied due date is invalid.

    * 4a1. ClientBook shows an error message.

      Use case resumes at step 4.

**Use case: Add an important date**

**MSS**

1.  User requests to add an important date to an existing client or prospect.
2.  ClientBook identifies the specified client or prospect.
3.  User provides the important date and its description.
4.  ClientBook validates the supplied information.
5.  ClientBook adds the important date to the person's record and displays a confirmation message.

    Use case ends.

**Extensions**

* 2a. The specified client or prospect does not exist.

    * 2a1. ClientBook shows an error message.

      Use case ends.

* 3a. The required information is not supplied.

    * 3a1. ClientBook shows an error message.

      Use case ends.

* 4a. The supplied date is invalid.

    * 4a1. ClientBook shows an error message.

      Use case resumes at step 3.

**Use case: Convert a prospect to a client**

**MSS**

1.  User issues a search command with name keywords to locate the target prospect.
2.  ClientBook identifies the specified prospect.
3.  User issues a conversion command specifying the target prospect's index number.
4.  ClientBook verifies the contact is currently classified as a prospect and prompts the user to input mandatory client-specific onboarding details (e.g., billing address and tax ID).
5.  User inputs the required onboarding details.
6.  ClientBook validates the new information, changes the contact's classification from "prospect" to "client", saves the data, and displays a confirmation message.

    Use case ends.

**Extensions**

* 1a. The specified client or prospect does not exist.

    * 1a1. ClientBook shows an error message.

      Use case ends.

* 3a. The user provides an invalid index number (e.g., out of bounds or non-numeric).

    * 3a1. ClientBook displays an error message stating the index is invalid.

      Use case resumes at step 3.

* 4a. The selected contact is already classified as a client.

    * 4a1. ClientBook displays an error message stating the contact cannot be converted because they are already a client.

      Use case ends.

* 5a. The user provides invalid formatting for the onboarding details (e.g., an alphanumeric string for a strictly numeric tax ID).

    * 5a1. ClientBook displays an error message detailing the specific parameter constraint and re-prompts the user for the details.

      Use case resumes at step 5.

* 5b. The user inputs an abort command.

    * 5b1. ClientBook cancels the conversion process and leaves the contact as a prospect.

      Use case ends.

**Use case: Find clients or prospects by name or relationship**

**MSS**

1.  User requests to find clients or prospects using one or more name keywords, or a client/prospect relationship.
2.  ClientBook searches the stored records using the supplied criterion.
3.  ClientBook displays all matching clients and prospects.

    Use case ends.

**Extensions**

* 1a. The user does not provide a search criterion.

    * 1a1. ClientBook displays an error message explaining the valid command formats.

      Use case resumes at step 1.

* 1b. The user provides an unsupported relationship value.

    * 1b1. ClientBook displays an error message explaining the valid relationship values.

      Use case resumes at step 1.

* 1c. The user combines name keywords with a relationship filter.

    * 1c1. ClientBook displays an error message explaining the valid command formats.

      Use case resumes at step 1.

* 3a. No record matches the supplied criterion.

    * 3a1. ClientBook informs the user that no matching record was found.

      Use case ends.

### Non-Functional Requirements

1. ClientBook should work on any _mainstream OS_ as long as Java `25` or above is installed.
2. ClientBook should be able to store at least 1000 persons.
3. For a data set of up to 1000 persons, ClientBook should process a command and update the graphical user interface within 1 second after the user submits the command.
4. ClientBook should start within 10 seconds on a computer that meets the stated software requirements.
5. A user with above-average typing speed for regular English text should be able to complete most core tasks faster using commands than using the mouse.
6. All core client-record management features should remain available without an Internet connection.
7. Every successful command that modifies client data should be saved automatically to local storage and remain available after ClientBook is restarted.
8. An invalid command should not modify any existing client data.
9. ClientBook should handle a missing, inaccessible, or corrupted data file without crashing or freezing, and should inform the user when recovery is required.
10. ClientBook should not silently overwrite an unreadable or corrupted data file.
11. If ClientBook cannot save a change to local storage, it should inform the user that the change may not persist.
12. ClientBook should store client data locally and should not transmit it to external services.
13. ClientBook should not require an online account or authentication credentials to access locally stored records.
14. All core client-record management operations should be accessible using keyboard input.
15. Error messages should identify the invalid input and provide sufficient information for the user to correct it.
16. ClientBook should be distributed as a single executable JAR file that does not require a separate installation process.
17. ClientBook should store its data in a human-readable JSON format to support inspection and manual backup.
18. The codebase should comply with the project coding standard and pass the configured automated checks before changes are merged.
19. Unexpected errors should be logged with sufficient diagnostic information for developers while avoiding unnecessary exposure of client information.
20. ClientBook should support a single user only; its data file should not be accessed concurrently or shared during normal operation.
21. ClientBook should read and write files only within its home folder (the folder containing the JAR file) and its subfolders,
    and should not change OS-level configuration settings.
22. The graphical user interface should work well at resolutions of 1920x1080 and above at 100% and 125% screen scaling, and
    should remain usable at resolutions of 1280x720 and above at 150% screen scaling.
23. The distributable JAR file should not exceed 100 MB.

### Glossary

* **Mainstream OS**: Windows, Linux, Unix, or macOS
* **Prospect**: A person who is being tracked as a potential future client but is not currently an active client.
* **Client**: A person with whom the financial consultant has an active professional relationship.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test the app manually.

<box type="info" seamless>

**Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing.
</box>

### Launch and shutdown

1. Initial launch

   1. Download the JAR file and copy it into an empty folder.

   2. Double-click the JAR file.<br>
      Expected: The GUI opens with a set of sample contacts. The window size may not be optimal.

2. Saving window preferences

   1. Resize the window to an optimal size. Move the window to a different location. Close the window.

   2. Relaunch the app by double-clicking the JAR file.<br>
       Expected: The most recent window size and location are retained.

3. _{ more test cases … }_

### Deleting a client/prospect

Use a separate test folder with fictional records. Run these commands to prepare the test data:

```text
clear
add r/Client n/John Doe e/john@example.com p/91234567
add r/Prospect n/Jane Tan e/jane@example.com p/98765432
info 2 /fn Retirement planning /pr High /ai Life insurance
note 2 d/2026-10-06 n/Discussed next steps.
```

1. Deleting a client/prospect while the full list is shown

    1. Prerequisites: Prepare the records above and run `list`.

    2. Test case: `delete 1`<br>
       Expected: `Client John Doe has been deleted successfully.` John is removed. Jane moves to index 1,
       with her financial information and interaction note unchanged.

    3. Test case: `exit`, then reopen the app from the same folder.<br>
       Expected: John remains absent. Jane and her recorded details are retained.

    4. Test case: `delete 1`<br>
       Expected: `Prospect Jane Tan has been deleted successfully.` The list becomes empty.

    5. Test case: `exit`, then reopen the app again.<br>
       Expected: The list stays empty; sample records do not replace the saved empty list.

2. Deleting from a filtered list

    1. Prerequisites: Recreate the test records above, then run `find Jane`.

    2. Test case: `delete 2`<br>
       Expected: Jane is deleted using her unchanged full-list index. The search results become empty.

    3. Test case: `list`<br>
       Expected: John is still present. Repeat this test using `find r/prospect` to check relationship-filtered deletion.

3. Rejecting invalid deletion

    1. Prerequisites: Recreate the test records above and run `list`. None of the following commands should change
       either record, their details or the current search results.

    2. Test case: `delete`<br>
       Expected: `Client index is required.`

    3. Test case: `delete 1 2` or `delete 1 abc`<br>
       Expected: `Delete command only accepts a client index.`

    4. Test case: `delete 0`, `delete -1`, `delete abc`, `delete 1.5`, or `delete 2147483648`<br>
       Expected: `Client index must be a positive integer.`

    5. Test case: `delete 3`<br>
       Expected: `No client exists at index 3.`

    6. Test case: `find Jane`, then `delete 1`<br>
       Expected: `No client exists at index 1.` Jane remains visible at index 2, and John is still present in the full
       list.

    7. Test case: `find Nobody`, then `delete 1`<br>
       Expected: `No client exists at index 1.` The empty search stays active. Use `list` and restart the app
       to confirm that both records and their details are retained.

These cases are covered in `DeleteIntegrationTest`. Parser whitespace and integer boundaries are covered in
`DeleteCommandParserTest`.

### Finding persons by relationship

1. Filtering clients and prospects

   1. Prerequisites: Add multiple clients and prospects, then display all persons using the `list` command.

   2. Test case: `find r/client`<br>
      Expected: Only clients are displayed, in the same relative order as in the full list.

   3. Test case: `find r/PROSPECT`<br>
      Expected: Only prospects are displayed, in the same relative order as in the full list.

   4. Test case: `find r/Client`<br>
      Expected: The same persons are displayed as for `find r/client`.

2. Invalid relationship filters

    <ol type="i">
      <li>
        <p>Test case: <code>find r/customer</code><br>
        Expected: No filtering occurs. The status message explains that the relationship must be <code>Client</code> or <code>Prospect</code>.</p>
      </li>
      <li>
        <p>Other incorrect commands to try: <code>find r/</code>, <code>find r/client r/prospect</code>.<br>
        Expected: No filtering occurs. The status message shows the relevant error details.</p>
      </li>
    </ol>

3. Combined name and relationship filtering

   1. Test case: `find Alex r/client`.<br>
      Expected: Only clients whose names contain the full word `Alex` are displayed. Matching is case-insensitive.

### Saving data

1. Dealing with missing/corrupted data files

   1. _{Explain how to simulate missing or corrupted data files and state the expected behavior.}_

2. _{ more test cases … }_

