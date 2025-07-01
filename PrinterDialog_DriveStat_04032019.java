package de.gsi.csco.ap.app_drivestat.utils;

import java.util.ArrayList;
import java.util.List;

import de.gsi.csco.ap.app_drivestat.model.domain.Drive;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * @author fschirru
 */
public class PrinterDialog {

	private final Button print_bt = new Button("Print");
	private final Button print_exit_bt = new Button("Close");
	private final ButtonBar button_bar = new ButtonBar();
	private final Callback<TableColumn<Drive, Integer>, TableCell<Drive, Integer>> callBackColumnFunctionDS;
	private final Callback<TableColumn<Drive, Integer>, TableCell<Drive, Integer>> callBackColumnFunctionPLA;
	private final Callback<TableColumn<Drive, String>, TableCell<Drive, String>> callBackColumnFunctionDeviceDescription;
	private final ComboBox<Printer> availablePrinters_cb = new ComboBox<>();
	private final GridPane gridpane = new GridPane();
	private final Group root = new Group();
	private final Label path_lb = new Label("Printer: ");
	private final Scene scene = new Scene(root);
	private final Separator hSeparator = new Separator();
	private final Stage printDialStage = new Stage();
	private final Scale scale = new Scale();
	private final TableView<Drive> temporaryTable = new TableView<>();
	private final TableColumn<Drive, String> deviceNameCol = new TableColumn<>();
	private final TableColumn<Drive, String> deviceTypeCol = new TableColumn<>();
	private final TableColumn<Drive, String> deviceNameDescrCol = new TableColumn<>();
	private final TableColumn<Drive, Integer> minPosDSCol = new TableColumn<>();
	private final TableColumn<Drive, Integer> absPosiDSCol = new TableColumn<>();
	private final TableColumn<Drive, Integer> maxPosDSCol = new TableColumn<>();
	private final TableColumn<Drive, Integer> absPosiPLACol = new TableColumn<>();
	private final TableColumn<Drive, String> extra1Col = new TableColumn<>();
	private final VBox vBox = new VBox();

	private String date_time = null;
	private final Group rootTable = new Group();
	private final Scene sceneTable = new Scene(rootTable, 750, 600);
	private Stage stageTable = new Stage();
	private final VBox vBoxTable = new VBox();

	private static final PrinterDialog INSTANCE = new PrinterDialog();

	private double scaleX;
	private double scaleY;
	private String selectedPattern;
	private String selectedChain;

	private final Label printerStatus_lb = new Label("");
	// private final Label node_lb = new Label("Questa e' una prova!");
	private ObservableSet<Printer> printers;
	private PrinterJob job;
	private Printer myPrinter;

	// private Node nodeToBePrinted;

	private PageLayout pageLayout;

	// private WritableImage snapshot;

	private final DriveDataJoiner ddpj = DriveDataJoiner.getInstance();
	private final DeviceUserNameGenerator dung = DeviceUserNameGenerator.getInstance();

	private ObservableList<Drive> tempdata = FXCollections.observableArrayList();

	public PrinterDialog() {

		// Create callBack functions to display data properly
		callBackColumnFunctionDS = new Callback<TableColumn<Drive, Integer>, TableCell<Drive, Integer>>() {

			@Override
			public TableCell<Drive, Integer> call(final TableColumn<Drive, Integer> param) {
				return new TableCell<Drive, Integer>() {

					@Override
					protected void updateItem(final Integer item, final boolean empty) {
						super.updateItem(item, empty);

						if (!empty) {

							// driveTable.refresh(); // very important!!!!!
							final int index = getTableRow().getIndex();

							if (getTableRow().getIndex() < tempdata.size() && index >= 0) {

								if (tempdata.get(index).getDeviceType().equals("DS")) {

									setText(Double.toString((double) item / (double) 10));

								} else {

									setText(null);
								}
							}
						}
					}
				};
			}
		};

		callBackColumnFunctionPLA = new Callback<TableColumn<Drive, Integer>, TableCell<Drive, Integer>>() {

			@Override
			public TableCell<Drive, Integer> call(final TableColumn<Drive, Integer> param) {
				return new TableCell<Drive, Integer>() {

					@Override
					protected void updateItem(final Integer item, final boolean empty) {
						super.updateItem(item, empty);

						if (!empty) {

							// driveTable.refresh(); // very important!!!!!
							final int index = getTableRow().getIndex();

							if (getTableRow().getIndex() < tempdata.size() && index >= 0) {

								if (tempdata.get(index).getDeviceType().equals("PLA")) {

									setText(Integer.toString(item));

								} else {

									setText(null);
								}
							}
						}
					}
				};
			}
		};

		callBackColumnFunctionDeviceDescription = new Callback<TableColumn<Drive, String>, TableCell<Drive, String>>() {

			@Override
			public TableCell<Drive, String> call(final TableColumn<Drive, String> param) {
				return new TableCell<Drive, String>() {

					@Override
					protected void updateItem(final String item, final boolean empty) {
						super.updateItem(item, empty);

						if (!empty) {

							final int index = getTableRow().getIndex();

							if (index < tempdata.size() && index >= 0) {

								// Get the device description corresponding to
								// the
								// given device name
								final String device_description = dung
										.retrieveUserFriendlyDevicesNames(tempdata.get(index).getDeviceName());

								setText(device_description);
							}
						}
					}
				};
			}
		};

		setAvaiablePrinters();

		hSeparator.setStyle("-fx-border-style: solid; -fx-border-width: 0.1px;");

		printDialStage.setTitle("Print");
		printDialStage.setResizable(false);
		printDialStage.initModality(Modality.APPLICATION_MODAL);
		printDialStage.setScene(scene);

		gridpane.setHgap(10);
		gridpane.add(path_lb, 0, 1);

		availablePrinters_cb.setPrefWidth(300);
		availablePrinters_cb.getItems().addAll(printers);
		availablePrinters_cb.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Printer>() {

			@Override
			public void changed(final ObservableValue<? extends Printer> observable, final Printer oldValue,
					final Printer newValue) {

				// setSelectedPrinterJob(newValue);
				myPrinter = newValue;
			}
		});

		gridpane.add(availablePrinters_cb, 1, 1);

		vBox.setSpacing(15);
		vBox.setPadding(new Insets(10, 10, 10, 10));

		print_bt.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(final ActionEvent event) {

				myPrinter = availablePrinters_cb.getSelectionModel().getSelectedItem();
				setSelectedPrinterJob(myPrinter); // Start new printer job with
													// the selected Printer
				setPageLayout();
				createVboxSinglePage(); // For single page print
				printDialStage.close();
			}
		});

		print_exit_bt.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(final ActionEvent event) {
				printDialStage.close();
			}
		});

		button_bar.getButtons().addAll(print_bt, print_exit_bt);

		vBox.getChildren().addAll(gridpane, hSeparator, button_bar);

		root.getChildren().add(vBox);
	}

	public static PrinterDialog getInstance() {

		return INSTANCE;
	}

	public void setStage(final Stage stage) {

		stageTable = stage;
	}

	public void setPatternAndChain(final String pattern, final String chain) {

		selectedPattern = pattern;
		selectedChain = chain;
	}

	public void ShowPrinterDialog() {

		printDialStage.show();
		print_exit_bt.requestFocus();

		for (final Printer printer : printers) {

			if (printer.getName().equals("p293")) {

				availablePrinters_cb.setValue(printer); // Messhutte Printer by
														// Default
				break;
			}
		}
	}

	private void setAvaiablePrinters() {

		printers = Printer.getAllPrinters();
	}

	private void setSelectedPrinterJob(final Printer selectedPrinter) {

		job = PrinterJob.createPrinterJob(selectedPrinter);

		printerStatus_lb.setText(job.jobStatusProperty().getValue().toString());
	}

	private void resizedNode(final Node node) {

		setPageLayout();

		scaleX = pageLayout.getPrintableWidth() / node.getBoundsInParent().getWidth();
		scaleY = pageLayout.getPrintableHeight() / node.getBoundsInParent().getHeight();

		scale.setX(scaleX);
		scale.setY(scaleY);

		node.getTransforms().add(scale);
	}

	private void setPageLayout() {

		pageLayout = job.getPrinter().createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.EQUAL);
		// System.out.println("PageLayout: " + pageLayout.toString());
	}

	private void print(final Node node) {

		setPageLayout();
		// Print the node
		final boolean printed = job.printPage(pageLayout, node);

		if (printed) {

			job.endJob();
		}
	}

	private void createTemporaryTable() {

		temporaryTable.getColumns().clear();
		temporaryTable.setPrefWidth(812);
		// temporaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

		final Label deviceNameCol_lb = new Label("Device");
		deviceNameCol_lb.setPrefHeight(50);
		deviceNameCol_lb.setWrapText(true);
		deviceNameCol_lb.setTextAlignment(TextAlignment.CENTER);
		deviceNameCol.setGraphic(deviceNameCol_lb);
		// idColumn.setResizable(false);
		deviceNameCol.setMinWidth(100);
		deviceNameCol.setStyle("-fx-alignment: CENTER-LEFT");

		final Label deviceNamedescriptionCol_lb = new Label("Description");
		deviceNamedescriptionCol_lb.setPrefHeight(50);
		deviceNamedescriptionCol_lb.setWrapText(true);
		deviceNamedescriptionCol_lb.setTextAlignment(TextAlignment.CENTER);
		deviceNameDescrCol.setGraphic(deviceNamedescriptionCol_lb);
		// idColumn.setResizable(false);
		deviceNameDescrCol.setMinWidth(260);
		deviceNameDescrCol.setStyle("-fx-alignment: CENTER-LEFT");

		final Label deviceTypeCol_lb = new Label("Type");
		deviceTypeCol_lb.setPrefHeight(50);
		deviceTypeCol_lb.setWrapText(true);
		deviceTypeCol_lb.setTextAlignment(TextAlignment.CENTER);
		deviceTypeCol.setGraphic(deviceTypeCol_lb);
		// statusColumn.setResizable(false);
		deviceTypeCol.setMaxWidth(75);
		deviceTypeCol.setStyle("-fx-alignment: CENTER");

		final Label minPosDSCol_lb = new Label("Min Pos." + "\n(mm)");
		minPosDSCol_lb.setPrefHeight(50);
		minPosDSCol_lb.setWrapText(true);
		minPosDSCol_lb.setTextAlignment(TextAlignment.CENTER);
		minPosDSCol.setGraphic(minPosDSCol_lb);
		// dcmodeColumn.setResizable(false);
		minPosDSCol.setMaxWidth(75);
		minPosDSCol.setStyle("-fx-alignment: CENTER-RIGHT");

		final Label absPosiDSCol_lb = new Label("Pos." + "\n(mm)");
		absPosiDSCol_lb.setPrefHeight(50);
		absPosiDSCol_lb.setWrapText(true);
		absPosiDSCol_lb.setTextAlignment(TextAlignment.CENTER);
		absPosiDSCol.setGraphic(absPosiDSCol_lb);
		// dcmodeColumn.setResizable(false);
		absPosiDSCol.setMaxWidth(75);
		absPosiDSCol.setStyle("-fx-alignment: CENTER-RIGHT");

		final Label maxPosDSCol_lb = new Label("Max Pos." + "\n(mm)");
		maxPosDSCol_lb.setPrefHeight(50);
		maxPosDSCol_lb.setWrapText(true);
		maxPosDSCol_lb.setTextAlignment(TextAlignment.CENTER);
		maxPosDSCol.setGraphic(maxPosDSCol_lb);
		// dcmodeColumn.setResizable(false);
		maxPosDSCol.setMaxWidth(75);
		maxPosDSCol.setStyle("-fx-alignment: CENTER-RIGHT");

		final Label absPosiPLACol_lb = new Label("Pos." + "\n(IN/OUT)");
		absPosiPLACol_lb.setPrefHeight(50);
		absPosiPLACol_lb.setWrapText(true);
		absPosiPLACol_lb.setTextAlignment(TextAlignment.CENTER);
		absPosiPLACol.setGraphic(absPosiPLACol_lb);
		// dcmodeColumn.setResizable(false);
		absPosiPLACol.setMaxWidth(75);
		absPosiPLACol.setStyle("-fx-alignment: CENTER-RIGHT");

		final Label extra1Col_lb = new Label("");
		extra1Col_lb.setPrefHeight(50);
		extra1Col_lb.setWrapText(true);
		extra1Col_lb.setTextAlignment(TextAlignment.CENTER);
		extra1Col.setGraphic(extra1Col_lb);
		// dcmodeColumn.setResizable(false);
		extra1Col.setMaxWidth(75);
		extra1Col.setStyle("-fx-alignment: CENTER");

		deviceNameCol.setCellValueFactory(new PropertyValueFactory<Drive, String>("deviceName"));
		deviceTypeCol.setCellValueFactory(new PropertyValueFactory<Drive, String>("deviceType"));
		minPosDSCol.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("minPos_ds"));
		absPosiDSCol.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("absPosi_ds"));
		maxPosDSCol.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("maxPos_ds"));
		absPosiPLACol.setCellValueFactory(new PropertyValueFactory<Drive, Integer>("absPosi_pla"));

		deviceNameDescrCol.setCellFactory(callBackColumnFunctionDeviceDescription);
		minPosDSCol.setCellFactory(callBackColumnFunctionDS);
		maxPosDSCol.setCellFactory(callBackColumnFunctionDS);
		absPosiDSCol.setCellFactory(callBackColumnFunctionDS);
		absPosiPLACol.setCellFactory(callBackColumnFunctionPLA);

		temporaryTable.getColumns().add(deviceNameCol);
		temporaryTable.getColumns().add(deviceNameDescrCol);
		temporaryTable.getColumns().add(deviceTypeCol);
		temporaryTable.getColumns().add(minPosDSCol);
		temporaryTable.getColumns().add(absPosiDSCol);
		temporaryTable.getColumns().add(maxPosDSCol);
		temporaryTable.getColumns().add(absPosiPLACol);
		temporaryTable.getColumns().add(extra1Col);

		temporaryTable.setSelectionModel(null);
	}

	private void createVboxSinglePage() {

		vBoxTable.setSpacing(20);

		createTemporaryTable();

		temporaryTable.getItems().clear(); // it points to the data!!! creates
											// proble

		// setSelectedPrinterJob(myPrinter); // Start new printer job with the
		// selected Printer

		vBoxTable.getChildren().clear();
		rootTable.getChildren().clear();
		// filteredDriveData.clear();
		// temporaryTable.getItems().clear();

		final Label tableHead_lb = new Label();
		// tableHead_lb.setMinWidth(300);
		// tableHead_lb.setText("Date: " + date_time.get(0) + ", Time: " +
		// date_time.get(1));
		tableHead_lb.setText("Date: " + date_time);

		final Label tablePage_lb = new Label();
		// tableHead_lb.setStyle("-fx-border-color:red; -fx-background-color:
		// blue;");
		// tablePage_lb.setMinWidth(300);
		tablePage_lb.setText("Page: 1/1");
		tablePage_lb.setTextAlignment(TextAlignment.RIGHT);

		final Label patternAndChain_lb = new Label();
		patternAndChain_lb.setText(selectedPattern + "/" + selectedChain);

		final GridPane grid = new GridPane();
		final ColumnConstraints column1 = new ColumnConstraints();
		column1.setPercentWidth(50);
		final ColumnConstraints column2 = new ColumnConstraints();
		column2.setPercentWidth(50);
		grid.getColumnConstraints().addAll(column1, column2); // each get 50% of
																// width
		grid.setHgap(10);
		grid.setVgap(10);

		grid.prefWidthProperty().bind(vBox.widthProperty());

		GridPane.setHalignment(tableHead_lb, HPos.LEFT);
		grid.add(tableHead_lb, 0, 0); // add title and a string of local date
										// and time
		// grid.setHalignment(tableHead_lb, HPos.LEFT);
		grid.add(patternAndChain_lb, 0, 1, 2, 1); // Last two figures: ColSpan,
													// RowSpan

		GridPane.setHalignment(tablePage_lb, HPos.RIGHT);
		grid.add(tablePage_lb, 1, 0); // add a string of the current page which
										// is being printed out

		// temporaryTable.setMinHeight(rowsPerPage * 25);
		temporaryTable.setFixedCellSize(25);
		// temporaryTable.setMinHeight(430);
		temporaryTable.setPrefHeight(50 + 25 * generateTableRows());

		vBoxTable.getChildren().add(grid);

		temporaryTable.setItems(tempdata);
		System.out.println("AAAXXX");
		System.out.println(tempdata.size());

		vBoxTable.getChildren().add(temporaryTable);
		vBoxTable.setAlignment(Pos.CENTER);

		rootTable.getChildren().add(vBoxTable);
		rootTable.requestFocus();

		stageTable.setScene(sceneTable);
		stageTable.show();
		// stageTable.close();
		// In order to initialize the dimension of ANY node, at first the node
		// must be attached to a stage!!!
		resizedNode(vBoxTable);
		// print(rootTable);

	} // end for page

	private int generateTableRows() {

		int tableRows;

		// tableRows = (ddpj.getData().size() + 5) / 10 * 10;

		tableRows = tempdata.size() + 1;
		// tableRows = ((int) Math.ceil(a / 100)
		if (tableRows < 50) {

			tableRows = 50;
		}

		return tableRows;
	}

	public void setData(final ObservableList<Drive> data) {

		tempdata = data;

		System.out.println("Temp SIZE" + tempdata.size());
	}

	public void setDateAndTime(final String time) {

		date_time = time;
	}
}