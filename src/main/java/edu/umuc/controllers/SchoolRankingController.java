package edu.umuc.controllers;

import edu.umuc.models.RankWeight;
import edu.umuc.models.School;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SchoolRankingController extends Controller implements Initializable {

    private DecimalFormat decimalFormat = new DecimalFormat( "0.00" );
	private final ScrapeData scrapeData = new ScrapeData();
    private static final int OLDEST_YEAR = 2015;
    
    @FXML
    private TableView<SchoolRankingController.FXSchoolRankingTable> tbSchoolRanking;

    @FXML
    public TableColumn<SchoolRankingController.FXSchoolRankingTable, Integer> rank;

    @FXML
    public TableColumn<SchoolRankingController.FXSchoolRankingTable, String> schoolName;

    @FXML
    public TableColumn<SchoolRankingController.FXSchoolRankingTable, Integer> wins;

    @FXML
    public TableColumn<SchoolRankingController.FXSchoolRankingTable, Integer> losses;

    @FXML
    public TableColumn<SchoolRankingController.FXSchoolRankingTable, String> league;

    @FXML
    public TableColumn<SchoolRankingController.FXSchoolRankingTable, Integer> oppWins;

    @FXML
    public TableColumn<SchoolRankingController.FXSchoolRankingTable, Float> avgPointDiff;

    @FXML
    public TableColumn<SchoolRankingController.FXSchoolRankingTable, Float> totalPoints;

    @FXML
    public ChoiceBox<String> yearChoice;

    @FXML
    public ChoiceBox<String> sportChoice;

    @FXML
    private Label lblWinLoss;
    
    @FXML
    private Label lblOppWins;

    @FXML
    private Label lblAvgPointDiff;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
		List<String> yearList = new ArrayList<>();
        for (int year = LocalDate.now().getYear(); year >= OLDEST_YEAR; year--) {
            yearList.add(String.valueOf(year));
        }
        yearChoice.setItems((FXCollections.observableArrayList(yearList)));
        yearChoice.getSelectionModel().selectFirst();

        try {
            sportChoice.setItems(FXCollections.observableArrayList(scrapeData.scrapeSportsData()));
            sportChoice.getSelectionModel().selectFirst();
        } catch (Exception ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, "Exception thrown during data scraping.", ex);
        }
		
        lblWinLoss.setText(decimalFormat.format(1.1));
        lblOppWins.setText(decimalFormat.format(.1));
        lblAvgPointDiff.setText(decimalFormat.format(.15));
    }

    @FXML
    private void processRankSchoolsEvent(ActionEvent event) {
        scrapeData();
    }

    private void scrapeData() {
        //TODO get Year and Sport Choices
        rank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        schoolName.setCellValueFactory(new PropertyValueFactory<>("schoolName"));
        wins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        losses.setCellValueFactory(new PropertyValueFactory<>("losses"));
        league.setCellValueFactory(new PropertyValueFactory<>("league"));
        oppWins.setCellValueFactory(new PropertyValueFactory<>("oppWins"));
        avgPointDiff.setCellValueFactory(new PropertyValueFactory<>("avgPointDiff"));
        totalPoints.setCellValueFactory(new PropertyValueFactory<>("totalPoints"));

        final ObservableList<SchoolRankingController.FXSchoolRankingTable> rankedSchools = FXCollections.observableArrayList();

        // Mocked up data
        //final List<School> schools = SportRankingUIManager.getSingletonInstance().getSchools();

        try {
            final Dialog dialog = new Dialog();

            dialog.setTitle("Ranking Schools");
            dialog.setContentText("This process may take a few minutes, please wait...");
            dialog.show();

            //Scrapes data
            final ArrayList<School> schools = scrapeData.scrapeData("2018", "fall", "football", new RankWeight(0.75f, 0.1f, 0.15f));

            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
            dialog.close();

            //Ranking and sorting
            schools.sort((school1, school2) -> (int) ((school2.getRankPoints() * 100) - (school1.getRankPoints() * 100)));

            //Output schools with wrong information
            schools.stream().filter(school -> school.getWins() != 0 && school.getLosses() != 0).map(school -> school.getSchoolName() + ", League: " + school.getLeague().getLeagueName() + ", Rank Points: " + school.getRankPoints() + ", Record incorrect: " + school.isWinLossRecordIncorrect()).forEach(System.out::println);

            schools.forEach(school -> rankedSchools.add(new FXSchoolRankingTable(schools.indexOf(school), school.getSchoolName(),
                    school.getWins(), school.getLosses(), school.getLeague().getLeagueName(),
                    school.getOpponentsTotalWins(), school.getAvgPointDifference(), school.getRankPoints())));

            tbSchoolRanking.setItems(rankedSchools);

        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException | InterruptedException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, "Exception thrown during data scraping.", ex);
        }
    }

    /**
     * In order to load information on the UI, we need to create a class with the JavaFX
     * bean properties ex: SimpleStringProperty and SimpleFloatProperty.
     *
     * Created a local class FXSchoolRankingTable for this purpose
     */
    public class FXSchoolRankingTable {
        private final SimpleIntegerProperty rank;
        private final SimpleStringProperty schoolName;
        private final SimpleIntegerProperty wins;
        private final SimpleIntegerProperty losses;
        private final SimpleStringProperty league;
        private final SimpleIntegerProperty oppWins;
        private final SimpleFloatProperty avgPointDiff;
        private final SimpleFloatProperty totalPoints;

        private FXSchoolRankingTable(Integer rank, String schoolName, Integer wins, Integer losses, String league,
                                     Integer oppWins, Float avgPointDiff, Float totalPoints) {
            this.rank = new SimpleIntegerProperty(rank);
            this.schoolName = new SimpleStringProperty(schoolName);
            this.wins = new SimpleIntegerProperty(wins);
            this.losses = new SimpleIntegerProperty(losses);
            this.league = new SimpleStringProperty(league);
            this.oppWins = new SimpleIntegerProperty(oppWins);
            this.avgPointDiff = new SimpleFloatProperty(avgPointDiff);
            this.totalPoints = new SimpleFloatProperty(totalPoints);
        }

        public int getRank() {
            return rank.get();
        }

        public SimpleIntegerProperty rankProperty() {
            return rank;
        }

        public String getSchoolName() {
            return schoolName.get();
        }

        public SimpleStringProperty schoolNameProperty() {
            return schoolName;
        }

        public int getWins() {
            return wins.get();
        }

        public SimpleIntegerProperty winsProperty() {
            return wins;
        }

        public int getLosses() {
            return losses.get();
        }

        public SimpleIntegerProperty lossesProperty() {
            return losses;
        }

        public String getLeague() {
            return league.get();
        }

        public SimpleStringProperty leagueProperty() {
            return league;
        }

        public int getOppWins() {
            return oppWins.get();
        }

        public SimpleIntegerProperty oppWinsProperty() {
            return oppWins;
        }

        public float getAvgPointDiff() {
            return avgPointDiff.get();
        }

        public SimpleFloatProperty avgPointDiffProperty() {
            return avgPointDiff;
        }

        public float getTotalPoints() {
            return totalPoints.get();
        }

        public SimpleFloatProperty totalPointsProperty() {
            return totalPoints;
        }
    }
}
