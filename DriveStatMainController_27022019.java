package de.gsi.csco.ap.app_drivestat.main;

import java.rmi.RemoteException;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.Timer;
import java.util.TimerTask;

import cern.lsa.domain.settings.BeamProductionChain;
import cern.lsa.domain.settings.Pattern;
import de.gsi.csco.ap.app_drivestat.model.domain.Drive;
import de.gsi.csco.ap.app_drivestat.utils.AboutButton;
import de.gsi.csco.ap.app_drivestat.utils.AutoSavingDialog;
import de.gsi.csco.ap.app_drivestat.utils.DeviceUpdateFESA;
import de.gsi.csco.ap.app_drivestat.utils.DeviceUserNameGenerator;
import de.gsi.csco.ap.app_drivestat.utils.DevicesStatus;
import de.gsi.csco.ap.app_drivestat.utils.DriveDataJoiner;
import de.gsi.csco.ap.app_drivestat.utils.DriveDataProcessorFESA;
import de.gsi.csco.ap.app_drivestat.utils.ManualSavingDialog;
import de.gsi.csco.ap.app_drivestat.utils.PrinterDialog;
import de.gsi.csco.ap.app_drivestat.utils.PwdDialog;
import de.gsi.csco.ap.app_drivestat.utils.RetrieveOpticsName;
import de.gsi.csco.ap.app_drivestat.utils.RetrievePatternsAndChains;
import de.gsi.csco.ap.app_drivestat.utils.SaveData;
import de.gsi.sequencer.sequences.superfrs.SetAllFRSDrives;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

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
	private Button settings_bt;
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
	private Circle dataSavingLed;

	private Pattern selectedPattern;
	private List<BeamProductionChain> chains;

	private final AboutButton ab = AboutButton.getInstance();
	private final AutoSavingDialog asd = AutoSavingDialog.getInstance();
	private final DeviceUserNameGenerator dung = DeviceUserNameGenerator.getInstance();
	private final DeviceUpdateFESA duFESA = DeviceUpdateFESA.getInstance();
	private final DevicesStatus ds = DevicesStatus.getInstance();
	private final DriveDataProcessorFESA ddpFESA = DriveDataProcessorFESA.getInstance();
	private final ManualSavingDialog msd = ManualSavingDialog.getInstance();
	private final RetrieveOpticsName ron = RetrieveOpticsName.getInstance();
	private final DriveDataJoiner ddpj = DriveDataJoiner.getInstance();
	private final PrinterDialog pd = PrinterDialog.getInstance();
	private final PwdDialog pwdd = PwdDialog.getInstance();
	private final RetrievePatternsAndChains retrievePatternsAndChains = RetrievePatternsAndChains.getInstance();
	private final SaveData sd = SaveData.getInstance();

	private Map<Pattern, List<BeamProductionChain>> patternsAndChainsMap = new HashMap<>();
	private static int groupIndex = 0;

	private Timer timer_dataRecording; // timer for data recorded on disk
	private Timer timer_updateDrives;
	private Boolean timer_dataRecording_started = false;
	private final Boolean timer_updateDrives_started = false;
	// private final Boolean aredevicesNamesPainted = false;
	// private final Boolean destination_directory_isSelected = false;

	// private String selected_directory;

	private boolean accessForUpdateDrivesGranted = false;
	private boolean listOfPatternshasChanged;
	private int counter;

	// private Stage dialogSetDirectoryStage;
	// private Stage pwdDialogStage;

	private RadialGradient gradient1;
	private RadialGradient gradient2;

	// private final IntegerProperty isDestinationDirectorySelected = new
	// SimpleIntegerProperty(0);
	private final IntegerProperty isTableFilledWithData = new SimpleIntegerProperty(0);
	private final IntegerProperty isAppRunning = new SimpleIntegerProperty(0);

	final Map<String, String> userFriendlyDevicesNamesMap = new HashMap<>();

	private final Color[] devicesNamesColor = new Color[200]; // temporary
																// solution with
																// 200 elements

	@FXML
	public void initialize() {

		gradient1 = new RadialGradient(0, 0, dataSavingLed.getCenterX(), dataSavingLed.getCenterY(),
				dataSavingLed.getRadius(), false, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#ffebe6", 1.0)),
				new Stop(1, Color.LIGHTGRAY));
		gradient2 = new RadialGradient(0, 0, dataSavingLed.getCenterX(), dataSavingLed.getCenterY(),
				dataSavingLed.getRadius(), false, CycleMethod.NO_CYCLE, new Stop(0, Color.RED),
				new Stop(1, Color.LIGHTGRAY));

		dataSavingLed.setFill(gradient1);

		save_bt.disableProperty().bind(Bindings.when(asd.getDestinationDirectoryIsSelectedProperty().isNotEqualTo(1)
				.or(isTableFilledWithData.isNotEqualTo(1))).then(true).otherwise(false));

		settings_bt.disableProperty()
				.bind(Bindings.when(isTableFilledWithData.isNotEqualTo(1).or(isAppRunning.isNotEqualTo(1))).then(true)
						.otherwise(false));

		status_bt.disableProperty().bind(isTableFilledWithData.isNotEqualTo(1));

		print_bt.disableProperty().bind(isTableFilledWithData.isNotEqualTo(1));

		about_bt.disableProperty().bind(isAppRunning.isNotEqualTo(1));
		refresh_bt.disableProperty().bind(isAppRunning.isNotEqualTo(1));

		// updateDrives_cb.disableProperty()
		// .bind(Bindings.when(isAppRunning.isNotEqualTo(1).or(isTableFilledWithData.isNotEqualTo(1))).then(true)
		// .otherwise(false));
		initializeUpdateDrivesAction();

		status_lb.setText("");

		driveTable.setSelectionModel(null); // it removes the row selection
		/*-	it changes the focus color to not visible -*/
		driveTable.setStyle("-fx-focus-color: LightGray;" + "-fx-faint-focus-color: transparent;");
		driveTable.setFocusTraversable(false); // focusing

		setPatternsAndChains();

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
		extra_col1.setResizable(false);
		extra_col1.setPrefWidth(118);
		extra_col1.setStyle("-fx-alignment: CENTER-RIGHT;");
		extra_col1.setSortable(false);

		extra_col2.setResizable(false);
		extra_col2.setPrefWidth(107);
		extra_col2.setStyle("-fx-alignment: CENTER-RIGHT;");
		extra_col2.setSortable(false);

		deviceName_col.setCellValueFactory(new PropertyValueFactory<Drive, String>("deviceName"));
		typeName_col.setCellValueFactory(new PropertyValueFactory<Drive, String>("deviceType"));
		min_Pos_ds_col.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("minPos_ds"));
		abs_Posi_ds_col.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("absPosi_ds"));
		max_Pos_ds_col.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("maxPos_ds"));
		abs_Posi_pla_col.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("absPosi_pla"));

		deviceNameDescr_col.setCellFactory(new Callback<TableColumn<Drive, String>, TableCell<Drive, String>>() {

			@Override
			public TableCell<Drive, String> call(final TableColumn<Drive, String> param) {
				return new TableCell<Drive, String>() {

					@Override
					protected void updateItem(final String item, final boolean empty) {
						super.updateItem(item, empty);

						if (!empty) {

							final int index = getTableRow().getIndex();

							if (index < ddpj.getData().size() && index >= 0) {

								// Get the device description corresponding to
								// the given device name
								final String device_description = dung
										.retrieveUserFriendlyDevicesNames(ddpj.getData().get(index).getDeviceName());

								// final String device_description =
								// retrieveUserFriendlyDevicesNames(
								// ddpj.getData().get(index).getDeviceName());

								// System.out.println(index);
								// System.out.println(ddpj.getData().size());

								setText(device_description);
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

						driveTable.refresh(); // very important!!!!!

						final int index = getTableRow().getIndex();

						if (index < ddpj.getData().size() && index >= 0) {

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

						driveTable.refresh(); // very important!!!!!

						final int index = getTableRow().getIndex();

						if (getTableRow().getIndex() < ddpj.getData().size() && index >= 0) {

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

						driveTable.refresh(); // very important!!!!!

						final int index = getTableRow().getIndex();

						if (getTableRow().getIndex() < ddpj.getData().size() && index >= 0) {

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

						driveTable.refresh(); // very important!!!!!

						final int index = getTableRow().getIndex();

						if (getTableRow().getIndex() < ddpj.getData().size() && index >= 0) {

							if (ddpj.getData().get(index).getDeviceType().equals("PLA")) {

								setText(Integer.toString(item));

								// Green OUT, RED in
								if (item == 0) {
									// Update Drives
									// startDataRecording();
									setTextFill(Color.GREEN);

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

		// deviceName_col.setCellFactory(null);

		driveTable.setMaxHeight(650);
		driveTable.setFixedCellSize(25);

		// Creating the mouse event handler
		final EventHandler<ActionEvent> eventHandler_chains_cb = new EventHandler<ActionEvent>() {

			@Override
			public void handle(final ActionEvent e) {

				/*
				 *
				 * if (timer_started) {
				 *
				 * // the following operation must be done otherwise will be
				 * created multiple timers timer.cancel(); // this is vital to
				 * restart the timer (for data saving and updating LSA) //
				 * whenever user switches a patter/chain }
				 *
				 * if (timer_dataRecording_started) {
				 *
				 * timer_dataRecording.cancel(); // this is vital to restart the
				 * timer (for data saving and // updating LSA) whenever user
				 * switches a patter/chain }
				 *
				 * //closeAllSubscriptions(); //mdpj.getData().clear();
				 *
				 * // createTable();
				 */

				// restore groudIndex for group color
				// groupIndex = 0;
				createTable();
			}
		};

		// Adding event Filter
		chains_cb.setOnAction(eventHandler_chains_cb);
		patterns_cb.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Pattern>() {

			@Override
			public void changed(final ObservableValue<? extends Pattern> observable, final Pattern oldValue,
					final Pattern newValue) {

				selectedPattern = patterns_cb.getSelectionModel().getSelectedItem();

				chains = patternsAndChainsMap.get(selectedPattern);

				chains_cb.setOnAction(null);
				chains_cb.getItems().clear();
				chains_cb.getItems().addAll(chains);
				chains_cb.setOnAction(eventHandler_chains_cb);
				chains_cb.getSelectionModel().selectFirst();
			}
		});

		// restore groudIndex for group color
		// groupIndex = 0;
		createTable();
	}

	private void createDeviceNamesTextColor() {

		// Disable ComboBoxes patterns and chains
		patterns_cb.setDisable(true);
		chains_cb.setDisable(true);

		groupIndex = 0;

		driveTable.refresh();

		// Reset colors
		for (int c = 0; c < devicesNamesColor.length; c++) {

			devicesNamesColor[c] = null;
		}

		if (ddpj.getData().size() != 0) {

			for (int i = 0; i < ddpj.getData().size(); i++) {

				String acceleratorZoneItem = "";
				String acceleratorZonePreviousItem = "";

				// acceleratorZoneItem =
				// ddpj.getData().get(i).getDeviceAcceleratorZone().getName();

				acceleratorZoneItem = dung.retrieveUserFriendlyDevicesNames(ddpj.getData().get(i).getDeviceName())
						.substring(0, 3);

				if (i == 0) {

					devicesNamesColor[i] = Color.DARKBLUE;

				} else {

					acceleratorZoneItem = dung.retrieveUserFriendlyDevicesNames(ddpj.getData().get(i).getDeviceName())
							.substring(0, 3);
					// acceleratorZonePreviousItem = ddpj.getData().get(i -
					// 1).getDeviceAcceleratorZone().getName();
					acceleratorZonePreviousItem = dung
							.retrieveUserFriendlyDevicesNames(ddpj.getData().get(i - 1).getDeviceName())
							.substring(0, 3);

					if (!acceleratorZoneItem.equals(acceleratorZonePreviousItem)) {

						groupIndex++;
					}

					if (groupIndex % 2 == 0) {

						devicesNamesColor[i] = Color.DARKBLUE;
						// devicesNamesColor.add(Color.DARKBLUE);

					} else {

						devicesNamesColor[i] = Color.CORNFLOWERBLUE;
						// devicesNamesColor.add(Color.CORNFLOWERBLUE);
					}

				}
			}

			deviceName_col.setCellFactory(new Callback<TableColumn<Drive, String>, TableCell<Drive, String>>() {

				@Override
				public TableCell<Drive, String> call(final TableColumn<Drive, String> param) {

					return new TableCell<Drive, String>() {

						@Override
						protected void updateItem(final String item, final boolean empty) {

							if (!empty) {

								if (ddpj.getData().size() != 0) {

									driveTable.refresh();

									super.updateItem(item, empty);

									final int index = getTableRow().getIndex();

									// setTextFill(devicesNamesColor[index]);
									setText(item);
									setTextFill(devicesNamesColor[index]);
								}
							}
						}
					};
				}
			});
		}
	}

	private void setPatternsAndChains() {

		patternsAndChainsMap = retrievePatternsAndChains.getPatternsAndChains();

		try {

			final Iterator<Map.Entry<Pattern, List<BeamProductionChain>>> it = patternsAndChainsMap.entrySet()
					.iterator();

			while (it.hasNext()) {

				final Map.Entry<Pattern, List<BeamProductionChain>> pair = it.next();

				// System.out.println(pair//Update Drives
				// startDataRecording();.getKey() + " = " + pair.getValue());
				// it.remove(); // avoids a ConcurrentModificationException
				patterns_cb.getItems().add(pair.getKey());
			}

			patterns_cb.getSelectionModel().selectFirst();

			selectedPattern = patterns_cb.getSelectionModel().getSelectedItem();

			chains = patternsAndChainsMap.get(selectedPattern);
			// chains = selectedPattern.getChains();

			chains_cb.getItems().clear();
			chains_cb.getItems().addAll(chains);
			chains_cb.getSelectionModel().selectFirst();

		} catch (final Exception e) {

			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Application Error");
			// alert.setHeaderText("Look, an Error Dialog");
			alert.setContentText("Error while loading the Patterns and Chains. Please try later.");

			alert.showAndWait();

			// timer.cancel();
			Platform.exit();
			System.exit(0);
			// System.exit(0); // using Platform.exit() the main Stage pops up
			// for a while!
		}
	}

	private void createTable() {

		accessForUpdateDrivesGranted = false;

		updateDrives_cb.setDisable(true);

		// Disable ComboBoxes while loading data
		patterns_cb.setDisable(true);
		chains_cb.setDisable(true);

		// closeAllSubscriptions();
		ddpFESA.closeDeviceSubscriptions();

		// deviceName_col.setCellFactory(null);
		// devicesNamesColor.clear();
		isAppRunning.set(0);
		isTableFilledWithData.set(0);// Update Drives

		// aredevicesNamesPainted = false;

		// startDataRecording();

		// groupIndex = 0;

		final BeamProductionChain chain = chains_cb.getValue();

		final Task<Void> task = new Task<Void>() {

			@Override
			public Void call() throws InterruptedException {

				/*
				 * Platform.runLater(() -> {
				 *
				 * loadingDevicesDialogStage.show(); //
				 * showLoadingDevicesDialogue(); });
				 */

				updateMessage("Please wait while loading data...");

				driveTable.getItems().clear();

				ron.setBeamChain(chain);

				ddpFESA.setListOfSubscriptions(duFESA.get_subscriptionHandle());
				ddpFESA.setBeamChain(chain);
				ddpFESA.setDevices();
				ddpj.joinData();

				driveTable.refresh();

				return null;
			}
		};

		status_lb.textProperty().bind(task.messageProperty());

		// java 8 construct, replace with java 7 code if using java 7.
		task.setOnSucceeded(e -> {

			status_lb.textProperty().unbind();
			// this message will be seen.

			if (ddpj.getData().size() != 0) {

				status_lb.setText("Data successfully loaded. Elements [" + ddpj.getData().size() + "]");
				isTableFilledWithData.set(1);
				// aredevicesNamesPainted = true;

				// driveTable.setItems(ddpj.getData());

				driveTable.setItems(ddpj.getData());
				driveTable.refresh();
				createDeviceNamesTextColor();

				// Enable ComboBoxes
				// patterns_cb.setDisable(false);
				// chains_cb.setDisable(false);

				// Enable ComboBoxes patterns and chains
				patterns_cb.setDisable(false);
				chains_cb.setDisable(false);

				updateDrives_cb.setSelected(false);
				updateDrives_cb.setDisable(false);

				// Start all device subscriptions

				ddpFESA.generateDeviceSubscriptions();

			} else {

				status_lb.setText("Can not retrieve data from the selected Pattern/Chain.");

				isTableFilledWithData.set(0);

				// Enable ComboBoxes patterns and chains
				patterns_cb.setDisable(false);
				chains_cb.setDisable(false);
			}

			// Start timer for dataRecording
			if (!timer_dataRecording_started) {

				timer_dataRecording = new Timer();
				timer_dataRecording.scheduleAtFixedRate(new TimerTask() {

					@Override
					public void run() {

						startDataRecording();
					}
				}, 0, 60000);

				timer_dataRecording_started = true;
			}

			// Start timer for drives update
			if (!timer_updateDrives_started) {

				timer_updateDrives = new Timer();
				timer_updateDrives.scheduleAtFixedRate(new TimerTask() {

					@Override
					public void run() {

						if (accessForUpdateDrivesGranted) {

							final String patternName = selectedPattern.getName();
							final List<String> deviceNames = new ArrayList<>();

							System.out.println("ACCESS GRANTED");
							System.out.println("List of devices injected to runSequence");
							// System.exit(0);

							// filling the list containing all device names
							for (int n = 0; n < ddpj.getData().size(); n++) {

								deviceNames.add(ddpj.getData().get(n).getDeviceName());

								System.out.println(ddpj.getData().get(n).getDeviceName());
							}
							System.out.println(patternName);

							// System.exit(0);
							try {

								SetAllFRSDrives.runSequence(deviceNames, patternName);

							} catch (final RemoteException e) {

								// TODO Auto-generated catch block
								e.printStackTrace();
								// System.exit(0);
							}
						}
					}
				}, 0, 10000);

				timer_dataRecording_started = true;
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

	@FXML
	private void handleRefresh() {

		listOfPatternsUpdater();
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

		// maybe not necessary since tasks are Daemon
		if (timer_dataRecording_started) {

			timer_dataRecording.cancel();
		}

		if (timer_updateDrives_started) {

			timer_updateDrives.cancel();
		}

		// closeAllSubscriptions();
		ddpFESA.closeDeviceSubscriptions();

		/*
		 * System.out.println("*** DATA ***"); for (int i = 0; i <
		 * ddpj.getData().size(); i++) {
		 *
		 * System.out.println(ddpj.getData().get(i).getDeviceName() + " " +
		 * ddpj.getData().get(i).getDeviceAcceleratorZone().getParticleTransfers
		 * ());
		 *
		 * }
		 */

		Platform.exit();
		System.exit(0);
	}

	@FXML
	private void handleSettings() {

		asd.setData();
		// destination_directory_isSelected = false;
		asd.getAutoSavingDialogStage().showAndWait();
		// asd.requestFocusOnCloseButton(); // can not work because previously
		// Showand"wait" is invoked!!

		if (asd.getDestinationDirectoryIsSelectedProperty().getValue() == 1
				&& asd.getIsAutoSavingOnProperty().getValue() == 1) {

			dataSavingLed.setFill(gradient2);
			startTimerForDataRecording(asd.getSavingFileFreq());

		} else {

			dataSavingLed.setFill(gradient1);
		}
	}

	@FXML
	private void handleManualSaving() {

		if (ddpj.getData().size() != 0 && asd.getDestinationDirectoryIsSelectedProperty().getValue() == 1) {

			msd.setData(patterns_cb.getSelectionModel().getSelectedItem().getName(),
					chains_cb.getSelectionModel().getSelectedItem().getName(), ron.getOpticsName(),
					asd.getSelectedDirectory());

			msd.getManualSavingDialogStage().show();
		}
	}

	@FXML
	private void handlePrint() {

		pd.setPatternAndChain(patterns_cb.getSelectionModel().getSelectedItem().getName(),
				chains_cb.getSelectionModel().getSelectedItem().getName());
		pd.ShowPrinterDialog();
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

	private void startTimerForDataRecording(final int savingFreq) {

		if (timer_dataRecording_started) {

			timer_dataRecording.cancel();
			timer_dataRecording_started = false;
		}

		// Start new timer for dataRecording
		if (!timer_dataRecording_started) {

			timer_dataRecording = new Timer();
			timer_dataRecording.scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {

					startDataRecording();
				}
			}, 0, savingFreq);

			timer_dataRecording_started = true;
		}
	}

	private void startDataRecording() {

		if (ddpj.getData().size() != 0 && asd.getDestinationDirectoryIsSelectedProperty().getValue() == 1
				&& asd.getIsAutoSavingOnProperty().getValue() == 1) {

			dataSavingLed.setFill(gradient2);

			System.out.println("Data are being recorded on Disk!");
			sd.writeDataToDisk(patterns_cb.getSelectionModel().getSelectedItem().getName(),
					chains_cb.getSelectionModel().getSelectedItem().getName(), ron.getOpticsName(),
					asd.getSelectedDirectory(), "file");

		} else {

			dataSavingLed.setFill(gradient1);
			System.out.println("No written data on Disk!");

		}
	}

	private boolean listOfPatternsUpdater() {

		listOfPatternshasChanged = false;

		patternsAndChainsMap = retrievePatternsAndChains.getPatternsAndChains();

		// Check if patterns in combobox are not anymore existing
		for (int i = 0; i < patterns_cb.getItems().size(); i++) {

			counter = 0; // for patternKey

			for (final Pattern patternKey : patternsAndChainsMap.keySet()) {

				if (patternKey.equals(patterns_cb.getItems().get(i))) {

					break;
				}

				if (counter == patternsAndChainsMap.size() - 1) {
					// if element is not found then
					listOfPatternshasChanged = true;
					// break;
				}

				counter++;
			}

		}

		// Check if new patterns are present
		for (final Pattern patternKey : patternsAndChainsMap.keySet()) {

			// if value (chain) is need ...
			// patternsAndChainsMap.get(patternKey).get(0);

			for (int i = 0; i < patterns_cb.getItems().size(); i++) {

				if (patternKey.equals(patterns_cb.getItems().get(i))) {

					// System.out.println(patternKey);
					// System.out.println(patterns_cb.getItems().get(i));

					break;
				}

				if (i == patterns_cb.getItems().size() - 1) {

					// System.out.println("Action performed");
					// patterns_cb.getItems().add(patternKey);
					listOfPatternshasChanged = true;
				}
			}
		}

		if (listOfPatternshasChanged) {

			System.out.println("New list of Patterns is available!!!!");

		} else {

			System.out.println("No changes have been found in the list of Patterns!!!!");
		}

		return listOfPatternshasChanged;
	}

	/*
	 * private static void closeAllSubscriptions() {
	 *
	 * final DeviceUpdateFESA duf = new DeviceUpdateFESA("");
	 *
	 * if (duf.get_subscriptionHandle().size() != 0) {
	 *
	 * for (int i = 0; i < duf.get_subscriptionHandle().size(); i++) {
	 *
	 * System.out.println(i);
	 * System.out.println(duf.get_subscriptionHandle().get(i).toString());
	 * DeviceUpdateFESA.stopSubscriptions(duf.get_subscriptionHandle().get(i));
	 *
	 * if (!duf.get_subscriptionHandle().get(i).isMonitoring()) {
	 *
	 * System.out.println("Subscription already stopped!"); } } } }
	 */
}