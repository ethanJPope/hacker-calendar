package com.hackercalendar;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;


public class App extends Application {
    private static final Path LEGACY_EVENTS_FILE = Path.of("events.csv");
    private static final Path EVENTS_FILE = getEventsFilePath();
    
    private YearMonth currentMonth = YearMonth.now();
    private List<CalendarEvent> events = new ArrayList<>();
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
    
    private Label monthLabel;
    private Label hackClubHoursLabel;
    private GridPane calendarGrid;

    private TrayIcon hackClubTrayIcon;


    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        HBox topBar = createTopBar(stage);
        HBox categoryBar = createCategoryBar();

        calendarGrid = new GridPane();
        calendarGrid.getStyleClass().add("calendar-grid");

        BorderPane mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("main-layout");
        mainLayout.setTop(categoryBar);
        mainLayout.setCenter(calendarGrid);

        root.setTop(topBar);
        root.setCenter(mainLayout);

        loadEvents();
        drawCalendar();

        Timeline taskbarTimer = new Timeline(
                new KeyFrame(javafx.util.Duration.minutes(1), event -> {
                    updateHackClubTrayIcon();
                })
        );

        taskbarTimer.setCycleCount(Timeline.INDEFINITE);
        taskbarTimer.play();

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("Hacker Calendar");
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setOnCloseRequest(event -> exitApplication());
        stage.show();

        setupHackClubTrayIcon();
    }

    private HBox createTopBar(Stage stage) {
        Label appTitle = new Label("Hacker Calendar");
        appTitle.getStyleClass().add("app-title");

        Button todayButton = new Button("Today");
        todayButton.getStyleClass().add("primary-button");
        Button previousButton = new Button("<");
        previousButton.getStyleClass().add("icon-button");
        monthLabel = new Label();
        monthLabel.getStyleClass().add("month-label");
        Button nextButton = new Button(">");
        nextButton.getStyleClass().add("icon-button");

        Button minimizeButton = new Button("−");
        Button maximizeButton = new Button("□");
        Button closeButton = new Button("X");

        minimizeButton.getStyleClass().add("window-button");
        maximizeButton.getStyleClass().add("window-button");
        closeButton.getStyleClass().add("window-close-button");

        minimizeButton.setOnAction(event -> {
            stage.setIconified(true);
        });

        maximizeButton.setOnAction(event -> {
            stage.setMaximized(!stage.isMaximized());
        });

        closeButton.setOnAction(event -> exitApplication());

        todayButton.setOnAction(event -> {
            currentMonth = YearMonth.now();
            drawCalendar();
        });

        previousButton.setOnAction(event -> {
            currentMonth = currentMonth.minusMonths(1);
            drawCalendar();
        });

        nextButton.setOnAction(event -> {
            currentMonth = currentMonth.plusMonths(1);
            drawCalendar();
        });

        hackClubHoursLabel = new Label();
        hackClubHoursLabel.getStyleClass().add("hours-label");

        HBox monthControls = new HBox(12, todayButton, previousButton, monthLabel, nextButton);
        monthControls.setAlignment(Pos.CENTER_RIGHT);

        VBox monthArea = new VBox(4, monthControls, hackClubHoursLabel);
        monthArea.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox windowControls = new HBox(6, minimizeButton, maximizeButton, closeButton);
        windowControls.setAlignment(Pos.CENTER_RIGHT);

        HBox topBar = new HBox(12, appTitle, spacer, monthArea, windowControls);
        topBar.getStyleClass().add("top-bar");
        topBar.setPadding(new Insets(16));
        topBar.setAlignment(Pos.CENTER_LEFT);
        makeStageDraggable(stage, topBar);

        return topBar;
    }

    private HBox createCategoryBar() {
        Button workButton = new Button("Work");
        Button vacationButton = new Button("Vacation");
        Button hackClubButton = new Button("Hack Club");
        Button breaksButton = new Button("Breaks");
        Button tasksButton = new Button("Tasks");

        workButton.getStyleClass().add("category-button");
        vacationButton.getStyleClass().add("category-button");
        hackClubButton.getStyleClass().add("category-button");
        breaksButton.getStyleClass().add("category-button");
        tasksButton.getStyleClass().add("category-button");

        HBox categoryBar = new HBox(10, workButton, vacationButton, hackClubButton, breaksButton, tasksButton);
        categoryBar.getStyleClass().add("category-bar");
        categoryBar.setPadding(new Insets(0, 16, 16, 16));
        categoryBar.setAlignment(Pos.CENTER_LEFT);

        return categoryBar;
    }

    private Label createEventLabel(String text, String color) {
        Label eventLabel = new Label(text);

        eventLabel.setMaxWidth(Double.MAX_VALUE);
        eventLabel.getStyleClass().add("event-label");
        eventLabel.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;"
        );

        return eventLabel;
    }

    private void saveEvents() {
        List<String> lines = new ArrayList<>();

        for (CalendarEvent event : events) {
            String line = event.getTitle() + ","
                    + event.getDate() + ","
                    + event.getStartTime() + ","
                    + event.getEndTime() + ","
                    + event.getCategory() + ","
                    + event.getColor();

            lines.add(line);
        }

        try {
            Files.createDirectories(EVENTS_FILE.getParent());
            Files.write(
                    EVENTS_FILE,
                    lines,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException error) {
            System.out.println("Could not save events: " + error.getMessage());
        }
    }

    private void loadEvents() {
        events.clear();
        migrateLegacyEventsFile();

        if (!Files.exists(EVENTS_FILE)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(EVENTS_FILE);

            for (String line : lines) {
                String[] parts = line.split(",");

                if (parts.length == 6) {
                    CalendarEvent event = new CalendarEvent(
                            parts[0],
                            LocalDate.parse(parts[1]),
                            parseTime(parts[2]),
                            parseTime(parts[3]),
                            parts[4],
                            parts[5]
                    );

                    events.add(event);
                }
            }
        } catch (IOException error) {
            System.out.println("Could not load events: " + error.getMessage());
        }
    }

    private static Path getEventsFilePath() {
        String appData = System.getenv("APPDATA");

        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "Hacker Calendar", "events.csv");
        }

        return Path.of(System.getProperty("user.home"), ".hacker-calendar", "events.csv");
    }

    private void migrateLegacyEventsFile() {
        if (Files.exists(EVENTS_FILE) || !Files.exists(LEGACY_EVENTS_FILE)) {
            return;
        }

        try {
            Files.createDirectories(EVENTS_FILE.getParent());
            Files.copy(LEGACY_EVENTS_FILE, EVENTS_FILE);
        } catch (IOException error) {
            System.out.println("Could not migrate old events file: " + error.getMessage());
        }
    }

    private void loadSampleEvents() {
        events.clear();

        events.add(new CalendarEvent(
                "Hack Club",
                LocalDate.of(2026, 5, 1),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                "Hack Club",
                "#2563eb"
        ));

        events.add(new CalendarEvent(
                "Work",
                LocalDate.of(2026, 5, 7),
                LocalTime.of(9, 0),
                LocalTime.of(15, 0),
                "Work",
                "#16a34a"
        ));

        events.add(new CalendarEvent(
                "Break",
                LocalDate.of(2026, 5, 7),
                LocalTime.of(13, 30),
                LocalTime.of(14, 0),
                "Break",
                "#f97316"
        ));
    }

    private String getColorForCategory(String category) {
        switch (category) {
            case "Work":
                return "#16a34a";
            case "Vacation":
                return "#0ea5e9";
            case "Hack Club":
                return "#2563eb";
            case "Break":
                return "#f97316";
            case "Task":
                return "#7c3aed";
            default:
                return "#64748b";
        }
    }

    private VBox createDayCell(int day) {
        Label dayNumber = new Label(String.valueOf(day));
        dayNumber.getStyleClass().add("day-number");

        VBox dayCell = new VBox(4);
        dayCell.getStyleClass().add("day-cell");
        dayCell.getChildren().add(dayNumber);

        LocalDate date = currentMonth.atDay(day);
        if (date.equals(LocalDate.now())) {
            dayCell.getStyleClass().add("today-cell");
            dayNumber.getStyleClass().add("today-number");
        }

        int visibleEventCount = 0;
        int hiddenEventCount = 0;

        List<CalendarEvent> eventsForDate = getEventsForDate(date);

        for (CalendarEvent event : eventsForDate) {
            if (visibleEventCount < 1) {
                Label eventLabel = createEventLabel(
                        formatEventText(event),
                        event.getColor()
                );

                dayCell.getChildren().add(eventLabel);
                visibleEventCount++;
            } else {
                hiddenEventCount++;
            }
        }

        if (hiddenEventCount > 0) {
            Label moreLabel = new Label("+" + hiddenEventCount + " more");
            moreLabel.getStyleClass().add("more-label");

            dayCell.getChildren().add(moreLabel);
        }

        dayCell.setMinSize(120, 90);
        dayCell.setPadding(new Insets(8));

        LocalDate selectedDate = currentMonth.atDay(day);
        dayCell.setOnMouseClicked(event -> {
            showDayDetailsDialog(selectedDate);
        });

        // Rectangle clip  = new Rectangle(120, 90);
        // clip.setArcWidth(6);
        // clip.setArcHeight(6);

        // dayCell.setClip(clip);
        return dayCell;
    }

    private void showDayDetailsDialog(LocalDate date) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        applyTheme(dialog);
        dialog.setTitle("Day Details");

        ButtonType addEventButtonType = new ButtonType("Add Event", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addEventButtonType, ButtonType.CLOSE);
        dialog.getDialogPane().setHeader(createDialogTitleBar(dialog, "Day Details"));

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label dateLabel = new Label(date.toString());
        dateLabel.getStyleClass().add("dialog-title");

        content.getChildren().add(dateLabel);

        List<CalendarEvent> eventsForDate = getEventsForDate(date);

        if (eventsForDate.isEmpty()) {
            Label emptyLabel = new Label("No events yet.");
            emptyLabel.getStyleClass().add("muted-label");
            content.getChildren().add(emptyLabel);
        } else {
            for (CalendarEvent event : eventsForDate) {
                Label eventLabel = createEventLabel(
                        formatEventText(event),
                        event.getColor()
                );
                
                Button editButton = new Button("Edit");
                Button deleteButton = new Button("Delete");
                editButton.getStyleClass().add("secondary-button");
                deleteButton.getStyleClass().add("danger-button");

                editButton.setOnAction(e -> {
                    dialog.close();

                    Platform.runLater(() -> {
                        showEditEventDialog(event);
                    });
                });
                
                deleteButton.setOnAction(e -> {
                    events.remove(event);
                    saveEvents();
                    drawCalendar();
                    updateHackClubTrayIcon();
                    dialog.close();
                    //showDayDetailsDialog(date);
                });

                HBox eventRow = new HBox(8, eventLabel, editButton, deleteButton);
                eventRow.setAlignment(Pos.CENTER_LEFT);

                content.getChildren().add(eventRow);
            }
        }

        dialog.getDialogPane().setContent(content);

        Node addEventButton = dialog.getDialogPane().lookupButton(addEventButtonType);

        addEventButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            dialog.close();

            Platform.runLater(() -> {
                showAddEventDialog(date);
            });
        });

        dialog.showAndWait();
    }

    private void showEditEventDialog(CalendarEvent originalEvent) {
        Dialog<CalendarEvent> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Edit Event");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        applyTheme(dialog);
        dialog.getDialogPane().setHeader(createDialogTitleBar(dialog, "Edit Event"));

        TextField titleField = new TextField(originalEvent.getTitle());

        TextField startTimeField = new TextField(originalEvent.getStartTime().toString());        
        startTimeField.setPromptText("Start Time, like 14:00");

        TextField endTimeField = new TextField(originalEvent.getEndTime().toString());
        endTimeField.setPromptText("End Time, like 16:00");

        ChoiceBox<String> categoryChoice = new ChoiceBox<>();
        categoryChoice.getItems().addAll("Work", "Vacation", "Hack Club", "Break", "Task");
        categoryChoice.setValue(originalEvent.getCategory());

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        VBox form = new VBox(10);
        form.getChildren().addAll(
                new Label("Date: " + originalEvent.getDate()),
                titleField,
                startTimeField,
                endTimeField,
                categoryChoice,
                errorLabel
        );
        dialog.getDialogPane().setContent(form);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);

        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, actionEvent -> {
            String errorMessage = validateEventInput(
                    titleField.getText(),
                    startTimeField.getText(),
                    endTimeField.getText()
            );

            if (errorMessage != null) {
                errorLabel.setText(errorMessage);
                actionEvent.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                String category = categoryChoice.getValue();

                return new CalendarEvent(
                        titleField.getText(),
                        originalEvent.getDate(),
                        parseTime(startTimeField.getText()),
                        parseTime(endTimeField.getText()),
                        category,
                        getColorForCategory(category)
                );
            }

            return null;
        });

        dialog.showAndWait().ifPresent(updatedEvent -> {
            int eventIndex = events.indexOf(originalEvent);

            if (eventIndex >= 0) {
                events.set(eventIndex, updatedEvent);
            } else {
                events.add(updatedEvent);
            }

            saveEvents();
            drawCalendar();
            updateHackClubTrayIcon();
        });
    }

    private void showAddEventDialog(LocalDate date) {
        Dialog<CalendarEvent> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        applyTheme(dialog);
        dialog.setTitle("Add Event");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setHeader(createDialogTitleBar(dialog, "Add Event"));

        TextField titleField = new TextField();
        titleField.setPromptText("Title");

        TextField startTimeField = new TextField();
        startTimeField.setPromptText("Start Time, like 14:00");

        TextField endTimeField = new TextField();
        endTimeField.setPromptText("End Time, like 16:00");

        ChoiceBox<String> categoryChoice = new ChoiceBox<>();
        categoryChoice.getItems().addAll("Work", "Vacation", "Hack Club", "Break", "Task");
        categoryChoice.setValue("Hack Club");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        VBox form = new VBox(10);
        form.getChildren().addAll(
            new Label("Date: " + date),
            titleField,
            startTimeField,
            endTimeField,
            categoryChoice,
            errorLabel
        );
        dialog.getDialogPane().setContent(form);

        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);

        addButton.addEventFilter(javafx.event.ActionEvent.ACTION, actionEvent -> {
            String errorMessage = validateEventInput(
                    titleField.getText(),
                    startTimeField.getText(),
                    endTimeField.getText()
            );

            if (errorMessage != null) {
                errorLabel.setText(errorMessage);
                actionEvent.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button == addButtonType) {
                String category = categoryChoice.getValue();
                return new CalendarEvent(
                    titleField.getText(),
                    date,
                    parseTime(startTimeField.getText()),
                    parseTime(endTimeField.getText()),
                    category,
                    getColorForCategory(category)
                );
            }

            return null;
        });

        dialog.showAndWait().ifPresent(event -> {
            events.add(event);
            saveEvents();
            drawCalendar();
            updateHackClubTrayIcon();
        });
    }

    private void drawCalendar() {
        calendarGrid.getChildren().clear();

        calendarGrid.setPadding(new Insets(16));
        calendarGrid.setHgap(8);
        calendarGrid.setVgap(8);

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        monthLabel.setText(currentMonth.format(monthFormatter));
        updateHackClubHoursLabel();

        String[] daysOfWeek = {
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        };

        for (int column = 0; column < daysOfWeek.length; column++) {
            Label dayLabel = new Label(daysOfWeek[column]);
            dayLabel.getStyleClass().add("weekday-label");
            dayLabel.setMinHeight(30);
            calendarGrid.add(dayLabel, column, 0);
        }

        LocalDate firstDayOfMonth = currentMonth.atDay(1);
        int daysInMonth = currentMonth.lengthOfMonth();

        int startColumn = firstDayOfMonth.getDayOfWeek().getValue();

        if (startColumn == 7) {
            startColumn = 0;
        }

        int row = 1;
        int column = startColumn;

        for (int day = 1; day <= daysInMonth; day++) {
            VBox dayCell = createDayCell(day);

            calendarGrid.add(dayCell, column, row);

            column++;

            if (column > 6) {
                column = 0;
                row++;
            }
        }
    }

    private String validateEventInput(String title, String startTimeText, String endTimeText) {
        if (title.isBlank()) {
            return "Title cannot be empty.";
        }

        LocalTime startTime;
        LocalTime endTime;

        try {
            startTime = parseTime(startTimeText);
        } catch (Exception error) {
            return "Start time must look like 8:00 or 14:30.";
        }

        try {
            endTime = parseTime(endTimeText);
        } catch (Exception error) {
            return "End time must look like 9:00 or 16:30.";
        }

        if (!endTime.isAfter(startTime)) {
            return "End time must be after start time.";
        }

        return null;
    }
    private void updateHackClubHoursLabel() {
        Duration total = getHackClubTimeForCurrentMonth();

        long hours = total.toHours();
        long minutes = total.toMinutesPart();

        hackClubHoursLabel.setText("Hack Club: " + hours + "h " + minutes + "m scheduled");
    }

    private Duration getHackClubTimeForCurrentMonth() {
        Duration total = Duration.ZERO;

        for (CalendarEvent event : events) {
            boolean isHackClub = event.getCategory().equals("Hack Club");
            boolean isInCurrentMonth = YearMonth.from(event.getDate()).equals(currentMonth);

            if (isHackClub && isInCurrentMonth) {
                Duration eventDuration = Duration.between(
                        event.getStartTime(),
                        event.getEndTime()
                );

                total = total.plus(eventDuration);
            }
        }

        return total;
    }

    private List<CalendarEvent> getEventsForDate(LocalDate date) {
        List<CalendarEvent> eventsForDate = new ArrayList<>();

        for (CalendarEvent event : events) {
            if (event.getDate().equals(date)) {
                eventsForDate.add(event);
            }
        }

        eventsForDate.sort(Comparator.comparing(CalendarEvent::getStartTime));

        return eventsForDate;
    }

    private LocalTime parseTime(String text) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
        return LocalTime.parse(text, formatter);
    }

    private String formatEventText(CalendarEvent event) {
        String startTime = event.getStartTime().format(timeFormatter);
        String endTime = event.getEndTime().format(timeFormatter);

        return event.getTitle() + " " + startTime + "-" + endTime;
    }

    private void applyTheme(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
    }

    @Override
    public void stop() {
        removeHackClubTrayIcon();
    }

    private void exitApplication() {
        removeHackClubTrayIcon();
        Platform.exit();
        System.exit(0);
    }

    private void removeHackClubTrayIcon() {
        if (hackClubTrayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(hackClubTrayIcon);
            hackClubTrayIcon = null;
        }
    }

    private HBox createDialogTitleBar(Dialog<?> dialog, String title) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-window-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button("X");
        closeButton.getStyleClass().add("window-close-button");
        closeButton.setOnAction(event -> dialog.close());

        HBox titleBar = new HBox(12, titleLabel, spacer, closeButton);
        titleBar.getStyleClass().add("dialog-title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(10, 10, 10, 14));

        makeDialogDraggable(dialog, titleBar);

        return titleBar;
    }

    private void makeStageDraggable(Stage stage, Node dragHandle) {
        final double[] dragOffset = new double[2];

        dragHandle.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            dragOffset[0] = event.getScreenX() - stage.getX();
            dragOffset[1] = event.getScreenY() - stage.getY();
        });

        dragHandle.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (stage.isMaximized()) {
                stage.setMaximized(false);
            }

            stage.setX(event.getScreenX() - dragOffset[0]);
            stage.setY(event.getScreenY() - dragOffset[1]);
        });
    }

    private void makeDialogDraggable(Dialog<?> dialog, Node dragHandle) {
        final double[] dragOffset = new double[2];

        dragHandle.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            Window window = dialog.getDialogPane().getScene().getWindow();
            dragOffset[0] = event.getScreenX() - window.getX();
            dragOffset[1] = event.getScreenY() - window.getY();
        });

        dragHandle.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            Window window = dialog.getDialogPane().getScene().getWindow();
            window.setX(event.getScreenX() - dragOffset[0]);
            window.setY(event.getScreenY() - dragOffset[1]);
        });
    }

    private boolean isHackClubTimeNow() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        for (CalendarEvent event : events) {
            boolean isHackClub = event.getCategory().equals("Hack Club");
            boolean isToday = event.getDate().equals(today);
            boolean hasStarted = !now.isBefore(event.getStartTime());
            boolean hasNotEnded = now.isBefore(event.getEndTime());

            if (isHackClub && isToday && hasStarted && hasNotEnded) {
                return true;
            }
        
        }

        return false;
    }

    private void setupHackClubTrayIcon() {
        if (!SystemTray.isSupported()) {
            return;
        }

        try {
            hackClubTrayIcon = new TrayIcon(createTrayImage(false), "Hack Club inactive");
            hackClubTrayIcon.setImageAutoSize(true);

            SystemTray.getSystemTray().add(hackClubTrayIcon);
            updateHackClubTrayIcon();
        } catch (Exception error) {
            System.out.println("Could not create tray icon: " + error.getMessage());
        }
    }

    private void updateHackClubTrayIcon() {
        if (hackClubTrayIcon == null) {
            return;
        }

        boolean active = isHackClubTimeNow();

        hackClubTrayIcon.setImage(createTrayImage(active));
        hackClubTrayIcon.setToolTip(active
                ? "Hack Club time: work on projects"
                : "Hack Club inactive");
    }

    private Image createTrayImage(boolean active) {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = image.createGraphics();
        graphics.setColor(active ? new Color(155, 92, 255) : new Color(75, 65, 92));
        graphics.fillOval(2, 2, 12, 12);

        graphics.setColor(Color.WHITE);
        graphics.drawString("H", 5, 12);

        graphics.dispose();
        return image;
    }

    public static void main(String[] args) {
        launch();
    }
}
