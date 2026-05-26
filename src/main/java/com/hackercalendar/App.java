package com.hackercalendar;

import java.time.YearMonth;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;


public class App extends Application {
    private YearMonth currentMonth = YearMonth.now();
    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        HBox topBar = createTopBar();
        HBox categoryBar = createCategoryBar();
        GridPane calendarGrid = createCalendarGrid();

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(categoryBar);
        mainLayout.setCenter(calendarGrid);

        root.setTop(topBar);
        root.setCenter(mainLayout);

        Scene scene = new Scene(root, 1000, 700);

        stage.setTitle("Hacker Calendar");
        stage.setScene(scene);
        stage.show();
    }

    private HBox createTopBar() {
        Label appTitle = new Label("Hacker Calendar");

        Button todayButton = new Button("Today");
        Button previousButton = new Button("<");
        Label monthLabel = new Label("May 2026");
        Button nextButton = new Button(">");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, appTitle, spacer, todayButton, previousButton, monthLabel, nextButton);
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

    private GridPane createCalendarGrid() {
        GridPane calendarGrid = new GridPane();
        calendarGrid.setPadding(new Insets(16));
        calendarGrid.setHgap(8);
        calendarGrid.setVgap(8);
        String[] daysOfWeek = {
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        };
        for (int column = 0; column < daysOfWeek.length; column++) {
            Label dayLabel = new Label(daysOfWeek[column]);
            dayLabel.setMinHeight(30);
            calendarGrid.add(dayLabel, column, 0);
        }

        int dayNumber = 1;

        for (int row = 1; row <= 5; row++) {
            for (int column = 0; column < 7; column++) {
                Button dayButton = new Button(String.valueOf(dayNumber));
                dayButton.setMinSize(120, 90);
                calendarGrid.add(dayButton, column, row);
                dayNumber++;
            }
        }

        return calendarGrid;
    }

    public static void main(String[] args) {
        launch();
    }
}