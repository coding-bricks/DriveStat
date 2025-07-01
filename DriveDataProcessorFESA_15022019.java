package de.gsi.csco.ap.app_drivestat.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cern.accsoft.commons.domain.particletransfers.ParticleTransfer;
import cern.japc.AcquiredParameterValue;
import cern.japc.MapParameterValue;
import cern.japc.Parameter;
import cern.japc.ParameterException;
import cern.japc.Parameters;
import cern.japc.Selector;
import cern.japc.factory.ParameterFactory;
import cern.japc.factory.SelectorFactory;
import cern.lsa.client.DeviceService;
import cern.lsa.client.Services;
import cern.lsa.domain.devices.Device;
import cern.lsa.domain.devices.DeviceMetaTypeEnum;
import cern.lsa.domain.devices.factory.DevicesRequestBuilder;
import cern.lsa.domain.settings.BeamProductionChain;
import cern.lsa.domain.settings.Contexts;
import cern.lsa.domain.settings.DrivableBeamProcess;
import cern.lsa.domain.settings.type.BeamProcessTypeCategory;
import de.gsi.aco.sv.japc.FAIRSelectorBuilder;

// for library loggers
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// for application loggers
// import de.gsi.cs.co.ap.common.gui.elements.logger.AppLogger;

/**
 * @author fschirru
 */
public class DriveDataProcessorFESA {

	// You can choose a logger (needed imports are given in the import section
	// as comments):
	// for libraries:
	// private static final Logger LOGGER =
	// LoggerFactory.getLogger(DriveDataProcessorFESA.class);
	// for applications:
	// private static final AppLogger LOGGER = AppLogger.getLogger();

	/**
	 * @author fschirru
	 */
	private static final DriveDataProcessorFESA INSTANCE = new DriveDataProcessorFESA();

	private final DriveDataProcessorLSA ddpLSA = DriveDataProcessorLSA.getInstance();
	private final DeviceUserNameGenerator dung = DeviceUserNameGenerator.getInstance();

	private final static String[] propertyname = { "POSIABSI", "CONSTANT", "CONSTANT", "POSITI" };
	// private final static String[] propertyname = { "POSIABSS", "CONSTANT",
	// "CONSTANT", "POSITI" };
	private final static String[] propertyfield = { "data_pos", "minPosition", "maxPosition", "position" };
	// private final static String[] propertyfield = { "pos", "minPosition",
	// "maxPosition", "position" };

	private static final Selector MULTIPLEXED_SELECTOR = SelectorFactory.newSelector("FAIR.SELECTOR.ALL");
	
	
	// private static String devicename = "GTH4QD21";
	private static String parameterName;
	private static Parameter parameter;
	private static AcquiredParameterValue ParameterValue;
	private static MapParameterValue mapParameterValue;
	private static Object[] fieldValue = new Object[propertyname.length]; // to
																			// be
																			// changed
																			// into
																			// generic
																			// object

	private Set<Device> myDSdevices = new HashSet<>(); // The list containing
														// the DS devices
	private Set<Device> myPLAdevices = new HashSet<>(); // The list containing
														// the PLA devices

	private final List<Object[]> driveDataList = new ArrayList<>(); // to be
																	// changed
																	// into
																	// generic
																	// object

	private BeamProductionChain chain;

	private Set<Device> myDevices = new HashSet<>();

	private final DevicesRequestBuilder builder = new DevicesRequestBuilder();

	private final DeviceService deviceService = Services.getDeviceService();

	private Set<ParticleTransfer> particleTransfers;

	DriveDataProcessorFESA() {

	}

	public static DriveDataProcessorFESA getInstance() {

		return INSTANCE;
	}

	public void setBeamChain(final BeamProductionChain chain) {

		this.chain = chain;
	}

	public void setFrsDevices() {

		myDevices = ddpLSA.getStaticDevices();

		// System.out.println(myDevices.size());

		getDevicesData();
	}

	public void setDevices() {

		// Find all the devices belonging to the selected chainstatic list of
		// FRS devices
		final DevicesRequestBuilder devicesRequestBuilder_DS = new DevicesRequestBuilder();
		final DevicesRequestBuilder devicesRequestBuilder_PLA = new DevicesRequestBuilder();

		particleTransfers = Contexts.getParticleTransfersFromBeamProcesses(chain.getDrivableBeamProcesses());

		devicesRequestBuilder_DS.setParticleTransfers(particleTransfers);
		devicesRequestBuilder_DS.setMetaType(DeviceMetaTypeEnum.ACTUAL);
		devicesRequestBuilder_DS.setDeviceTypeName("DS"); // StepperMotor Drive

		devicesRequestBuilder_PLA.setParticleTransfers(particleTransfers);
		devicesRequestBuilder_PLA.setMetaType(DeviceMetaTypeEnum.ACTUAL);
		devicesRequestBuilder_PLA.setDeviceTypeName("PLA"); // Pneumatic Drives

		myDSdevices = deviceService.findDevices(devicesRequestBuilder_DS.build());
		myPLAdevices = deviceService.findDevices(devicesRequestBuilder_PLA.build());

		System.out.println("DS: " + myDSdevices.size());
		System.out.println("PLA: " + myPLAdevices.size());

		myDevices.addAll(myDSdevices);
		myDevices.addAll(myPLAdevices);

		System.out.println("*********");
		System.out.println(myDevices);

		System.out.println("*********");

		System.out.println("FESA myDevice DIM " + myDevices.size());

		getDevicesData();
	}

	public void getDevicesData() {

		// Clear list to avoid summing up of items while changing the chain
		driveDataList.clear();

		for (final Device device : myDevices) {

			if (dung.isDeviceNameExisting(device.getName())) {

				// System.out.println("Name of device: " + device.getName());

				// final Selector newMULTIPLEXED_SELECTOR = getSelector(device);

				// System.out.println(device.getName());
				// System.out.println(newMULTIPLEXED_SELECTOR);
				// System.out.println("TYPE " +
				// getDeviceType(device.getName()));

				// Data processing according to the device type
				if (getDeviceType(device.getName()).equals("DS")) {

					for (int p = 0; p < 3; p++) {

						try {

							// parameterName =
							// Parameters.buildParameterName(device.getName().substring(8),
							// propertyname[p]); // if setFRSDevice is used
							parameterName = Parameters.buildParameterName(device.getName(), propertyname[p]);

							parameter = ParameterFactory.newInstance().newParameter(parameterName);

							// ParameterValue =
							// parameter.getValue(newMULTIPLEXED_SELECTOR);
							ParameterValue = parameter.getValue(MULTIPLEXED_SELECTOR);
							mapParameterValue = (MapParameterValue) ParameterValue.getValue();

							// System.out.println(parameterName);
							// System.out.println(mapParameterValue);

							// acquire the value of the considered property of
							// the related device.
							fieldValue[p] = mapParameterValue.getString(propertyfield[p]);

						} catch (final ParameterException e) {

							System.out.println(e);
							fieldValue[p] = "0";
						}
					}
					fieldValue[3] = "0"; // by default for DS devices

				}

				if (getDeviceType(device.getName()).equals("PLA")) {

					for (int p = 3; p < 4; p++) {

						try {

							// parameterName =
							// Parameters.buildParameterName(device.getName().substring(8),
							// propertyname[p]); // if setFRSDevice is used
							parameterName = Parameters.buildParameterName(device.getName(), propertyname[p]);

							parameter = ParameterFactory.newInstance().newParameter(parameterName);
							// ParameterValue =
							// parameter.getValue(newMULTIPLEXED_SELECTOR);
							ParameterValue = parameter.getValue(MULTIPLEXED_SELECTOR);
							mapParameterValue = (MapParameterValue) ParameterValue.getValue();

							// System.out.println(parameterName);
							// System.out.println(mapParameterValue);

							// acquire the value of the considered property of
							// the related device.
							fieldValue[p] = mapParameterValue.getString(propertyfield[p]);

						} catch (final ParameterException e) {

							System.out.println(e);
							fieldValue[p] = "0";
						}
					}
					fieldValue[0] = "0"; // by default for PLA devices
					fieldValue[1] = "0"; // by default for PLA devices
					fieldValue[2] = "0"; // by default for PLA devices
				}

				/*
				 * System.out.println("DATA ACQUIRED");
				 * System.out.println(device.getName().substring(8));
				 * System.out.println(fieldValue[0]);
				 * System.out.println(fieldValue[1]);
				 * System.out.println(fieldValue[2]);
				 * System.out.println(fieldValue[3]);
				 */

				// Add processed device to the list

				// Use device.getName().substr(8) if setFrsDevice is used!!!
				try {

					driveDataList.add(new Object[] { device.getName(), fieldValue[0], fieldValue[1], fieldValue[2],
							fieldValue[3] });

				} catch (final Exception e) {

					System.out.println(e);
					System.exit(0);
				}

			} // End if deviceNameExists

		} // End for Device

		// retrieveDataFromFESA();
	}

	public List<Object[]> getDriveData() {

		return driveDataList;
	}

	// test method to check all devices processed within the getDeviceData
	// method
	public void retrieveDataFromFESA() {

		System.out.println("******FESA DATA******");
		System.out.println(driveDataList.size());

		for (int i = 0; i < driveDataList.size(); i++) {

			try {
				System.out.println(driveDataList.get(i)[0] + " - " + driveDataList.get(i)[1] + " - "
						+ driveDataList.get(i)[2] + " - " + driveDataList.get(i)[3] + " - " + driveDataList.get(i)[4]);

			} catch (final Exception e) {

				System.out.println(e);
				System.exit(0);
			}
		}

		// System.exit(0);
	}

	public void generateDeviceSubscriptions() {

		for (final Device device : myDevices) {

			if (dung.isDeviceNameExisting(device.getName())) {

				if (getDeviceType(device.getName()).equals("DS")) {

					// final DeviceUpdateFESA duf = new
					// DeviceUpdateFESA(device.getName().substring(8));
					final DeviceUpdateFESA duf = new DeviceUpdateFESA(device.getName());
					// final Selector newMULTIPLEXED_SELECTOR =
					// getSelector(device);

					for (int p = 0; p < 1; p++) { // CONSTANT property does not
													// need to be updated!
						// for (int p = 0; p < 3; p++) {

						try {

							duf.subscribe(MULTIPLEXED_SELECTOR, propertyname[p]);
							// duf.subscribe(newMULTIPLEXED_SELECTOR,
							// propertyname[p]);

						} catch (final Exception e) {

							System.out.println("Exception: " + e);
							System.exit(0);
						}
					} // for property
				}

				if (getDeviceType(device.getName()).equals("PLA")) {

					// final DeviceUpdateFESA duf = new
					// DeviceUpdateFESA(device.getName().substring(8));
					final DeviceUpdateFESA duf = new DeviceUpdateFESA(device.getName());

					// final Selector newMULTIPLEXED_SELECTOR =
					// getSelector(device);

					for (int p = 3; p < 4; p++) {

						try {

							// duf.subscribe(newMULTIPLEXED_SELECTOR,
							// propertyname[p]);
							duf.subscribe(MULTIPLEXED_SELECTOR, propertyname[p]);

						} catch (final Exception e) {

							System.out.println("Exception: " + e);
							System.exit(0);
						}
					} // for property
				}
			} // end if filter on device user friendly names
		}
	}

	private Selector getSelector(final Device device) {

		final Set<ParticleTransfer> particleTransfersForDevice = device.getAcceleratorZone().getParticleTransfers();
		if (particleTransfersForDevice.size() > 1) {
			throw new IllegalStateException("There was more then one particle transfer found for device "
					+ device.getName() + " but it should be only one.");
		}
		final List<DrivableBeamProcess> chainBeamProcesses = chain.getDrivableBeamProcesses();
		final ParticleTransfer particleTransferForDevice = particleTransfersForDevice.iterator().next();
		final List<DrivableBeamProcess> beamProcessesForDevice = Contexts.filterBeamProcesses(chainBeamProcesses,
				particleTransferForDevice, BeamProcessTypeCategory.FUNCTION_BEAM_IN);

		final Selector selector = FAIRSelectorBuilder.parse(beamProcessesForDevice.get(0).getUser()).build();

		return selector;
	}

	private String getDeviceType(final String deviceName) {

		String deviceType = "";

		for (int i = 0; i < ddpLSA.getDriveData().size(); i++) {

			// if
			// (deviceName.substring(8).equals(ddpLSA.getDriveData().get(i)[0]))
			// {
			if (deviceName.equals(ddpLSA.getDriveData().get(i)[0])) {

				deviceType = ddpLSA.getDriveData().get(i)[1].toString();
			}
		}

		return deviceType;
	}
}