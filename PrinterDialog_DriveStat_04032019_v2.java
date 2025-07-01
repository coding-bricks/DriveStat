package de.gsi.csco.ap.app_drivestat.utils;

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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @author fschirru
 */
public class PrinterDialog {

	private final Button print_bt = new Button("Print");
	private final Button print_exit_bt = new Button("Close");
	private final ButtonBar button_bar = new ButtonBar();
	private final ComboBox<Printer> availablePrinters_cb = new ComboBox<>();
	private final GridPane gridpane = new GridPane();
	private final Group root = new Group();
	private final Label path_lb = new Label("Printer: ");
	private final Label patternAndChain_lb = new Label();
	private final Label tableHead_lb = new Label();
	private final Label tablePage_lb = new Label();
	private final Scene scene = new Scene(root);
	private final Separator hSeparator = new Separator();
	private final Stage printDialStage = new Stage();

	private final TableNodeGenerator tng = TableNodeGenerator.getInstance();

	private final VBox vBox = new VBox();
	private final VBox vBoxTable = new VBox();

	private static final PrinterDialog INSTANCE = new PrinterDialog();

	private final Label printerStatus_lb = new Label("");
	// private final Label node_lb = new Label("Questa e' una prova!");
	private ObservableSet<Printer> printers;

	private PageLayout pageLayout;

	private final Group rootTable = new Group();
	private final VBox tableContainer_vb = new VBox();
	private final Scene sceneTable = new Scene(rootTable, 750, 600);
	private final Stage table_st = new Stage();

	private final Scale scale = new Scale();

	private double scaleX = 0;
	private double scaleY = 0;

	private int tableRows = 0;

	private String date_time = null;
	private String selectedChain = null;
	private String selectedPattern = null;

	private ObservableList<Drive> tempTableData = FXCollections.observableArrayList();

	public PrinterDialog() {

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
				// myPrinter = newValue;
			}
		});

		gridpane.add(availablePrinters_cb, 1, 1);

		vBox.setSpacing(15);
		vBox.setPadding(new Insets(10, 10, 10, 10));

		print_bt.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(final ActionEvent event) {

				final Printer myPrinter = availablePrinters_cb.getSelectionModel().getSelectedItem();
				final PrinterJob job = PrinterJob.createPrinterJob(myPrinter);

				printerStatus_lb.setText(job.jobStatusProperty().getValue().toString());

				pageLayout = job.getPrinter().createPageLayout(Paper.A4, PageOrientation.PORTRAIT,
						Printer.MarginType.EQUAL);

				// prepareNode(); funziona

				tng.setTableDataHeader(date_time, selectedPattern, selectedChain);
				tng.setData(tempTableData);
				tng.prepareNode();
				tng.resizedNode(pageLayout.getPrintableWidth(), pageLayout.getPrintableHeight());

				final boolean printed = job.printPage(pageLayout, tng.getVBoxNode());

				if (printed) {

					job.endJob();
				}

				// print prepared node
				// final boolean printed = job.printPage(pageLayout,
				// getNodeToBePrinted());

				// if (printed) {

				// job.endJob();
				// }
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

	public void ShowPrinterDialog() {

		for (final Printer printer : printers) {

			if (printer.getName().equals("p293")) {

				availablePrinters_cb.setValue(printer); // Messhutte Printer by
														// Default
				break;
			}
		}

		printDialStage.show();
		print_exit_bt.requestFocus();
	}

	private void setAvaiablePrinters() {

		printers = Printer.getAllPrinters();
	}

	public static PrinterDialog getInstance() {

		return INSTANCE;
	}

	private int generateTableRows() {

		tableRows = tempTableData.size() + 1;
		// tableRows = ((int) Math.ceil(a / 100)
		if (tableRows < 50) {

			tableRows = 50;
		}

		return tableRows;
	}

	private void resizedNode() {

		tableContainer_vb.getTransforms().clear();

		scaleX = pageLayout.getPrintableWidth() / tableContainer_vb.getBoundsInParent().getWidth();
		scaleY = pageLayout.getPrintableHeight() / tableContainer_vb.getBoundsInParent().getHeight();

		scale.setX(scaleX);
		scale.setY(scaleY);

		tableContainer_vb.getTransforms().add(scale);
	}

	public void setTableDataHeader(final String infoTableHeader, final String pattern, final String chain) {

		date_time = infoTableHeader;
		tableHead_lb.setText("Date: " + date_time);

		selectedPattern = pattern;
		selectedChain = chain;
		patternAndChain_lb.setText(selectedPattern + "/" + selectedChain);
	}

	private void prepareNode() {

		tableContainer_vb.setSpacing(20);
		tableContainer_vb.getChildren().clear();
		rootTable.getChildren().clear();

		tng.setData(tempTableData);
		tng.getTableNode().setItems(tempTableData);
		tng.getTableNode().setPrefHeight(50 + 25 * generateTableRows());

		tableHead_lb.setText("Date: " + date_time);

		tablePage_lb.setText("Page: 1/1");
		tablePage_lb.setTextAlignment(TextAlignment.RIGHT);

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
		grid.add(tableHead_lb, 0, 0); /*- add title and a string of local date and time */
		grid.add(patternAndChain_lb, 0, 1, 2, 1); /*- Last two figures: ColSpan, RowSpan */
		GridPane.setHalignment(tablePage_lb, HPos.RIGHT);
		grid.add(tablePage_lb, 1, 0); /*- add a string of the current page which is being printed out */

		tableContainer_vb.getChildren().add(grid);
		tableContainer_vb.getChildren().add(tng.getTableNode());
		tableContainer_vb.setAlignment(Pos.CENTER);

		rootTable.getChildren().add(tableContainer_vb);
		rootTable.requestFocus();

		table_st.setScene(sceneTable);
		table_st.show();
		// stageTable.close();
		// In order to initialize the dimension of ANY node, at first the node
		// must be attached to a stage!!!
		resizedNode();
		// print(rootTable);

	}

	private VBox getNodeToBePrinted() {

		return tableContainer_vb;
	}

	public void setData(final ObservableList<Drive> data) {

		tempTableData = data;
	}

}