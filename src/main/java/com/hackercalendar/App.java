package com.hackercalendar;

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

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class App extends Application {
    private static final Path EVENTS_FILE = Path.of("events.csv");
    
    private YearMonth currentMonth = YearMonth.now();
    private List<CalendarEvent> events = new ArrayList<>();
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
    
    private Label monthLabel;
    private Label hackClubHoursLabel;
    private GridPane calendarGrid;


    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        HBox topBar = createTopBar();
        HBox categoryBar = createCategoryBar();

        calendarGrid = new GridPane();

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(categoryBar);
        mainLayout.setCenter(calendarGrid);

        root.setTop(topBar);
        root.setCenter(mainLayout);

        loadEvents();
        drawCalendar();

        Scene scene = new Scene(root, 1000, 700);

        stage.setTitle("Hacker Calendar");
        stage.setScene(scene);
        stage.show();
    }

    private HBox createTopBar() {
        Label appTitle = new Label("Hacker Calendar");

        Button todayButton = new Button("Today");
        Button previousButton = new Button("<");
        monthLabel = new Label();
        Button nextButton = new Button(">");

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
        hackClubHoursLabel.setStyle(
                "-fx-text-fill: #475569;" +
                "-fx-font-size: 12px;"
        );

        HBox monthControls = new HBox(12, todayButton, previousButton, monthLabel, nextButton);
        monthControls.setAlignment(Pos.CENTER_RIGHT);

        VBox monthArea = new VBox(4, monthControls, hackClubHoursLabel);
        monthArea.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, appTitle, spacer, monthArea);
        topBar.setPadding(new Insets(16));
        topBar.setAlignment(Pos.CENTER_LEFT);

        return topBar;
    }

    private HBox createCategoryBar() {
        Button workButton = new Button("Work");
        Button vacationButton = new Button("Vacation");
        Button hackClubButton = new Button("Hack Club");
        Button breaksButton = new Button("Breaks");
        Button tasksButton = new Button("Tasks");

        HBox categoryBar = new HBox(10, workButton, vacationButton, hackClubButton, breaksButton, tasksButton);
        categoryBar.setPadding(new Insets(0, 16, 16, 16));
        categoryBar.setAlignment(Pos.CENTER_LEFT);

        return categoryBar;
    }

    private Label createEventLabel(String text, String color) {
        Label eventLabel = new Label(text);

        eventLabel.setMaxWidth(Double.MAX_VALUE);
        eventLabel.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 1 5 1 5;" +
                "-fx-background-radius: 4;" +
                "-fx-font-size: 11px;"
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

        VBox dayCell = new VBox(4);
        dayCell.getChildren().add(dayNumber);

        LocalDate date = currentMonth.atDay(day);

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
            moreLabel.setStyle(
                    "-fx-text-fill: #475569;" +
                    "-fx-font-size: 10px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 0 0 0 2;"
            );

            dayCell.getChildren().add(moreLabel);
        }

        dayCell.setMinSize(120, 90);
        dayCell.setPadding(new Insets(8));
        dayCell.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #d0d7de;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;"
        );

        LocalDate selectedDate = currentMonth.atDay(day);
        dayCell.setOnMouseClicked(event -> {
            showDayDetailsDialog(selectedDate);
        });

        Rectangle clip  = new Rectangle(120, 90);
        clip.setArcWidth(6);
        clip.setArcHeight(6);
        return dayCell;
    }

    private void showDayDetailsDialog(LocalDate date) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Day Details");

        ButtonType addEventButtonType = new ButtonType("Add Event", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addEventButtonType, ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label dateLabel = new Label(date.toString());
        dateLabel.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;"
        );

        content.getChildren().add(dateLabel);

        List<CalendarEvent> eventsForDate = getEventsForDate(date);

        if (eventsForDate.isEmpty()) {
            Label emptyLabel = new Label("No events yet.");
            emptyLabel.setStyle("-fx-text-fill: #64748b;");
            content.getChildren().add(emptyLabel);
        } else {
            for (CalendarEvent event : eventsForDate) {
                Label eventLabel = createEventLabel(
                        formatEventText(event),
                        event.getColor()
                );

                content.getChildren().add(eventLabel);
            }
        }

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(button -> {
            if (button == addEventButtonType) {
                showAddEventDialog(date);
            }

            return null;
        });

        dialog.showAndWait();
    }

    private void showAddEventDialog(LocalDate date) {
        Dialog<CalendarEvent> dialog = new Dialog<>();
        dialog.setTitle("Add Event");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("Title");

        TextField startTimeField = new TextField();
        startTimeField.setPromptText("Start Time, like 14:00");

        TextField endTimeField = new TextField();
        endTimeField.setPromptText("End Time, like 16:00");

        ChoiceBox<String> categoryChoice = new ChoiceBox<>();
        categoryChoice.getItems().addAll("Work", "Vacation", "Hack Club", "Break", "Task");
        categoryChoice.setValue("Hack Club");

        VBox form = new VBox(10);
        form.getChildren().addAll(
            new Label("Date: " + date),
            titleField,
            startTimeField,
            endTimeField,
            categoryChoice
        );
        dialog.getDialogPane().setContent(form);

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

    public static void main(String[] args) {
        launch();
    }
}