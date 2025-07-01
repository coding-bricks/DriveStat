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

/**
 * @author fschirru
 */
public class DriveDataProcessorFESA {

	private static final DriveDataProcessorFESA INSTANCE = new DriveDataProcessorFESA();
	private static final DeviceUserNameGenerator dung = DeviceUserNameGenerator.getInstance();
	private static final Selector MULTIPLEXED_SELECTOR = SelectorFactory.newSelector("FAIR.SELECTOR.ALL");
	private static final String[] DRIVE_TYPES = new String[] { "DS", "PLA" };

	private static final String[] propertyname = { "POSIABSI", "CONSTANT", "CONSTANT", "POSITI" };
	// private final static String[] propertyname = { "POSIABSS", "CONSTANT",
	// "CONSTANT", "POSITI" };
	private static final String[] propertyfield = { "data_pos", "minPosition", "maxPosition", "position" };
	// private final static String[] propertyfield = { "pos", "minPosition",
	// "maxPosition", "position" };
	private static final DeviceComparator dc = new DeviceComparator();

	private static String parameterName;
	private static Parameter parameter;
	private static AcquiredParameterValue ParameterValue;
	private static MapParameterValue mapParameterValue;
	private static Object[] fieldValue = new Object[propertyname.length];

	private final List<Device> mySortedDevices = new ArrayList<>();
	private final List<Object[]> driveDataList = new ArrayList<>();

	private BeamProductionChain chain;

	private Set<Device> myDevices = new HashSet<>();

	private final DeviceService deviceService = Services.getDeviceService();

	private Set<ParticleTransfer> particleTransfers;

	private int p; // the property number

	public DriveDataProcessorFESA() {

	}

	public static DriveDataProcessorFESA getInstance() {

		return INSTANCE;
	}

	public void setBeamChain(final BeamProductionChain chain) {

		this.chain = chain;
	}

	public void setDevices() {

		final Set<String> drive_Types = new HashSet<>();
		Collections.addAll(drive_Types, DRIVE_TYPES);

		particleTransfers = Contexts.getParticleTransfersFromBeamProcesses(chain.getDrivableBeamProcesses());

		// Filter on drives per particleTransfers and types
		final DevicesRequestBuilder devicesRequestBuilder = new DevicesRequestBuilder();
		devicesRequestBuilder.setParticleTransfers(particleTransfers);
		devicesRequestBuilder.setMetaType(DeviceMetaTypeEnum.ACTUAL);
		devicesRequestBuilder.setDeviceTypeNames(drive_Types);

		myDevices = deviceService.findDevices(devicesRequestBuilder.build());

		getDevicesData();
	}

	// get data from hardware
	public void getDevicesData() {

		// Clear list to avoid summing up of items while changing the chain
		driveDataList.clear();
		mySortedDevices.clear();

		for (final Device device : myDevices) {

			if (dung.isDeviceNameExisting(device.getName())) {

				// Data processing according to the device type
				if (device.getDeviceType().getName().equals("DS")) {

					for (int p = 0; p < 3; p++) {

						try {

							parameterName = Parameters.buildParameterName(device.getName(), propertyname[p]);
							parameter = ParameterFactory.newInstance().newParameter(parameterName);
							ParameterValue = parameter.getValue(MULTIPLEXED_SELECTOR);
							mapParameterValue = (MapParameterValue) ParameterValue.getValue();

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

				if (device.getDeviceType().getName().equals("PLA")) {

					for (int p = 3; p < 4; p++) {

						try {

							parameterName = Parameters.buildParameterName(device.getName(), propertyname[p]);
							parameter = ParameterFactory.newInstance().newParameter(parameterName);
							ParameterValue = parameter.getValue(MULTIPLEXED_SELECTOR);
							mapParameterValue = (MapParameterValue) ParameterValue.getValue();

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

				// Once the device has been recognized and data acquired
				// the latter will be stored in the driveDataList for
				// visualization in the main table

				try {

					driveDataList.add(new Object[] { device.getName(), fieldValue[0], fieldValue[1], fieldValue[2],
							fieldValue[3] });
					mySortedDevices.add(device);

				} catch (final Exception e) {

					System.out.println(e);
					System.exit(0);
				}
			} // End if deviceNameExists
		} // End for Device

		// retrieveDataFromFESA();
		mySortedDevices.sort(dc); /*- Perform a sort respect device actual position */
	}

	public List<Object[]> getDriveData() {

		return driveDataList;
	}

	public List<Device> getSortedDevices() {

		return mySortedDevices;
	}

	// test method to check all devices processed within the getDeviceData
	// method
	public void retrieveDataFromFESA() {

		System.out.println("******FESA DATA******" + "Size: " + driveDataList.size());

		for (int i = 0; i < driveDataList.size(); i++) {

			try {
				System.out.println(driveDataList.get(i)[0] + " - " + driveDataList.get(i)[1] + " - "
						+ driveDataList.get(i)[2] + " - " + driveDataList.get(i)[3] + " - " + driveDataList.get(i)[4]);

			} catch (final Exception e) {

				System.out.println(e);
				System.exit(0);
			}
		}
	}

	public void generateDeviceSubscriptions() {

		/*- for "DS" devices the only property to be updated is p = 0 while for "PLA" p = 3 */

		for (final Device device : myDevices) {

			if (dung.isDeviceNameExisting(device.getName())) {

				final DeviceUpdateFESA duf = new DeviceUpdateFESA(device.getName());
				
				

				if (device.getDeviceType().getName().equals("DS")) {

					p = 0;

				} else {

					p = 3; // case "PLA" device
				}

				try {

					duf.subscribe(MULTIPLEXED_SELECTOR, propertyname[p]);

				} catch (final Exception e) {

					System.out.println("Exception: " + e);
					System.exit(0);

				}
			} // end if filter on device user-friendly names
		}
	}
}