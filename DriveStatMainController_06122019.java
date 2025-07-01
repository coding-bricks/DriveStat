package de.gsi.csco.ap.app_drivestat.main;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cern.japc.core.ParameterException;
import cern.lsa.domain.settings.BeamProductionChain;
import cern.lsa.domain.settings.Pattern;
import de.gsi.csco.ap.app_drivestat.model.domain.Drive;
import de.gsi.csco.ap.app_drivestat.utils.AboutButton;
import de.gsi.csco.ap.app_drivestat.utils.DeviceUpdateFESA;
import de.gsi.csco.ap.app_drivestat.utils.DeviceUserNameGenerator;
import de.gsi.csco.ap.app_drivestat.utils.DevicesStatus;
import de.gsi.csco.ap.app_drivestat.utils.DialogMessage;
import de.gsi.csco.ap.app_drivestat.utils.DriveDataJoiner;
import de.gsi.csco.ap.app_drivestat.utils.DriveDataProcessorFESA;
import de.gsi.csco.ap.app_drivestat.utils.PrinterDialog;
import de.gsi.csco.ap.app_drivestat.utils.PwdDialog;
import de.gsi.csco.ap.app_drivestat.utils.RectangleGenerator;
import de.gsi.csco.ap.app_drivestat.utils.RetrieveDateAndTime;
import de.gsi.csco.ap.app_drivestat.utils.RetrieveOpticsName;
import de.gsi.csco.ap.app_drivestat.utils.RetrievePatternsAndChains;
import de.gsi.csco.ap.app_drivestat.utils.SaveData;
import de.gsi.csco.ap.app_drivestat.utils.SaveDataDialog;
import de.gsi.sequencer.sequences.superfrs.SetAllFRSDrives;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

// for library loggers
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// for application loggers
// import de.gsi.cs.co.ap.common.gui.elements.logger.AppLogger;

/**
 * @author fschirru
 */
public class DriveStatMainController {

    // You can choose a logger (needed imports are given in the import section
    // as comments):
    // for libraries:
    // private static final Logger LOGGER =
    // LoggerFactory.getLogger(DriveStatMainController.class);
    // for applications:
    // private static final AppLogger LOGGER = AppLogger.getLogger();

    @FXML
    private BorderPane borderPane;
    @FXML
    private TableView<Drive> driveTable;
    @FXML
    private TableColumn<Drive, String> deviceName_col;
    @FXML
    private TableColumn<Drive, String> deviceNameDescr_col;
    @FXML
    private TableColumn<Drive, String> typeName_col;
    @FXML
    private TableColumn<Drive, Integer> abs_Posi_ds_col;
    @FXML
    private TableColumn<Drive, Integer> min_Pos_ds_col;
    @FXML
    private TableColumn<Drive, Integer> max_Pos_ds_col;
    @FXML
    private TableColumn<Drive, Integer> abs_Posi_pla_col;
    @FXML
    private TableColumn<Drive, Integer> extra_col1;
    @FXML
    private TableColumn<Drive, Integer> extra_col2;
    @FXML
    private Button refresh_bt;
    @FXML
    private Button save_bt;
    @FXML
    private Button exit_bt;
    @FXML
    private Button print_bt;
    @FXML
    private Button status_bt;
    @FXML
    private Button about_bt;
    @FXML
    private ComboBox<Pattern> patterns_cb;
    @FXML
    private ComboBox<BeamProductionChain> chains_cb;
    @FXML
    private CheckBox updateDrives_cb;
    @FXML
    private Label status_lb;
    
    @FXML
    private ProgressBar progressBar_pb;

    @FXML
    private Circle dataSavingLed;

    private final AboutButton ab = AboutButton.getInstance();
    // private final AutoSavingDialog asd = AutoSavingDialog.getInstance();
    private final DeviceUserNameGenerator dung = DeviceUserNameGenerator.getInstance();
    private final DeviceUpdateFESA duFESA = DeviceUpdateFESA.getInstance();
    private final DevicesStatus ds = DevicesStatus.getInstance();
    private final DialogMessage dm = DialogMessage.getInstance();
    private final DriveDataProcessorFESA ddpFESA = DriveDataProcessorFESA.getInstance();
    // private final ManualSavingDialog msd = ManualSavingDialog.getInstance();
    private final RetrieveOpticsName ron = RetrieveOpticsName.getInstance();
    private final DriveDataJoiner ddpj = DriveDataJoiner.getInstance();
    private final PrinterDialog pd = PrinterDialog.getInstance();
    private final PwdDialog pwdd = PwdDialog.getInstance();
    private final RetrieveDateAndTime dt = RetrieveDateAndTime.getInstance();
    private final RetrievePatternsAndChains retrievePatternsAndChains = RetrievePatternsAndChains.getInstance();
    private final SaveDataDialog sdd = SaveDataDialog.getInstance();
    private final SaveData sd = SaveData.getInstance();
    
    private static final RectangleGenerator rectangle = new RectangleGenerator();

    private Pattern selectedPattern;
    private List<BeamProductionChain> chains;
    private final Color[] devicesNamesColor = new Color[100];
    // private final List<Color> listOfColors = new ArrayList<>();

    private Map<Pattern, List<BeamProductionChain>> patternsAndChainsMap = new HashMap<>();
    private int groupIndex = 0;

    private Timer timer_dataRecording; // timer for data recorded on disk
    private Timer timer_updateDrives;
    
    private Timer timer_connectionStatus;
    
    // private Boolean timer_dataRecording_started = false;
    private final Boolean timer_updateDrives_started = false;
   
    // private final Boolean aredevicesNamesPainted = false;
    // private final Boolean destination_directory_isSelected = false;  alert.close();

    private final List<String> devicesNames = new ArrayList<>();

    private boolean accessForUpdateDrivesGranted = false;
    private boolean listOfPatternshasChanged;
    private boolean timer_dataRecording_started;
    private boolean timer_connectionStatus_started = false;
   

    private RadialGradient gradient1;
    private RadialGradient gradient2;

    private final IntegerProperty isTableFilledWithData = new SimpleIntegerProperty(0);
    private final IntegerProperty isAppRunning = new SimpleIntegerProperty(0);
    
   

    // temporary solution with 200 elements
    // private final Color[] devicesNamesColor = new Color[200];

    // private BeamProductionChain chain;

    private ChangeListener<Pattern> patternsChangeListener;
    private ChangeListener<BeamProductionChain> chainsChangeListener;

    private String acceleratorZoneItem; // for color usage
    private String acceleratorZonePreviousItem; // for color usage
    
    //private final ObservableList<DeviceConnectionStatus> connectionStatus_list = FXCollections.observableArrayList();
    private final Object[] temp_data = new Object[3]; //property_name, connectionStatus
    private boolean isMonitoring;

    @FXML
    public void initialize() {

        initializeLedGUI();
        initializeButtonsGUI();
        initializeUpdateDrivesAction();
        initializeGUI();
        listenerGenerator();
        setPatternsAndChains();
    }

    private void setPatternsAndChains() {

        patterns_cb.valueProperty().removeListener(patternsChangeListener);
        patterns_cb.getItems().clear();

        chains_cb.valueProperty().removeListener(chainsChangeListener);
        chains_cb.getItems().clear();

        patternsAndChainsMap = retrievePatternsAndChains.getPatternsAndChains();

        try {

            patterns_cb.getItems().addAll(patternsAndChainsMap.keySet());
            patterns_cb.valueProperty().addListener(patternsChangeListener);
            patterns_cb.getSelectionModel().selectFirst();

        } catch (final Exception e) {

            dm.setIconAndText(null, "Error while loading Patterns and Chains. Please try later.");
            dm.showMessangerStage();

            handleExit();
        }
    }

    @FXML
    private void handleRefresh() {

        listOfPatternshasChanged = listOfPatternsUpdater();

        if (!listOfPatternshasChanged) {

            setPatternsAndChains();

        } else {

            dm.setIconAndText(null, "Update of Patterns/Chains currently not available. Please try later.");
            dm.showMessangerStage();
        }
    }

    @FXML
    private void handleAbout() {

        ab.showAboutStage();
    }

    @FXML
    private void handleDevicesStatus() {

        ds.setListOfSubscriptions(duFESA.get_subscriptionHandle());
        ds.setSortedDevices(ddpFESA.getSortedDevices());
        ds.ShowDevicesStatusDialog();
    }

    @FXML
    private void handleExit() {

        status_lb.textProperty().unbind();
        status_lb.setText("Please wait while application is closing...");

        final Task<Void> taskOnExitingApp = new Task<Void>() {

            @Override
            public Void call() throws InterruptedException {

                // updateMessage("Please wait while application is closing...");

                // maybe not necessary since tasks are Daemon
                if (timer_dataRecording_started) {

                    timer_dataRecording.cancel();
                }

                if (timer_updateDrives_started) {

                    timer_updateDrives.cancel();
                }

                ddpFESA.closeDeviceSubscriptions();

                Platform.exit();
                System.exit(0);

                return null;
            }
        };

        // status_lb.textProperty().bind(taskOnExitingApp.messageProperty());

        final Thread threadOnExitingApp = new Thread(taskOnExitingApp);
        threadOnExitingApp.setDaemon(true);
        threadOnExitingApp.start();
    }

    @FXML
    private void handleManualSaving() {

        sdd.setDialogMessageStage(dm);
        sdd.stageShowAndWait();

        if (sdd.getSelectedOption() == 0) {

            sd.setDateAndTime(dt.getDateAndTime());
            sd.setDevicesNamesDescription(dung.getDevicesNamesDescriptions_map());
            sd.setData(ddpj.getData());
            sd.writeDataToDisk(patterns_cb.getSelectionModel().getSelectedItem().getName(),
                    chains_cb.getSelectionModel().getSelectedItem().getName(), ron.getOpticsName(), sdd.getFilePath(),
                    sdd.getFileName());
        }

        if (sdd.getSelectedOption() == 1) {

            dataSavingLed.setFill(gradient2);
            startTimerForDataRecording(sdd.getFrequency());
            timer_dataRecording_started = true;

        }

        if (sdd.getSelectedOption() == 2) {

            dataSavingLed.setFill(gradient1);
            timer_dataRecording.cancel();
            timer_dataRecording_started = false;
        }
    }

    @FXML
    private void handlePrint() {

        pd.setDevicesNames_map(dung.getDevicesNamesDescriptions_map());
        pd.setTableDataHeader(dt.getDateAndTime(), patterns_cb.getSelectionModel().getSelectedItem().getName(),
                chains_cb.getSelectionModel().getSelectedItem().getName());
        pd.setData(ddpj.getData());

        pd.ShowPrinterDialog();
    }

    private void createTable() {
        
        if(timer_connectionStatus_started) {
            
            timer_connectionStatus.cancel();
        }
        
        ddpFESA.setDeviceNameProcessedProperty("");

        accessForUpdateDrivesGranted = false;
        updateDrives_cb.setSelected(false);
        updateDrives_cb.setDisable(true);
        // Disable ComboBoxes while loading data
        patterns_cb.setDisable(true);
        chains_cb.setDisable(true);
        // closeAllSubscriptions();
        ddpFESA.closeDeviceSubscriptions();
        isAppRunning.set(0);
        isTableFilledWithData.set(0);// Update Drives

        /*- data should be cleared but app works also without the following line...*/
        ddpj.getData().clear();

        driveTable.getItems().clear();
        driveTable.refresh();

        performeTaskDataTable(chains_cb.getValue());
    }

    private void initializeUpdateDrivesAction() {

        updateDrives_cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue,
                    final Boolean newValue) {

                if (newValue) {
                    if (updateDrives_cb.isSelected()) {

                        // accessForUpdateDrivesGranted = false;
                        // updateDrives_cb.setSelected(false);
                        // pwdDialogStage.show();
                        pwdd.showPwdDialogStage();
                        pwdd.closePwdDialog();

                        // Grant access
                        if (pwdd.getAccess().getValue().intValue() == 1) {

                            accessForUpdateDrivesGranted = true;
                            updateDrives_cb.setDisable(true);

                        } else {

                            accessForUpdateDrivesGranted = false;
                            updateDrives_cb.setSelected(false);
                        }
                    }
                }
                // } else {

                // accessForUpdateDrivesGranted = false;
                // }
            }
        });
    }

    private void createDeviceNamesTextColor() {

        // Disable ComboBoxes patterns and chains
        patterns_cb.setDisable(true);
        chains_cb.setDisable(true);

        groupIndex = 0;

        // Reset colors
        for (int c = 0; c < devicesNamesColor.length; c++) {

            devicesNamesColor[c] = null;
        }

        if (ddpj.getData().size() != 0) {

            for (int i = 0; i < ddpj.getData().size(); i++) {

                acceleratorZoneItem = "";
                acceleratorZonePreviousItem = "";

                acceleratorZoneItem = dung.retrieveUserFriendlyDevicesNames(ddpj.getData().get(i).getDeviceName())
                        .substring(0, 3);

                if (i == 0) {

                    devicesNamesColor[i] = Color.DARKBLUE;

                } else {

                    acceleratorZoneItem = dung.retrieveUserFriendlyDevicesNames(ddpj.getData().get(i).getDeviceName())
                            .substring(0, 3);

                    acceleratorZonePreviousItem = dung
                            .retrieveUserFriendlyDevicesNames(ddpj.getData().get(i - 1).getDeviceName())
                            .substring(0, 3);

                    if (!acceleratorZoneItem.equals(acceleratorZonePreviousItem)) {

                        groupIndex++;
                    }

                    if ((groupIndex % 2) == 0) {

                        devicesNamesColor[i] = Color.DARKBLUE;

                    } else {

                        devicesNamesColor[i] = Color.CORNFLOWERBLUE;
                    }
                }
            }
        }
    }

    private void listenerGenerator() {

        // listener for Patterns
        patternsChangeListener = new ChangeListener<Pattern>() {

            @Override
            public void changed(final ObservableValue<? extends Pattern> observable, final Pattern oldValue,
                    final Pattern newValue) {

                if (newValue != null) {
                    
                    /*if(timer_connectionStatus_started) {
                        
                        timer_connectionStatus.cancel();
                       
                    } */

                    chains_cb.valueProperty().removeListener(chainsChangeListener);

                    selectedPattern = patterns_cb.getSelectionModel().getSelectedItem();

                    chains = patternsAndChainsMap.get(selectedPattern);

                    chains_cb.getItems().clear();
                    chains_cb.getItems().addAll(chains);
                    chains_cb.valueProperty().addListener(chainsChangeListener);
                    chains_cb.getSelectionModel().selectFirst();
                }
            }
        };

        // listener for Chains
        chainsChangeListener = new ChangeListener<BeamProductionChain>() {

            @Override
            public void changed(final ObservableValue<? extends BeamProductionChain> observable,
                    final BeamProductionChain oldValue, final BeamProductionChain newValue) {

                if (newValue != null) {

                    createTable();
                }
            }
        };
    }

    private void startTimerForDataRecording(final int savingFreq) {

        timer_dataRecording = new Timer();
        timer_dataRecording.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {

                sd.setDateAndTime(dt.getDateAndTime());
                sd.setDevicesNamesDescription(dung.getDevicesNamesDescriptions_map());
                sd.setData(ddpj.getData());
                sd.writeDataToDisk(patterns_cb.getSelectionModel().getSelectedItem().getName(),
                        chains_cb.getSelectionModel().getSelectedItem().getName(), ron.getOpticsName(),
                        sdd.getFilePath(), "DriveStat_file");
                System.out.println("Data are being recorded on Disk!");
            }
        }, 0, savingFreq);
    }

    private void initializeButtonsGUI() {

        about_bt.disableProperty().bind(isAppRunning.isNotEqualTo(1));
        print_bt.disableProperty().bind(isTableFilledWithData.isNotEqualTo(1));
        refresh_bt.disableProperty().bind(isAppRunning.isNotEqualTo(1));
        save_bt.disableProperty()
                .bind(Bindings.when(isTableFilledWithData.isNotEqualTo(1)).then(true).otherwise(false));
        status_bt.disableProperty().bind(isTableFilledWithData.isNotEqualTo(1));
    }

    private void initializeLedGUI() {

        gradient1 = new RadialGradient(0, 0, dataSavingLed.getCenterX(), dataSavingLed.getCenterY(),
                dataSavingLed.getRadius() + 3, false, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#ffebe6", 1.0)),
                new Stop(1, Color.LIGHTGRAY));

        gradient2 = new RadialGradient(0, 0, dataSavingLed.getCenterX(), dataSavingLed.getCenterY(),
                dataSavingLed.getRadius() + 3, false, CycleMethod.NO_CYCLE, new Stop(0, Color.RED),
                new Stop(1, Color.LIGHTGRAY));

        dataSavingLed.setFill(gradient1);
    }

    private void initializeGUI() {

        driveTable.setSelectionModel(null); // it removes the row selection
        /*-	it changes the focus color to not visible -*/
        driveTable.setStyle("-fx-focus-color: LightGray;" + "-fx-faint-focus-color: transparent;");
        driveTable.setFocusTraversable(false); // focusing
        driveTable.setMaxHeight(650);
        driveTable.setFixedCellSize(25);

        // Initialize column Titles and settings
        final Label deviceName_colTitle_lb = new Label("Device ID");
        deviceName_colTitle_lb.setPrefHeight(50);
        deviceName_colTitle_lb.setWrapText(true);
        deviceName_colTitle_lb.setTextAlignment(TextAlignment.CENTER);
        deviceName_col.setGraphic(deviceName_colTitle_lb);
        deviceName_col.setResizable(false);
        deviceName_col.setPrefWidth(118);
        deviceName_col.setStyle("-fx-alignment: CENTER-LEFT;");
        deviceName_col.setSortable(false);

        final Label deviceUFName_colTitle_lb = new Label("Description");
        deviceUFName_colTitle_lb.setPrefHeight(50);
        deviceUFName_colTitle_lb.setWrapText(true);
        deviceUFName_colTitle_lb.setTextAlignment(TextAlignment.CENTER);
        deviceNameDescr_col.setGraphic(deviceUFName_colTitle_lb);
        deviceNameDescr_col.setResizable(false);
        deviceNameDescr_col.setPrefWidth(236);
        deviceNameDescr_col.setStyle("-fx-alignment: CENTER-LEFT;");
        deviceNameDescr_col.setSortable(false);

        final Label deviceType_colTitle_lb = new Label("Type");
        deviceType_colTitle_lb.setPrefHeight(50);
        deviceType_colTitle_lb.setWrapText(true);
        deviceType_colTitle_lb.setTextAlignment(TextAlignment.CENTER);
        typeName_col.setGraphic(deviceType_colTitle_lb);
        typeName_col.setResizable(false);
        typeName_col.setPrefWidth(118);
        typeName_col.setStyle("-fx-alignment: CENTER;");
        typeName_col.setSortable(false);

        final Label minPos_ds_colTitle_lb = new Label("Min Pos." + "\n(mm)");
        minPos_ds_colTitle_lb.setPrefHeight(50);
        minPos_ds_colTitle_lb.setWrapText(true);
        minPos_ds_colTitle_lb.setTextAlignment(TextAlignment.CENTER);
        min_Pos_ds_col.setGraphic(minPos_ds_colTitle_lb);
        min_Pos_ds_col.setResizable(false);
        min_Pos_ds_col.setPrefWidth(118);
        min_Pos_ds_col.setStyle("-fx-alignment: CENTER-RIGHT;");
        min_Pos_ds_col.setSortable(false);

        final Label absPosi_ds_colTitle_lb = new Label("Pos." + "\n(mm)");
        absPosi_ds_colTitle_lb.setPrefHeight(50);
        absPosi_ds_colTitle_lb.setWrapText(true);
        absPosi_ds_colTitle_lb.setTextAlignment(TextAlignment.CENTER);
        abs_Posi_ds_col.setGraphic(absPosi_ds_colTitle_lb);
        abs_Posi_ds_col.setResizable(false);
        abs_Posi_ds_col.setPrefWidth(118);
        abs_Posi_ds_col.setStyle("-fx-alignment: CENTER-RIGHT;");
        abs_Posi_ds_col.setSortable(false);

        final Label maxPos_ds_colTitle_lb = new Label("Max Pos." + "\n(mm)");
        maxPos_ds_colTitle_lb.setPrefHeight(50);
        maxPos_ds_colTitle_lb.setWrapText(true);
        maxPos_ds_colTitle_lb.setTextAlignment(TextAlignment.CENTER);
        max_Pos_ds_col.setGraphic(maxPos_ds_colTitle_lb);
        max_Pos_ds_col.setResizable(false);
        max_Pos_ds_col.setPrefWidth(118);
        max_Pos_ds_col.setStyle("-fx-alignment: CENTER-RIGHT;");
        max_Pos_ds_col.setSortable(false);

        final Label absPosi_pla_colTitle_lb = new Label("Pos." + "\n(IN/OUT[1/0])");
        absPosi_pla_colTitle_lb.setPrefHeight(50);
        absPosi_pla_colTitle_lb.setWrapText(true);
        absPosi_pla_colTitle_lb.setTextAlignment(TextAlignment.CENTER);
        abs_Posi_pla_col.setGraphic(absPosi_pla_colTitle_lb);
        abs_Posi_pla_col.setResizable(false);
        abs_Posi_pla_col.setPrefWidth(118);
        abs_Posi_pla_col.setStyle("-fx-alignment: CENTER-RIGHT;");
        abs_Posi_pla_col.setSortable(false);

        // Columns temporarily not used
        final Label connectionStatus_colTitle_lb = new Label("Connection" + "\nStatus");
        connectionStatus_colTitle_lb.setPrefHeight(50);
        connectionStatus_colTitle_lb.setWrapText(true);
        connectionStatus_colTitle_lb.setTextAlignment(TextAlignment.CENTER);
        extra_col1.setGraphic(connectionStatus_colTitle_lb);
        extra_col1.setResizable(false);
        extra_col1.setPrefWidth(118);
        extra_col1.setStyle("-fx-alignment: CENTER;");
        extra_col1.setSortable(false);

        extra_col2.setResizable(false);
        extra_col2.setPrefWidth(107);
        extra_col2.setStyle("-fx-alignment: CENTER-RIGHT;");
        extra_col2.setSortable(false);

        // Connection to the model
        deviceName_col.setCellValueFactory(new PropertyValueFactory<Drive, String>("deviceName"));
        typeName_col.setCellValueFactory(new PropertyValueFactory<Drive, String>("deviceType"));
        min_Pos_ds_col.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("minPos_ds"));
        abs_Posi_ds_col.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("absPosi_ds"));
        max_Pos_ds_col.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("maxPos_ds"));
        abs_Posi_pla_col.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("absPosi_pla"));
       // extra_col1.setCellValueFactory(cellData -> cellData.getValue().deviceStatus_Property());
        extra_col1.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("deviceStatus"));
        

        // Create cell factories for the columns
        deviceNameDescr_col.setCellFactory(new Callback<TableColumn<Drive, String>, TableCell<Drive, String>>() {

            @Override
            public TableCell<Drive, String> call(final TableColumn<Drive, String> param) {
                return new TableCell<Drive, String>() {

                    @Override
                    protected void updateItem(final String item, final boolean empty) {
                        super.updateItem(item, empty);

                        if (!empty) {

                            final int index = getTableRow().getIndex();

                            if ((index < ddpj.getData().size()) && (index >= 0)) {

                                /*- Get the device description corresponding to the given device name */
                                final String device_description = dung
                                        .retrieveUserFriendlyDevicesNames(ddpj.getData().get(index).getDeviceName());

                                setText(device_description);
                            }
                        }
                    }
                };
            }
        });
        
        
        extra_col1.setCellFactory(new Callback<TableColumn<Drive, Integer>, TableCell<Drive, Integer>>() {

            @Override
            public TableCell<Drive, Integer> call(final TableColumn<Drive, Integer> param) {
                return new TableCell<Drive, Integer>() {

                    @Override
                    protected void updateItem(final Integer item, final boolean empty) {
                        super.updateItem(item, empty);

                        if (!empty) {
                            
                            //setText(String.valueOf(item));

                            final int index = getTableRow().getIndex();

                            if ((index < ddpj.getData().size()) && (index >= 0)) {
                                
                                //setText(Integer.toString(item));
                                
                                if (item == 0) {

                                    setGraphic(rectangle.getRectangle(0));

                                } else {

                                    setGraphic(rectangle.getRectangle(1));
                                    
                                
                                } 
                                
                                //driveTable.refresh(); 
                            }
                        }
                    }
                };
            }
        });

        min_Pos_ds_col.setCellFactory(new Callback<TableColumn<Drive, Integer>, TableCell<Drive, Integer>>() {

            @Override
            public TableCell<Drive, Integer> call(final TableColumn<Drive, Integer> param) {
                return new TableCell<Drive, Integer>() {

                    @Override
                    protected void updateItem(final Integer item, final boolean empty) {
                        super.updateItem(item, empty);

                        // if (!empty) {

                        // driveTable.refresh(); // very important!!!!!

                        final int index = getTableRow().getIndex();

                        if ((index < ddpj.getData().size()) && (index >= 0)) {

                            // System.out.println(index);
                            // System.out.println(ddpj.getData().size());

                            if (ddpj.getData().get(index).getDeviceType().equals("DS")) {

                                setText(Double.toString((double) item / (double) 10));

                            } else {

                                setText(null);
                            }
                        }
                    }
                };
            }
        });

        max_Pos_ds_col.setCellFactory(new Callback<TableColumn<Drive, Integer>, TableCell<Drive, Integer>>() {

            @Override
            public TableCell<Drive, Integer> call(final TableColumn<Drive, Integer> param) {
                return new TableCell<Drive, Integer>() {

                    @Override
                    protected void updateItem(final Integer item, final boolean empty) {
                        super.updateItem(item, empty);

                        // if (!empty) {

                        // driveTable.refresh(); // very important!!!!!

                        final int index = getTableRow().getIndex();

                        if ((getTableRow().getIndex() < ddpj.getData().size()) && (index >= 0)) {

                            if (ddpj.getData().get(index).getDeviceType().equals("DS")) {

                                setText(Double.toString((double) item / (double) 10));

                            } else {

                                setText(null);
                            }
                        }
                    }
                };
            }
        });

        abs_Posi_ds_col.setCellFactory(new Callback<TableColumn<Drive, Integer>, TableCell<Drive, Integer>>() {

            @Override
            public TableCell<Drive, Integer> call(final TableColumn<Drive, Integer> param) {
                return new TableCell<Drive, Integer>() {

                    @Override
                    protected void updateItem(final Integer item, final boolean empty) {
                        super.updateItem(item, empty);

                        // if (!empty) {

                        // driveTable.refresh(); // very important!!!!!

                        final int index = getTableRow().getIndex();

                        if ((getTableRow().getIndex() < ddpj.getData().size()) && (index >= 0)) {

                            if (ddpj.getData().get(index).getDeviceType().equals("DS")) {

                                setText(Double.toString((double) item / (double) 10));

                            } else {

                                setText(null);
                            }
                        }
                    }
                };
            }
        });

        abs_Posi_pla_col.setCellFactory(new Callback<TableColumn<Drive, Integer>, TableCell<Drive, Integer>>() {

            @Override
            public TableCell<Drive, Integer> call(final TableColumn<Drive, Integer> param) {
                return new TableCell<Drive, Integer>() {

                    @Override
                    protected void updateItem(final Integer item, final boolean empty) {
                        super.updateItem(item, empty);

                        // if (!empty) {

                        // driveTable.refresh(); // very important!!!!!

                        final int index = getTableRow().getIndex();

                        if ((getTableRow().getIndex() < ddpj.getData().size()) && (index >= 0)) {

                            if (ddpj.getData().get(index).getDeviceType().equals("PLA")) {

                                setText(Integer.toString(item));

                                // Green OUT, RED in
                                if (item == 0) {

                                    setTextFill(Color.GREEN);  updateDrives_cb.setSelected(false);
                                }

                                if (item == 1) {

                                    setTextFill(Color.RED);
                                }

                                if (item == 2) {

                                    setTextFill(Color.GRAY);
                                }

                            } else {

                                setText(null);
                            }
                        }
                    }
                };
            }
        });

        deviceName_col.setCellFactory(new Callback<TableColumn<Drive, String>, TableCell<Drive, String>>() {

            @Override
            public TableCell<Drive, String> call(final TableColumn<Drive, String> param) {

                return new TableCell<Drive, String>() {

                    @Override
                    protected void updateItem(final String item, final boolean empty) {

                        if (!empty) {

                            if (ddpj.getData().size() != 0) {

                                super.updateItem(item, empty);

                                final int index = getTableRow().getIndex();

                                setText(item);
                                setTextFill(devicesNamesColor[index]);
                            }
                        }
                    }
                };
            }
        });
    }
    
    private void performeTaskDataTable(final BeamProductionChain chain) {

        // Task is usable only one at time!
        // For multiple use "Service"Task-Concurrency should be used!
        //progressBar_pb.setProgress(0);
        progressBar_pb.progressProperty().bind(ddpFESA.getCounterDoubleProperty());
        status_lb.setText("");
        status_lb.textProperty().bind(ddpFESA.getDeviceNameProperty());

        final Task<Void> task = new Task<Void>() {

            @Override
            public Void call() throws InterruptedException {

                //updateMessage("Please wait while loading data...");

                ron.setBeamChain(chain);

                ddpFESA.setListOfSubscriptions(duFESA.get_subscriptionHandle());
                ddpFESA.setBeamChain(chain);
                ddpFESA.setDevices();
                
                Thread.sleep(1000);
                ddpFESA.setCounterDoubleProperty(0);
                progressBar_pb.progressProperty().bind(ddpFESA.getCounterDoubleProperty());
                ddpFESA.generateDeviceSubscriptions();
                
                ddpj.setListOfSortedDevices(ddpFESA.getSortedDevices());
                ddpj.setDataFESA(ddpFESA.getDriveData());
                ddpj.joinData();

                // driveTable.refresh();

                return null;
            }
        };

        //status_lb.textProperty().bind(task.messageProperty());

        // java 8 construct, replace with java 7 code if using java 7.
        task.setOnSucceeded(e -> {

            status_lb.textProperty().unbind();
            // this message will be seen.

            if (ddpj.getData().size() != 0) {
                

                status_lb.setText("Data successfully loaded. Elements [" + ddpj.getData().size() + "]");
                isTableFilledWithData.set(1);

                driveTable.setItems(ddpj.getData());
                driveTable.refresh();
                createDeviceNamesTextColor();

                // Enable ComboBoxes patterns and chains
                patterns_cb.setDisable(false);
                chains_cb.setDisable(false);

                updateDrives_cb.setSelected(false);
                updateDrives_cb.setDisable(false);

                // Start all device subscriptions
                //ddpFESA.generateDeviceSubscriptions();

                // filling the list containing all device names
                devicesNames.clear();
                for (int n = 0; n < ddpj.getData().size(); n++) {

                    devicesNames.add(ddpj.getData().get(n).getDeviceName());

                    System.out.println(ddpj.getData().get(n).getDeviceName());
                }
                
                timer_connectionStatus = new Timer();
                timer_connectionStatus.scheduleAtFixedRate(new TimerTask() {

                    @Override
                    public void run() {
                        
                        checkDeviceConnectionStatus();
                    }
                    
                }, 0, 20000);

                timer_connectionStatus_started = true;
                
              

            } else {
                

                status_lb.setText("Can not retrieve data from the selected Pattern/Chain..");

                isTableFilledWithData.set(0);

                // Enable ComboBoxes patterns and chains
                patterns_cb.setDisable(false);
                chains_cb.setDisable(false);
            }

            // Start timer for drives update
            if (!timer_updateDrives_started) {

                timer_updateDrives = new Timer();
                timer_updateDrives.scheduleAtFixedRate(new TimerTask() {

                    @Override
                    public void run() {

                        if (accessForUpdateDrivesGranted) {

                            // System.out.println("ACCESS GRANTED");

                            try {

                                System.out.println("runSequence started...");
                                System.out.println(devicesNames.size() + " " + selectedPattern.getName());

                                /*
                                 * for (int i = 0; i < devicesNames.size(); i++) {
                                 *
                                 * System.out.println(devicesNames.get(i));
                                 * }
                                 */

                                SetAllFRSDrives.runSequence(devicesNames, selectedPattern.getName());


                            } catch (final RemoteException rem_exep) {

                                rem_exep.printStackTrace();
                                // System.exit(0);
                            }
                        }
                    }
                }, 0, 60000);
            }

            // Make the related buttons operative
            isAppRunning.set(1);
        });

        task.setOnFailed(e -> {
            

            status_lb.textProperty().unbind();
            status_lb.setText("Can not retrieve data from the selected Pattern/Chain.");
            isTableFilledWithData.set(0);

            // Unlock patterns and chains combos
            patterns_cb.setDisable(false);
            chains_cb.setDisable(false);
        });

        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        
        
        
        
    }

   
    private boolean listOfPatternsUpdater() {

        patternsAndChainsMap = retrievePatternsAndChains.getPatternsAndChains();
        System.out.println(patternsAndChainsMap);

        final List<Pattern> temp_listOfPatterns_cb = new ArrayList<>();
        temp_listOfPatterns_cb.addAll(patterns_cb.getItems());
        final List<Pattern> temp_listOfPatterns_map = new ArrayList<>();
        temp_listOfPatterns_map.addAll(patternsAndChainsMap.keySet());

        Collections.sort(temp_listOfPatterns_cb);
        Collections.sort(temp_listOfPatterns_map);

        return temp_listOfPatterns_cb.equals(temp_listOfPatterns_map);
    }
    
    private void checkDeviceConnectionStatus() {
        
        System.out.println("Checking connection status...");
        //System.out.println(connectionStatus_list.size());
        //System.out.println(connectionStatus_list.get(0).getDeviceName() + " " + 
        //connectionStatus_list.get(0).getParameterName() + " " + connectionStatus_list.get(0).getStatus());
        
       for(int k = 0; k < ddpj.getData().size(); k++) {

            for (int i = 0; i < duFESA.get_subscriptionHandle().size(); i++) {

                if (ddpj.getData().get(k).getDeviceName().equals(duFESA.get_subscriptionHandle().get(i).getParameter().getDeviceName())) {      
                    
                    temp_data[0] = duFESA.get_subscriptionHandle().get(i).getParameter().getDeviceName();
                    isMonitoring = duFESA.get_subscriptionHandle().get(i).isMonitoring();

                    //System.out.println(isMonitoring);
                    
                    temp_data[1] = duFESA.get_subscriptionHandle().get(i).getParameter().getPropertyName();
                    
                    if (isMonitoring) {

                        temp_data[2] = 0;
                        
                        ddpj.getData().get(k).setDeviceStatus(0);
                        
                        //ddpj.getData().get(k).setDeviceStatus(Integer.parseInt(temp_data[2].toString()));
                  
                    } else {

                        temp_data[2] = 1;
                        
                        ddpj.getData().get(k).setDeviceStatus(1);
                        //ddpj.getData().get(k).setDeviceStatus(Integer.parseInt(temp_data[2].toString()));
                        
                        //try to reconnect the device
                        System.out.println(ddpj.getData().get(k).getDeviceName());
                        
                        try {
                            
                            System.out.println(duFESA.get_subscriptionHandle().get(i).getParameter());
                            duFESA.get_subscriptionHandle().get(i).startMonitoring();
                        } catch (ParameterException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
       
                        //break;
                    }            
                    
                   
                    //connectionStatus_list.add(new DeviceConnectionStatus(temp_data[0].toString(), 
                          //  temp_data[1].toString(), Integer.parseInt(temp_data[2].toString())));
                    //connectionStatus_list.add(new Object[] {temp_data[0], temp_data[1]});  
                    
                    break;
                }
            }
        }
    }
}