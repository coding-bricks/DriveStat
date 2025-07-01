package de.gsi.csco.ap.app_drivestat.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import cern.lsa.domain.settings.Pattern;

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
	private static final DeviceUpdateFESA list_of_substricptions = new DeviceUpdateFESA();

	private static String parameterName;
	private static Parameter parameter;
	private static AcquiredParameterValue ParameterValue;
	private static MapParameterValue mapParameterValue;
	private static Object[] fieldValue = new Object[propertyname.length];

	private final List<Device> mySortedDevices = new ArrayList<>();
	private final List<Object[]> driveDataList = new ArrayList<>();
	// private final List<Object[]> tempData = new ArrayList<>(1); // deviceName
	// +// fieldValues

	private final Object[] tempData = new Object[5];

	private BeamProductionChain chain;

	private Set<Device> myDevices = new HashSet<>();

	private final DeviceService deviceService = Services.getDeviceService();

	private Set<ParticleTransfer> particleTransfers;

	private int p; // the property number
	private boolean subscription_exists;
	private boolean chainData_exists;
	int counter = 0;

	private final Map<BeamProductionChain, List<Object[]>> drivesDataMap = new HashMap<>();

	public DriveDataProcessorFESA() {

	}

	public static DriveDataProcessorFESA getInstance() {

		return INSTANCE;
	}

	public void setBeamChain(final BeamProductionChain chain) {

		this.chain = chain;
	}

	public void setDevices() {

		chainData_exists = false;

		final Set<String> drive_Types = new HashSet<>();
		Collections.addAll(drive_Types, DRIVE_TYPES);

		particleTransfers = Contexts.getParticleTransfersFromBeamProcesses(chain.getDrivableBeamProcesses());

		// Filter on drives per particleTransfers and types
		final DevicesRequestBuilder devicesRequestBuilder = new DevicesRequestBuilder();
		devicesRequestBuilder.setParticleTransfers(particleTransfers);
		devicesRequestBuilder.setMetaType(DeviceMetaTypeEnum.ACTUAL);
		devicesRequestBuilder.setDeviceTypeNames(drive_Types);

		myDevices = deviceService.findDevices(devicesRequestBuilder.build());

		// Check if the chain was already processed with all related drives
		for (final BeamProductionChain key : drivesDataMap.keySet()) {

			if (key.equals(chain)) {

				System.out.println("***Data already processed***");
				System.out.println("key : " + key);
				System.out.println("value : " + drivesDataMap.get(key));

				chainData_exists = true;
				getDevicesData(drivesDataMap.get(key));

				break;

			}
		}

		if (!chainData_exists) {

			getDevicesData(null);
		}

	}

	// get data from hardware
	private void getDevicesData(final List<Object[]> data) {

		// Clear list to avoid summing up of items while changing the chain
		driveDataList.clear();
		mySortedDevices.clear();
		counter = 0;

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

				// store temporary data in the array
				tempData[0] = device.getName();
				tempData[1] = fieldValue[0];
				tempData[2] = fieldValue[1];
				tempData[3] = fieldValue[2];
				tempData[4] = fieldValue[3];

				// Once the device has been recognized and data acquired
				// the latter will be stored in the driveDataList for
				// visualization in the main table

				try {

					if (!chainData_exists) {

						driveDataList.add(new Object[] { device.getName(), fieldValue[0], fieldValue[1], fieldValue[2],
								fieldValue[3] });

						cloneDriveData(driveDataList);

					} else {

						System.out.println(tempData[0] + " " + tempData[1] + " " + tempData[2] + " " + tempData[3] + " "
								+ tempData[4]);

						data.set(counter, tempData);
						counter++;
					}

					mySortedDevices.add(device);

				} catch (final Exception e) {

					System.out.println(e);
					System.exit(0);
				}
			} // End if deviceNameExists
		} // End for Device

		if (chainData_exists) {

			System.out.println(data.size());

			for (int i = 0; i < data.size(); i++) {

				System.out.println(data.get(i)[0] + " " + data.get(i)[1] + " " + data.get(i)[2] + " " + data.get(i)[3]
						+ " " + data.get(i)[4]);
			}

		}

		// retrieveDataFromFESA();
		mySortedDevices.sort(dc); /*- Perform a sort respect device actual position */
	}

	private void cloneDriveData(final List<Object[]> drivesData) {

		// Create a copy of list of processed drives and related chain
		final List<Object[]> drivesDataCopy = new ArrayList<>(drivesData.size());
		final BeamProductionChain chainCopy = chain;

		for (final Object[] item : drivesData) {

			drivesDataCopy.add(item);
		}

		// Store into Map for future usege
		drivesDataMap.put(chainCopy, drivesDataCopy);

		/*
		 *
		 * final Iterator<Map.Entry<BeamProductionChain, List<Object[]>>> it =
		 * drivesDataMap.entrySet().iterator();
		 *
		 * while (it.hasNext()) {
		 *
		 * final Map.Entry<BeamProductionChain, List<Object[]>> pair =
		 * it.next();
		 *
		 * // System.out.println(pair//Update Drives //
		 * startDataRecording();.getKey() + " = " + pair.getValue()); //
		 * it.remove(); // avoids a ConcurrentModificationException
		 *
		 * System.out.println(pair.getKey()); if (!pair.getValue().isEmpty()) {
		 * System.out.println(pair.getValue().get(0)[0].toString()); }
		 *
		 * } /*
		 *
		 * for (final Map.Entry<BeamProductionChain, List<Object[]>> e :
		 * drivesDataMap.entrySet()) {
		 *
		 * final Object key = e.getKey();
		 *
		 * System.out.println(key);
		 *
		 * final Object value = e.getValue();
		 *
		 * System.out.println((Object[])value.); }
		 */

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

				subscription_exists = false;

				if (device.getDeviceType().getName().equals("DS")) {

					p = 0;

				} else {

					p = 3; // case "PLA" device
				}

				final String parametername = Parameters.buildParameterName(device.getName(), propertyname[p]);
				Parameter parameter = null;

				try {
					parameter = ParameterFactory.newInstance().newParameter(parametername);
				} catch (final ParameterException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				/*- Check if subscription is already initialized for a particular parameter and selector */

				for (int s = 0; s < list_of_substricptions.get_subscriptionHandle().size(); s++) {

					if (list_of_substricptions.get_subscriptionHandle().get(s).getParameter() == parameter
							&& list_of_substricptions.get_subscriptionHandle().get(s)
									.getSelector() == MULTIPLEXED_SELECTOR) {

						System.out.println("Subscription already initialized! Index: " + s);
						System.out.println(
								"Restarting monitoring..." + "Parameter " + parameter.getName() + " Index: " + s);

						subscription_exists = true;

						try {
							list_of_substricptions.get_subscriptionHandle().get(s).startMonitoring();
						} catch (final ParameterException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						break;
					}
				}

				if (!subscription_exists) {

					System.out.println("A new instance has been created for " + device.getName());

					final DeviceUpdateFESA duf = new DeviceUpdateFESA(device.getName());

					try {

						duf.subscribe(MULTIPLEXED_SELECTOR, propertyname[p]);

					} catch (final Exception e) {

						System.out.println("Exception: " + e);
						System.exit(0);
					}
				}
			} // end if filter on device user-friendly names
		}
	}

	public void closeDeviceSubscriptions() {

		for (int s = 0; s < list_of_substricptions.get_subscriptionHandle().size(); s++) {

			list_of_substricptions.get_subscriptionHandle().get(s).stopMonitoring();

			System.out.println("Subscription stopped. Index " + s);
		}
	}
}